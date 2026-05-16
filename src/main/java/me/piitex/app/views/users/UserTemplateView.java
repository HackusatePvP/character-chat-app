package me.piitex.app.views.users;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.utils.ImageCardExporter;
import me.piitex.app.views.LoadingView;
import me.piitex.app.views.Positions;
import me.piitex.app.views.SidebarView;
import me.piitex.app.views.creator.users.UserCreator;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.DialogueContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.FlowLayout;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.FontLoader;
import me.piitex.engine.loaders.image.ImageLoader;
import me.piitex.engine.overlays.*;
import org.apache.commons.io.FileUtils;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class UserTemplateView extends EmptyContainer {
    private HorizontalLayout root;
    private final AppSettings appSettings = App.getInstance().getAppSettings();
    private final SidebarView sidebarView;

    public UserTemplateView() {
        int height = App.getInstance().getAppSettings().getHeight() - 50;
        super(600, height);
        this.sidebarView = new SidebarView();
        if (App.mobile) {
            root = new HorizontalLayout(600, height);
        } else {
            setWidth(appSettings.getWidth());
            setHeight(height);
            root = new HorizontalLayout(appSettings.getWidth() - 20, height);
        }
        root.setSpacing(10);
        addElement(root);
        addStyle(Styles.BG_INSET);
        init();
    }

    public void init() {
        root.addElement(sidebarView);
        root.setSpacing(35);

        if (App.getInstance().isLoading()) {
            root.addElement(new LoadingView("Loading data...", root.getWidth(), 650));
            App.getThreadPoolManager().submitSchedule(() -> {
                boolean loading = App.getInstance().isLoading();
                while (loading) {
                    loading = App.getInstance().isLoading();
                    if (!loading) break;
                }
                Platform.runLater(() -> {
                    root.removeElement(1);
                    buildUsers();
                });
            }, 1, TimeUnit.SECONDS);
        } else {
            buildUsers();
        }
    }

    public void buildUsers() {
        AppSettings appSettings = App.getInstance().getAppSettings();

        VerticalLayout layout = new VerticalLayout(-1, -1);
        layout.setMaxSize(appSettings.getWidth() - Positions.SIDEBAR_WIDTH - 25, layout.getHeight());
        layout.setSpacing(20);

        int imageWidth;
        int imageHeight;
        double cardWidth;
        double cardHeight;
        ScrollContainer body;
        if (App.mobile) {
            body = new ScrollContainer(layout, 0, 0, 400, -1);
            body.setMaxSize(400, 1000);
            imageWidth = 128;
            imageHeight = 128;
            cardWidth = 160;
            cardHeight = 250;
            layout.setSpacing(70);
        } else {
            body = new ScrollContainer(layout, 0, 0, layout.getMaxWidth(), appSettings.getHeight() - 50);
            body.setMaxSize(body.getWidth(), body.getHeight());
            imageWidth = 256;
            imageHeight = 256;
            cardWidth = 280;
            cardHeight = 350;
        }
        body.setScrollWhenNeeded(false);
        body.setHorizontalScroll(false);
        body.setVerticalScroll(true);
        root.addElement(body);

        FlowLayout base = new FlowLayout(body.getWidth(), -1);
        base.setVerticalSpacing(20);
        base.setHorizontalSpacing(20);

        layout.addElement(base);
        for (User user : App.getInstance().getUserTemplates().values()) {

            CardContainer card = new CardContainer(0,0, cardWidth, cardHeight);
            card.setMaxSize(cardWidth, cardHeight);

            VerticalLayout displayBox = new VerticalLayout(0, cardHeight - 50);
            displayBox.setSpacing(15);
            displayBox.setAlignment(Pos.TOP_CENTER);

            ContextMenu contextMenu = new ContextMenu();
            MenuItem edit = new MenuItem("Edit");
            edit.setOnAction(_ -> editUser(user));
            MenuItem copy = new MenuItem("Copy");
            copy.setOnAction(_ -> duplicateUser(user));
            MenuItem delete = new MenuItem("Delete");
            delete.setOnAction(_ -> deleteUser(user, base, card));

            contextMenu.getItems().add(edit);
            contextMenu.getItems().add(copy);
            contextMenu.getItems().add(delete);

            ImageOverlay icon = User.getUserAvatar(user.getIconPath(), imageWidth, imageHeight);
            if (icon != null && icon.getImage() != null) {
                icon.setPreserveRatio(false);
                displayBox.addElement(icon);
            }
            TextOverlay name = new TextOverlay(user.getId());
            displayBox.addElement(name);

            card.setBody(displayBox);

            card.setFooter(buildControlBox(base, card, user));

            base.addElement(card);
        }
    }

    public HorizontalLayout buildControlBox(FlowLayout base, CardContainer card, me.piitex.app.backend.User user) {
        HorizontalLayout root = new HorizontalLayout(200, 25);
        root.setIndex(10);

        int spacing = 20;
        root.setSpacing(spacing);
        if (!App.mobile) {
            root.setAlignment(Pos.BASELINE_CENTER);
        }

        IconOverlay edit = new IconOverlay(Material2AL.EDIT);
        edit.setTooltip("Edit the user");
        edit.setColor(Color.GREEN);
        edit.onClick(_ -> editUser(user));
        root.addElement(edit);

        IconOverlay duplicate = new IconOverlay(Material2AL.FILE_COPY);
        duplicate.setColor(Color.YELLOW);
        duplicate.setTooltip("Duplicate the user.");
        duplicate.onClick(_ -> duplicateUser(user));
        root.addElement(duplicate);

        IconOverlay delete = new IconOverlay(Material2AL.DELETE_FOREVER);
        delete.setColor(Color.RED);
        delete.setTooltip("Delete the user.");
        delete.onClick(event -> {
            deleteUser(base, card, user, event.getHandler().getSceneX(), event.getHandler().getSceneY());
        });
        root.addElement(delete);

        IconOverlay export = new IconOverlay(Material2AL.CLOUD_DOWNLOAD);
        export.setIconSize(16);
        export.setColor(Color.BLUE);
        export.setTooltip("Export the user.");
        export.onClick(_ -> {
            FileChooser chooser = new FileChooser();
            chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Save user card as.", "*.png"));
            chooser.setInitialFileName(user.getId() + ".png");

            File file = chooser.showSaveDialog(App.window.getStage());
            if (file != null) {
                try {
                    ImageCardExporter.exportUser(user, file);
                } catch (IOException e) {
                    App.logger.error("Could not save user card!", e);
                }
            } else {
                App.logger.error("Could not locate user file!");
            }
        });
        root.addElement(export);

        return root;
    }

    private void editUser(me.piitex.app.backend.User user) {
        App.window.clearContainers();
        EmptyContainer progressContainer = new EmptyContainer(App.getInstance().getAppSettings().getWidth(), App.getInstance().getAppSettings().getHeight());
        progressContainer.addElement(new LoadingView("Loading user data...", progressContainer.getWidth(), progressContainer.getHeight()));
        App.window.addContainer(progressContainer);

        App.getThreadPoolManager().submitTask(() -> {
            UserCreator userCreator = new UserCreator(user);
            Node assemble = userCreator.assemble();
            Platform.runLater(() -> {
                App.window.clearContainers();
                App.window.addContainer(userCreator, assemble);
            });
        });
    }

    private void duplicateUser(me.piitex.app.backend.User user) {
        // Duplicate the user.
        String newId = user.getId() + " (copy)";
        while (App.getInstance().getUser(newId) != null) {
            newId += " (copy)";
        }

        // Edit the user in the edit view rather than duplicating the files
        // Allow the id to be edited and changed.

        // Create a copy of the user.
        App.window.clearContainers();
        EmptyContainer progressContainer = new EmptyContainer(App.getInstance().getAppSettings().getWidth(), App.getInstance().getAppSettings().getHeight());
        progressContainer.addElement(new LoadingView("Loading user data...", progressContainer.getWidth(), progressContainer.getHeight()));
        App.window.addContainer(progressContainer);

        App.getThreadPoolManager().submitTask(() -> {
            UserCreator userCreator = new UserCreator(user);
            Node assemble = userCreator.assemble();
            Platform.runLater(() -> {
                App.window.clearContainers();
                App.window.addContainer(userCreator, assemble);
            });
        });
    }

    private void deleteUser(FlowLayout base, CardContainer card, me.piitex.app.backend.User user, double x, double y) {
        DialogueContainer dialogueContainer = new DialogueContainer("Delete '" + user.getId() + "'?", 500, 500);

        ButtonOverlay cancel = new ButtonBuilder("cancel").setText("Keep").build();
        cancel.setWidth(150);
        cancel.addStyle(Styles.SUCCESS);
        cancel.onClick(_ -> App.window.removeContainer(dialogueContainer));

        ButtonOverlay confirm = new ButtonBuilder("confirm").setText("Delete").build();
        confirm.setWidth(150);
        confirm.addStyle(Styles.DANGER);
        confirm.onClick(_ -> {
            App.getInstance().getUserTemplates().remove(user.getId());
            App.window.removeContainer(dialogueContainer);

            // Cleanup image usage
            VerticalLayout verticalLayout = (VerticalLayout) card.getBody();
            ImageOverlay imageOverlay = (ImageOverlay) verticalLayout.getElementAt(0);

            // When setting to null the engine will dispose of the image and the JVM will call gc.
            imageOverlay.setImage(null);

            base.removeElement(card);

            // Add a buffer to ensure image resources are disposed.
            App.getThreadPoolManager().submitSchedule(() -> {
                try {
                    App.logger.info("Removing image from cache '{}'", user.getIconPath());
                    ImageLoader.clearCache();
                    App.logger.info("Deleting User: {}", user.getId());
                    FileUtils.deleteDirectory(user.getUserDirectory());
                    App.getInstance().getUserTemplates().remove(user.getId());
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
