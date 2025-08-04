package me.piitex.app.views.characters;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import com.drew.lang.annotations.Nullable;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.backend.server.Server;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.configuration.InfoFile;
import me.piitex.app.views.HomeView;
import me.piitex.app.views.SidebarView;
import me.piitex.app.views.characters.tabs.*;
import me.piitex.engine.Container;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.DialogueContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.containers.tabs.TabsContainer;
import me.piitex.app.backend.Character;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonBuilder;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.MessageOverlay;
import org.fxmisc.richtext.StyledTextArea;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;

public class CharacterEditView {
    private Container root;

    private InfoFile infoFile;

    @Nullable
    private Character character;
    @Nullable
    private User user;

    private boolean duplicate = false;

    private File characterIconPath;
    private File userIconPath;
    private String characterId = "";
    private String characterDisplay = "";
    private String characterPersona = "";
    private String userDisplay = "";
    private String userPersona = "";
    private String chatFirstMessage = "";
    private String chatScenario = "";
    private int chatContextSize = 4096;

    private final Map<String, String> loreItems = new TreeMap<>();

    private TabsContainer tabsContainer;

    private AppSettings appSettings = App.getInstance().getAppSettings();

    private CharacterTab characterTabInstance;
    private UserTab userTabInstance;
    private ChatTab chatTabInstance;
    private ModelTab modelTabInstance;


    public CharacterEditView(@Nullable Character character) {
        this(character, false);
    }

    public CharacterEditView(@Nullable Character character, boolean duplicate) {
        this.infoFile = new InfoFile();
        this.character = character;
        this.duplicate = duplicate;
        infoFile.set("duplicate", duplicate);

        initializeFields();
        if (character != null) {
            this.user = character.getUser();
            updateFieldsFromCharacter();
        }

        if (user != null) {
            updateFieldsFromUser();
        }

        build(null);
    }

    public CharacterEditView(@Nullable Character character, @Nullable User user, @Nullable InfoFile infoFile, @Nullable Tab tab) {
        this.character = character;
        this.user = user;
        this.infoFile = (infoFile != null) ? infoFile : new InfoFile();

        initializeFields();

        if (character != null) {
            updateFieldsFromCharacter();
        }

        if (user != null) {
            updateFieldsFromUser();
        }

        if (infoFile != null) {
            updateFieldsFromInfoFile();
        }

        build(tab);
    }

    private void initializeFields() {
        characterId = "";
        characterDisplay = "";
        characterPersona = "";
        characterIconPath = new File(App.getAppDirectory(), "icons/character.png");

        userDisplay = "";
        userPersona = "";
        userIconPath = new File(App.getAppDirectory(), "icons/character.png");

        chatFirstMessage = "";
        chatScenario = "";
        chatContextSize = 4096;
        loreItems.clear();
    }

    private void updateFieldsFromCharacter() {
        this.characterId = character.getId();
        this.characterDisplay = character.getDisplayName();
        this.characterPersona = character.getPersona();
        this.characterIconPath = new File(character.getIconPath());
        this.loreItems.clear();
        this.loreItems.putAll(character.getLorebook());
        this.user = character.getUser();
        this.chatFirstMessage = character.getFirstMessage();
        this.chatScenario = character.getChatScenario();
        this.chatContextSize = character.getChatContext();
    }

    private void updateFieldsFromInfoFile() {
        if (infoFile.hasKey("character-id")) {
            characterId = infoFile.get("character-id");
        }
        if (infoFile.hasKey("character-display")) {
            characterDisplay = infoFile.get("character-display");
        }
        if (infoFile.hasKey("character-persona")) {
            characterPersona = infoFile.get("character-persona");
        }
        if (infoFile.hasKey("icon-path")) {
            characterIconPath = new File(infoFile.get("icon-path"));
        }
        if (infoFile.hasKey("user-id")) {
            user = App.getInstance().getUser(infoFile.get("user-id"));
        }
        if (infoFile.hasKey("user-display")) {
            userDisplay = infoFile.get("user-display");
        }
        if (infoFile.hasKey("user-persona")) {
            userPersona = infoFile.get("user-persona");
        }
        if (infoFile.hasKey("icon-path-user")) {
            userIconPath = new File(infoFile.get("icon-path-user"));
        }
        if (infoFile.hasKey("lore")) {
            loreItems.clear();
            loreItems.putAll(infoFile.getStringMap("lore"));
        }
        if (infoFile.hasKey("first-message")) {
            chatFirstMessage = infoFile.get("first-message");
        }
        if (infoFile.hasKey("scenario")) {
            chatScenario = infoFile.get("scenario");
        }
        if (infoFile.hasKey("chat-context-size")) {
            chatContextSize = infoFile.getInteger("chat-context-size");
        }
        if (infoFile.hasKey("duplicate")) {
            duplicate = infoFile.getBoolean("duplicate");
        }
    }

    private void updateFieldsFromUser() {
        if (user != null) {
            userDisplay = user.getDisplayName();
            userPersona = user.getPersona();
            userIconPath = new File(user.getIconPath());
        }
    }

    public void updateInfoData() {
        infoFile.set("duplicate", duplicate);
        infoFile.set("character-id", characterId);
        infoFile.set("character-display", characterDisplay);
        if (characterIconPath != null && characterIconPath.isFile()) {
            infoFile.set("icon-path", characterIconPath.getAbsolutePath());
        }
        infoFile.set("character-persona", characterPersona);
        infoFile.set("user-display", userDisplay);
        infoFile.set("user-persona", userPersona);
        if (userIconPath != null && !userIconPath.isFile()) {
            infoFile.set("icon-path-user", userIconPath.getAbsolutePath());
        }
        infoFile.set("lore", loreItems);
        infoFile.set("first-message", chatFirstMessage);
        infoFile.set("scenario", chatScenario);
        infoFile.set("chat-context-size", chatContextSize);
    }

    public void build(@Nullable Tab tabToSelect) {
        this.root = new EmptyContainer(0, 0, 192, appSettings.getHeight());
        root.addStyle(Styles.BG_INSET);

        HorizontalLayout mainLayout = new HorizontalLayout(appSettings.getWidth() - 100, appSettings.getHeight());
        mainLayout.addElement(new SidebarView(mainLayout, true).getRoot());
        root.addElement(mainLayout);

        VerticalLayout contentLayout = new VerticalLayout(appSettings.getWidth() - 300, appSettings.getHeight());
        mainLayout.addElement(contentLayout);

        tabsContainer = new TabsContainer(0, 0, appSettings.getWidth() - 300, appSettings.getHeight());
        contentLayout.addElement(tabsContainer);

        characterTabInstance = new CharacterTab(appSettings, character, user, duplicate, this);
        tabsContainer.addTab(characterTabInstance);

        userTabInstance = new UserTab(appSettings, infoFile, character, this);
        tabsContainer.addTab(userTabInstance);

        tabsContainer.addTab(new LorebookTab(appSettings, infoFile, this));
        chatTabInstance = new ChatTab(appSettings, infoFile,this);
        tabsContainer.addTab(chatTabInstance);

        modelTabInstance = new ModelTab(appSettings, infoFile, character, this);
        tabsContainer.addTab(modelTabInstance);

        if (tabToSelect != null) {
            tabsContainer.setSelectedTab(tabToSelect.getText());
        }

        contentLayout.addElement(buildSubmitBox());
        warnTokens();
    }

    public HorizontalLayout buildSubmitBox() {
        HorizontalLayout layout = new HorizontalLayout(appSettings.getWidth() - 300, 200);
        layout.setY(appSettings.getHeight() - 275);
        layout.setSpacing(20);
        layout.setAlignment(Pos.CENTER);

        ButtonOverlay cancel = new ButtonBuilder("cancel").setText("Cancel").build();
        cancel.addStyle(Styles.DANGER);
        cancel.addStyle(Styles.BUTTON_OUTLINED);
        layout.addElement(cancel);

        ButtonOverlay submit = new ButtonBuilder("submit").setText("Submit").build();
        submit.addStyle(Styles.SUCCESS);
        submit.addStyle(Styles.BUTTON_OUTLINED);
        layout.addElements(submit);

        cancel.onClick(event -> {
            DialogueContainer dialogueContainer = new DialogueContainer("Do you want to exit without saving?", 500, 500);

            ButtonOverlay stay = new ButtonBuilder("stay").setText("Stay").build();
            stay.setWidth(150);
            stay.addStyle(Styles.SUCCESS);
            stay.onClick(event1 -> {
                App.window.removeContainer(dialogueContainer);
            });

            ButtonOverlay leave = new ButtonBuilder("leave").setText("Leave").build();
            leave.setWidth(150);
            leave.addStyle(Styles.DANGER);
            leave.onClick(event1 -> {
                App.window.clearContainers();
                App.window.addContainer(new HomeView());
            });

            dialogueContainer.setCancelButton(stay);
            dialogueContainer.setConfirmButton(leave);

            App.window.renderPopup(dialogueContainer, PopupPosition.CENTER, 500, 500);
        });

        submit.onClick(event -> {
            if (!validate()) return;

            try {
                Character currentCharacterInstance;
                if (character == null) {
                    currentCharacterInstance = new Character(characterId);
                    App.getInstance().getCharacters().put(characterId, currentCharacterInstance);
                } else if (duplicate) {
                    currentCharacterInstance = new Character(characterId);
                    currentCharacterInstance.copy(character);
                    App.getInstance().getCharacters().put(characterId, currentCharacterInstance);
                } else {
                    currentCharacterInstance = character;
                }

                currentCharacterInstance.setDisplayName(((TextField) characterTabInstance.getCharDisplayName().getNode()).getText());
                currentCharacterInstance.setPersona(((StyledTextArea<?, ?>) characterTabInstance.getCharDescription().getNode()).getText());
                currentCharacterInstance.setLorebook(loreItems);
                currentCharacterInstance.setFirstMessage(((StyledTextArea<?, ?>) chatTabInstance.getFirstMessageInput().getNode()).getText());
                currentCharacterInstance.setChatScenario(((StyledTextArea<?, ?>) chatTabInstance.getChatScenarioInput().getNode()).getText());
                currentCharacterInstance.setChatContext(((Spinner<Double>) chatTabInstance.getChatContextSpinner().getNode()).getValue().intValue());

                currentCharacterInstance.setOverride(((ToggleSwitch) modelTabInstance.getModelOverride().getNode()).isSelected());
                currentCharacterInstance.setModel(((ComboBox) modelTabInstance.getModelSelection().getNode()).getSelectionModel().getSelectedItem().toString());

                // Handle character icon file copy
                if (characterIconPath != null && characterIconPath.exists()) {
                    File output = new File(currentCharacterInstance.getCharacterDirectory(), "character.png");
                    Files.copy(characterIconPath.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    currentCharacterInstance.setIconPath(output.getAbsolutePath());
                }

                this.character = currentCharacterInstance;

                if (user != null) {
                    InfoFile characterUserInfoFile = new InfoFile(new File(currentCharacterInstance.getUserDirectory(), "user.info"), true);
                    User characterSpecificUser = new User(characterUserInfoFile);

                    characterSpecificUser.setDisplayName(userDisplay);
                    characterSpecificUser.setPersona(userPersona);

                    if (userIconPath != null && userIconPath.exists()) {
                        File output = new File(currentCharacterInstance.getUserDirectory(), userIconPath.getName());
                        Files.copy(userIconPath.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        characterSpecificUser.setIconPath(output.getAbsolutePath());
                    }

                    character.setUser(characterSpecificUser);
                } else {
                    user = new User(((TextField) userTabInstance.getUserDisplayNameInput().getNode()).getText(), new InfoFile(new File(currentCharacterInstance.getUserDirectory(), "user.info"), true));
                    user.setDisplayName(((TextField) userTabInstance.getUserDisplayNameInput().getNode()).getText());
                    user.setPersona(((StyledTextArea) userTabInstance.getUserDescription().getNode()).getText());
                    character.setUser(user);
                }

                App.window.clearContainers();
                App.window.addContainer(new HomeView());
            } catch (IOException e) {
                App.logger.error("Failed to save character: ", e);
            }
        });

        return layout;
    }

    public boolean validate() {
        if (characterId.isEmpty() || ((TextField) characterTabInstance.getCharIdInput().getNode()).getText().isEmpty()) {
            tabsContainer.getTabPane().getSelectionModel().select(characterTabInstance.getJfxTab());
            MessageOverlay required = new MessageOverlay(0, 0, 600, 100, "Character ID", "Character ID is required.");
            required.addStyle(Styles.WARNING);
            required.addStyle(Styles.BG_DEFAULT);
            App.window.renderPopup(required, PopupPosition.CENTER, 600, 100, true);
            characterTabInstance.getCharIdInput().getNode().requestFocus();
            return false;
        }
        if ((character == null || (duplicate && !character.getId().equals(characterId))) && App.getInstance().containsCharacter(characterId)) {
            tabsContainer.getTabPane().getSelectionModel().select(characterTabInstance.getJfxTab());
            MessageOverlay required = new MessageOverlay(0, 0, 600, 100, "Character ID", "Character ID already exists!");
            required.addStyle(Styles.WARNING);
            required.addStyle(Styles.BG_DEFAULT);
            App.window.renderPopup(required, PopupPosition.CENTER, 600, 100, true);
            characterTabInstance.getCharIdInput().getNode().requestFocus();
            return false;
        }
        if (characterDisplay.isEmpty() || ((TextField) characterTabInstance.getCharDisplayName().getNode()).getText().isEmpty()) {
            tabsContainer.getTabPane().getSelectionModel().select(characterTabInstance.getJfxTab());
            MessageOverlay required = new MessageOverlay(0, 0, 600, 100, "Character Display Name", "Character display name is required.");
            required.addStyle(Styles.WARNING);
            required.addStyle(Styles.BG_DEFAULT);
            App.window.renderPopup(required, PopupPosition.CENTER, 600, 100, true);
            characterTabInstance.getCharDisplayName().getNode().requestFocus();
            return false;
        }

        if (userDisplay.isEmpty() || ((TextField) userTabInstance.getUserDisplayNameInput().getNode()).getText().isEmpty()) {
            tabsContainer.getTabPane().getSelectionModel().select(userTabInstance.getJfxTab());
            MessageOverlay required = new MessageOverlay(0, 0, 600, 100, "User Display Name", "User display name is required.");
            required.addStyle(Styles.WARNING);
            required.addStyle(Styles.BG_DEFAULT);
            App.window.renderPopup(required, PopupPosition.CENTER, 600, 100, true);
            userTabInstance.getUserDisplayNameInput().getNode().requestFocus();
            return false;
        }

        try {
            int contextSize = ((Spinner<Double>) chatTabInstance.getChatContextSpinner().getNode()).getValue().intValue();
            if (contextSize <= 0) {
                tabsContainer.getTabPane().getSelectionModel().select(chatTabInstance.getJfxTab());
                MessageOverlay error = new MessageOverlay(0, 0, 500, 50, "Invalid Input", "Context size must be a positive number.");
                error.addStyle(Styles.DANGER);
                error.addStyle(Styles.BG_DEFAULT);
                App.window.renderPopup(error, PopupPosition.CENTER, 500, 50, false, null);
                ((Spinner<Double>) chatTabInstance.getChatContextSpinner().getNode()).requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            tabsContainer.getTabPane().getSelectionModel().select(chatTabInstance.getJfxTab());
            MessageOverlay error = new MessageOverlay(0, 0, 500, 50, "Invalid Input", "Please enter a valid number for context size.");
            error.addStyle(Styles.DANGER);
            error.addStyle(Styles.BG_DEFAULT);
            App.window.renderPopup(error, PopupPosition.CENTER, 500, 50, false, null);
            ((Spinner<Double>) chatTabInstance.getChatContextSpinner().getNode()).requestFocus();
            return false;
        }

        return true;
    }

    public void warnTokens() {
        ServerProcess currentServer = ServerProcess.getCurrentServer();
        if (currentServer == null || (!currentServer.isAlive() && !currentServer.isLoading())) {
            Platform.runLater(() -> {
                MessageOverlay configWarning = new MessageOverlay(0, 0, 600, 100, "Server Not Configured", "Backend server is not configured. Please set up your model in settings.");
                configWarning.addStyle(Styles.WARNING);
                configWarning.addStyle(Styles.BG_DEFAULT);
                App.window.renderPopup(configWarning, 650, 870, 600, 100, false, null);
            });
            return;
        }

        if (currentServer.isLoading()) {
            App.logger.info("Backend server is still loading. Retrying token check in 10 seconds...");
            Platform.runLater(() -> {
                PauseTransition delay = new PauseTransition(Duration.seconds(10));
                delay.setOnFinished(event -> warnTokens());
                delay.play();
            });
            return;
        }

        StringBuilder textToTokenize = new StringBuilder();
        if (characterPersona != null && !characterPersona.isEmpty()) {
            textToTokenize.append(characterPersona).append("\n");
        }
        if (userPersona != null && !userPersona.isEmpty()) {
            textToTokenize.append(userPersona).append("\n");
        }
        if (chatFirstMessage != null && !chatFirstMessage.isEmpty()) {
            textToTokenize.append(chatFirstMessage).append("\n");
        }
        if (chatScenario != null && !chatScenario.isEmpty()) {
            textToTokenize.append(chatScenario).append("\n");
        }
        loreItems.forEach((key, value) -> textToTokenize.append(key).append(": ").append(value).append("\n"));

        App.getThreadPoolManager().submitTask(() -> {
            try {
                int tokenSize = Server.tokenize(textToTokenize.toString());

                Platform.runLater(() -> {
                    if (tokenSize > (chatContextSize / 2)) {
                        MessageOverlay tokenWarning = new MessageOverlay(0, 0, 500, 50, "Token Size", "Your character uses more context than you have configured. (" + tokenSize + "/" + chatContextSize + ")");
                        tokenWarning.addStyle(Styles.WARNING);
                        tokenWarning.addStyle(Styles.BG_DEFAULT);
                        tokenWarning.setIcon(new FontIcon(Material2MZ.OUTLINED_FLAG));
                        App.window.renderPopup(tokenWarning, 650, 870, 500, 50, false, null);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    App.logger.error("Error during tokenization: ", e);
                    MessageOverlay errorOverlay = new MessageOverlay(0, 0, 500, 50, "Tokenization Failed", "Could not calculate token size: " + e.getMessage() + ". Please check server status.");
                    errorOverlay.addStyle(Styles.DANGER);
                    errorOverlay.addStyle(Styles.BG_DEFAULT);
                    App.window.renderPopup(errorOverlay, 650, 870, 500, 50, false, null);
                });
            }
        });
    }

    public Container getRoot() {
        return root;
    }

    public File getCharacterIconPath() {
        return characterIconPath;
    }

    public File getUserIconPath() {
        return userIconPath;
    }

    public String getCharacterId() {
        return characterId;
    }

    public String getCharacterDisplay() {
        return characterDisplay;
    }

    public String getCharacterPersona() {
        return characterPersona;
    }

    public String getUserDisplay() {
        return userDisplay;
    }

    public void setCharacterPersona(String characterPersona) {
        this.characterPersona = characterPersona;
    }

    public String getUserPersona() {
        return userPersona;
    }

    public String getChatFirstMessage() {
        return chatFirstMessage;
    }

    public String getChatScenario() {
        return chatScenario;
    }

    public int getChatContextSize() {
        return chatContextSize;
    }

    public Map<String, String> getLoreItems() {
        return loreItems;
    }

    public Character getCharacter() {
        return character;
    }

    public User getUser() {
        return user;
    }


    public void setCharacterIconPath(File characterIconPath) {
        this.characterIconPath = characterIconPath;
    }

    public void setUserIconPath(File userIconPath) {
        this.userIconPath = userIconPath;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    public void setCharacterDisplay(String characterDisplay) {
        this.characterDisplay = characterDisplay;
    }

    public void setUserDisplay(String userDisplay) {
        this.userDisplay = userDisplay;
    }

    public void setUserPersona(String userPersona) {
        this.userPersona = userPersona;
    }

    public void setChatFirstMessage(String chatFirstMessage) {
        this.chatFirstMessage = chatFirstMessage;
    }

    public void setChatScenario(String chatScenario) {
        this.chatScenario = chatScenario;
    }

    public void setChatContextSize(int chatContextSize) {
        this.chatContextSize = chatContextSize;
    }

    public InfoFile getInfoFile() {
        return infoFile;
    }

    public void setLoreItems(Map<String, String> loreItems) {
        this.loreItems.clear();
        this.loreItems.putAll(loreItems);
    }

    public void setUser(User user) {
        this.user = user;
    }
}
