package me.piitex.app.views.users;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.LoadingView;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.containers.Container;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.DialogueContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.FlowLayout;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.apache.commons.io.FileUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class UsersView extends EmptyContainer {
    private static final AppSettings appSettings = App.getInstance().getAppSettings();
    private final VerticalLayout mainPage;

    public UsersView() {
        super(appSettings.getWidth(), appSettings.getHeight());

        HorizontalLayout root = new HorizontalLayout(getWidth(), getHeight());
        root.setMaxSize(root.getWidth(), root.getHeight());
        addElement(root);

        SidebarView sidebarView = new SidebarView(false);
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
        newUser.onClick(_ -> {
            App.window.clearContainers();
            App.window.addContainer(new UserEditView());
        });
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
            edit.setOnAction(_ -> editUser(user));
            MenuItem copy = new MenuItem("Copy");
            copy.setOnAction(_ -> duplicateUser(user));
            MenuItem delete = new MenuItem("Delete");
            delete.setOnAction(_ -> deleteUser(user, flowLayout, card));

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
        edit.setTooltip("Edit the user");
        edit.addStyle(Styles.ACCENT);
        edit.onClick(event -> {
            editUser(user);
        });
        root.addElement(edit);

        FontIcon duplicateIcon = new FontIcon(Material2AL.FILE_COPY);
        duplicateIcon.setIconSize(16);
        TextOverlay duplicate = new TextOverlay(duplicateIcon);
        duplicate.setTooltip("Duplicate the user.");
        duplicate.addStyle(Styles.WARNING);
        duplicate.onClick(event -> {
            duplicateUser(user);
        });
        root.addElement(duplicate);

        FontIcon deleteIcon = new FontIcon(Material2AL.DELETE_FOREVER);
        TextOverlay delete = new TextOverlay(deleteIcon);

        delete.addStyle(Styles.DANGER);
        delete.setTooltip("Delete the user.");
        delete.onClick(event -> {
            deleteUser(base, card, user, event.getHandler().getSceneX(), event.getHandler().getSceneY());
        });
        root.addElement(delete);
        return root;
    }

    private void editUser(User user) {
        App.window.clearContainers();
        EmptyContainer progressContainer = new EmptyContainer(App.getInstance().getAppSettings().getWidth(), App.getInstance().getAppSettings().getHeight());
        progressContainer.addElement(new LoadingView("Loading User data...", progressContainer.getWidth(), progressContainer.getHeight()));
        App.window.addContainer(progressContainer);

        App.getThreadPoolManager().submitTask(() -> {
            Container container = new UserEditView(user);
            Node assemble = container.assemble();
            Platform.runLater(() -> {
                App.window.clearContainers();
                App.window.addContainer(container, assemble);
            });
        });
    }

    private void duplicateUser(User user) {
        // Duplicate the User.
        String newId = user.getId() + " (copy)";
        while (App.getInstance().getUser(newId) != null) {
            newId += " (copy)";
        }

        // Edit the User in the edit view rather than duplicating the files
        // Allow the id to be edited and changed.

        // Create a copy of the User.
        App.window.clearContainers();
        EmptyContainer progressContainer = new EmptyContainer(App.getInstance().getAppSettings().getWidth(), App.getInstance().getAppSettings().getHeight());
        progressContainer.addElement(new LoadingView("Loading User data...", progressContainer.getWidth(), progressContainer.getHeight()));
        App.window.addContainer(progressContainer);

        User duplicated = new User(newId, null);
        App.getThreadPoolManager().submitTask(() -> {
            duplicated.copy(user);
            UserEditView editView = new UserEditView(duplicated);
            Node assemble = editView.assemble();
            Platform.runLater(() -> {
                App.window.clearContainers();
                App.window.addContainer(editView, assemble);
            });
        });
    }

    private void deleteUser(FlowLayout base, CardContainer card, User user, double x, double y) {
        DialogueContainer dialogueContainer = new DialogueContainer("Delete '" + user.getId() + "'?", 500, 500);

        ButtonOverlay cancel = new ButtonBuilder("cancel").setText("Keep").build();
        cancel.setWidth(150);
        cancel.addStyle(Styles.SUCCESS);
        cancel.onClick(_ -> {
            App.window.removeContainer(dialogueContainer);
        });

        ButtonOverlay confirm = new ButtonBuilder("confirm").setText("Delete").build();
        confirm.setWidth(150);
        confirm.addStyle(Styles.DANGER);
        confirm.onClick(_ -> {
            App.getInstance().getUserTemplates().remove(user.getId());
            App.window.removeContainer(dialogueContainer);

            // Cleanup image usage
            VerticalLayout verticalLayout = (VerticalLayout) card.getBody();
            ImageOverlay imageOverlay = (ImageOverlay) verticalLayout.getElementAt(1);

            // When setting to null the engine will dispose of the image and the JVM will call gc.
            imageOverlay.setImage(null);

            deleteUserDirectory(user);
            base.removeElement(card);
        });

        dialogueContainer.setCancelButton(cancel);
        dialogueContainer.setConfirmButton(confirm);

        // Render this on top
        App.window.renderPopup(dialogueContainer, x, y, 500, 500);
    }

    private void deleteUser(User user, FlowLayout base, CardContainer card) {
        App.getInstance().getUserTemplates().remove(user.getId());
        deleteUserDirectory(user);
        base.removeElement(card);
    }

    private void deleteUserDirectory(User user) {
        // Add a delay to ensure all io operations are completed.
        App.getThreadPoolManager().submitSchedule(() -> {
            App.logger.info("Deleting User: {}", user.getId());
            try {
                FileUtils.deleteDirectory(user.getUserDirectory());
            } catch (IOException e) {
                App.logger.error("Could not delete directory!", e);
            }
        }, 1, TimeUnit.SECONDS);
    }
}
