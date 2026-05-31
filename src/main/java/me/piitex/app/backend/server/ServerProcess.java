package me.piitex.app.backend.server;


import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.overlays.MessageOverlay;
import me.piitex.os.OSUtil;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class ServerProcess {
    private Process process;
    private final Model model;

    private static ServerProcess currentServer;

    private boolean error = false;
    private volatile boolean loading = false;
    private final List<ServerLoadingListener> listeners = new CopyOnWriteArrayList<>();


    public ServerProcess(Model model) {
        this.model = model;
        if (currentServer != null) {
            App.logger.info("Shutting down previous server...");
            currentServer.stop();
        }

        App.logger.info("Verifying server PID...");

        // Checks current state of PID. If the pid is active properly shut it down.
        checkProcessID();

        currentServer = this;

        if (model == null) {
            App.logger.error("Model was undefined. Unable to start server.");
            error = true;
            return;
        }
        App.logger.info("Loading {}", model.getFile().getAbsolutePath());

        if (model.getSettings().isChange() || model.getSettings().getTotalLayers() == 0) {
            App.logger.info("Gathering model data...");
            new ModelTestProcess(model);
        }

        loading = true;

        // Fetch server/model settings.
        ServerSettings settings = App.getInstance().getSettings();

        File server;
        File backendDirectory = new File(App.getBackendDirectory(), settings.getBackend().toLowerCase() + "/");
        
        if (OSUtil.getOS().contains("Windows")) {
            server = new File(backendDirectory, "llama-server.exe");
        } else {
            server = new File(backendDirectory, "llama-server");
            server.setExecutable(true, true);
        }
        List<String> parameters = getParameters(server, settings);
        App.logger.debug("Server Parameters: {}", parameters);

        // Build the process
        ProcessBuilder builder = new ProcessBuilder(parameters);

        // Set env if needed
        Map<String, String> environmentVariables = builder.environment();
        environmentVariables.put("GGML_VK_DISABLE_HOST_VISIBLE_VIDMEM", "1"); // Should fix BSOD with vulkan

        // The server output will be errors even though it's not errors. This is how Java works
        builder.redirectError(new File(App.getAppDirectory(), "server.txt"));

        process = null;
        try {
            // When the server starts it will not be automatically shutdown.
            // The process will remain open until this application is properly closed.
            process = builder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (process != null) {
                // Save PID to terminate the process later.
                ProcessHandle handle = process.toHandle();
                App.getInstance().getSettings().getInfoFile().set("pid", handle.pid());
            }
        }

        // Creates a thread-blocking scanner to ensure the server has started properly.
        // It also checks for errors and logs them.
        waitForServer();

        if (!error) {
            App.logger.info("Started llama-server successfully.");
        }
    }

    private void checkProcessID() {
        if (App.getInstance().getSettings().getInfoFile().hasKey("pid")) {
            long pid = App.getInstance().getSettings().getInfoFile().getLong("pid");
            App.logger.info("Previous PID: {}", pid);
            // Destroy old process...
            Optional<ProcessHandle> processHandleOptional = ProcessHandle.of(pid);
            if (processHandleOptional.isPresent()) {
                ProcessHandle processHandle = processHandleOptional.get();
                if (processHandle.isAlive()) {
                    App.logger.info("Destroying PID: {}...", pid);
                    boolean destroy = processHandle.destroy();
                    if (destroy) {
                        try {
                            processHandle.onExit().get();
                            App.logger.info("Destroying {} gracefully...", pid);
                        } catch (Exception e) {
                            if (processHandle.isAlive()) {
                                App.logger.info("Process is still alive. Destroying {} forcefully.", pid);
                                processHandle.destroyForcibly();
                            }
                        }
                    } else {
                        if (processHandle.isAlive()) {
                            App.logger.info("Could not destroy. Destroying {} forcefully.", pid);
                            processHandle.destroyForcibly();
                        }
                    }
                }
            } else {
                App.logger.info("{} is already destroyed.", pid);
            }
        }
    }

    private LinkedList<String> getParameters(File server, ServerSettings settings) {
        LinkedList<String> parameters = new LinkedList<>();
        parameters.add(server.getAbsolutePath());

        // Model file
        parameters.add("-m");
        parameters.add(model.getFile().getAbsolutePath());
        if (!settings.getDevice().equalsIgnoreCase("auto")) {
            App.logger.debug("Setting device...");
            parameters.add("--device");
            parameters.add(settings.getFormattedDevice().trim());
        }

        // Vision model file
        if (!model.getSettings().getMmProj().equalsIgnoreCase("")) {
            if (model.getSettings().getMmProj().startsWith("None")) {
                App.logger.warn("MMProj not specified.");
                parameters.add("--no-mmproj");
            } else {

                String dir = model.getSettings().getMmProj().split("/")[0];
                String file = model.getSettings().getMmProj().split("/")[1];
                Model mmproj = App.getModelByName(dir, file);
                if (mmproj == null) {
                    App.logger.error("Could not load mmproj. (Invalid file)");
                } else {
                    App.logger.debug("MMPROJ: {}", mmproj.getFile().getAbsolutePath());
                    parameters.add("--mmproj");
                    parameters.add(mmproj.getFile().getAbsolutePath());
                }
            }
        }

        // GPU layers have been refactored to GPU usage.
        // The usage is a percentage of the total layers (model.getGpuLayers());
        double TOTAL_AVAILABLE_VRAM_MIB = App.getInstance().getAppSettings().getTotalGpuVram();

        if (TOTAL_AVAILABLE_VRAM_MIB <= 0) {
            App.logger.warn("VRAM is set to 0. Attempting to fetch VRAM...");
            // Fallback to Oshi
            SystemInfo systemInfo = new SystemInfo();
            HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
            GraphicsCard graphicsCard = hardwareAbstractionLayer.getGraphicsCards().getFirst();
            if (graphicsCard != null && graphicsCard.getVRam() > 0) {
                App.logger.info("Using GPU memory...");
                TOTAL_AVAILABLE_VRAM_MIB = graphicsCard.getVRam() / (1024.0 * 1024.0);
            } else {
                App.logger.error("Could not find dedicated GPU. Using global memory pool...");
                TOTAL_AVAILABLE_VRAM_MIB = systemInfo.getHardware().getMemory().getTotal() / (1024.0 * 1024.0);
                App.logger.info("VRAM: {} MiB", TOTAL_AVAILABLE_VRAM_MIB);
            }

            App.getInstance().getAppSettings().setTotalGpuVram(TOTAL_AVAILABLE_VRAM_MIB);
        }

        double KV_CACHE = model.getSettings().getKvCacheSize();
        double COMPUTED_BUFFER_SIZE = model.getSettings().getComputeBufferSize();
        double FIXED_OVERHEAD_MIB = KV_CACHE + COMPUTED_BUFFER_SIZE;

        if (FIXED_OVERHEAD_MIB <= 0.0) {
            // Fallback check
            FIXED_OVERHEAD_MIB = 4246.00;
        }

        final double VRAM_PER_LAYER_MIB = model.getSettings().getDataPerLayer();
        double VRAM_ALLOCATION = settings.getGpuUsage();

        // Calculate the total vram allowed by the percentage.
        double VRAM_ALLOC_BUDGET = TOTAL_AVAILABLE_VRAM_MIB * (VRAM_ALLOCATION / 100.0);

        // Calculate the max by subtracting the required FIXED OVERHEAD.
        double LAYER_ALLOC_BUDGET = VRAM_ALLOC_BUDGET - FIXED_OVERHEAD_MIB;

        // Ensure nothing is negative.
        if (LAYER_ALLOC_BUDGET < 0) {
            App.logger.warn("Total VRAM Budget ({}) is less than Fixed Overhead ({}). Setting Layer Budget to 0.", VRAM_ALLOC_BUDGET, FIXED_OVERHEAD_MIB);
            LAYER_ALLOC_BUDGET = 0;
        }

        int TOTAL_MODEL_LAYERS = model.getSettings().getTotalLayers();

        int layers;
        // Calculate the number of layers that fit into the budget
        if (VRAM_PER_LAYER_MIB > 0.0) {
            layers = (int) Math.floor(LAYER_ALLOC_BUDGET / VRAM_PER_LAYER_MIB);
        } else {
            App.logger.warn("VRAM per layer (dataPerLayer) is 0.0, defaulting layers to 0.");
            layers = 0;
        }

        // Cap the offloaded layers
        layers = Math.min(layers, TOTAL_MODEL_LAYERS);

        parameters.add("-ngl");
        App.logger.info("Using VRAM utilization of '{}%' (Total VRAM Budget: {} MiB, Layers VRAM Usage: {} MiB) to load '{}'/'{}' layers)",
                String.format("%d", (long) VRAM_ALLOCATION),
                (int) VRAM_ALLOC_BUDGET,
                (int) (layers * VRAM_PER_LAYER_MIB),
                layers,
                TOTAL_MODEL_LAYERS);

        String num = Integer.toString(layers);
        parameters.add(num);

        // Memory swapping
        if (settings.isMemoryLock()) {
            parameters.add("--mlock");
        }
        // Flash attention
        if (settings.isFlashAttention()) {
            parameters.add("-fa");
            parameters.add("auto");
        }

        // Reasoning Template
        if (!model.getSettings().getReasoningTemplate().equalsIgnoreCase("disabled") && !settings.getReasoningTemplate().equalsIgnoreCase("none")) {
            App.logger.debug("Enabling response format...");
            parameters.add("--reasoning-format");
            parameters.add(model.getSettings().getReasoningTemplate());
        }

        if (model.getSettings().isForceDisableReasoning()) {
            App.logger.info("Force disable reasoning....");
            parameters.add("--no-prefill-assistant");
            parameters.add("-rea");
            parameters.add("off");
        } else {
            if (model.getSettings().isReasoning()) {
                App.logger.info("Enabling thinking mode....");
                parameters.add("-rea");
                parameters.add("on");
            }
        }

        if (!model.getSettings().getChatTemplate().equalsIgnoreCase("default")) {
            App.logger.debug("Setting chat template...");
            parameters.add("--chat-template");
            parameters.add(model.getSettings().getChatTemplate());
        }

        // Jinja Chat Template
        if (model.getSettings().isJinja()) {
            App.logger.debug("Using jinja...");
            parameters.add("--jinja");
        }

        parameters.add("-c");
        parameters.add(model.getSettings().getContextSize() + "");

        if (model.getSettings().isContextShift()) {
            parameters.add("--context-shift");
        }

        // Server port and WebUI
        parameters.add("--port");
        parameters.add("8187");
        parameters.add("--no-webui");

        if (settings.isHost()) {
            App.logger.info("Server is listening on 0.0.0.0");
            parameters.add("--host");
            parameters.add("0.0.0.0");
        }

        return parameters;
    }

    protected void waitForServer() {
        App.logger.info("Checking server state...");
        File output = new File(App.getAppDirectory(), "server.txt");
        boolean started = false;
        while (!started) {
            try {
                Thread.sleep(100); // Wait for 100 milliseconds before checking the file again
                try (Scanner scanner = new Scanner(new FileInputStream(output))) {
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        if (line.contains("cleaning up before exit...") || line.contains("failed to load model") || line.contains("error while handling") || line.startsWith("error:") || line.startsWith("ROCm error:")) {
                            App.logger.error("ERROR: Could not start backend server.");
                            error = true;
                            process.destroy();
                            break;
                        }
                        if (line.contains("starting the main loop") || line.contains("model loaded")) {
                            App.logger.info("Backend server stated!");
                            started = true;
                            break;
                        }
                        if (line.startsWith("print_info: n_layer") && model.getSettings().getTotalLayers() == 0) {
                            line = line.split("=")[1].trim();
                            App.logger.info("Total Model Layers: {}", line);
                            model.getSettings().setTotalLayers(Integer.parseInt(line));
                        }
                    }
                }

            } catch (FileNotFoundException e) {
                App.logger.warn("Server output file not found yet (will retry): {}", e.getMessage());
            } catch (InterruptedException e) {
                // Handle if the thread is interrupted while sleeping
                Thread.currentThread().interrupt();
                App.logger.error("Server validation thread interrupted.");
                error = true;
                break;
            }

            if (error) {
                Platform.runLater(() -> {
                    MessageOverlay errorOverlay = new MessageOverlay(0, 0, 600, 100,"Error", "An error occurred when starting the backend server. The process never started or failed to start.");
                    errorOverlay.addStyle(Styles.DANGER);
                    errorOverlay.addStyle(Styles.BG_DEFAULT);
                    App.window.renderPopup(errorOverlay, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                });
                break;
            }
        }
        loading = false;
        fireServerLoadingCompleteEvent(!error);
    }

    public Process getProcess() {
        return process;
    }

    public synchronized boolean isError() {
        return error;
    }

    public synchronized boolean isAlive() {
        if (process == null) return false;
        return process.isAlive();
    }

    public synchronized boolean isLoading() {
        return loading;
    }

    public boolean stop() {
        listeners.clear();

        if (process == null) {
            return true;
        }
        if (!process.isAlive()) {
            return true;
        }

        App.logger.info("Attempting to gracefully shutdown llama-server.");
        process.destroy();

        try {
            // Wait for the process to exit, with a timeout
            boolean exited = process.waitFor(10, TimeUnit.SECONDS); // Give it up to 10 seconds
            if (exited) {
                App.logger.info("llama-server exited.");
                return true;
            } else {
                App.logger.info("Forcefully terminating llama-server...");
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                return !process.isAlive();
            }
        } catch (InterruptedException e) {
            App.logger.info("Waiting to destroy process...");
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return !process.isAlive();
        }
    }
    public static ServerProcess getCurrentServer() {
        return currentServer;
    }

    public void addServerLoadingListener(ServerLoadingListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeServerLoadingListener(ServerLoadingListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void fireServerLoadingCompleteEvent(boolean success) {
        for (ServerLoadingListener listener : listeners) {
            listener.onServerLoadingComplete(success);
        }
    }

    public Model getModel() {
        return model;
    }
}
