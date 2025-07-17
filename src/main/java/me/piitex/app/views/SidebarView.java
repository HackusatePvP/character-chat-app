package me.piitex.app.views;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import me.piitex.app.App;
import me.piitex.app.views.characters.CharacterEditMobileView;
import me.piitex.app.views.characters.CharacterEditView;
import me.piitex.app.views.models.ModelsView;
import me.piitex.app.views.settings.SettingsView;
import me.piitex.app.views.users.UsersView;
import me.piitex.engine.Container;
import me.piitex.engine.Renderer;
import me.piitex.engine.layouts.Layout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.TextOverlay;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

public class SidebarView {
    private final Renderer parent;
    private final VerticalLayout root;

    public SidebarView(Renderer parent) {
        this.parent = parent;
        int width;
        int height;
        if (App.mobile) {
            width = 100;
            height = 1280;
        } else {
            width = 200;
            height = 1080;
        }

        root = new VerticalLayout(width, height);
        root.setAlignment(Pos.BASELINE_CENTER);
        root.addStyle(Styles.BORDER_DEFAULT);
        root.addStyle(Styles.BG_INSET);
        double rootWidth = 150;

        TextOverlay close = new TextOverlay(new FontIcon(Material2AL.CLOSE));
        close.setX(root.getMaxWidth() - 5);
        root.addElement(close);
        close.onClick(event -> {
            root.getPane().setMaxWidth(50);
            root.getPane().setMinWidth(50);

            FontIcon fontIcon = new FontIcon(Material2AL.EXPAND_MORE);
            root.getPane().getChildren().clear();
            root.getPane().getChildren().addFirst(fontIcon);

            fontIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event1 -> {
                if (parent instanceof Layout layout) {
                    layout.getPane().getChildren().removeFirst();
                    layout.getPane().getChildren().addFirst(new SidebarView(parent).getRoot().render());
                }
            });

        });

        ButtonOverlay home = new ButtonOverlay("home", "Home");
        home.setWidth(rootWidth);
        root.addElement(home);
        home.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new HomeView().getContainer());
            App.window.render();
        });

        ButtonOverlay settings = new ButtonOverlay("home", "Settings");
        settings.setWidth(rootWidth);
        root.addElement(settings);
        settings.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new SettingsView().getContainer());
            App.window.render();
        });

        ButtonOverlay models = new ButtonOverlay("models", "Models");
        models.setWidth(rootWidth);
        root.addElement(models);
        models.onClick(event -> {
            App.window.getStage().getScene().setCursor(Cursor.WAIT);

            Container container = new ModelsView().getContainer();
            App.window.clearContainers();
            App.window.addContainer(container);

            Platform.runLater(() -> {
                App.window.render();
                App.window.getStage().getScene().setCursor(Cursor.DEFAULT);
            });
        });

        ButtonOverlay users = new ButtonOverlay("users", "User Templates");
        users.setWidth(rootWidth);
        root.addElement(users);
        users.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new UsersView().getRoot());
            App.window.render();
        });

        ButtonOverlay characters = new ButtonOverlay("characters", "Create Character");
        characters.setWidth(rootWidth);
        root.addElement(characters);
        characters.onClick(event -> {
            App.window.clearContainers();
            if (App.mobile) {
                App.window.addContainer(new CharacterEditMobileView(null).getRoot());
            } else {
                App.window.addContainer(new CharacterEditView(null).getRoot());
            }
            App.window.render();
        });
    }

    public VerticalLayout getRoot() {
        return root;
    }
}
