package me.piitex.app.backend.server;


import me.piitex.app.App;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;

public class DeviceProcess {
    private final Process process;

    public DeviceProcess(String backend) throws IOException {
        App.logger.info("Scanning devices for {} backend.", backend);

        backend = backend.replace("-", "").toLowerCase();

        File backendDirectory = new File(App.getBackendDirectory(), backend + "/");
        File server = new File(backendDirectory, "llama-server.exe");
        String[] parameters = new String[] {
                server.getAbsolutePath(),
                "--list-devices"
        };

        ProcessBuilder builder = new ProcessBuilder(parameters);
        builder.redirectOutput(new File(App.getDataDirectory(), "devices.txt"));

        process = builder.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        handleOutput();
    }

    public void handleOutput() {
        App.logger.info("Handling input...");
        try {
            LinkedList<String> lines = new LinkedList<>(Files.readAllLines(new File(App.getDataDirectory(), "devices.txt").toPath()));
            lines.removeFirst();
            App.getInstance().getSettings().setDevices(lines);

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
