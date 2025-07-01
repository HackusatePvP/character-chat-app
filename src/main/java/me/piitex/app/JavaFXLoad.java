package me.piitex.app;

import atlantafx.base.theme.PrimerDark;
import com.dustinredmond.fxtrayicon.FXTrayIcon;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.server.DeviceProcess;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.configuration.InfoFile;
import me.piitex.app.views.HomeView;
import me.piitex.engine.Container;
import me.piitex.engine.Window;
import me.piitex.engine.WindowBuilder;
import me.piitex.engine.loaders.ImageLoader;

import java.awt.*;
import java.io.File;
import java.io.IOException;


public class JavaFXLoad extends Application {

    @Override
    public void start(Stage s) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // Configure dimensions
        int setWidth = 1280;
        int setHeight = 720;
        InfoFile appData = new InfoFile(new File(App.getAppDirectory(), "app.info"), false);
        if (appData.hasKey("width")) {
            setWidth = appData.getInteger("width");
        } else {
            appData.set("width", setWidth);
        }
        if (appData.hasKey("height")) {
            setHeight = appData.getInteger("height");
        } else {
            appData.set("height", setHeight);
        }

        App.logger.info("Setting initial dimensions ({},{})", setWidth, setHeight);

        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();

        int width = dimension.width;

        Window window = new WindowBuilder("Chat App").setIcon(new ImageLoader(new File(App.getAppDirectory(), "logo.png"))).setDimensions(setWidth, setHeight).build();
        window.updateBackground(Color.rgb(13, 17, 23));

        // Always true, later set the width to dimension.getWidth()
        if (width < 720) {
            // Set mobile view
            App.mobile = true;
        }

        App.window = window;

        Stage stage = window.getStage();
        stage.setOnCloseRequest(windowEvent -> {
            App.logger.info("Attempting shutdown...");
            if (ServerProcess.getCurrentServer() != null) {
                // This call will now block and wait for the server to stop
                boolean stopped = ServerProcess.getCurrentServer().stop();
                if (!stopped) {
                    App.logger.warn("Forcefully shutting down llama-server...");
                    // You might consider a short delay here before exiting the JVM
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            // When exiting, can cause BSOD with Vulkan.
            Platform.exit();
            System.exit(0);
        });

        // Build home view
        App.logger.info("Navigating to home page.");
        Container container = new HomeView().getContainer();
        window.addContainer(container);

        FXTrayIcon icon = new FXTrayIcon(window.getStage(), new File(App.getAppDirectory(), "logo.png"), 128, 128);
        icon.addExitItem("Exit", e -> {
            if (ServerProcess.getCurrentServer() != null) {
                // This call will now block and wait for the server to stop
                boolean stopped = ServerProcess.getCurrentServer().stop();
                if (!stopped) {
                    App.logger.warn("Forcefully shutting down llama-server...");
                    // You might consider a short delay here before exiting the JVM
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            // When exiting, can cause BSOD with Vulkan.
            Platform.exit();
            System.exit(0);
        });
        icon.show();

        window.render();

        // Sub thread as not to block JavaFX from initializing.
        new Thread(() -> {
            try {
                new DeviceProcess(App.getInstance().getSettings().getBackend());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            App.logger.info("Looking for model to load...");
            if (App.getInstance().getSettings().getLastModel() != null) {
                new ServerProcess(App.getInstance().getSettings().getLastModel());
            } else {
                for (Model model : App.getModels("exclude")) {
                    if (model.getSettings().isDefault()) {
                        new ServerProcess(model);
                        break;
                    }
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        // Load App then render gui
        new App();
        launch();
    }
}