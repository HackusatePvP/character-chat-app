package me.piitex.app;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import javafx.application.Application;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.server.DeviceProcess;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.HomeView;
import me.piitex.app.views.Positions;
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

        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();

        int width = dimension.width;
        int height = dimension.height;

        // For testing, remove later.
        //width = 600;
        //height = 1200;

        if (width < 900) {
            App.logger.info("Using mobile layouts...");
            // Set mobile view
            App.mobile = true;
            RenConfiguration.setWidth(width);
            RenConfiguration.setHeight(height);
            App.getInstance().getAppSettings().setWidth(width);
            App.getInstance().getAppSettings().setHeight(height);
            setWidth = 600;
            setHeight = 1200;

        // When changing from mobile to desktop view the configuration must be reverted.
        // Application will default to 720p.
        } else if (App.getInstance().getAppSettings().getWidth() <= 720) {
            App.logger.info("Forcefully resetting view to 1280x720.");
            setWidth = 1280;
            setHeight = 720;
            App.getInstance().getAppSettings().setWidth(setWidth);
            App.getInstance().getAppSettings().setHeight(setHeight);
        }

        App.logger.info("Setting initial dimensions ({},{})", setWidth, setHeight);
        App.logger.info("Screen Size ({},{})", dimension.width, dimension.height);

        // Initialize global positions. Needed for the rendering process.
        Positions.initialize();

        // Disable image caching.
        // Useful for most apps but not this one
        // Causes issues when changing a user or character image as the path will remain the same.
        // This is because the pathing for the image doesn't change but the image gets replaced by the new image.
        ImageLoader.useCache = false;

        Window window = new WindowBuilder("Chat App").setIcon(new ImageLoader(new File(App.getAppDirectory(), "logo.png"))).setScale(false).setDimensions(setWidth, setHeight).build();
        App.window = window;

        Stage stage = window.getStage();
        stage.setOnCloseRequest(windowEvent -> App.shutdown());

        // Debug hot keys.
        stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.R && event.isControlDown() && event.isShiftDown()) {
                App.logger.debug("Resetting view...");
                App.window.clearContainers();
                App.window.addContainer(new HomeView());
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
        window.clearContainers();
        HomeView homeView = new HomeView();
        window.addContainer(homeView);

        FXTrayIcon icon = new FXTrayIcon(window.getStage(), new File(App.getAppDirectory(), "logo.png"), 128, 128);
        icon.addExitItem("Exit", e -> App.shutdown());
        icon.setOnAction(event -> {
            App.logger.info("Handling tray action");
            stage.show();
            stage.toFront();
            stage.setIconified(false);
        });

        icon.show();

        // Sub thread as not to block JavaFX from initializing.
        App.getThreadPoolManager().submitTask(() -> {
            try {
                new DeviceProcess(App.getInstance().getSettings().getBackend());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            App.logger.info("Looking for model to load...");
            if (App.getInstance().getSettings().getGlobalModel() != null) {
                new ServerProcess(App.getInstance().getSettings().getGlobalModel());
            } else {
                for (Model model : App.getModels("exclude").values()) {
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