package me.piitex.app.views.users;

import atlantafx.base.theme.Styles;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.FlowLayout;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

public class UsersView extends EmptyContainer {
    private static final AppSettings appSettings = App.getInstance().getAppSettings();
    private final HorizontalLayout root;
    private final VerticalLayout mainPage;

    public UsersView() {
        super(appSettings.getWidth(), appSettings.getHeight());

        root = new HorizontalLayout(getWidth(), getHeight());
        root.setMaxSize(root.getWidth(), root.getHeight());
        addElement(root);

        SidebarView sidebarView = new SidebarView(this, false);
        root.addElement(sidebarView);

        mainPage = new VerticalLayout(appSettings.getWidth() - 200, 0);
        mainPage.setMaxSize(mainPage.getWidth(), mainPage.getHeight());
        root.addElement(mainPage);

        init();
    }

    public void init() {
        VerticalLayout header = new VerticalLayout(appSettings.getWidth() - 200, 200);
        header.setMaxSize(header.getWidth(), header.getHeight());
        header.setAlignment(Pos.CENTER);
        mainPage.addElement(header);

        ButtonOverlay newUser = new ButtonBuilder("new").setText("New User").build();
        newUser.addStyle(Styles.SUCCESS);
        newUser.addStyle(Styles.BUTTON_OUTLINED);
        header.addElement(newUser);
        header.addElement(new SeparatorOverlay(Orientation.HORIZONTAL));

        buildFlowLayout();
    }

    public void buildFlowLayout() {
        VerticalLayout flowRoot = new VerticalLayout(appSettings.getWidth() - 200, 0);

        ScrollContainer base = new ScrollContainer(flowRoot, 10, 10, mainPage.getMaxWidth(), -1);
        base.setMaxSize(base.getWidth(), base.getHeight());

        FlowLayout flowLayout = new FlowLayout(appSettings.getWidth() - 200, 0);
        flowLayout.setMaxSize(flowLayout.getWidth(), flowLayout.getHeight());
        mainPage.addElement(flowLayout);
        flowLayout.addStyle(Styles.BORDER_DEFAULT);

        for (User user : App.getInstance().getUserTemplates().values()) {
            CardContainer card = new CardContainer(0,0, 280, 380);
            card.setMaxSize(card.getWidth(), card.getHeight());

            VerticalLayout displayBox = new VerticalLayout(0, 0);
            displayBox.setAlignment(Pos.BASELINE_CENTER);

            TextOverlay helper = new TextOverlay("Click to chat");
            helper.setUnderline(true);
            displayBox.addElement(helper);

            ContextMenu contextMenu = new ContextMenu();

            MenuItem edit = new MenuItem("Edit");
            edit.setOnAction(event -> {

            });
            MenuItem copy = new MenuItem("Copy");
            copy.setOnAction(event -> {

            });
            MenuItem delete = new MenuItem("Delete");
            delete.setOnAction(event -> {

            });

            contextMenu.getItems().add(edit);
            contextMenu.getItems().add(copy);
            contextMenu.getItems().add(delete);

            ImageOverlay icon = User.getUserAvatar(user.getIconPath(), 256, 256);
            if (icon != null && icon.getImage() != null) {
                icon.setPreserveRatio(false);
                displayBox.addElement(icon);
            }
            TextOverlay name = new TextOverlay(user.getId());
            displayBox.addElement(name);

            card.setBody(displayBox);

            card.setFooter(buildControlBox(flowLayout, card, user));

            flowLayout.addElement(card);
        }
    }

    public HorizontalLayout buildControlBox(FlowLayout base, CardContainer card, User user) {
        HorizontalLayout root = new HorizontalLayout(200, 25);
        root.setIndex(10);
        root.setSpacing(20);
        if (!App.mobile) {
            root.setAlignment(Pos.BASELINE_CENTER);
        }

        FontIcon editIcon = new FontIcon(Material2AL.EDIT);
        editIcon.setIconSize(16);
        TextOverlay edit = new TextOverlay(editIcon);
        edit.setTooltip("Edit the character");
        edit.addStyle(Styles.ACCENT);
        edit.onClick(event -> {

        });
        root.addElement(edit);

        FontIcon duplicateIcon = new FontIcon(Material2AL.FILE_COPY);
        duplicateIcon.setIconSize(16);
        TextOverlay duplicate = new TextOverlay(duplicateIcon);
        duplicate.setTooltip("Duplicate the character.");
        duplicate.addStyle(Styles.WARNING);
        duplicate.onClick(event -> {

        });
        root.addElement(duplicate);

        FontIcon deleteIcon = new FontIcon(Material2AL.DELETE_FOREVER);
        TextOverlay delete = new TextOverlay(deleteIcon);

        delete.addStyle(Styles.DANGER);
        delete.setTooltip("Delete the character.");
        delete.onClick(event -> {

        });
        root.addElement(delete);
        return root;
    }
}
