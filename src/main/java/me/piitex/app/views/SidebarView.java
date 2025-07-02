package me.piitex.app.views;

import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.views.characters.CharacterEditMobileView;
import me.piitex.app.views.characters.CharacterEditView;
import me.piitex.app.views.models.ModelsView;
import me.piitex.app.views.settings.SettingsView;
import me.piitex.app.views.users.UsersView;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonOverlay;

public class SidebarView {
    private final VerticalLayout root;

    private int width, height;

    public SidebarView() {
        if (App.mobile) {
            width = 100;
            height = 1280;
        } else {
            width = 200;
            height = 1080;
        }

        root = new VerticalLayout(width, height);
        root.setAlignment(Pos.BASELINE_CENTER);
        root.setBackgroundColor(Color.rgb(10, 13, 18));
        double width = 150;

        ButtonOverlay home = new ButtonOverlay("home", "Home");
        home.setWidth(width);
        root.addElement(home);
        home.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new HomeView().getContainer());
            App.window.render();
        });

        ButtonOverlay settings = new ButtonOverlay("home", "Settings");
        settings.setWidth(width);
        root.addElement(settings);
        settings.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new SettingsView().getContainer());
            App.window.render();
        });

        ButtonOverlay models = new ButtonOverlay("models", "Models");
        models.setWidth(width);
        root.addElement(models);
        models.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new ModelsView(0).getContainer());
            App.window.render();
        });

        ButtonOverlay users = new ButtonOverlay("users", "User Templates");
        users.setWidth(width);
        root.addElement(users);
        users.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new UsersView().getRoot());
            App.window.render();
        });

        ButtonOverlay characters = new ButtonOverlay("characters", "Create Character");
        characters.setWidth(width);
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
