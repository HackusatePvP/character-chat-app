package me.piitex.app.views.creator.characters;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.Positions;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.containers.Container;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonBuilder;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.IconOverlay;
import me.piitex.engine.overlays.TextOverlay;
import me.piitex.os.configurations.InfoFile;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CharacterCreator extends EmptyContainer {
    private final AppSettings appSettings = App.getInstance().getAppSettings();
    private final Character character;
    private final InfoFile infoFile;

    private String characterId;

    // Icons to set coloring. This is for progress indication.
    private IconOverlay characterIcon;
    private IconOverlay userIcon;
    private IconOverlay chatIcon;
    private ButtonOverlay submission;

    // Display content holds the configuration tab
    private HorizontalLayout displayContent;

    // Cache all current displays to quickly naviagte between them
    private CharacterCustomizationView characterCustomizationView;
    private UserCustomizationView userCustomizationView;
    private ChatCustomizationView chatCustomizationView;
    private List<File> importedChatFiles = new ArrayList<>();
    private Container currentView;

    // Calculated sizing for panel content
    private final double contentWidth = appSettings.getWidth() - Positions.SIDEBAR_WIDTH - 350;
    private final double contentHeight = appSettings.getHeight();

    public CharacterCreator(@Nullable Character character) {
        // Two buttons that will guide through either the character or user view.
        super(800, 600);
        this.character = character;
        if (character != null) {
            infoFile = InfoFile.copy(character.getInfoFile());
            if (character.getUser() != null) {
                infoFile.set("user-display-name", character.getUser().getDisplayName());
                infoFile.set("user-icon-path", character.getUser().getIconPath());
                infoFile.set("user-persona", character.getUser().getPersona());
            }
        } else {
            infoFile = new InfoFile();
        }
        setWidth(appSettings.getWidth());
        setHeight(appSettings.getHeight());
        addStyle(Styles.BG_INSET);
        init();
    }

    public void init() {
        HorizontalLayout layout = new HorizontalLayout(appSettings.getWidth() - 100, appSettings.getHeight());
        layout.addStyle(Styles.BG_INSET);
        layout.addElement(new SidebarView(false));
        addElement(layout);

        VerticalLayout main = new VerticalLayout(appSettings.getWidth() - 265, appSettings.getHeight());
        main.setSpacing(20);
        layout.addElement(main);

        displayContent = new HorizontalLayout(appSettings.getWidth(), appSettings.getHeight());
        displayContent.setSpacing(20);
        main.addElement(displayContent);

        characterCustomizationView = new CharacterCustomizationView(this, infoFile, contentWidth, contentHeight);
        userCustomizationView = new UserCustomizationView(this, infoFile, contentWidth, contentHeight);
        chatCustomizationView = new ChatCustomizationView(this, infoFile, contentWidth, contentHeight);

        currentView = characterCustomizationView;

        displayContent.addElement(buildChecklist());

        // Character Customization will be displayed first.
        displayContent.addElement(characterCustomizationView);
    }

    private VerticalLayout buildChecklist() {
        VerticalLayout root = new VerticalLayout(300, appSettings.getHeight());
        root.setSpacing(40);
        root.addStyle(Styles.BORDER_SUBTLE);
        root.setAlignment(Pos.TOP_CENTER);

        ButtonOverlay characterButton = new ButtonBuilder("cc").setGraphic(buildCharacterButton(root.getWidth())).addStyle(Styles.FLAT).build();
        root.addElement(characterButton);
        characterButton.onClick(_ -> {
            displayContent.removeElement(displayContent.getElements().lastKey());
            displayContent.addElement(characterCustomizationView);
            currentView = characterCustomizationView;
        });

        ButtonOverlay userButton = new ButtonBuilder("uc").setGraphic(buildUserButton(root.getWidth())).addStyle(Styles.FLAT).build();
        root.addElement(userButton);
        userButton.onClick(_ -> {
            displayContent.removeElement(displayContent.getElements().lastKey());
            displayContent.addElement(userCustomizationView);
            currentView = userCustomizationView;
        });

        ButtonOverlay chatButton = new ButtonBuilder("ccc").setGraphic(buildChatButton(root.getWidth())).addStyle(Styles.FLAT).build();
        root.addElement(chatButton);
        chatButton.onClick(_ -> {
            displayContent.removeElement(displayContent.getElements().lastKey());
            displayContent.addElement(chatCustomizationView);
            currentView = userCustomizationView;
        });

        ButtonOverlay finishButton = new ButtonBuilder("finish").setGraphic(buildFinishButton(root.getWidth())).addStyle(Styles.FLAT).build();
        root.addElement(finishButton);
        finishButton.onClick(_ -> {
            displayContent.removeElement(displayContent.getElements().lastKey());
            displayContent.addElement(new FinishCharacterCreatorView(this, infoFile, contentWidth, contentHeight));
            currentView = userCustomizationView;
        });

        if (currentView != null && currentView.hasProperty("progress")) {
            submission = new ButtonBuilder("fin").setText("Next").addStyle(Styles.SUCCESS).addStyle(Styles.BUTTON_OUTLINED).setWidth(root.getWidth()).build();
            submission.onClick(event -> {
                if (currentView == characterCustomizationView) {
                    // View user
                    displayContent.removeElement(displayContent.getElements().lastKey());
                    displayContent.addElement(userCustomizationView);
                    currentView = userCustomizationView;
                } else if (currentView == userCustomizationView) {
                    // View chat
                    displayContent.removeElement(displayContent.getElements().lastKey());
                    displayContent.addElement(chatCustomizationView);
                    currentView = chatCustomizationView;
                } else if (currentView == chatCustomizationView) {
                    displayContent.removeElement(displayContent.getElements().lastKey());
                    displayContent.addElement(new FinishCharacterCreatorView(this, infoFile, contentWidth, contentHeight));
                    currentView = chatCustomizationView;
                }
            });
            root.addElement(submission);
        }

        return root;
    }

    private HorizontalLayout buildCharacterButton(double width) {
        HorizontalLayout root = new HorizontalLayout(width, 0);
        root.setSpacing(20);

        characterIcon = new IconOverlay(Material2AL.CHECK_CIRCLE_OUTLINE);
        characterIcon.setColor(Color.rgb(50, 50, 50,0.5));
        characterIcon.setIconSize(26);
        root.addElement(characterIcon);

        TextOverlay textOverlay = new TextOverlay("Character Customization");
        textOverlay.addStyle(Styles.TITLE_4);
        root.addElement(textOverlay);

        return root;
    }

    private HorizontalLayout buildUserButton(double width) {
        HorizontalLayout root = new HorizontalLayout(width, 0);
        root.setSpacing(20);

        userIcon = new IconOverlay(Material2AL.CHECK_CIRCLE_OUTLINE);
        userIcon.setColor(Color.rgb(50, 50, 50,0.5));
        userIcon.setIconSize(26);
        root.addElement(userIcon);

        TextOverlay textOverlay = new TextOverlay("User Customization");
        textOverlay.addStyle(Styles.TITLE_4);
        root.addElement(textOverlay);

        return root;
    }

    private HorizontalLayout buildChatButton(double width) {
        HorizontalLayout root = new HorizontalLayout(width, 0);
        root.setSpacing(20);

        chatIcon = new IconOverlay(Material2AL.CHECK_CIRCLE_OUTLINE);
        chatIcon.setColor(Color.rgb(50, 50, 50,0.5));
        chatIcon.setIconSize(26);
        root.addElement(chatIcon);

        TextOverlay textOverlay = new TextOverlay("Chat Customization");
        textOverlay.addStyle(Styles.TITLE_4);
        root.addElement(textOverlay);

        return root;
    }

    private HorizontalLayout buildFinishButton(double width) {
        HorizontalLayout root = new HorizontalLayout(width, 0);
        root.setSpacing(20);

        chatIcon = new IconOverlay(Material2AL.CHECK_CIRCLE_OUTLINE);
        chatIcon.setColor(Color.rgb(50, 50, 50,0.5));
        chatIcon.setIconSize(26);
        root.addElement(chatIcon);

        TextOverlay textOverlay = new TextOverlay("Finish Character");
        textOverlay.addStyle(Styles.TITLE_4);
        root.addElement(textOverlay);

        return root;
    }

    public void revalidate() {
        boolean characterFailed = false;
        boolean userFailed = false;
        if (characterCustomizationView.getCharacterIdInput().getCurrentText().isEmpty()) {
            characterFailed = true;
        }
        if (characterCustomizationView.getCharacterDisplayInput().getCurrentText().isEmpty()) {
            characterFailed = true;
        }
        if (userCustomizationView.getUserDisplayInput().getCurrentText().isEmpty()) {
            userFailed = true;
        }

        if (characterFailed) {
            getCharacterIcon().setColor(Color.RED);
        } else {
            getCharacterIcon().setColor(Color.GREEN);
        }

        if (userFailed) {
            getUserIcon().setColor(Color.RED);
        } else {
            getUserIcon().setColor(Color.GREEN);
        }

        if (characterFailed || userFailed) {
            getSubmission().getButton().getStyleClass().removeAll(Styles.SUCCESS);
            getSubmission().getButton().getStyleClass().add(Styles.DANGER);
        } else {
            getSubmission().getButton().getStyleClass().removeAll(Styles.DANGER);
            getSubmission().getButton().getStyleClass().add(Styles.SUCCESS);
        }
    }

    public Character getCharacter() {
        return character;
    }

    public IconOverlay getCharacterIcon() {
        return characterIcon;
    }

    public IconOverlay getUserIcon() {
        return userIcon;
    }

    public IconOverlay getChatIcon() {
        return chatIcon;
    }

    public ButtonOverlay getSubmission() {
        return submission;
    }

    public HorizontalLayout getDisplayContent() {
        return displayContent;
    }

    public Container getCurrentView() {
        return currentView;
    }

    public void setCurrentView(Container currentView) {
        this.currentView = currentView;
    }

    public CharacterCustomizationView getCharacterCustomizationView() {
        return characterCustomizationView;
    }

    public UserCustomizationView getUserCustomizationView() {
        return userCustomizationView;
    }

    public List<File> getImportedChatFiles() {
        return importedChatFiles;
    }
}