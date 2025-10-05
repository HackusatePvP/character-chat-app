package me.piitex.app;
import com.dustinredmond.fxtrayicon.FXTrayIcon;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.User;
import me.piitex.app.backend.server.DeviceProcess;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.HomeView;
import me.piitex.app.views.Positions;
import me.piitex.engine.WindowBuilder;
import me.piitex.engine.configurations.InfoFile;
import me.piitex.engine.Window;
import me.piitex.engine.fxloader.FXLoad;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.os.FileDownloader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class App extends FXLoad {
    private ServerSettings settings;
    private AppSettings appSettings;

    // Character ID, Character object
    private final Map<String, Character> characters = new TreeMap<>();
    private final Map<String, User> userTemplates = new TreeMap<>();
    private static final FileDownloader fileDownloader = new FileDownloader();

    private static App instance;
    public static ThreadPoolManager threadPoolManager;

    public static boolean dev = false;

    public static Window window;

    // Doesn't support natively ran mobile, but can at least make it viewable with remote connection.
    public static boolean mobile = false;

    public static final Logger logger = LogManager.getLogger(App.class);

    private volatile boolean loading = true;

    @Override
    public void preInitialization() {
        logger.info("Initializing application...");
        instance = this;
        threadPoolManager = new ThreadPoolManager();

        if (getAppDirectory().mkdirs()) {
            logger.info("Created app directory: {}", getAppDirectory().getAbsolutePath());
        }

        if (getBackendDirectory().mkdirs()) {
            logger.info("Created backend directory: {}", getBackendDirectory().getAbsolutePath());
        }

        if (getModelsDirectory().mkdirs()) {
            logger.info("Created models directory: {}", getModelsDirectory().getAbsolutePath());
        }

        if (getCharactersDirectory().mkdirs()) {
            logger.info("Created characters directory: {}", getCharactersDirectory().getAbsolutePath());
        }

        if (getUsersDirectory().mkdirs()) {
            logger.info("Created users directory: {}", getUsersDirectory().getAbsolutePath());
        }

        threadPoolManager.submitTask(() -> {
            loading = true;
            if (Main.run) {
                performUpdates();
            }
            loadUserTemplates();
            loadCharacters();
            App.logger.info("Loaded character data.");
            loading = false;
        });
        appSettings = new AppSettings();
        settings = new ServerSettings();
        settings.getInfoFile().set("main-pid", ProcessHandle.current().pid());
    }

    @Override
    public void initialization(Stage initialStage) {
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
            logger.info("Using mobile layouts...");
            // Set mobile view
            mobile = true;
            appSettings.setWidth(width);
            appSettings.setHeight(height);
            setWidth = 600;
            setHeight = 1200;

            // When changing from mobile to desktop view the configuration must be reverted.
            // Application will default to 720p.
        } else if (getAppSettings().getWidth() <= 1000) {
            logger.info("Forcefully resetting view to 1280x720.");
            setWidth = 1280;
            setHeight = 720;
            getAppSettings().setWidth(setWidth);
            getAppSettings().setHeight(setHeight);
        } else if (width < appSettings.getWidth()) {
            logger.warn("Monitor size does not fit configured dimensions.");
            appSettings.setWidth(1280);
            appSettings.setHeight(720);
            width = 1280;
            height = 720;
            setWidth = width;
            setHeight = height;
        }


        logger.info("Setting initial dimensions ({},{})", setWidth, setHeight);
        logger.info("Screen Size ({},{})", dimension.width, dimension.height);


        // Disable image caching.
        // Useful for most apps but not this one
        // Causes issues when changing a user or character image as the path will remain the same.
        // This is because the pathing for the image doesn't change but the image gets replaced by the new image.
        ImageLoader.useCache = false;

        window = new WindowBuilder("Chat App").setIcon(new ImageLoader(new File(App.getAppDirectory(), "logo.png"))).setScale(false).setDimensions(setWidth, setHeight).build();

        // Initialize global positions. Needed for the rendering process.
        Positions.initialize();

        Stage stage = window.getStage();
        stage.setOnCloseRequest(windowEvent -> App.shutdown());

        // Debug hot keys.
        setStageInput(window);

        // Build home view
        logger.info("Navigating to home page.");
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

    private void setStageInput(Window window) {
        Stage stage = window.getStage();
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

            if (event.getCode() == KeyCode.S && event.isControlDown() && event.isShiftDown()) {
                App.logger.debug("Resetting stage...");
                stage.setOnCloseRequest(null); // Prevent the application from exiting
                stage.close();
                stage.getScene().setRoot(new Pane()); // Needed to release the WindowBuilder pane.

                start(new Stage());
            }
        });
    }

    public boolean isLoading() {
        return loading;
    }

    public void loadCharacters() {
        logger.info("Loading characters...");
        File[] files = getCharactersDirectory().listFiles();
        if (files == null) {
            logger.error("Could not initialize characters directory. Program may lack permission to access file system.");
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                String id = file.getName();
                // Check if info file exists
                File info = new File(file, "character.info");
                if (info.exists()) {
                    InfoFile infoFile = new InfoFile(info, true);
                    characters.put(id, new Character(id, infoFile));
                }
            }
        }
    }

    public void loadUserTemplates() {
        logger.info("Loading users...");
        File[] files = getUsersDirectory().listFiles();
        if (files == null) {
            logger.error("Could not initialize users directory. Program may lack permission to access file system.");
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                String id = file.getName();
                // Check if info file exists
                File info = new File(file, "user.info");
                if (info.exists()) {
                    InfoFile infoFile = new InfoFile(info, true);
                    User user = new User(id, infoFile);
                    userTemplates.put(id, user);
                }
            }
        }
    }

    public void performUpdates() {
        logger.info("Checking for updates...");
        // Automatically update model cache
        String dataFileUrl = "https://raw.githubusercontent.com/HackusatePvP/character-chat-app/refs/heads/master/src/main/resources/windows/chat-app/models/model-list.dat";
        FileDownloader downloader = new FileDownloader();
        File currentData = new File(getModelsDirectory(), "model-list.dat");
        if (!currentData.exists()) {
            logger.warn("Model list data was missing...");
        }

        long currentSize = currentData.length();
        long downloadSize = downloader.getRemoteFileSize(dataFileUrl);
        if (currentSize != downloadSize) {
            logger.info("Updating model list...");
            downloader.startDownload(dataFileUrl, currentData);
        }

        logger.info("Model list updated...");
        downloader.shutdown();

    }

    public AppSettings getAppSettings() {
        return appSettings;
    }

    public ServerSettings getSettings() {
        return settings;
    }

    public static ThreadPoolManager getThreadPoolManager() {
        return threadPoolManager;
    }

    public Map<String, Character> getCharacters() {
        return characters;
    }

    public User getUser(String id) {
        return userTemplates.get(id);
    }

    public Character getCharacter(String id) {
        return characters.get(id);
    }

    public boolean containsCharacter(String id) {
        for (String key : characters.keySet()) {
            if (key.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, User> getUserTemplates() {
        return userTemplates;
    }

    public static App getInstance() {
        return instance;
    }

    public static File getAppDirectory() {
        // If Main.run passes this is being executed by the jar file.
        // When Main.run does not pass, it being executed by the IDE.
        // For testing within the IDE, use JavaFXLoad.main() as your entry point
        // For standard installation, run will pass.
        if (Main.run) {
            return new File(System.getProperty("user.dir"));
        } else {
            return new File(System.getenv("APPDATA") + "/chat-app/");
        }
    }

    public static FileDownloader getFileDownloader() {
        return fileDownloader;
    }

    public static File getDataDirectory() {
        return new File(System.getenv("APPDATA") + "/chat-app/");
    }

    public static File getBackendDirectory() {
        return new File(getAppDirectory(), "/backend/");
    }

    public static File getModelsDirectory() {
        return new File(getAppDirectory(), "models/");
    }

    public static File getCharactersDirectory() {
        return new File(getDataDirectory(), "characters/");
    }

    public static File getUsersDirectory() {
        return new File(getDataDirectory(), "users/");
    }

    public static File getImagesDirectory() {
        return new File(getDataDirectory(), "images/");
    }

    public static Model getDefaultModel() {
        for (Model model : getModels("exclude").values()) {
            if (model.getSettings().isDefault()) {
                return model;
            }
        }
        return null;
    }

    public static TreeMap<String, Model> getModels(String filterType) {
        TreeMap<String, Model> models = new TreeMap<>();

        if (App.getInstance().getSettings().getModelPath().isEmpty()) {
            return models;
        }

        String path = App.getInstance().getSettings().getModelPath().replace("%APPDATA%", System.getenv("APPDATA"));
        File modelPath = new File(path);

        if (!modelPath.exists()) {
            if (modelPath.mkdirs()) {
                App.logger.info("Created models directory.");
            }
            return models;
        }

        if (!modelPath.isDirectory()) {
            return models;
        }
        findGGUFModelsRecursive(modelPath, models, filterType);
        return models;
    }

    public static TreeMap<String, Model> getModels() {
        return getModels(null); // Call the overloaded method with null to get all.
    }

    private static void findGGUFModelsRecursive(@NotNull File directory, TreeMap<String, Model> models, String filterType) {
        File[] files = directory.listFiles();
        if (files == null) return;
        String actualFilterType = (filterType == null || filterType.isEmpty()) ? "all" : filterType.toLowerCase();
        for (File file : files) {
            if (file.isDirectory()) {
                findGGUFModelsRecursive(file, models, filterType);
            } else if (file.isFile() && file.getName().endsWith(".gguf")) {
                String fileName = file.getName();
                boolean isMMProj = fileName.contains("mmproj");
                switch (actualFilterType) {
                    case "mmproj":
                        if (isMMProj) {
                            models.put(fileName, new Model(file));
                        }
                        break;
                    case "exclude":
                        if (!isMMProj) {
                            models.put(fileName, new Model(file));
                        }
                        break;
                    case "all":
                    default:
                        models.put(fileName, new Model(file));
                        break;
                }
            }
        }
    }

    public static Set<String> getModelNames(String filter) {
        Set<String> toReturn = new TreeSet<>();
        for (Model model : getModels(filter).values()) {
            toReturn.add(new File(model.getFile().getParent()).getName() + "/" + model.getFile().getName());
        }
        return toReturn;
    }

    public static Model getModelByName(String directory, String name) {
        return getModels("all").values().stream().filter(model -> model.getFile().getName().equalsIgnoreCase(name) && new File(model.getFile().getParent()).getName().equalsIgnoreCase(directory)).findAny().orElse(null);
    }

    public static void shutdown() {
        App.logger.info("Attempting shutdown...");
        if (ServerProcess.getCurrentServer() != null) {
            // This call will now block and wait for the server to stop
            boolean stopped = ServerProcess.getCurrentServer().stop();
            if (!stopped) {
                App.logger.warn("Forcefully shutting down llama-server...");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        Platform.exit();
        System.exit(0);
    }
}
