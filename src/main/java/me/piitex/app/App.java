package me.piitex.app;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.User;
import me.piitex.app.backend.server.DeviceProcess;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.backend.server.TestProcess;
import me.piitex.app.configuration.InfoFile;
import me.piitex.app.configuration.UserSettings;
import me.piitex.engine.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class App {
    private final ServerSettings settings;
    private final UserSettings userSettings;

    // Character ID, Character object
    private final Map<String, Character> characters = new TreeMap<>();
    private final Map<String, User> userTemplates = new TreeMap<>();

    private static App instance;

    public static boolean dev = false;

    public static Window window;

    // static abuse for buffered scaling
    public static double width = 1920;
    public static double height = 1080;

    // Doesn't support natively ran mobile, but can at least make it viewable with remote connection.
    public static boolean mobile = false;

    public static final Logger logger = LogManager.getLogger(App.class);

    public App() {
        logger.info("Initializing application...");
        instance = this;

        getAppDirectory().mkdirs();
        getBackendDirectory().mkdirs();
        getModelsDirectory().mkdirs();
        getCharactersDirectory().mkdirs();
        getUsersDirectory().mkdirs();

        loadUserTemplates();
        loadCharacters();

        settings = new ServerSettings();
        settings.getInfoFile().set("main-pid", ProcessHandle.current().pid());
        userSettings = new UserSettings();
    }

    public void loadCharacters() {
        logger.info("Loading characters...");
        for (File file : getCharactersDirectory().listFiles()) {
            if (file.isDirectory()) {
                String id = file.getName();
                // Check if info file exsists
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
                // Check if info file exsists
                File info = new File(file, "user.info");
                if (info.exists()) {
                    InfoFile infoFile = new InfoFile(info, true);
                    User user = new User(id, infoFile);
                    userTemplates.put(id, user);
                }
            }
        }
    }

    public UserSettings getUserSettings() {
        return userSettings;
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
        for (Model model : getModels()) {
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

    public static List<Model> getModels() {
        List<Model> models = new ArrayList<>();
        if (App.getInstance().getSettings().getModelPath().isEmpty()) return models;
        File modelPath = new File(App.getInstance().getSettings().getModelPath().replace("%APPDATA%", System.getenv("APPDATA")));
        if (!modelPath.isDirectory()) return models;
        modelPath.mkdirs();
        for (File file : modelPath.listFiles()) {
            if (file.getName().endsWith("gguf")) {
                Model model = new Model(file);
                models.add(model);
            }
        }

        return models;
    }

    public static Set<String> getModelNames() {
        Set<String> toReturn = new TreeSet<>();
        for (Model model : getModels()) {
            toReturn.add(model.getFile().getName());
        }
        return toReturn;
    }

    public static Model getModelByName(String name) {
        return getModels().stream().filter(model -> model.getFile().getName().equalsIgnoreCase(name)).findAny().orElse(null);
    }
}
