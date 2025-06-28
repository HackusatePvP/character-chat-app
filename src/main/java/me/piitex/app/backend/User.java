package me.piitex.app.backend;

import me.piitex.app.App;
import me.piitex.app.configuration.InfoFile;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.ImageOverlay;

import java.io.File;

import java.util.HashMap;
import java.util.Map;

public class User {
    private final String id;
    private String displayName = "";
    private String persona = "";
    private String iconPath = "";

    private InfoFile infoFile;

    private Map<String, String> lorebook = new HashMap<>();

    // Create user
    public User(String id) {
        this.id = id;
        getUserDirectory().mkdirs();
        infoFile = new InfoFile(new File(getUserDirectory(), "user.info"), true);
        infoFile.set("id", id);
    }

    public User(String id, InfoFile infoFile) {
        this.id = id;
        infoFile.set("id", id);
        this.infoFile = infoFile;
        if (infoFile.hasKey("display-name")) {
            this.displayName = infoFile.get("display-name");
        }
        if (infoFile.hasKey("persona")) {
            this.persona = infoFile.get("persona");
        }
        if (infoFile.hasKey("icon-path")) {
            this.iconPath = infoFile.get("icon-path");
        }
        if (infoFile.hasKey("lore")) {
            this.lorebook = infoFile.getStringMap("lore");
        }
    }

    public User(InfoFile infoFile) {
        this.infoFile = infoFile;
        this.id = infoFile.get("id");
        if (infoFile.hasKey("display-name")) {
            this.displayName = infoFile.get("display-name");
        }
        if (infoFile.hasKey("persona")) {
            this.persona = infoFile.get("persona");
        }
        if (infoFile.hasKey("icon-path")) {
            this.iconPath = infoFile.get("icon-path");
        }
        if (infoFile.hasKey("lore")) {
            this.lorebook = infoFile.getStringMap("lore");
        }
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        infoFile.set("display-name", displayName);
    }

    public String getPersona() {
        return persona;
    }

    public void setPersona(String persona) {
        this.persona = persona;
        infoFile.set("persona", persona);
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
        infoFile.set("icon-path", iconPath);
    }

    public File getUserDirectory() {
        return new File(App.getUsersDirectory(), id + "/");
    }

    public void reloadFile() {
        this.infoFile = new InfoFile(new File(getUserDirectory(), "user.info"), true);
    }

    public Map<String, String> getLorebook() {
        return lorebook;
    }

    public void addLoreItem(String key, String value) {
        lorebook.put(key, value);
        infoFile.set("lore", lorebook);
    }

    public void removeLoreItem(String key) {
        lorebook.remove(key);
        infoFile.set("lore", lorebook);
    }

    public void setLorebook(Map<String, String> lorebook) {
        this.lorebook = lorebook;
        infoFile.set("lore", lorebook);
    }

    public InfoFile getInfoFile() {
        return infoFile;
    }

    public static ImageOverlay getUserAvatar(String iconPath, double width, double height) {
        File file = new File(iconPath);
        if (!file.exists()) {
            file = new File(App.getAppDirectory(), "icons/avatar.png");
        }

        if (!file.exists()) {
            return null;
        }

        ImageLoader loader = new ImageLoader(file);
        ImageOverlay overlay = new ImageOverlay(loader);
        overlay.setWidth(width);
        overlay.setHeight(height);
        return overlay;
    }

    public void copy(User user) {
        setDisplayName(user.getDisplayName());
        setPersona(user.getPersona());
        setIconPath(user.getIconPath());
        setLorebook(user.getLorebook());
    }

}
