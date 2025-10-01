package me.piitex.app;
import javafx.application.Platform;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.User;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.engine.configurations.InfoFile;
import me.piitex.engine.Window;
import me.piitex.os.FileDownloader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class App {
    private final ServerSettings settings;
    private final AppSettings appSettings;

    // Character ID, Character object
    private final Map<String, Character> characters = new TreeMap<>();
    private final Map<String, User> userTemplates = new TreeMap<>();

    private static App instance;
    public static ThreadPoolManager threadPoolManager;

    public static boolean dev = false;

    public static Window window;

    // Doesn't support natively ran mobile, but can at least make it viewable with remote connection.
    public static boolean mobile = false;

    public static final Logger logger = LogManager.getLogger(App.class);

    private volatile boolean loading = false;

    public App() {
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
        // When exiting, can cause BSOD with Vulkan.
        Platform.exit();
        System.exit(0);
    }
}
