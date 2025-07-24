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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
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
        // See if the server didn't properly shutdown
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
                                App.logger.info("Destroying {} forcefully.", pid);
                                processHandle.destroyForcibly();
                            }
                        }
                    } else {
                        if (processHandle.isAlive()) {
                            App.logger.info("Destroying {} forcefully.", pid);
                            processHandle.destroyForcibly();
                        }
                    }
                }
            } else {
                App.logger.info("{} is already destroyed.", pid);
            }
        }

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
        // Set the backend
        File server = new File(backendDirectory, "llama-server.exe");

        // Create the model/llama server parameters.
        // -m is a hard requirement
        LinkedList<String> parameters = new LinkedList<>();
        parameters.add(server.getAbsolutePath());

        // Model file
        parameters.add("-m");
        parameters.add(model.getFile().getAbsolutePath());
        if (!settings.getDevice().equalsIgnoreCase("auto")) {
            App.logger.debug("Setting device...");
            parameters.add("-dev");
            parameters.add(settings.getFormattedDevice());
        }

        // Vision model file
        if (!model.getSettings().getMmProj().equalsIgnoreCase("")) {
            if (model.getSettings().getMmProj().startsWith("None")) {
                App.logger.warn("MMProj not specified.");
                parameters.add("--no-mmproj");
            } else {
                Model mmproj = App.getModelByName(model.getFile().getParent(), model.getSettings().getMmProj());
                App.logger.debug("MMPROJ: " + mmproj.getFile().getAbsolutePath());
                parameters.add("--mmproj");
                parameters.add(mmproj.getFile().getAbsolutePath());
            }
        }

        // GPU layers
        parameters.add("-ngl");

        //FIXME: Vulkan will BSOD on application close if the getGpuLayers > model layers
        // Add a correct fix when using vulkan to set a hard limit on the amount of layers
        // Still causes BSOD after applying fix. More testing is required.
        if (settings.getBackend().equalsIgnoreCase("vulkan")) {
            if (model.getGpuLayers() > 0 && settings.getGpuLayers() > model.getGpuLayers()) {
                settings.setGpuLayers(model.getGpuLayers());
            }
        }

        parameters.add(settings.getGpuLayers() + "");

        // Memory swapping
        if (settings.isMemoryLock()) {
            parameters.add("--mlock");
        }
        // Flash attention
        if (settings.isFlashAttention()) {
            parameters.add("-fa");
        }

        // Reasoning Template
        if (!settings.getReasoningTemplate().equalsIgnoreCase("disabled") && !settings.getReasoningTemplate().equalsIgnoreCase("none")) {
            App.logger.debug("Enabling response format...");
            parameters.add("--reasoning-format");
            parameters.add(settings.getReasoningTemplate());
        }

        // Chat Template
        if (!settings.getChatTemplate().equalsIgnoreCase("default")) {
            App.logger.debug("Setting chat template...");
            parameters.add("--chat-template");
            parameters.add(settings.getChatTemplate());
        }

        // Jinja Chat Template
        if (settings.isJinja()) {
            App.logger.debug("Using jinja...");
            parameters.add("--jinja");
        }

        // Qwen3 think mode
        if (!settings.isThinkMode()) {
            App.logger.debug("Disabling think mode...");
            parameters.add("--reasoning-budget");
            parameters.add("0");
        }

        parameters.add("-c");
        parameters.add(model.getSettings().getContextSize() + "");

        // Server port and WebUI
        parameters.add("--port");
        parameters.add("8187");
        parameters.add("--no-webui");

        App.logger.debug("Server Parameters: " + parameters.toString());

        if (App.dev) {
            App.logger.debug("Server Parameters: " + parameters);
        }

        // Build the process
        ProcessBuilder builder = new ProcessBuilder(parameters);

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
                        if (line.contains("starting the main loop")) {
                            App.logger.info("Backend server stated!");
                            started = true;
                            break;
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
        if (process == null) {
            return true;
        }
        if (!process.isAlive()) {
            return true;
        }

        App.logger.info("Attempting to gracefully shutdown llama-server.");
        process.destroy(); // Send a termination signal

        try {
            // Wait for the process to exit, with a timeout
            boolean exited = process.waitFor(10, TimeUnit.SECONDS); // Give it up to 10 seconds
            if (exited) {
                App.logger.info("llama-server exited.");
                return true; // Process successfully exited
            } else {
                App.logger.info("Forcefully terminating llama-server...");
                process.destroyForcibly(); // Force kill if it didn't respond
                // Wait again for forcible destruction (might not be necessary but safer)
                process.waitFor(5, TimeUnit.SECONDS);
                return !process.isAlive(); // Return true if it's dead
            }
        } catch (InterruptedException e) {
            App.logger.info("Waiting to destroy process...");
            Thread.currentThread().interrupt(); // Restore interrupted status
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
