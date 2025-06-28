package me.piitex.app.backend.server;

import me.piitex.app.App;
import me.piitex.app.backend.Model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Tests the model and gathers information for the model
// This will only predict 1 token. May add loading time.
public class TestProcess {
    private Process process;
    private final Model model;

    public TestProcess(Model model) throws IOException {
        this.model = model;

        ServerSettings settings = App.getInstance().getSettings();

        File backendDirectory = new File(App.getBackendDirectory(), settings.getBackend() + "/");
        // Set the backend
        File server = new File(backendDirectory, "llama-cli.exe");
        if (!server.exists()) {
            return;
        }

        LinkedList<String> parameters = new LinkedList<>();
        parameters.add(server.getAbsolutePath());

        // Model file
        parameters.add("-m");
        parameters.add(model.getFile().getAbsolutePath());

        parameters.add("-n");
        parameters.add("1"); // Don't let the model actually generate anything.
        parameters.add("-no-cnv");

        // Build the process
        ProcessBuilder builder = new ProcessBuilder(parameters);

        // The server output will be errors even though it's not errors. This is how Java works
        builder.redirectError(new File(App.getAppDirectory(), "model-metadata.txt"));

        process = builder.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Applies a fix for vulkan BSOD
        // It seems it still causes BSOD but far less frequent.
        if (settings.getBackend().equalsIgnoreCase("vulkan")) {
            handleOutput();
        }

    }

    public void handleOutput() {
        try {
            LinkedList<String> lines = new LinkedList<>(Files.readAllLines(new File(App.getAppDirectory(), "model-metadata.txt").toPath()));
            for (String line : lines) {
                if (line.contains("offloaded") && line.contains("layers to GPU")) {
                    Matcher matcher = Pattern.compile("offloaded \\d+/(\\d+) layers to GPU").matcher(line);
                    if (matcher.find()) {
                        model.setGpuLayers(Integer.parseInt(matcher.group(1)));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
