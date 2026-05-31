package me.piitex.app.backend.server;

import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.configuration.ServerSettings;
import me.piitex.os.OSUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ModelTestProcess {
    private Process process;
    private final Model model;

    private boolean error = false;

    public ModelTestProcess(Model model) {
        this.model = model;
        checkProcessID();

        if (model == null) {
            App.logger.error("Model was undefined. Unable to start server.");
            error = true;
            return;
        }

        if (!model.getFile().exists()) {
            App.logger.error("Incompatible model!: {}", model.getFile().getAbsolutePath());
        } else {

            if (model.getFile().setExecutable(true)) {
                App.logger.info("Changed file permissions for '{}'", model.getFile().getAbsolutePath());
            }
        }


        App.logger.info("Loading {}", model.getFile().getAbsolutePath());

        // Fetch server/model settings.
        ServerSettings settings = App.getInstance().getSettings();

        File server;
        File backendDirectory = new File(App.getBackendDirectory(), settings.getBackend().toLowerCase() + "/");

        if (!backendDirectory.exists()) {
            App.logger.warn("Backend not installed.");
            return;
        }

        if (OSUtil.getOS().contains("Windows")) {
            server = new File(backendDirectory, "llama-completion.exe");
        } else {
            server = new File(backendDirectory, "llama-completion");
            server.setExecutable(true, false);
        }

        if (!server.canExecute()) {
            App.logger.error("Failed to set executable permission on: {}", server.getAbsolutePath());
            // The permission may fail if the file is on a non-UNIX filesystem
            // (like FAT32 or NTFS) mounted without execution permissions.
        }

        List<String> parameters = getParameters(server, settings);
        App.logger.debug("Parameters: {}", parameters);

        // Build the process
        ProcessBuilder builder = new ProcessBuilder(parameters);

        // Set env if needed
        Map<String, String> environmentVariables = builder.environment();
        environmentVariables.put("GGML_VK_DISABLE_HOST_VISIBLE_VIDMEM", "1"); // Should fix BSOD with vulkan

        // The server output will be errors even though it's not errors. This is how Java works
        builder.redirectError(new File(App.getAppDirectory(), "model-output.txt"));

        process = null;
        try {
            // When the server starts it will not be automatically shutdown.
            // The process will remain open until this application is properly closed.
            process = builder.start();
            App.logger.info("Process started...");
        } catch (IOException e) {
            App.logger.error("Error occurred while gathering model data!", e);
        } finally {
            if (process != null) {
                // Save PID to terminate the process later.
                ProcessHandle handle = process.toHandle();
                App.getInstance().getSettings().getInfoFile().set("pid", handle.pid());
            } else {
                App.logger.error("Process returned null.");
            }
        }

        App.logger.info("Process created, waiting for output...");

        // Creates a thread-blocking scanner to ensure the server has started properly.
        // It also checks for errors and logs them.
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        App.logger.info("Process completed.");
        processOutput();

        if (!error) {
            App.logger.info("Started llama-server successfully.");
        }
    }

    private LinkedList<String> getParameters(File server, ServerSettings settings) {
        LinkedList<String> parameters = new LinkedList<>();
        parameters.add(server.getAbsolutePath());

        // Model file
        parameters.add("-m");
        parameters.add(model.getFile().getAbsolutePath());
        if (!settings.getDevice().equalsIgnoreCase("auto")) {
            parameters.add("-dev");
            parameters.add(settings.getFormattedDevice().trim());
        }

        parameters.add("-c");
        parameters.add(model.getSettings().getContextSize() + "");

        parameters.add("-p");
        parameters.add("The quick brown fox jumps.");

        parameters.add("-n");
        parameters.add("1");

        parameters.add("-no-cnv");
        parameters.add("--no-warmup");
        parameters.add("-lv");
        parameters.add("4");

        return parameters;
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

    protected void processOutput() {
        App.logger.info("Processing model data...");
        File output = new File(App.getAppDirectory(), "model-output.txt");
        if (!output.exists()) {
            App.logger.error("Process output was not created!");
            return;
        }
        double totalModelVramMiB = 0.0;
        double kvCacheSizeMiB = 0.0;
        double computeBufferMiB = 0.0;

        try (Scanner scanner = new Scanner(new FileInputStream(output))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                line = line.substring(15);
                if (line.contains("cleaning up before exit...") || line.contains("failed to load model") || line.contains("error while handling") || line.startsWith("error:") || line.startsWith("ROCm error:")) {
                    App.logger.error("ERROR: Could not start backend server.");
                    error = true;
                    break;
                }
                if (line.startsWith("print_info: n_layer") && model.getSettings().getTotalLayers() == 0) {
                    line = line.split("=")[1].trim();
                    App.logger.info("Total Model Layers: {}", line);
                    model.getSettings().setTotalLayers(Integer.parseInt(line));
                }
                if (line.contains("common_memory_breakdown_print:") && line.contains("|") && line.contains("=")) {
                    String[] parts = line.split("\\|");

                    // Ensure we have enough columns and avoid parsing the CPU "Host" RAM
                    if (parts.length > 2) {
                        String devicePart = parts[1].toLowerCase();
                        String totalPart = parts[2];

                        if (!devicePart.contains("host") && totalPart.contains("=")) {
                            String totalVramStr = totalPart.split("=")[0].trim();
                            try {
                                double totalVram = Double.parseDouble(totalVramStr);
                                if (totalVram > 0) {
                                    App.getInstance().getAppSettings().setTotalGpuVram(totalVram);
                                    App.logger.info("Found GPU VRAM: {} MiB", totalVram);
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                if (!line.contains("CPU_Mapped") && line.contains("model buffer size =") && totalModelVramMiB == 0.0) {
                    String valueWithUnit = line.split("=")[1].trim();
                    String vramValueStr = valueWithUnit.split(" ")[0];

                    totalModelVramMiB = Double.parseDouble(vramValueStr);
                    App.logger.debug("Extracted Model Weights VRAM: {} MiB", totalModelVramMiB);
                }
                if (line.contains("llama_kv_cache: size =") && line.contains("MiB") && kvCacheSizeMiB == 0.0) {
                    try {
                        String part = line.split("size =")[1].trim();
                        String kvSizeStr = part.split(" ")[0];
                        kvCacheSizeMiB = Double.parseDouble(kvSizeStr);
                        App.logger.debug("Extracted KV Cache Size: {} MiB", kvCacheSizeMiB);
                    } catch (Exception e) {
                        App.logger.error("Failed to parse KV cache size: {}", e.getMessage());
                    }
                }
                if (line.contains("compute buffer size =") && line.contains("MiB") && !line.contains("Host")) {
                    try {
                        String part = line.split("=")[1].trim();
                        String bufferSizeStr = part.split(" ")[0];
                        double currentComputeMiB = Double.parseDouble(bufferSizeStr);

                        // Keep the largest value found (which will be the GPU allocation)
                        if (currentComputeMiB > computeBufferMiB) {
                            computeBufferMiB = currentComputeMiB;
                            App.logger.info("Using '{}'MiB for compute buffer.", computeBufferMiB);
                        }
                    } catch (Exception e) {
                        App.logger.error("Failed to parse Compute Buffer MiB: {}", e.getMessage());
                    }
                }
            }

            int totalLayers = model.getSettings().getTotalLayers();
            if (totalLayers > 0 && totalModelVramMiB > 0.0) {
                double vramPerLayer = totalModelVramMiB / totalLayers;
                model.getSettings().setDataPerLayer(vramPerLayer);
                App.logger.info("Calculated VRAM per layer: {} MiB/layer", String.format("%.2f", vramPerLayer));
            }
            if (kvCacheSizeMiB > 0.0) {
                model.getSettings().setKvCacheSize(kvCacheSizeMiB);
            }
            if (computeBufferMiB > 0.0) {
                model.getSettings().setComputeBufferSize(computeBufferMiB);
            }
        } catch (FileNotFoundException e) {
            error = true;
            stop();
        }
        stop();
    }

    public boolean stop() {
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
}
