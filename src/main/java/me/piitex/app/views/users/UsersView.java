package me.piitex.app.views.users;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.Container;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.DialogueContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.FlowLayout;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonBuilder;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.ImageOverlay;
import me.piitex.engine.overlays.TextOverlay;
import org.apache.commons.io.FileUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class UsersView extends EmptyContainer {
    private final VerticalLayout layout;

    private final AppSettings appSettings = App.getInstance().getAppSettings();

    private int imageWidth, imageHeight, cardWidth, cardHeight;

    public UsersView() {
        super(App.getInstance().getAppSettings().getWidth(), App.getInstance().getAppSettings().getHeight());

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setSpacing(35);
        addElement(root);

        root.addElement(new SidebarView(root, false));

        layout = new VerticalLayout(getWidth(), getHeight());
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setOffsetX(20);
        layout.setSpacing(20);
        root.addElement(layout);

        init();
    }

    public void init() {
        ButtonOverlay newUser = new ButtonBuilder("add").setText("New User").build();
        newUser.addStyle(Styles.SUCCESS);
        newUser.addStyle(Styles.BUTTON_OUTLINED);
        layout.addElement(newUser);

        ScrollContainer scrollContainer;
        VerticalLayout base = new VerticalLayout(0, -1);
        if (App.mobile) {
            scrollContainer = new ScrollContainer(base, 0, 0, 400, -1);
            scrollContainer.setMaxSize(400, 1000);
            imageWidth = 128;
            imageHeight = 128;
            cardWidth = 160;
            cardHeight = 250;
            layout.setSpacing(70);
        } else {
            scrollContainer = new ScrollContainer(base, 10, 10, appSettings.getWidth() - 310, -1);
            scrollContainer.setMaxSize(appSettings.getWidth() - 300, appSettings.getHeight() - 100);
            imageWidth = 256;
            imageHeight = 256;
            cardWidth = 280;
            cardHeight = 380;
        }
        scrollContainer.setScrollWhenNeeded(false);
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setVerticalScroll(true);


        FlowLayout flowLayout = new FlowLayout(scrollContainer.getWidth(), -1);
        base.addElement(flowLayout);

        for (User user : App.getInstance().getUserTemplates().values()) {
            flowLayout.addElement(createUserCard(user, flowLayout));
        }
    }

    public CardContainer createUserCard(User user, FlowLayout base) {
        CardContainer card = new CardContainer(0,0, cardWidth, cardHeight);
        card.setMaxSize(cardWidth, cardHeight);

        VerticalLayout displayBox = new VerticalLayout(0, 0);
        displayBox.setAlignment(Pos.BASELINE_CENTER);

        TextOverlay helper = new TextOverlay("Click to chat");
        helper.setUnderline(true);
        displayBox.addElement(helper);

        ContextMenu contextMenu = new ContextMenu();

        MenuItem edit = new MenuItem("Edit");
        edit.setOnAction(event -> {
            editUser(user);
        });
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(event -> {
            duplicateUser(user);
        });
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(event -> {
            deleteUser(user, base, card);
        });

        contextMenu.getItems().add(edit);
        contextMenu.getItems().add(copy);
        contextMenu.getItems().add(delete);

        displayBox.setClickEvent(event -> {
            if (event.getFxClick().getButton() == MouseButton.SECONDARY) {
                if (contextMenu.isShowing()) return;
                contextMenu.show(displayBox.getPane(), Side.BOTTOM, 60, 0);
            }

        });

        ImageOverlay icon = User.getUserAvatar(user.getIconPath(), imageWidth, imageHeight);
        if (icon != null && icon.getImage() != null) {
            icon.setPreserveRatio(false);
            displayBox.addElement(icon);
        }
        TextOverlay name = new TextOverlay(user.getId());
        displayBox.addElement(name);

        card.setBody(displayBox);

        card.setFooter(buildControlBox(base, card, user));

        return card;
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
            editUser(user);
        });
        root.addElement(edit);

        FontIcon duplicateIcon = new FontIcon(Material2AL.FILE_COPY);
        duplicateIcon.setIconSize(16);
        TextOverlay duplicate = new TextOverlay(duplicateIcon);
        duplicate.setTooltip("Duplicate the character.");
        duplicate.addStyle(Styles.WARNING);
        duplicate.onClick(event -> {
            duplicateUser(user);
        });
        root.addElement(duplicate);

        FontIcon deleteIcon = new FontIcon(Material2AL.DELETE_FOREVER);
        TextOverlay delete = new TextOverlay(deleteIcon);

        delete.addStyle(Styles.DANGER);
        delete.setTooltip("Delete the character.");
        delete.onClick(event -> {
            deleteUser(base, card, user, event.getHandler().getSceneX(), event.getHandler().getSceneY());
        });
        root.addElement(delete);
        return root;
    }

    private void editUser(User user) {
        App.window.getStage().getScene().setCursor(Cursor.WAIT);

        Container container;
        if (App.mobile) {
            // TODO: Open edit screen.
        } else {
            // TODO: Open edit screen.
        }

        Platform.runLater(() -> {
            App.window.clearContainers();
            //TODO: App.window.addContainer(container);
            App.window.getStage().getScene().setCursor(Cursor.DEFAULT);
        });
    }

    private void duplicateUser(User user) {
        String newId = user.getId() + " (copy)";
        while (App.getInstance().getUser(newId) != null) {
            newId += " (copy)";
        }

        User duplicated = new User(newId, null);
        duplicated.copy(user);

//        CharacterEditView editView = new CharacterEditView(duplicated, true);
//        App.window.clearContainers();
//        App.window.addContainer(editView.getRoot());
    }

    private void deleteUser(FlowLayout base, CardContainer card, User user, double x, double y) {
        DialogueContainer dialogueContainer = new DialogueContainer("Delete '" + user.getId() + "'?", 500, 500);

        ButtonOverlay cancel = new ButtonBuilder("cancel").setText("Keep").build();
        cancel.setWidth(150);
        cancel.addStyle(Styles.SUCCESS);
        cancel.onClick(event1 -> {
            App.window.removeContainer(dialogueContainer);
        });

        ButtonOverlay confirm = new ButtonBuilder("confirm").setText("Delete").build();
        confirm.setWidth(150);
        confirm.addStyle(Styles.DANGER);
        confirm.onClick(event1 -> {
            App.getInstance().getUserTemplates().remove(user.getId());
            App.window.removeContainer(dialogueContainer);

            // Cleanup image usage
            VerticalLayout verticalLayout = (VerticalLayout) card.getBody();
            ImageOverlay imageOverlay = (ImageOverlay) verticalLayout.getElementAt(1);

            // When setting to null the engine will dispose of the image and the JVM will call gc.
            imageOverlay.setImage(null);

            base.removeElement(card);

            // Add a buffer to ensure image resources are disposed.
            App.getThreadPoolManager().submitSchedule(() -> {
                try {
                    App.logger.info("Deleting: {}", user.getId());
                    FileUtils.deleteDirectory(user.getUserDirectory());
                } catch (IOException e) {
                    App.logger.error("Could not delete directory!", e);
                }
            }, 1, TimeUnit.SECONDS);
        });

        dialogueContainer.setCancelButton(cancel);
        dialogueContainer.setConfirmButton(confirm);

        // Render this on top
        App.window.renderPopup(dialogueContainer, x, y, 500, 500);
    }

    private void deleteUser(User user, FlowLayout base, CardContainer card) {
        App.getInstance().getUserTemplates().remove(user.getId());

        App.getThreadPoolManager().submitSchedule(() -> {
            try {
                App.logger.info("Deleting: {}", user.getId());
                FileUtils.deleteDirectory(user.getUserDirectory());
            } catch (IOException e) {
                App.logger.error("Could not delete directory!", e);
            }
        }, 1, TimeUnit.SECONDS);

        base.removeElement(card);
    }
}
