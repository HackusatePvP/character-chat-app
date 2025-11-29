package me.piitex.app.views.creator.characters;

import atlantafx.base.theme.Styles;
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

public class CharacterCreator extends EmptyContainer {
    private final AppSettings appSettings = App.getInstance().getAppSettings();
    private final Character character;
    private final InfoFile infoFile;

    private String characterId;

    // Icons to set coloring. This is for progress indication.
    private IconOverlay characterIcon;
    private IconOverlay userIcon;
    private IconOverlay chatIcon;

    private HorizontalLayout displayContent;
    private Container currentView;

    public CharacterCreator(@Nullable Character character) {
        // Two buttons that will guide through either the character or user view.
        super(800, 600);
        this.character = character;
        if (character != null) {
            infoFile = InfoFile.copy(character.getInfoFile());
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

        displayContent.addElement(buildChecklist());

        currentView = new CharacterCustomizationView((infoFile), appSettings.getWidth() - Positions.SIDEBAR_WIDTH - 350, appSettings.getHeight());
        displayContent.addElement(currentView);
    }

    private VerticalLayout buildChecklist() {
        VerticalLayout root = new VerticalLayout(300, appSettings.getHeight());
        root.setSpacing(40);
        root.addStyle(Styles.BORDER_SUBTLE);
        root.setAlignment(Pos.CENTER);

        ButtonOverlay characterButton = new ButtonBuilder("cc").setGraphic(buildCharacterButton(root.getWidth())).addStyle(Styles.FLAT).build();
        root.addElement(characterButton);
        characterButton.onClick(event -> {
            if (currentView != null) {
                displayContent.replaceElement(currentView.getIndex(), new CharacterCustomizationView(infoFile, appSettings.getWidth() - Positions.SIDEBAR_WIDTH - 350, appSettings.getHeight()));
            }
        });

        ButtonOverlay userButton = new ButtonBuilder("uc").setGraphic(buildUserButton(root.getWidth())).addStyle(Styles.FLAT).build();
        root.addElement(userButton);

        ButtonOverlay chatButton = new ButtonBuilder("ccc").setGraphic(buildChatButton(root.getWidth())).addStyle(Styles.FLAT).build();
        root.addElement(chatButton);

        ButtonOverlay finishButton = new ButtonBuilder("finish").setGraphic(buildFinishButton(root.getWidth())).addStyle(Styles.FLAT).build();
        root.addElement(finishButton);

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

    public Character getCharacter() {
        return character;
    }
}