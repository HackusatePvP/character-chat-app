package me.piitex.app.views.characters;


import atlantafx.base.theme.Styles;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.annotations.Nullable;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import javafx.stage.FileChooser;
import javafx.util.Duration;
import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.User;
import me.piitex.app.backend.server.Server;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.configuration.InfoFile;
import me.piitex.app.utils.CharacterCardImporter;
import me.piitex.app.views.HomeView;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.Container;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.DialogueContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.containers.tabs.TabsContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.*;
import org.json.JSONObject;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class CharacterEditView {
    private Container root;

    private InfoFile infoFile;

    @Nullable private Character character;
    @Nullable private User user;

    private boolean duplicate = false;

    /* Tabs to be saved so rendering can work again */
    private Tab characterTab = null;
    private Tab userTab = null;

    // Fields for boxes
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

    /*
        Required fields for validation and importing
     */
    private InputFieldOverlay charIdInput;
    private InputFieldOverlay charDisplayName;
    private TextAreaOverlay charDescription;
    private TextAreaOverlay firstMessageInput;
    private TextAreaOverlay chatScenarioInput;

    private InputFieldOverlay userDisplayName;

    private final Map<String, String> loreItems = new TreeMap<>();

    private TabsContainer container;

    private AppSettings appSettings = App.getInstance().getAppSettings();

    public CharacterEditView(@Nullable Character character) {
        this.infoFile = new InfoFile();
        this.character = character;
        if (character != null) {
            updateCharacterFields();
            warnTokens();
        }
        build(null);

        warnTokens();
    }

    public CharacterEditView(@Nullable Character character, boolean duplicate) {
        this.infoFile = new InfoFile();
        this.character = character;
        this.duplicate = duplicate;
        infoFile.set("duplicate", duplicate);
        if (character != null) {
            updateCharacterFields();
        }
        build(null);

        warnTokens();
    }

    public CharacterEditView(@Nullable Character character, @Nullable User user, @Nullable InfoFile infoFile, @Nullable Tab tab) {
        this.character = character;
        this.infoFile = infoFile;
        this.user = user;
        if (character != null) {
            updateCharacterFields();
        }
        if (infoFile != null) {
            updateInfoFields();
        }
        build(tab);

        warnTokens();
    }

    public void build(@Nullable Tab tab) {
        if (user != null) {
            updateUserFields();
        } else {
            if (character != null && character.getUser() != null) {
                user = character.getUser();
                updateUserFields();
            }
        }

        this.root = new EmptyContainer(0, 0, 192, appSettings.getHeight());

        HorizontalLayout layout = new HorizontalLayout(appSettings.getWidth() - 100, appSettings.getHeight());
        layout.addElement(new SidebarView().getRoot());
        root.addElement(layout);

        VerticalLayout main = new VerticalLayout(appSettings.getWidth() - 300, appSettings.getHeight());
        layout.addElement(main);

        // Add the views
        container = new TabsContainer(0, 0, appSettings.getWidth() - 300, appSettings.getHeight());
        main.addElement(buildTopTab());

        if (tab != null) {
            container.setSelectedTab(tab.getText());
        }
    }

    public TabsContainer buildTopTab() {
        Tab character = buildCharacterTab();
        container.addTab(character);

        Tab user = buildUserTab();
        container.addTab(user);

        Tab lorebook = buildLorebookTab();
        container.addTab(lorebook);

        Tab chat = buildChatTab();
        container.addTab(chat);

        Tab model = buildModelTab();
        container.addTab(model);

        return container;
    }

    public Tab buildCharacterTab() {
        characterTab = new Tab("Character");
        characterTab.setWidth(appSettings.getWidth() - 300);
        characterTab.setHeight(appSettings.getHeight());

        VerticalLayout root = new VerticalLayout(appSettings.getWidth() - 300 , appSettings.getHeight());
        root.setSpacing(50);
        root.setAlignment(Pos.TOP_CENTER);
        characterTab.addElement(root);

        HorizontalLayout displayBox = new HorizontalLayout(500, 200);
        displayBox.setMaxSize(500, 200);
        displayBox.addStyle(Styles.BORDER_SUBTLE);
        displayBox.setSpacing(20);
        root.addElement(displayBox);

        CardContainer displayCard = buildCharacterDisplay();
        displayBox.addElement(displayCard);
        displayBox.addElement(buildCharacterInput());

        double scaleFactor = (double) appSettings.getWidth() / 1920.0;
        charDescription = new TextAreaOverlay((character != null ? character.getPersona() : characterPersona),0, 0, 600, 400 * scaleFactor);
        charDescription.setHintText("Describe the character and provide key lore.");
        root.addElement(charDescription);
        charDescription.onInputSetEvent(event -> {
            characterPersona = event.getInput();
            infoFile.set("character-persona", characterPersona);
            warnTokens();
        });

        // Make this a file chooser
        ButtonOverlay importCard = new ButtonOverlay("import", "Import Character Card");
        if (character != null) {
            importCard.setEnabled(false);
        }
        importCard.addStyle(Styles.ACCENT);
        importCard.addStyle(Styles.BUTTON_OUTLINED);
        importCard.setWidth(400);


        FileChooserOverlay fileSelector = new FileChooserOverlay(App.window, importCard);
        root.addElement(fileSelector);
        fileSelector.onFileSelect(event -> {
            // Import the card metadata using CharacterCardImporter
            File file = event.getDirectory(); // This is a file not a directory. The naming can be misleading
            try {
                JSONObject metadata = CharacterCardImporter.getImageMetaData(file);
                infoFile.set("character-id", CharacterCardImporter.getCharacterId(metadata));
                infoFile.set("character-display", CharacterCardImporter.getCharacterDisplayName(metadata));
                infoFile.set("character-persona", CharacterCardImporter.getCharacterPersona(metadata));
                infoFile.set("lore", CharacterCardImporter.getLoreItems(metadata));
                infoFile.set("first-message", CharacterCardImporter.getFirstMessage(metadata));
                infoFile.set("scenario", CharacterCardImporter.getChatScenario(metadata));
                infoFile.set("icon-path", file.getAbsolutePath());

                loreItems.clear();
                loreItems.putAll(CharacterCardImporter.getLoreItems(metadata));

                App.window.clearContainers();
                App.window.addContainer(new CharacterEditView(character, user, infoFile, characterTab).getRoot());
                App.window.render();

            } catch (ImageProcessingException | IOException e) {
                throw new RuntimeException(e);
            }

        });

        characterTab.addElement(buildSubmitBox());

        return characterTab;
    }

    public CardContainer buildCharacterDisplay() {
        CardContainer root = new CardContainer(0, 0,200, 200);
        root.setMaxSize(200, 200);

        VerticalLayout layout = new VerticalLayout(200,200);
        layout.setMaxSize(200, 200);

        layout.setX(-10);
        layout.setY(-10);
        root.setBody(layout);
        layout.setAlignment(Pos.BASELINE_CENTER);
        layout.setSpacing(25);

        if (characterIconPath == null) {
            characterIconPath = new File(App.getAppDirectory(), "icons/character.png");
        }

        ImageOverlay image = new ImageOverlay(new ImageLoader(characterIconPath));
        image.setWidth(128);
        image.setHeight(128);
        image.setPreserveRatio(false);
        layout.addElement(image, 2);

        TextOverlay upload = new TextOverlay("Click to upload image");
        upload.setTextFill(Color.WHITE);
        upload.setUnderline(true);
        layout.addElement(upload, 5);


        root.onClick(event -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Select an image.", "*.img", "*.png", "*.webp", "*.jpg"));
            File directory = chooser.showOpenDialog(App.window.getStage());
            if (directory != null) {
                // This is the full directory of the selected file.
                characterIconPath = directory;
                infoFile.set("icon-path", directory.getAbsolutePath());
                App.window.clearContainers();
                App.window.addContainer(new CharacterEditView(character, user, infoFile, characterTab).getRoot());
                App.window.render();
            }
        });

        return root;
    }

    public VerticalLayout buildCharacterInput() {
        VerticalLayout root = new VerticalLayout(250, 150);
        root.setAlignment(Pos.BASELINE_CENTER);
        root.setMaxSize(250, 200);
        root.setSpacing(10);

        charIdInput = new InputFieldOverlay(characterId, 0, 0, 200, 50);
        if (character != null && !duplicate) {
            charIdInput.setEnabled(false);
        }
        charIdInput.setHintText("Character ID (Must be unique)");
        charIdInput.onInputSetEvent(event -> {
            characterId = event.getInput();
            infoFile.set("character-id", characterId);
        });
        root.addElement(charIdInput);

        charDisplayName = new InputFieldOverlay((character != null ? character.getDisplayName() : characterDisplay), 0, 0, 200, 50);
        charDisplayName.setHintText("Display Name");
        charDisplayName.onInputSetEvent(event -> {
            characterDisplay = event.getInput();
            infoFile.set("character-display", characterDisplay);
        });
        root.addElement(charDisplayName);

        return root;
    }

    public Tab buildUserTab() {
        userTab = new Tab("User");
        userTab.setWidth(appSettings.getWidth() - 300);
        userTab.setHeight(appSettings.getHeight());

        VerticalLayout root = new VerticalLayout(appSettings.getWidth() - 300 , appSettings.getHeight());
        root.setSpacing(50);
        root.setAlignment(Pos.TOP_CENTER);
        userTab.addElement(root);

        HorizontalLayout displayBox = new HorizontalLayout(500, 200);
        displayBox.setMaxSize(500, 200);
        displayBox.addStyle(Styles.BORDER_SUBTLE);
        displayBox.setSpacing(20);
        root.addElement(displayBox);

        CardContainer displayCard = buildUserDisplay();
        displayBox.addElement(displayCard);
        displayBox.addElement(buildUserInput());

        double scaleFactor = (double) appSettings.getWidth() / 1920.0;
        TextAreaOverlay userDescription = new TextAreaOverlay(userPersona,0, 0, 600, 400 * scaleFactor);
        userDescription.setHintText("Describe the user and provide key lore.");
        root.addElement(userDescription);
        userDescription.onInputSetEvent(event -> {
            userPersona = event.getInput();
            infoFile.set("user-persona", userPersona);
            warnTokens();
        });

        userTab.addElement(buildSubmitBox());

        return userTab;
    }

    public CardContainer buildUserDisplay() {
        CardContainer root = new CardContainer(0, 0,200, 200);
        root.setMaxSize(200, 200);

        VerticalLayout layout = new VerticalLayout(200, 200);
        layout.setX(-10);
        layout.setY(-10);
        root.setBody(layout);
        layout.setAlignment(Pos.BASELINE_CENTER);
        layout.setSpacing(25);

        if (userIconPath == null ||!userIconPath.exists()) {
            userIconPath = new File(App.getAppDirectory(), "icons/character.png");
        }

        ImageOverlay image = new ImageOverlay(new ImageLoader(userIconPath));
        image.setWidth(128);
        image.setHeight(128);
        image.setPreserveRatio(false);
        layout.addElement(image, 2);

        TextOverlay upload = new TextOverlay("Click to upload image");
        upload.setTextFill(Color.WHITE);
        upload.setUnderline(true);
        layout.addElement(upload, 5);


        root.onClick(event -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Select an image.", "*.img", "*.png", "*.webp", "*.jpg"));
            File directory = chooser.showOpenDialog(App.window.getStage());
            if (directory != null) {
                // This is the full directory of the selected file.
                userIconPath = directory;
                infoFile.set("icon-path", directory.getAbsolutePath());
                App.window.clearContainers();
                App.window.addContainer(new CharacterEditView(character, user, infoFile, userTab).getRoot());
                App.window.render();
            }
        });

        return root;
    }

    public VerticalLayout buildUserInput() {
        VerticalLayout root = new VerticalLayout(250, 150);
        root.setAlignment(Pos.BASELINE_CENTER);
        root.setMaxSize(250, 200);
        root.setSpacing(10);

        List<String> users = new ArrayList<>();
        users.add("None");
        users.addAll(App.getInstance().getUserTemplates().keySet());

        ChoiceBoxOverlay templates = new ChoiceBoxOverlay(users, 200, 50);
        root.addElement(templates);

        userDisplayName = new InputFieldOverlay((user != null ? user.getDisplayName() : userDisplay), 0, 0, 200, 50);
        userDisplayName.setHintText("Display Name");
        userDisplayName.onInputSetEvent(event -> {
            userDisplay = event.getInput();
            infoFile.set("user-display", userDisplay);
        });
        root.addElement(userDisplayName);

        templates.onItemSelect(event -> {
            String item = event.getItem();
            if (item.isEmpty()) return;
            if (item.equalsIgnoreCase("none")) {
                user = null;
            } else {
                user = App.getInstance().getUser(item);
            }

            // Also want to render the user tab
            App.window.clearContainers();
            App.window.addContainer(new CharacterEditView(character, user, infoFile, userTab).getRoot());
            App.window.render();
        });

        return root;
    }

    public Tab buildLorebookTab() {
        Tab tab = new Tab("Lorebook");

        VerticalLayout root = new VerticalLayout(appSettings.getWidth() - 300 , appSettings.getHeight());
        root.setSpacing(50);
        root.setAlignment(Pos.TOP_CENTER);
        tab.addElement(root);

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Use the following placeholders; {char}, {{char}}, {chara}, {{chara}}, {character}, {{character}}, {user}, {{user}}, {usr}, {{usr}}");
        info.setX(200);
        info.setY(10);
        root.addElement(info);

        HorizontalLayout displayBox = new HorizontalLayout(0, 0);
        displayBox.setSpacing(100);
        displayBox.setMaxSize(600, 0);
        root.addElement(displayBox);

        CardContainer addContainer = new CardContainer(0, 0, 400, 400);
        addContainer.setMaxSize(400, 400);

        InputFieldOverlay addKey = new InputFieldOverlay("", "Separate multiple keys with a comma (,)", 0, 0, 200, 50);
        addKey.setId("key");
        addContainer.setHeader(addKey);

        TextAreaOverlay addValue = new TextAreaOverlay("", "Enter the lore info", 0, 0, 400, 200);
        addKey.setId("value");
        addContainer.setBody(addValue);

        HorizontalLayout buttonBox = new HorizontalLayout(200, 50);
        buttonBox.setAlignment(Pos.CENTER);

        ButtonOverlay add = new ButtonOverlay("add", "Add");
        add.addStyle(Styles.SUCCESS);
        add.addStyle(Styles.BUTTON_OUTLINED);
        buttonBox.addElement(add);

        addContainer.setFooter(buttonBox);

        // Got to make this the scroll pane is modifiable.
        ScrollContainer scrollLore = getLoreItems();
        add.onClick(event -> {
            if (addKey.getCurrentText().isEmpty() || addValue.getCurrentText().isEmpty()) {
                return;
            }

            ScrollPane pane = scrollLore.getScrollPane();
            if (pane == null) {
                return;
            }
            VBox vBox = (VBox) pane.getContent();
            if (vBox == null) {
                return;
            }

            // Now make card container for the added lore
            loreItems.put(addKey.getCurrentText(), addValue.getCurrentText());

            CardContainer toAdd = buildLoreEntry(addKey.getCurrentText(), (VerticalLayout) scrollLore.getLayout());
            scrollLore.getLayout().addElement(toAdd);

            Node node = toAdd.build().getKey();
            vBox.getChildren().add(node);
            // ALSO ADD TO RENDERED
            scrollLore.getLayout().getRenderedNodes().put(addKey.getCurrentText(), node);

            TextField input = (TextField) addKey.getNode();
            input.setText("");

            TextArea area = (TextArea) addValue.getNode();
            area.setText("");
        });

        displayBox.addElement(addContainer);
        displayBox.addElement(scrollLore);

        tab.addElement(buildSubmitBox());

        return tab;
    }

    public ScrollContainer getLoreItems() {
        VerticalLayout scrollLayout = new VerticalLayout(0 ,0);

        ScrollContainer root = new ScrollContainer(scrollLayout, 0, 0, 450, 0);
        root.setMaxSize(450, appSettings.getHeight() - 300);
        root.setVerticalScroll(true);
        root.setScrollWhenNeeded(false);
        root.setHorizontalScroll(false);

        App.logger.info("Building lore cache...");
        for (String key : loreItems.keySet()) {
            scrollLayout.addElement(buildLoreEntry(key, scrollLayout));
        }
        return root;
    }

    private CardContainer buildLoreEntry(String key, @Nullable VerticalLayout scrollContainer) {
        CardContainer card = new CardContainer(0, 0, 400, 300);
        card.setId(key);

        InputFieldOverlay entry = new InputFieldOverlay(key, 0, 0, 400, 50);
        card.setHeader(entry);

        TextAreaOverlay value = new TextAreaOverlay(loreItems.get(key), 0, 0, 400, 200);
        card.setBody(value);

        ButtonOverlay remove = new ButtonOverlay("remove", "Remove");
        remove.setX(170);
        remove.addStyle(Styles.DANGER);
        remove.addStyle(Styles.BUTTON_OUTLINED);
        card.setFooter(remove);

        remove.onClick(event -> {
            loreItems.remove(key);
            // Update the layout
            if (scrollContainer != null) {
                VBox vBox = (VBox) scrollContainer.getPane();
                Node toRemove = scrollContainer.getNode(key);
                if (toRemove == null) {
                    return;
                }
                vBox.getChildren().remove(toRemove);
            }
        });

        return card;
    }

    public Tab buildChatTab() {
        Tab tab = new Tab("Chat");

        int layoutSpacing = 200;

        VerticalLayout layout = new VerticalLayout(appSettings.getWidth() - 300, appSettings.getHeight() - 100);
        layout.setAlignment(Pos.TOP_CENTER);
        tab.addElement(layout);

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Use the following placeholders; {char}, {{char}}, {chara}, {{chara}}, {character}, {{character}}, {user}, {{user}}, {usr}, {{usr}}");
        layout.addElement(info);

        CardContainer firstCard = new CardContainer(0, 0, 0, 0);
        firstCard.setMaxSize(appSettings.getWidth() - 300, 200);
        layout.addElement(firstCard);

        HorizontalLayout firstBox = new HorizontalLayout(0, 0);
        firstBox.setAlignment(Pos.BASELINE_LEFT);
        firstBox.setMaxSize(appSettings.getWidth() - 300, 120);
        firstBox.setSpacing(layoutSpacing);
        firstCard.setBody(firstBox);

        TextFlowOverlay firstDesc = new TextFlowOverlay("Set the first message from the assistant.", (appSettings.getWidth() - 300) / 2, 200);
        firstDesc.setTextFillColor(Color.WHITE);
        firstBox.addElement(firstDesc);

        firstMessageInput = new TextAreaOverlay(chatFirstMessage, 0, 0, 800, 400);
        firstBox.addElement(firstMessageInput);
        firstMessageInput.onInputSetEvent(event -> {
            chatFirstMessage = event.getInput();
            infoFile.set("first-message", event.getInput());
            warnTokens();
        });

        CardContainer scenarioCard = new CardContainer(0, 0, 0, 0);
        scenarioCard.setMaxSize(appSettings.getWidth() - 300, 200);
        layout.addElement(scenarioCard);

        HorizontalLayout scenarioBox = new HorizontalLayout(0, 0);
        scenarioBox.setAlignment(Pos.BASELINE_LEFT);
        scenarioBox.setMaxSize(appSettings.getWidth() - 300, 120);
        scenarioBox.setSpacing(layoutSpacing);
        scenarioCard.setBody(scenarioBox);

        TextFlowOverlay scenarioDesc = new TextFlowOverlay("Set the chat scenario. Can be used to define the tone or story of the chat.", (appSettings.getWidth() - 300) / 2, 50);
        scenarioDesc.setTextFillColor(Color.WHITE);
        scenarioBox.addElement(scenarioDesc);

        chatScenarioInput = new TextAreaOverlay(chatScenario, 0, 0, 800, 400);
        scenarioBox.addElement(chatScenarioInput);
        chatScenarioInput.onInputSetEvent(event -> {
            chatScenario = event.getInput();
            infoFile.set("scenario", event.getInput());
            warnTokens();
        });

        CardContainer contextCard = new CardContainer(0, 0, 0, 0);
        contextCard.setMaxSize(appSettings.getWidth() - 300, 50);
        layout.addElement(contextCard);

        HorizontalLayout contextBox = new HorizontalLayout(0, 0);
        contextBox.setAlignment(Pos.BASELINE_LEFT);
        contextBox.setMaxSize(appSettings.getWidth() - 300, 50);
        contextBox.setSpacing(layoutSpacing);
        contextCard.setBody(contextBox);

        TextFlowOverlay contextDesc = new TextFlowOverlay("Set the chat scenario. Can be used to define the tone or story of the chat.", (appSettings.getWidth() - 300) / 2, 50);
        contextDesc.setTextFillColor(Color.WHITE);
        contextBox.addElement(contextDesc);

        SpinnerNumberOverlay context = new SpinnerNumberOverlay(1024, Integer.MAX_VALUE, chatContextSize);
        contextBox.addElement(context);
        context.onValueChange(event -> {
            chatContextSize = (int) event.getNewValue();
            infoFile.set("context", chatContextSize);
        });

        tab.addElement(buildSubmitBox());

        return tab;
    }

    public Tab buildModelTab() {
        Tab tab = new Tab("Model");

        TextOverlay test = new TextOverlay("Under development. Use the ' Models ' page instead.");
        tab.addElement(test);

        tab.addElement(buildSubmitBox());

        return tab;
    }

    public HorizontalLayout buildSubmitBox() {
        HorizontalLayout layout = new HorizontalLayout(appSettings.getWidth() - 300, 200);
        layout.setY(appSettings.getHeight() - 275);
        layout.setSpacing(20);
        layout.setAlignment(Pos.CENTER);

        ButtonOverlay cancel = new ButtonOverlay("cancel", "Cancel");
        cancel.addStyle(Styles.DANGER);
        cancel.addStyle(Styles.BUTTON_OUTLINED);
        layout.addElement(cancel);

        ButtonOverlay submit = new ButtonOverlay("submit", "Submit");
        submit.addStyle(Styles.SUCCESS);
        submit.addStyle(Styles.BUTTON_OUTLINED);
        layout.addElements(submit);

        cancel.onClick(event -> {
            // Confirm the cancel before leaving!!!
            DialogueContainer dialogueContainer = new DialogueContainer("Do you want to exit?", 500, 500);

            ButtonOverlay stay = new ButtonOverlay("cancel", "Stay");
            stay.setWidth(150);
            stay.addStyle(Styles.SUCCESS);
            stay.onClick(event1 -> {
                // Close dialogue
                App.window.removeContainer(dialogueContainer);
            });

            ButtonOverlay leave = new ButtonOverlay("cancel", "Leave");
            leave.setWidth(150);
            leave.addStyle(Styles.DANGER);
            leave.onClick(event1 -> {
                // Close dialogue and return to home page
                App.window.clearContainers();
                App.window.addContainer(new HomeView().getContainer());
                App.window.render();
            });

            dialogueContainer.setCancelButton(stay);
            dialogueContainer.setConfirmButton(leave);

            App.window.renderPopup(dialogueContainer, PopupPosition.CENTER, 500, 500);

        });

        submit.onClick(event -> {
            if (!validate()) return;
            // Handle creation

            // Write the files
            if (character == null) {
                character = new Character(characterId);
            }
            if (duplicate) {
                // Duplicating is a little weird.
                // Since the id is editable when duplicating it has to be reflected.
                // However, character id is a final and should not be changed after creation
                // This is how the id is used for creating the folders and files

                // Create a new character that acts as the final duplication
                // This allows us to set the chara id since it's a final.
                Character dupe = new Character(characterId);

                // Copy the duplicated character info into the new character with the configured id
                dupe.copy(character);

                // Change character into the dupe
                character = dupe;
            }

            character.setDisplayName(characterDisplay);
            character.setPersona(characterPersona);
            if (characterIconPath != null) {
                File output = new File(character.getCharacterDirectory(), "character.png");
                try {
                    Files.copy(characterIconPath.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    character.setIconPath(output.getAbsolutePath());
                }
            }
            character.setLorebook(loreItems);
            character.setFirstMessage(chatFirstMessage);
            character.setChatScenario(chatScenario);
            character.setChatContext(chatContextSize);
            if (user == null) {
                InfoFile newInfo = new InfoFile(new File(character.getUserDirectory(), "user.info"), true);
                user = new User(newInfo);
            } else {
                // Copy user template into character
                try {
                    Files.copy(user.getInfoFile().getFile().toPath(), new File(character.getUserDirectory(), "user.info").toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            user.setDisplayName(userDisplay);
            user.setPersona(userPersona);
            if (userIconPath != null) {
                File output = new File(character.getUserDirectory(), userIconPath.getName());
                try {
                    Files.copy(userIconPath.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    user.setIconPath(output.getAbsolutePath());
                }
            }
            character.setUser(user);

            App.getInstance().getCharacters().put(characterId, character);

            App.window.clearContainers();
            App.window.addContainer(new HomeView().getContainer());
            App.window.render();

        });

        return layout;
    }

    private void updateCharacterFields() {
        characterIconPath = new File(character.getIconPath());
        characterId = character.getId();
        characterDisplay = character.getDisplayName();
        characterPersona = character.getPersona();
        loreItems.putAll(character.getLorebook());

        chatFirstMessage = character.getFirstMessage();
        chatScenario = character.getChatScenario();
        chatContextSize = character.getChatContext();
    }

    private void updateUserFields() {
        userIconPath = new File(user.getIconPath());
        userDisplay = user.getDisplayName();
        userPersona = user.getPersona();
        loreItems.putAll(user.getLorebook());
    }

    public void updateInfoFields() {
        // Only call if info is not null!
        if (infoFile.hasKey("icon-path")) {
            characterIconPath = new File(infoFile.get("icon-path").replace("!@!", "\n"));
        }
        if (infoFile.hasKey("character-id")) {
            characterId = infoFile.get("character-id").replace("!@!", "\n");
        }
        if (infoFile.hasKey("character-display")) {
            characterDisplay = infoFile.get("character-display").replace("!@!", "\n");
        }
        if (infoFile.hasKey("character-persona")) {
            characterPersona = infoFile.get("character-persona").replace("!@!", "\n");
        }
        if (infoFile.hasKey("user-display")) {
            userDisplay = infoFile.get("user-display").replace("!@!", "\n");
        }
        if (infoFile.hasKey("user-persona")) {
            userPersona = infoFile.get("user-persona").replace("!@!", "\n");
        }
        if (infoFile.hasKey("lore")) {
            loreItems.putAll(infoFile.getStringMap("lore"));
        }
        if (infoFile.hasKey("first-message")) {
            chatFirstMessage = infoFile.get("first-message").replace("!@!", "\n");
        }
        if (infoFile.hasKey("scenario")) {
            chatScenario = infoFile.get("scenario").replace("!@!", "\n");
        }
        if (infoFile.hasKey("context")) {
            chatContextSize = infoFile.getInteger("context");
        }
        if (infoFile.hasKey("duplicate")) {
            duplicate = infoFile.getBoolean("duplicate");
        }
    }

    public boolean validate() {
        // When validating make sure all of the required fields are set.
        // Which are: char id, char display, user display, chat context,

        if (characterId.isEmpty() || charIdInput == null) {
            // Go to character selection, and select the char box
            container.getTabPane().getSelectionModel().select(characterTab.getJfxTab());

            // Prompt them with a warning
            MessageOverlay required = new MessageOverlay(0, 0, 600, 100,"Character ID", "Character ID is required");
            required.addStyle(Styles.WARNING);
            App.window.renderPopup(required, PopupPosition.CENTER, 600, 100, true);

            charIdInput.getNode().requestFocus();
            return false;
        }
        if (character == null && App.getInstance().containsCharacter(characterId)) {
            container.getTabPane().getSelectionModel().select(characterTab.getJfxTab());

            // Prompt them with a warning
            MessageOverlay required = new MessageOverlay(0, 0, 600, 100,"Character ID", "Character ID already exists!");
            required.addStyle(Styles.WARNING);
            App.window.renderPopup(required, PopupPosition.CENTER, 600, 100, true);

            charIdInput.getNode().requestFocus();
            return false;
        }
        if (characterDisplay.isEmpty() || charDisplayName == null) {
            // Go to character selection, and select the char box
            container.getTabPane().getSelectionModel().select(characterTab.getJfxTab());

            // Prompt them with a warning
            MessageOverlay required = new MessageOverlay(0, 0, 600, 100,"Character Display Name", "Character display name is required");
            required.addStyle(Styles.WARNING);
            App.window.renderPopup(required, PopupPosition.CENTER, 600, 100, true);

            charDisplayName.getNode().requestFocus();
            return false;
        }
        if (userDisplay.isEmpty() || userDisplayName == null) {
            // Go to character selection, and select the char box
            container.getTabPane().getSelectionModel().select(userTab.getJfxTab());

            // Prompt them with a warning
            MessageOverlay required = new MessageOverlay(0, 0, 600, 100,"User Display Name", "User display name is required");
            required.addStyle(Styles.WARNING);
            App.window.renderPopup(required, PopupPosition.CENTER, 600, 100, true);

            userDisplayName.getNode().requestFocus();
            return false;
        }


        return true;
    }

    private void warnTokens() {
        // This part of the method (checking currentServer and isLoading) can safely
        // run on any thread that calls warnTokens, as it only accesses volatile fields.
        if (character == null) return;
        ServerProcess currentServer = ServerProcess.getCurrentServer();

        // If the server is not configured (null), there's no point in retrying.
        if (currentServer == null || (!currentServer.isAlive() && !currentServer.isLoading())) {
            // UI operation: must be on JavaFX thread
            Platform.runLater(() -> {
                MessageOverlay configWarning = new MessageOverlay(0, 0, 600, 100,"Server Not Configured", "Backend server is not configured. Please set up your model in settings.");
                configWarning.addStyle(Styles.DANGER);
                App.window.renderPopup(configWarning, 650, 870, 600, 100, false, null);
            });
            return;
        }

        // If the server exists but is still loading, schedule a retry.
        if (currentServer.isLoading()) {
            App.logger.info("Backend server is still loading. Retrying token check in 10 seconds...");
            // UI operation (creating and playing PauseTransition): must be on JavaFX thread
            Platform.runLater(() -> {
                PauseTransition delay = new PauseTransition(Duration.seconds(10)); // Increased delay for potentially long loading times
                delay.setOnFinished(event -> {
                    warnTokens(); // Recursively call this method after delay
                });
                delay.play();
            });
            return; // Exit this call if server is loading
        }

        // Server is configured and not loading, proceed with tokenization on a background thread
        new Thread(() -> {
            try {
                int tokenSize = Server.tokenize(characterPersona + userPersona + chatFirstMessage + chatScenario);

                // UI updates must be on the JavaFX Application Thread
                Platform.runLater(() -> {

                    if (tokenSize > (chatContextSize / 2)) {
                        MessageOverlay tokenWarning = new MessageOverlay(0, 0, 500, 50,"Token Size", "Your character uses more context than you have configured. (" + tokenSize + "/" + chatContextSize + ")");
                        tokenWarning.addStyle(Styles.WARNING);
                        tokenWarning.setIcon(new FontIcon(Material2MZ.OUTLINED_FLAG));

                        App.window.renderPopup(tokenWarning, 650, 870, 500, 50, false, null);
                    }
                });

            } catch (Exception e) {
                // Handle any exceptions from Server.tokenize() (e.g., IOException, JSONException)
                Platform.runLater(() -> {
                    App.logger.error("Error during tokenization: ", e);
                    // Show an error message overlay if tokenization fails
                    MessageOverlay errorOverlay = new MessageOverlay(0, 0, 500, 50,"Tokenization Failed", "Could not calculate token size: " + e.getMessage() + ". Please check server status.");
                    errorOverlay.addStyle(Styles.DANGER);
                    App.window.renderPopup(errorOverlay, 650, 870, 500, 50, false, null);
                });
            }
        }, "Tokenization-Thread").start(); // Give the thread a name for easier debugging
    }

    public Container getRoot() {
        return root;
    }
}
