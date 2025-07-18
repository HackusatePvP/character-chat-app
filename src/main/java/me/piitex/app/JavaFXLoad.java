package me.piitex.app;

import atlantafx.base.theme.PrimerDark;
import com.dustinredmond.fxtrayicon.FXTrayIcon;
import javafx.application.Application;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.server.DeviceProcess;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.HomeView;
import me.piitex.engine.Container;
import me.piitex.engine.RenConfiguration;
import me.piitex.engine.Window;
import me.piitex.engine.WindowBuilder;
import me.piitex.engine.loaders.ImageLoader;

import java.awt.*;
import java.io.File;
import java.io.IOException;


public class JavaFXLoad extends Application {

    @Override
    public void start(Stage s) {
        AppSettings appSettings = App.getInstance().getAppSettings();
        Application.setUserAgentStylesheet(appSettings.getStyleTheme(appSettings.getTheme()).getUserAgentStylesheet());

        int setWidth = appSettings.getWidth();
        int setHeight = appSettings.getHeight();

        App.logger.info("Setting initial dimensions ({},{})", setWidth, setHeight);

        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();

        App.logger.info("Screen Size ({},{})", dimension.width, dimension.height);

        int width = dimension.width;

        if (width < 720) {
            App.logger.info("Using mobile layouts...");
            // Set mobile view
            App.mobile = true;
            setWidth = 600;
            setHeight = 1200;
            RenConfiguration.setWidth(700);
            RenConfiguration.setHeight(1080);
        }

        // Disable image caching.
        // Useful for most app but not this one
        // Causes issues when changing a user or character image.
        ImageLoader.useCache = false;


        Window window = new WindowBuilder("Chat App").setIcon(new ImageLoader(new File(App.getAppDirectory(), "logo.png"))).setScale(true).setDimensions(setWidth, setHeight).build();

        App.window = window;

        Stage stage = window.getStage();
        stage.setOnCloseRequest(windowEvent -> App.shutdown());

        // Debug hot keys.
        stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.R && event.isControlDown() && event.isShiftDown()) {
                App.logger.debug("Resetting view...");
                App.window.clearContainers();
                App.window.addContainer(new HomeView().getContainer());
                App.window.render();
            }
            if (event.getCode() == KeyCode.C && event.isControlDown() && event.isShiftDown()) {
                App.logger.debug("Resetting character data...");
                App.getInstance().getCharacters().clear();
                App.getInstance().getUserTemplates().clear();
                App.getInstance().loadCharacters();
            }
        });

        // Build home view
        App.logger.info("Navigating to home page.");
        Container container = new HomeView().getContainer();
        window.addContainer(container);

        FXTrayIcon icon = new FXTrayIcon(window.getStage(), new File(App.getAppDirectory(), "logo.png"), 128, 128);
        icon.addExitItem("Exit", e -> App.shutdown());
        icon.setOnAction(event -> {
            App.logger.info("Handling tray action");

            // Not working
            stage.show();
            stage.toFront();
            stage.setIconified(false);
        });

        icon.show();

        window.render();

        // Sub thread as not to block JavaFX from initializing.
        App.getInstance().getThreadPoolManager().submitTask(() -> {
            try {
                new DeviceProcess(App.getInstance().getSettings().getBackend());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            App.logger.info("Looking for model to load...");
            if (App.getInstance().getSettings().getGlobalModel() != null) {
                new ServerProcess(App.getInstance().getSettings().getGlobalModel());
            } else {
                for (Model model : App.getModels("exclude")) {
                    if (model.getSettings().isDefault()) {
                        new ServerProcess(model);
                        break;
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        // Load App then render gui
        new App();
        launch();
    }
}