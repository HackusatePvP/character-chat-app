package me.piitex.app.views.users;

import atlantafx.base.theme.Styles;
import com.drew.lang.annotations.Nullable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.InfoFile;
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
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class UserEditView {
    private User user;
    private TabsContainer container;
    private Container root;

    private String id = "";
    private String displayName = "";
    private String persona = "";
    private File iconPath;
    private final InfoFile infoFile;

    private InputFieldOverlay userId;
    private InputFieldOverlay userDisplayName;
    private Map<String, String> loreItems = new HashMap<>();

    private boolean duplicate = false;

    public UserEditView(@Nullable User user) {
        this.user = user;
        this.infoFile = new InfoFile();
        if (user != null) {
            updateUserFields();
        } else {
            iconPath = new File(App.getAppDirectory(), "icons/character.png");
        }

        build();
    }

    public UserEditView(@Nullable User user, boolean duplicate) {
        this.user = user;
        this.duplicate = duplicate;
        this.infoFile = new InfoFile();
        if (user != null) {
            updateUserFields();
        } else {
            iconPath = new File(App.getAppDirectory(), "icons/character.png");
        }

        build();
    }

    public UserEditView(@Nullable User user, InfoFile infoFile) {
        this.user = user;
        this.infoFile = infoFile;

        if (user != null) {
            updateUserFields();
        } else {
            iconPath = new File(App.getAppDirectory(), "icons/character.png");
            updateInfoFields();
        }
    }


    public void build() {
        this.root = new EmptyContainer(0, 0, 192, 1080);

        HorizontalLayout layout = new HorizontalLayout(1920, 1080);
        layout.addElement(new SidebarView(layout, false).getRoot());
        root.addElement(layout);

        VerticalLayout main = new VerticalLayout(1600, 1000);
        layout.addElement(main);

        // Add the views
        container = new TabsContainer(0, 0, 1000, 1000);
        main.addElement(buildTopTab());

    }

    public TabsContainer buildTopTab() {
        Tab character = buildUserTab();
        container.addTab(character);

        Tab loreTab = buildLorebookTab();
        container.addTab(loreTab);

        return container;
    }

    public Tab buildUserTab() {
        Tab userTab = new Tab("User");
        userTab.setWidth(1000);
        userTab.setHeight(1600);

        HorizontalLayout root = new HorizontalLayout(1000, 1000);
        root.setX(400);
        root.setY(20);
        root.setSpacing(20);
        userTab.addElement(root);

        CardContainer displayBox = buildUserDisplay();
        root.addElement(displayBox);

        root.addElement(buildUserInput());

        TextAreaOverlay userDescription = new TextAreaOverlay(persona, 400, 300, 600, 400);
        userDescription.setHintText("Describe the user and provide key lore.");
        userTab.addElement(userDescription);
        userDescription.onInputSetEvent(event -> {
            persona = event.getInput();
            infoFile.set("user-persona", persona);
        });

        userTab.addElement(buildSubmitBox());

        return userTab;
    }

    public CardContainer buildUserDisplay() {
        CardContainer root = new CardContainer(0, 0, 200, 200);
        root.setMaxSize(200, 200);

        VerticalLayout layout = new VerticalLayout(200, 200);
        layout.setX(-10);
        layout.setY(-10);
        root.setBody(layout);
        layout.setAlignment(Pos.BASELINE_CENTER);
        layout.setSpacing(25);

        if (iconPath == null || !iconPath.exists()) {
            iconPath = new File(App.getAppDirectory(), "icons/character.png");
        }

        ImageOverlay image = new ImageOverlay(new ImageLoader(iconPath));
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
                iconPath = directory;
                infoFile.set("icon-path", directory.getAbsolutePath());
                App.window.clearContainers();
                App.window.addContainer(new UserEditView(user, infoFile).getRoot());
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

        userId = new InputFieldOverlay((user != null ? user.getId() : id), 0, 0, 200, 50);
        userId.setHintText("Unique User Id.");
        userId.onInputSetEvent(event -> {
            id = event.getInput();
            infoFile.set("user-id", id);
        });
        root.addElement(userId);

        userDisplayName = new InputFieldOverlay((user != null ? user.getDisplayName() : displayName), 0, 0, 200, 50);
        userDisplayName.setHintText("Display Name");
        userDisplayName.onInputSetEvent(event -> {
            displayName = event.getInput();
            infoFile.set("user-display", displayName);
        });
        root.addElement(userDisplayName);

        return root;
    }

    public Tab buildLorebookTab() {
        Tab tab = new Tab("Lorebook");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Use the following placeholders; {char}, {{char}}, {chara}, {{chara}}, {character}, {{character}}, {user}, {{user}}, {usr}, {{usr}}");
        info.setX(200);
        info.setY(10);
        tab.addElement(info);

        CardContainer addContainer = new CardContainer(20, 20, 400, 400);
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

        tab.addElement(addContainer);
        tab.addElement(scrollLore);

        tab.addElement(buildSubmitBox());

        return tab;
    }

    public ScrollContainer getLoreItems() {
        VerticalLayout scrollLayout = new VerticalLayout(0 ,0);

        ScrollContainer root = new ScrollContainer(scrollLayout, 600, 20, 600, 800);
        root.setVerticalScroll(true);
        root.setScrollWhenNeeded(true);
        root.setHorizontalScroll(false);

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

    public void updateUserFields() {
        id = user.getId();
        displayName = user.getDisplayName();
        iconPath = new File(user.getIconPath());
        persona = user.getPersona();
        loreItems = user.getLorebook();
    }

    public void updateInfoFields() {
        if (infoFile.hasKey("user-display")) {
            displayName = infoFile.get("user-display").replace("!@!", "\n");
        }
        if (infoFile.hasKey("user-persona")) {
            persona = infoFile.get("user-persona").replace("!@!", "\n");
        }
        if (infoFile.hasKey("lore")) {
            loreItems.putAll(infoFile.getStringMap("lore"));
        }
        if (infoFile.hasKey("persona")) {
            persona = infoFile.get("persona");
        }
    }

    public Container getRoot() {
        return root;
    }

    public HorizontalLayout buildSubmitBox() {
        HorizontalLayout layout = new HorizontalLayout(1000, 200);
        layout.setX(200);
        layout.setY(820);
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
            if (user == null) {
                user = new User(id);
            }
            if (duplicate) {
                // Duplicating is a little weird.
                // Since the id is editable when duplicating it has to be reflected.
                // However, character id is a final and should not be changed after creation
                // This is how the id is used for creating the folders and files

                // Create a new character that acts as the final duplication
                // This allows us to set the chara id since it's a final.
                User dupe = new User(id);
                dupe.copy(user);

                user = dupe;
            }

            user.setDisplayName(displayName);
            user.setPersona(persona);
            if (iconPath != null) {
                File output = new File(user.getUserDirectory(), "user.png");
                try {
                    Files.copy(iconPath.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    user.setIconPath(output.getAbsolutePath());
                }
            }
            user.setLorebook(loreItems);
            App.getInstance().getUserTemplates().put(id, user);

            App.window.clearContainers();
            App.window.addContainer(new UsersView().getRoot());
            App.window.render();

        });

        return layout;
    }

    public boolean validate() {
        if (id.isEmpty() || userId == null) {
            // Go to character selection, and select the char box

            // Prompt them with a warning
            MessageOverlay required = new MessageOverlay(0, 0, 600, 100,"User ID", "User ID is required");
            required.addStyle(Styles.WARNING);
            required.addStyle(Styles.BG_DEFAULT);
            App.window.renderPopup(required, PopupPosition.CENTER, 600, 100, true);

            userId.getNode().requestFocus();
            return false;
        }
        if (displayName.isEmpty() || userDisplayName == null) {
            // Go to character selection, and select the char box

            // Prompt them with a warning
            MessageOverlay required = new MessageOverlay(0, 0, 600, 100,"User Display Name", "User display name is required");
            required.addStyle(Styles.WARNING);
            required.addStyle(Styles.BG_DEFAULT);
            App.window.renderPopup(required, PopupPosition.CENTER, 600, 100, true);

            userDisplayName.getNode().requestFocus();
            return false;
        }

        return true;
    }
}
