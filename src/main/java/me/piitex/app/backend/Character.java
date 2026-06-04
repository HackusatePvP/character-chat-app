package me.piitex.app.backend;

import me.piitex.app.App;
import me.piitex.app.configuration.ModelSettings;
import me.piitex.app.views.chats.ChatView;
import me.piitex.engine.maps.LimitedHashMap;
import me.piitex.os.configurations.InfoFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class Character {
    private final String id;
    private String displayName = "";
    private String persona = "";
    private String iconPath = "";
    
    @Nullable
    private User user = null;

    private String firstMessage = "";
    private String chatScenario = "";

    private boolean override = false;
    private String model;
    private ModelSettings modelSettings;
    private InfoFile infoFile;

    private Map<String, String> lorebook = new TreeMap<>();

    // sender, dialogue
    // Assistant: "How can I help you?" *I asked pointedly.*
    private Map<String, String> exampleDialogue = new TreeMap<>();

    private final List<Chat> chats = new ArrayList<>();
    private final Map<Chat, ChatView> chatViewCachedNodes = new LimitedHashMap<>(5);

    private boolean shownDisclaimer = false;

    public Character(String id) {
        this.id = id;
        initializeDirectories();
        loadChats();
    }

    public void initializeDirectories() {
        if (getCharacterDirectory().mkdirs()) {
            App.logger.info("Created base character directory '{}'", getCharacterDirectory().getAbsolutePath());
        }
        if (getUserDirectory().mkdirs()) {
            App.logger.info("Created base user directory '{}'", getUserDirectory().getAbsolutePath());
        }
        if (getChatDirectory().mkdirs()) {
            App.logger.info("Created base chat directory '{}'", getCharacterDirectory().getAbsolutePath());
        }

        this.infoFile = new InfoFile(new File(getCharacterDirectory(), "character.info"), true);
        this.modelSettings = new ModelSettings(new InfoFile(new File(getCharacterDirectory(), "model-settings.info"), false));
    }

    public Character(String id, @Nullable InfoFile infoFile) {
        this.id = id;

        if (infoFile == null) {
            infoFile = new InfoFile();
        }

        loadChats();

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
        if (infoFile.hasKey("first-message")) {
            this.firstMessage = infoFile.get("first-message");
        }
        if (infoFile.hasKey("chat-scenario")) {
            this.chatScenario = infoFile.get("chat-scenario");
        }
        if (infoFile.hasKey("lore")) {
            this.lorebook = infoFile.getStringMap("lore");
        }
        if (infoFile.hasKey("dialogue")) {
            this.exampleDialogue = infoFile.getStringMap("dialogue");
        }
        if (infoFile.hasKey("disclaimer")) {
            this.shownDisclaimer = infoFile.getBoolean("disclaimer");
        } else {
            infoFile.set("disclaimer", shownDisclaimer);
        }

        File userFile = new File(getUserDirectory(), "user.info");
        if (userFile.exists()) {
            InfoFile userInfo = new InfoFile(userFile, true);
            user = new User(userInfo);
        }

        if (infoFile.hasKey("override")) {
            this.override = infoFile.getBoolean("override");
        }
        if (infoFile.hasKey("model")) {
            this.model = infoFile.get("model");
        }
        File modelFile = new File(getCharacterDirectory(), "model-settings.info");
        if (modelFile.exists()) {
            modelSettings = new ModelSettings(new InfoFile(modelFile, false));
        }
    }

    private void loadChats() {
        if (getChatDirectory() == null || !getChatDirectory().exists()) return;
        for (File file : getChatDirectory().listFiles()) {
            if (file.isDirectory()) continue;
            Chat chat = new Chat(file);
            chats.add(chat);
        }
    }

    public List<String> getChatFileNames() {
        List<String> toReturn = new ArrayList<>();
        for (Chat chat : chats) {
            toReturn.add(chat.getFile().getName());
        }
        return toReturn;
    }

    public List<Chat> getChats() {
        return chats;
    }

    public Chat getChat(File file) {
        return chats.stream().filter(chat -> chat.getFile().getName().equalsIgnoreCase(file.getName())).findAny().orElse(null);
    }

    public Chat getChat(String name) {
        return chats.stream().filter(chat -> chat.getFile().getName().equalsIgnoreCase(name)).findAny().orElse(null);
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
        if (iconPath.isEmpty()) {
            iconPath = new File(getCharacterDirectory(), "character.png").getAbsolutePath();
        }
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
        infoFile.set("icon-path", iconPath);
    }

    public boolean isShownDisclaimer() {
        return shownDisclaimer;
    }

    public void setShownDisclaimer(boolean shownDisclaimer) {
        this.shownDisclaimer = shownDisclaimer;
        infoFile.set("disclaimer", shownDisclaimer);
    }

    @Nullable
    public User getUser() {
        return user;
    }

    public void setUser(@Nullable User user) {
        this.user = user;
    }

    public String getFirstMessage() {
        return firstMessage;
    }

    public void setFirstMessage(String firstMessage) {
        this.firstMessage = firstMessage;
        infoFile.set("first-message", firstMessage);
    }

    public String getChatScenario() {
        return chatScenario;
    }

    public void setChatScenario(String chatScenario) {
        this.chatScenario = chatScenario;
        infoFile.set("chat-scenario", chatScenario);
    }

    public Chat getLastChat() {
        return chats.stream().filter(chat -> chat.getFile().getName().equalsIgnoreCase(infoFile.get("last-chat"))).findAny().orElse(null);
    }

    public void setLastChat(Chat lastChat) {
        infoFile.set("last-chat", lastChat.getFile().getName());
    }

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
        infoFile.set("override", override);
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
        infoFile.set("model", model);
    }

    public ModelSettings getModelSettings() {
        return modelSettings;
    }

    public void setModelSettings(ModelSettings modelSettings) {
        this.modelSettings = modelSettings;
    }

    public Map<String, String> getLorebook() {
        return lorebook;
    }

    public void setLorebook(Map<String, String> lorebook) {
        this.lorebook = lorebook;
        infoFile.set("lore", lorebook);
    }

    public Map<String, String> getExampleDialogue() {
        return exampleDialogue;
    }

    public void setExampleDialogue(Map<String, String> exampleDialogue) {
        this.exampleDialogue = exampleDialogue;
        infoFile.set("dialogue", exampleDialogue);
    }

    public File getCharacterDirectory() {
        return new File(App.getCharactersDirectory(), id + "/");
    }

    public File getUserDirectory() {
        return new File(getCharacterDirectory(), "user/");
    }

    public File getChatDirectory() {
        return new File(getCharacterDirectory(), "chats/");
    }

    public InfoFile getInfoFile() {
        return infoFile;
    }

    public Map<Chat, ChatView> getChatViewCachedNodes() {
        return chatViewCachedNodes;
    }

    public void copyFrom(Character character) {
        setDisplayName(character.getDisplayName());
        setPersona(character.getPersona());
        setUser(character.getUser());
        setIconPath(character.getIconPath());
        setLorebook(character.getLorebook());
        setFirstMessage(character.getFirstMessage());
        setChatScenario(character.getChatScenario());
        // Note: Chats are not copied.
    }
}
