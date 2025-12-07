package me.piitex.app;
import atlantafx.base.theme.PrimerDark;
import com.dustinredmond.fxtrayicon.FXTrayIcon;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
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
import me.piitex.app.updater.ApplicationUpdater;
import me.piitex.app.updater.BackendUpdater;
import me.piitex.app.views.HomeView;
import me.piitex.app.views.Positions;
import me.piitex.engine.WindowBuilder;
import me.piitex.os.OSPathing;
import me.piitex.os.OSUtil;
import me.piitex.os.configurations.InfoFile;
import me.piitex.engine.Window;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.fxloader.FXLoad;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.AlertOverlay;
import me.piitex.engine.overlays.ButtonBuilder;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.os.FileDownloader;
import me.piitex.os.ProcessUtil;
import me.piitex.os.configurations.MasterKeyManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

public class App extends FXLoad {
    private ServerSettings settings;
    private AppSettings appSettings;

    // Character ID, Character object
    private final Map<String, Character> characters = new TreeMap<>();
    // User ID, User object
    private final Map<String, User> userTemplates = new TreeMap<>();
    private final TreeMap<String, Model> models = new TreeMap<>();
    private final TreeMap<String, Model> mmprojModels = new TreeMap<>();

    // Cached FileDownloader for performance
    private static final FileDownloader fileDownloader = new FileDownloader();

    private static App instance;
    public static ThreadPoolManager threadPoolManager;

    public static boolean dev = false;

    public static Window window;

    // Doesn't support natively ran mobile, but can at least make it viewable with remote connection.
    public static boolean mobile = false;

    public static final Logger logger = LogManager.getLogger(App.class);

    private volatile boolean loading = true;
    private volatile boolean error = false;

    // Used for testing with the IDE!
    public static void main(String[] args) {
        logger.info("Initializing IDE run configuration...");
        if (Arrays.asList(args).contains("--force-updates")) {
            logger.info("Forcing app/backend updates.");
            Main.forceUpdate = true;
        }
        new App();
        Application.launch(App.class);
    }

    @Override
    public void preInitialization() {
        logger.info("Initializing application...");
        instance = this;
        setupDirectories();
        MasterKeyManager.getPersistentPassKey(); // Generates a new master key if one doesn't exist.

        settings = new ServerSettings();
        appSettings = new AppSettings();


        if (ProcessUtil.isValidOS()) {
            long currentPid = ProcessHandle.current().pid();
            if (settings.getInfoFile().hasKey("main-pid")) {
                String pid = settings.getInfoFile().get("main-pid");
                if (ProcessUtil.isProcessRunning(Long.parseLong(pid))) {
                    logger.error("Process already running! '{}'", pid);
                    error = true;
                    Platform.runLater(() -> {
                        buildErrorWindow("Process is already running!").render();
                    });
                    return;
                }
            }

            settings.getInfoFile().set("main-pid", currentPid);
        }

        threadPoolManager = new ThreadPoolManager();
        threadPoolManager.submitTask(() -> {
            loading = true;
            App.logger.info("Looking for model to load '{}'", getModelsDirectory().getAbsolutePath());
            reloadModelList();
            loadUserTemplates();
            loadCharacters();
            App.logger.info("Finished pre-initialization.");
            loading = false;
            // Will not perform updates when using App.main(); This prevents development builds from being backported.
            if (Main.app || Main.run) {
                performUpdates();
            } else {
                if (Main.forceUpdate) {
                    performUpdates();
                } else if (OSUtil.getOS().contains("Windows")) {
                    if (!new File(getBackendDirectory(), "vulkan/").exists() || !new File(getBackendDirectory(), "cuda/").exists() || !new File(getBackendDirectory(), "hip/").exists()) {
                        App.logger.info("Windows updates available.");
                        performUpdates();
                    }
                } else if (OSUtil.getOS().contains("Linux")) {
                    if (!new File(getBackendDirectory(), "vulkan/").exists()) {
                        performUpdates();
                    }
                }
            }
        });
    }

    @Override
    public void initialization(Stage initialStage) {
        // Error will pass if another instance is running,
        if (error) return;
        App.logger.info("Loading app from '{}'", getAppDirectory().getAbsolutePath());
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

        File logo = new File(getExecutedDirectory(), "logo.png");
        window = new WindowBuilder("Chat App").setIcon(new ImageLoader(logo)).setScale((appSettings.isWindowScaling()) && !mobile).setAntiAliasing(false).setDimensions(setWidth, setHeight).build();

        // Initialize global positions. Needed for the rendering process.
        Positions.initialize();

        Stage stage = window.getStage();
        stage.setOnCloseRequest(_ -> App.shutdown());

        // Debug hot keys.
        setStageInput(window);

        // Build home view
        logger.info("Navigating to home page.");
        window.clearContainers();
        HomeView homeView = new HomeView();
        window.addContainer(homeView);

        if (OSUtil.getOS().contains("Windows")) {
            FXTrayIcon icon = new FXTrayIcon(window.getStage(), logo, 128, 128);
            icon.addExitItem("Exit", e -> App.shutdown());
            icon.setOnAction(_ -> {
                App.logger.info("Handling tray action");
                stage.show();
                stage.toFront();
                stage.setIconified(false);
            });

            icon.show();
        }

        // Sub thread as not to block JavaFX from initializing.
        App.getThreadPoolManager().submitTask(() -> {
            try {
                new DeviceProcess(App.getInstance().getSettings().getBackend());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Model model = App.getInstance().getSettings().getGlobalModel();
            if (model == null) {
                for (Model model1 : App.getModels("exclude")) {
                    if (model1.getSettings().isDefault()) {
                        model = model1;
                        break;
                    }
                }
            }

            if (model != null) {
                // Run Server.
                new ServerProcess(model);
            }
        });
    }

    private void setupDirectories() {
        App.logger.info("Operating System: {} version: {}", OSUtil.getOS(), OSUtil.getVersion());

        if (getAppDirectory().mkdirs()) {
            logger.info("Created app directory: {}", getAppDirectory().getAbsolutePath());
        }

        if (getAppDirectory().mkdirs()) {
            logger.info("Created data directory: {}", getAppDirectory().getAbsolutePath());
        }

        if (getBackendDirectory().mkdirs()) {
            logger.info("Created backend directory: {}", getBackendDirectory().getAbsolutePath());
            logger.info("Creating placeholder llama version...");
            File file = new File(getBackendDirectory(), "0.txt");
            try {
                if (file.createNewFile()) {
                    logger.info("Created placeholder file: {}", file.getAbsolutePath());
                }
            } catch (IOException e) {
                logger.error("Failed to make placeholder file!", e);
            }
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
        try {
            long downloadSize = downloader.getRemoteFileSize(dataFileUrl);
            if (currentSize != downloadSize) {
                logger.info("Updating model list...");
                downloader.startDownload(dataFileUrl, currentData);
            }

            logger.info("Model list updated.");
            downloader.shutdown();
        } catch (IOException e) {
            App.logger.error("Failed to fetch download size.", e);
        }

        // Microsoft, the multi trillion dollar company that can't handle more than 50 API requests.
        App.logger.info("Checking for application updates...");
        ApplicationUpdater applicationUpdater = new ApplicationUpdater(getVersion());
        applicationUpdater.checkForUpdates();

        App.logger.info("Checking for backend version...");
        File backendVersionFile = Arrays.stream(getBackendDirectory().listFiles()).filter(file -> file.getName().endsWith(".txt")).findAny().orElse(null);
        if (backendVersionFile != null) {
            BackendUpdater updater = new BackendUpdater(backendVersionFile.getName().split(".txt")[0]);
            App.logger.info("Looking for updates...");
            updater.checkForUpdates();
        } else {
            App.logger.error("Update file not found!");
        }
        App.logger.info("Finished updates.");
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

    public Window buildErrorWindow(String message) {
        if (window != null) {
            window.close(true);
        }

        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        File logo = new File(getExecutedDirectory(), "logo.png");
        window = new WindowBuilder("Error").setDimensions(400, 150).setIcon(new ImageLoader(logo)).build();

        EmptyContainer emptyContainer = new EmptyContainer(window.getWidth(), window.getHeight());
        window.addContainer(emptyContainer);

        AlertOverlay alertOverlay = new AlertOverlay("Error", Alert.AlertType.ERROR);
        alertOverlay.setContent(message);
        emptyContainer.addElement(alertOverlay);

        ButtonOverlay kill = new ButtonBuilder("kill").setText("Kill").build();
        kill.setX(200);
        kill.setY(50);
        emptyContainer.addElement(kill);
        kill.onClick(event -> {
            App.logger.info("Killing old process.");
            if (ProcessUtil.killProcess(Long.parseLong(settings.getInfoFile().get("main-pid")))) {
                App.logger.info("Old process was destroyed gracefully.");
                Platform.exit();
                System.exit(0);
            } else {
                App.logger.info("Forcefully killing old process.");
                ProcessUtil.terminateProcess(Long.parseLong(settings.getInfoFile().get("main-pid")));
            }

            appSettings.getInfoFile().set("main-pid", "");
        });

        window.getStage().setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(1);
        });

        return window;
    }

    public static App getInstance() {
        return instance;
    }

    public static File getAppDirectory() {
        // If Main.app passes this is being executed by jpackage executable.
        // If Main.run passes this is being executed by the jar file.
        // When Main.run does not pass, it being executed by the IDE.
        // For testing within the IDE, use App.main() as your entry point
        // For standard installation, run will pass.

        if (OSUtil.getOS().contains("Linux")) {
            OSPathing.groupId = "me.piitex.cca";
            return OSPathing.getAppDataDirectory();
        }
        return new File(OSPathing.getAppDataDirectory(), "chat-app/");

    }

    public static File getExecutedDirectory() {
        if (Main.app) {
            return new File(System.getProperty("user.dir") + "/app/");
        }
        if (OSUtil.getOS().contains("Linux")) {
            return OSPathing.getAppDataDirectory();
        } else {
            return new File(OSPathing.getAppDataDirectory(), "chat-app/");
        }
    }

    public static FileDownloader getFileDownloader() {
        return fileDownloader;
    }

    public static File getBackendDirectory() {
        return new File(getAppDirectory(), "/backend/");
    }

    public static File getModelsDirectory() {
        if (getInstance().getSettings() != null) {
            File file = new File(getInstance().getSettings().getModelPath());
            if (file.exists()) {
                return file;
            }
        }
        return new File(getAppDirectory(), "models/");
    }

    public static File getCharactersDirectory() {
        return new File(getAppDirectory(), "characters/");
    }

    public static File getUsersDirectory() {
        return new File(getAppDirectory(), "users/");
    }

    public static File getImagesDirectory() {
        return new File(getAppDirectory(), "images/");
    }

    public static Model getDefaultModel() {
        for (Model model : getModels("exclude")) {
            if (model.getSettings().isDefault()) {
                return model;
            }
        }
        return null;
    }

    public TreeMap<String, Model> getModels() {
        return models;
    }

    public TreeMap<String, Model> getMmprojModels() {
        return mmprojModels;
    }

    public static void reloadModelList() {
        getInstance().getModels().clear();
        getInstance().getMmprojModels().clear();
        getInstance().getModels().putAll(loadModels("exclude"));
        getInstance().getMmprojModels().putAll(loadModels("mmproj"));

    }


    private static TreeMap<String, Model> loadModels(String filterType) {
        TreeMap<String, Model> models = new TreeMap<>();

        if (App.getInstance().getSettings().getModelPath().isEmpty()) {
            return models;
        }

        String path = App.getInstance().getSettings().getModelPath().replace("%APPDATA%", OSPathing.getAppDataDirectory().getAbsolutePath());
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

    public static Collection<Model> getModels(String filter) {
       Collection<Model> toReturn = new HashSet<>();
       if (filter.equalsIgnoreCase("exclude")) {
           toReturn.addAll(getInstance().getModels().values());
       } else if (filter.equalsIgnoreCase("mmproj")) {
           toReturn.addAll(getInstance().getModels().values());
       } else {
           toReturn.addAll(getInstance().getModels().values());
           toReturn.addAll(getInstance().getMmprojModels().values());
       }
       return toReturn;
    }

    public static Set<String> getModelNames(String filter) {
        Set<String> toReturn = new TreeSet<>();
        if (filter.equalsIgnoreCase("exlude")) {
            for (Model model : getInstance().getModels().values()) {
                toReturn.add(new File(model.getFile().getParent()).getName() + "/" + model.getFile().getName());
            }
        } else if (filter.equalsIgnoreCase("mmproj")) {
            for (Model model : getInstance().getMmprojModels().values()) {
                toReturn.add(new File(model.getFile().getParent()).getName() + "/" + model.getFile().getName());
            }
        } else {
            Collection<Model> all = new HashSet<>(getInstance().getModels().values());
            all.addAll(getInstance().getMmprojModels().values());
            for (Model model : all) {
                toReturn.add(new File(model.getFile().getParent()).getName() + "/" + model.getFile().getName());
            }
        }
        return toReturn;
    }

    public static Model getModelByName(String directory, String name) {
        return getModels("all").stream().filter(model -> model.getFile().getName().equalsIgnoreCase(name) && new File(model.getFile().getParent()).getName().equalsIgnoreCase(directory)).findAny().orElse(null);
    }

    public static List<Model> getModelsByName(String name) {
        return getModels("all").stream().filter(model -> model.getFile().getName().equalsIgnoreCase(name)).toList();
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

    public String getVersion() {
        String version = null;

        // try to load from maven properties first
        try {
            Properties p = new Properties();
            InputStream is = getClass().getResourceAsStream("/META-INF/maven/me.piitex.app/character-chat-app/pom.properties");
            if (is != null) {
                p.load(is);
                version = p.getProperty("version", "");
            }
        } catch (Exception e) {
            // ignore
        }

        // fallback to using Java API
        if (version == null) {
            Package aPackage = getClass().getPackage();
            if (aPackage != null) {
                version = aPackage.getImplementationVersion();
                if (version == null) {
                    version = aPackage.getSpecificationVersion();
                }
            }
        }

        if (version == null) {
            // we could not compute the version so use a blank
            version = "";
        }

        if (!Main.app && !Main.run) {
            version = "v1.1.0";
        }

        return version;
    }
}
