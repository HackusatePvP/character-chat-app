package me.piitex.app.backend.server;


import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.overlays.MessageOverlay;

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

        loading = true;

        // Fetch server/model settings.
        ServerSettings settings = App.getInstance().getSettings();

        File backendDirectory = new File(App.getBackendDirectory(), settings.getBackend() + "/");
        File server = new File(backendDirectory, "llama-server.exe");
        List<String> parameters = getParameters(server, settings);
        App.logger.debug("Server Parameters: {}", parameters);

        // Build the process
        ProcessBuilder builder = new ProcessBuilder(parameters);

        // Set env if needed
        Map<String, String> environmentVariables = builder.environment();
        environmentVariables.put("GGML_VK_DISABLE_HOST_VISIBLE_VIDMEM", "1"); // Should fix BSOD with vulkan

        // The server output will be errors even though it's not errors. This is how Java works
        builder.redirectError(new File(App.getDataDirectory(), "server.txt"));

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
            parameters.add("-dev");
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

        // Caculate the cache sizes llama.cpp uses. This is typically the kvCache and the compute buffer size.
        double kvCacheMiB = model.getSettings().getKvCacheSize();
        double computeBufferMiB = model.getSettings().getComputeBufferSize();

        // Combine the two values to accurately calculate the exact vram usage.
        double FIXED_OVERHEAD_MIB = kvCacheMiB + computeBufferMiB;

        if (FIXED_OVERHEAD_MIB <= 0.0) {
            // Fallback value for the total fixed overhead observed in the log (3300.00 + 946.00)
            App.logger.warn("Fixed overhead was 0.0. Using empirical fallback (4246.00 MiB).");
            FIXED_OVERHEAD_MIB = 4246.00;
        }

        // The size of each model layer. Represented by the total vram of the models layer / by the total layers.
        final double VRAM_PER_LAYER_MIB = model.getSettings().getDataPerLayer();

        // The percentage of VRAM to use.
        double usagePercentage = settings.getGpuUsage();

        // Calculate the VRAM available for offloading layers.
        double layerAllocatableVram = TOTAL_AVAILABLE_VRAM_MIB - FIXED_OVERHEAD_MIB;

        // Apply the percentage budget to the VRAM reserved for layers only.
        double layerBudgetMiB = layerAllocatableVram * (usagePercentage / 100.0);


        // Calculate the number of layers that fit into the Layer Budget
        int layers;

        if (VRAM_PER_LAYER_MIB > 0.0) {
            layers = (int) Math.floor(layerBudgetMiB / VRAM_PER_LAYER_MIB);
        } else {
            // Log a warning and default to 0 layers when VRAM per layer is unknown/zero.
            App.logger.warn("VRAM per layer (dataPerLayer) is 0.0, defaulting layers to 0 to prevent Integer.MAX_VALUE.");
            layers = 0;
        }

        int totalModelLayers = model.getSettings().getTotalLayers();
        System.out.println("Layers: " + layers);

        // Cap the offloaded layers
        layers = Math.min(layers, totalModelLayers);
        System.out.println("Adjusted Layers: " + layers);

        parameters.add("-ngl");
        App.logger.info("Using VRAM utilization of '{}'% (Layer Budget: {} MiB, Total VRAM Usage: {} MiB) to load '{}'/'{}' layers",
                usagePercentage,
                (int) layerBudgetMiB,
                (int) (layerBudgetMiB + FIXED_OVERHEAD_MIB), // This is the estimated TOTAL VRAM usage
                layers,
                totalModelLayers);

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

        // Server port and WebUI
        parameters.add("--port");
        parameters.add("8187");
        parameters.add("--no-webui");

        return parameters;
    }

    protected void waitForServer() {
        App.logger.info("Checking server state...");
        File output = new File(App.getDataDirectory(), "server.txt");
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
                        if (line.contains("starting the main loop")) {
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
