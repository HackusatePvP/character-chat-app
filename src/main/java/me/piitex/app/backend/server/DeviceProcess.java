package me.piitex.app.backend.server;


import me.piitex.app.App;
import me.piitex.os.OSUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

public class DeviceProcess {
    private final Process process;

    public DeviceProcess(String backend) throws IOException {
        App.logger.info("Scanning devices for {} backend.", backend);

        backend = backend.replace("-", "").toLowerCase();
        File server;
        String[] parameters;

        File backendDirectory = new File(App.getBackendDirectory(), backend + "/");
        App.logger.info("Backend Location: {}", backendDirectory.getAbsolutePath());

        if (OSUtil.getOS().contains("Windows")) {
            server = new File(backendDirectory, "llama-server.exe");
            parameters = new String[] {
                    server.getAbsolutePath(),
                    "--list-devices"
            };

        } else {
            server = new File(backendDirectory, "llama-server");
            if (server.setExecutable(true, false)) {
                App.logger.info("Modified file permissions: {}", server.getAbsolutePath());
            }
            parameters = new String[] {
                    server.getAbsolutePath(),
                    "--list-devices"
            };

        }
        ProcessBuilder builder = new ProcessBuilder(parameters);
        builder.directory(backendDirectory);
        builder.redirectOutput(new File(App.getAppDirectory(), "devices.txt"));
        builder.redirectError(new File(App.getAppDirectory(), "device_errors.txt"));

        process = builder.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        handleOutput();
    }

    public void handleOutput() {
        try {
            LinkedList<String> lines = new LinkedList<>(Files.readAllLines(new File(App.getAppDirectory(), "devices.txt").toPath()));
            lines.removeFirst();
            App.getInstance().getSettings().setDevices(lines);
            App.logger.info("Devices: {}", List.of(lines));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Process getProcess() {
        return process;
    }

    public boolean stop() {
        if (!process.isAlive()) {
            return true;
        }
        process.destroy();
        return process.isAlive();
    }
}
