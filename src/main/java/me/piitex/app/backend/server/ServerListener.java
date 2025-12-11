package me.piitex.app.backend.server;

import me.piitex.app.App;
import me.piitex.app.backend.Model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class will manage remote connections from other devices.
 */
public class ServerListener {

    public ServerListener() {
        try (ServerSocket serverSocket = new ServerSocket(9817)) {
            Socket client = serverSocket.accept();
            App.logger.info("Client connected: {}", client.getInetAddress().getHostAddress());

            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));

            PrintWriter writer = new PrintWriter(client.getOutputStream(), true);

            String command;
            while ((command = reader.readLine()) != null) {
                App.logger.info("Command: {}", command);

                String response;
                if (command.equalsIgnoreCase("status")) {
                    response = "Ok";
                } else if (command.equalsIgnoreCase("restart")) {
                    // Restart server
                    ServerProcess process = new ServerProcess(App.getInstance().getSettings().getGlobalModel());
                    boolean loading = process.isLoading();
                    while (!loading) {
                        loading = process.isLoading();
                    }
                    response = "Ok";
                } else if (command.startsWith("swap model")) {
                    String modelName = command.replace("swap model", "").trim();
                    Model model = App.getModelsByName(modelName).getFirst();
                    if (model != null) {
                        ServerProcess process = new ServerProcess(model);
                        boolean loading = process.isLoading();
                        while (!loading) {
                            loading = process.isLoading();
                        }
                    }
                    response = "Ok";
                } else {
                    response = "Bad";
                }

                writer.println(response);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
