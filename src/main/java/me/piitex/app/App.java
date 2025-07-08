package me.piitex.app;
import javafx.application.Platform;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.User;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.configuration.InfoFile;
import me.piitex.engine.Window;
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

    public static boolean dev = false;

    public static Window window;

    // Doesn't support natively ran mobile, but can at least make it viewable with remote connection.
    public static boolean mobile = false;
    public static boolean vulkanDisable = false;

    public static final Logger logger = LogManager.getLogger(App.class);

    private volatile boolean loading = false;


    public App() {
        logger.info("Initializing application...");
        instance = this;

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

        loadUserTemplates();

        new Thread(() -> {
            loading = true;
            loadCharacters();
            loading = false;
        }).start();

        appSettings = new AppSettings();
        settings = new ServerSettings();
        settings.getInfoFile().set("main-pid", ProcessHandle.current().pid());
    }

    public boolean isLoading() {
        return loading;
    }

    public void loadCharacters() {
        logger.info("Loading characters...");
        for (File file : getCharactersDirectory().listFiles()) {
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
        for (File file : getUsersDirectory().listFiles()) {
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

    public AppSettings getAppSettings() {
        return appSettings;
    }

    public ServerSettings getSettings() {
        return settings;
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

    public Model getDefaultModel() {
        for (Model model : getModels("exclude")) {
            if (model.getSettings().isDefault()) {
                return model;
            }
        }
        return null;
    }

    public Map<String, User> getUserTemplates() {
        return userTemplates;
    }

    public static App getInstance() {
        return instance;
    }

    public static File getAppDirectory() {
        return new File(System.getenv("APPDATA") + "/chat-app/");
    }

    public static File getBackendDirectory() {
        return new File(getAppDirectory(), "backend/");
    }

    public static File getModelsDirectory() {
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

    public static List<Model> getModels(String filterType) {
        List<Model> models = new ArrayList<>();

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

    public static List<Model> getModels() {
        return getModels(null); // Call the overloaded method with null to get all.
    }


    private static void findGGUFModelsRecursive(@NotNull File directory, List<Model> models, String filterType) {
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
                            models.add(new Model(file));
                        }
                        break;
                    case "exclude":
                        if (!isMMProj) {
                            models.add(new Model(file));
                        }
                        break;
                    case "all":
                    default:
                        models.add(new Model(file));
                        break;
                }
            }
        }
    }

    public static Set<String> getModelNames(String filter) {
        Set<String> toReturn = new TreeSet<>();
        for (Model model : getModels(filter)) {
            toReturn.add(model.getFile().getName());
        }
        return toReturn;
    }

    public static Model getModelByName(String name) {
        return getModels("all").stream().filter(model -> model.getFile().getName().equalsIgnoreCase(name)).findAny().orElse(null);
    }

    public static void shutdown() {
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
    }
}
