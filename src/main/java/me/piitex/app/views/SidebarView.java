package me.piitex.app.views;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
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

import static me.piitex.app.views.Positions.*;

public class SidebarView {
    private final VerticalLayout root;
    private final Renderer parent;

    public SidebarView(Renderer parent, boolean collapse) {
        this.parent = parent;
        root = new VerticalLayout(SIDEBAR_WIDTH, SIDEBAR_HEIGHT);
        root.setAlignment(Pos.BASELINE_CENTER);
        root.addStyle(Styles.BORDER_DEFAULT);
        root.addStyle(Styles.BG_INSET);
        if (collapse) {
            ButtonOverlay expand = buildExpand();
            root.setWidth(SIDEBAR_WIDTH_COLLAPSE);
            expand.onClick(event -> {
                if (parent instanceof Layout layout) {
                    layout.getPane().getChildren().removeFirst();
                    layout.getPane().getChildren().addFirst(new SidebarView(parent, false).getRoot().render());
                }
            });

            root.addElement(expand);
        } else {
            build();
        }
    }

    public void build() {
        double rootWidth = SIDEBAR_WIDTH - 20;

        TextOverlay close = new TextOverlay(new FontIcon(Material2AL.CLOSE));
        close.setX(root.getMaxWidth() - 5);
        root.addElement(close);
        close.onClick(event -> {
            root.getPane().setMaxWidth(50);
            root.getPane().setMinWidth(50);

            ButtonOverlay buttonOverlay = buildExpand();

            Node expand = buttonOverlay.render();
            root.getPane().getChildren().clear();
            root.getPane().getChildren().addFirst(expand);

            expand.addEventHandler(MouseEvent.MOUSE_CLICKED, event1 -> {
                if (parent instanceof Layout layout) {
                    layout.getPane().getChildren().removeFirst();
                    layout.getPane().getChildren().addFirst(new SidebarView(parent, false).getRoot().render());
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

        ButtonOverlay models = new ButtonOverlay("models", "Models / Backend");
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

    private ButtonOverlay buildExpand() {
        ButtonOverlay buttonOverlay = new ButtonOverlay("expand", new FontIcon(Material2AL.KEYBOARD_ARROW_RIGHT));
        buttonOverlay.setWidth(32);
        buttonOverlay.setHeight(32);
        return buttonOverlay;
    }

    public VerticalLayout getRoot() {
        return root;
    }
}
