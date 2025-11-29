package me.piitex.app.views.creator;

import atlantafx.base.theme.Styles;
import com.drew.lang.annotations.Nullable;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.SidebarView;
import me.piitex.app.views.creator.characters.CharacterCreator;
import me.piitex.engine.containers.Container;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;

public class CreatorView extends EmptyContainer {
    private final AppSettings appSettings = App.getInstance().getAppSettings();

    public CreatorView() {
        // Two buttons that will guide through either the character or user view.

        super(800, 600);
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
        main.setAlignment(Pos.CENTER);
        layout.addElement(main);

        TextOverlay header = new TextOverlay("Choose what to create.");
        header.addStyle(Styles.TITLE_2);
        main.addElement(header);

        SeparatorOverlay separatorOverlay = new SeparatorOverlay(Orientation.HORIZONTAL);
        separatorOverlay.setMaxWidth(1000);
        main.addElement(separatorOverlay);

        HorizontalLayout buttonLayout = new HorizontalLayout(main.getWidth(), 250);
        buttonLayout.setSpacing(50);
        buttonLayout.setAlignment(Pos.CENTER);
        buttonLayout.setMaxSize(buttonLayout.getWidth(), buttonLayout.getHeight());
        buttonLayout.addElement(getCharacterCreator());
        buttonLayout.addElement(getUserCreator());
        main.addElement(buttonLayout);
    }

    public ButtonOverlay getCharacterCreator() {
        ButtonOverlay button = new ButtonBuilder("cc").setGraphic(buildCharacterGraphic()).build();
        button.addStyle(Styles.SUCCESS);
        button.addStyle(Styles.BUTTON_OUTLINED);
        button.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new CharacterCreator(null));

        });

        return button;
    }


    public Container buildCharacterGraphic() {
        Container container = new EmptyContainer(400, 250);

        VerticalLayout root = new VerticalLayout(400, 250);
        root.setSpacing(50);
        container.addElement(root);
        root.setAlignment(Pos.CENTER);

        TextOverlay textOverlay = new TextOverlay("Character");
        textOverlay.addStyle(Styles.TITLE_3);
        root.addElement(textOverlay);

        TextFlowOverlay paragraph = new TextFlowOverlay("Create a new unique character to chat with. This character is only usable to you.", 400, 0);
        root.addElement(paragraph);

        return container;
    }

    public ButtonOverlay getUserCreator() {
        ButtonOverlay button = new ButtonBuilder("uc").setGraphic(buildUserCreator()).build();
        button.addStyle(Styles.BUTTON_OUTLINED);
        button.addStyle(Styles.ACCENT);

        return button;
    }


    public Container buildUserCreator() {
        Container container = new EmptyContainer(400, 250);

        VerticalLayout root = new VerticalLayout(400, 250);
        root.setSpacing(50);
        root.setAlignment(Pos.CENTER);
        container.addElement(root);

        TextOverlay textOverlay = new TextOverlay("User");
        textOverlay.addStyle(Styles.TITLE_3);
        root.addElement(textOverlay);

        TextFlowOverlay paragraph = new TextFlowOverlay("Create a new user template which can be used for every character.", 400, 0);
        root.addElement(paragraph);

        return container;
    }


}