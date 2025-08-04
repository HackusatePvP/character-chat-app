package me.piitex.app.views.users;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.views.HomeView;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.Container;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.DialogueContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.ImageOverlay;
import me.piitex.engine.overlays.MessageOverlay;
import me.piitex.engine.overlays.TextOverlay;
import org.apache.commons.io.FileUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.IOException;

public class UsersView {
    private final Container container;

    public UsersView() {
        container = new EmptyContainer(1920, 1080);

        HorizontalLayout root = new HorizontalLayout(1920, 1080);
        root.setSpacing(35);
        container.addElement(root);

        root.addElement(new SidebarView(root, false).getRoot());

        ButtonOverlay create = new ButtonOverlay("create", "Create User");
        create.addStyle(Styles.SUCCESS);
        create.addStyle(Styles.BUTTON_OUTLINED);
        create.setX(900);
        create.setY(20);
        container.addElement(create);
        create.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new UserEditView(null).getRoot());
        });

        root.addElement(build());
    }

    public ScrollContainer build() {
        VerticalLayout layout = new VerticalLayout(0, 0);
        layout.setSpacing(20);

        ScrollContainer root = new ScrollContainer(layout, 20, 100, 1500, 0);
        root.setMaxSize(1920, 900);
        root.setVerticalScroll(true);
        root.setScrollWhenNeeded(false);
        root.setHorizontalScroll(false);

        HorizontalLayout base = new HorizontalLayout(0, 0);
        base.setSpacing(20);
        layout.addElement(base);

        int i = 0;
        int max = (App.mobile ? 3 : 6);
        for (User user : App.getInstance().getUserTemplates().values()) {
            if (i == max) {
                // Start a new horizontal row
                base = new HorizontalLayout(0, 0);
                base.setSpacing(20);
                layout.addElement(base);
                i = 0;
            }

            CardContainer card = new CardContainer(0, 0, 250, 380);
            card.setMaxSize(250, 380);

            VerticalLayout displayBox = new VerticalLayout(250, 300);
            displayBox.setX(-10);
            displayBox.setY(-10);

            TextOverlay helper = new TextOverlay("Click to chat");
            helper.setUnderline(true);
            displayBox.addElement(helper);
            displayBox.setAlignment(Pos.BASELINE_CENTER);

            displayBox.setClickEvent(event -> {
                // Load chat window...
                MessageOverlay warning = new MessageOverlay(0, 0, 600, 100,"Warning", "This feature is still in development");
                warning.addStyle(Styles.WARNING);
                warning.addStyle(Styles.BG_DEFAULT);
                App.window.renderPopup(warning, PopupPosition.BOTTOM_CENTER, 600, 100, false);
            });

            ImageOverlay icon = User.getUserAvatar(user.getIconPath(), 256, 256);
            if (icon != null && icon.getImage() != null) {
                displayBox.addElement(icon);
            }
            TextOverlay name = new TextOverlay(user.getId());
            displayBox.addElement(name);

            card.setBody(displayBox);

            card.setFooter(buildControlBox(user));

            base.addElement(card);

            i++;
        }

        return root;
    }

    public HorizontalLayout buildControlBox(User user) {
        HorizontalLayout root = new HorizontalLayout(200, 25);
        root.setIndex(10);
        root.setSpacing(20);
        root.setAlignment(Pos.BASELINE_CENTER);

        FontIcon editIcon = new FontIcon(Material2AL.EDIT);
        editIcon.setIconSize(16);
        TextOverlay edit = new TextOverlay(editIcon);
        edit.setTooltip("Edit the user");
        edit.onClick(event -> {
            UserEditView editView = new UserEditView(user);
            App.window.clearContainers();
            App.window.addContainer(editView.getRoot());
        });
        root.addElement(edit);

        FontIcon duplicateIcon = new FontIcon(Material2AL.FILE_COPY);
        duplicateIcon.setIconSize(16);
        TextOverlay duplicate = new TextOverlay(duplicateIcon);
        duplicate.setTooltip("Duplicate the user.");
        duplicate.onClick(event -> {
            // Duplicate the user.
            String newId = user.getId() + " (copy)";
            while (App.getInstance().getUser(newId) != null) {
                newId += " (copy)";
            }

            // Edit the user in the edit view rather than duplicating the files
            // Allow the id to be edited and changed.

            // Create a copy of the user.
            User duplicated = new User(newId);
            duplicated.copy(user);

            UserEditView editView = new UserEditView(duplicated, true);
            App.window.clearContainers();
            App.window.addContainer(editView.getRoot());
        });
        root.addElement(duplicate);

        FontIcon deleteIcon = new FontIcon(Material2AL.DELETE_FOREVER);
        TextOverlay delete = new TextOverlay(deleteIcon);

        delete.addStyle(Styles.DANGER);
        delete.setTooltip("Delete the user.");
        delete.onClick(event -> {
            DialogueContainer dialogueContainer = new DialogueContainer("Delete '" + user.getId() + "'?", 500, 500);

            ButtonOverlay cancel = new ButtonOverlay("cancel", "Keep");
            cancel.setWidth(150);
            cancel.addStyle(Styles.SUCCESS);
            cancel.onClick(event1 -> App.window.removeContainer(dialogueContainer));

            ButtonOverlay confirm = new ButtonOverlay("confirm", "Delete");
            confirm.setWidth(150);
            confirm.addStyle(Styles.DANGER);
            confirm.onClick(event1 -> {
                App.getInstance().getUserTemplates().remove(user.getId());
                try {
                    FileUtils.deleteDirectory(user.getUserDirectory());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                App.window.clearContainers();
                App.window.addContainer(new HomeView());
            });

            dialogueContainer.setCancelButton(cancel);
            dialogueContainer.setConfirmButton(confirm);

            // Render this on top
            App.window.renderPopup(dialogueContainer, PopupPosition.CENTER, 500, 500);

        });
        root.addElement(delete);

        // Add icons
        return root;
    }

    public Container getRoot() {
        return container;
    }
}