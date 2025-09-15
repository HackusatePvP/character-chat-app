package me.piitex.app.views;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.characters.CharacterEditView;
import me.piitex.app.views.models.ModelsView;
import me.piitex.app.views.settings.SettingsView;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.Renderer;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonBuilder;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.MessageOverlay;
import me.piitex.engine.overlays.TextOverlay;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.util.function.Consumer;

import static me.piitex.app.views.Positions.*;

public class SidebarView extends VerticalLayout {
    private final Renderer parent;

    // Testing out consumer. Hopefully it's more efficient than interfaces.
    private Consumer<Boolean> onCollapseStateChange;

    private static final AppSettings appSettings = App.getInstance().getAppSettings();

    public SidebarView(Renderer parent, boolean collapse) {
        super(SIDEBAR_WIDTH, SIDEBAR_HEIGHT);
        this.parent = parent;
        setAlignment(Pos.BASELINE_CENTER);
        addStyle(Styles.BORDER_DEFAULT);
        addStyle(Styles.BG_INSET);
        if (collapse) {
            ButtonOverlay expand = buildExpand();
            setWidth(SIDEBAR_WIDTH_COLLAPSE);
            addElement(expand);
        } else {
            build();
        }
    }

    public void build() {
        double rootWidth = SIDEBAR_WIDTH - 20;

        TextOverlay close = new TextOverlay(new FontIcon(Material2AL.CLOSE));
        close.setX(getMaxWidth() - 5);
        addElement(close);
        close.onClick(event -> {
            setMaxSize(50, SIDEBAR_HEIGHT);
            setWidth(50);
            setHeight(SIDEBAR_HEIGHT);
            ButtonOverlay buttonOverlay = buildExpand();

            removeAllElements();
            addElement(buttonOverlay);
        });

        ButtonOverlay home = new ButtonBuilder("home").setText("Home").build();
        home.addStyle(appSettings.getGlobalTextSize());
        home.setWidth(rootWidth);
        addElement(home);
        home.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new HomeView());
        });

        ButtonOverlay settings = new ButtonBuilder("settings").setText("Settings").build();
        settings.addStyle(appSettings.getGlobalTextSize());
        settings.setWidth(rootWidth);
        addElement(settings);
        settings.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new SettingsView().getContainer());
        });

        ButtonOverlay models = new ButtonBuilder("models").setText("Models / Backend").build();
        models.addStyle(appSettings.getGlobalTextSize());
        models.setWidth(rootWidth);
        addElement(models);
        models.onClick(event -> {
            App.window.getStage().getScene().setCursor(Cursor.WAIT);

            App.window.clearContainers();
            App.window.addContainer(new ModelsView("Settings"));
            App.window.getStage().getScene().setCursor(Cursor.DEFAULT);

        });

        ButtonOverlay users = new ButtonBuilder("users").setText("User Template").build();
        users.addStyle(appSettings.getGlobalTextSize());
        users.setWidth(rootWidth);
        addElement(users);
        users.onClick(event -> {
            MessageOverlay warning = new MessageOverlay("Development", "User templates are still in development.");
            warning.addStyle(Styles.WARNING);
            App.window.renderPopup(warning, PopupPosition.BOTTOM_CENTER, 400, 100, true);
        });

        ButtonOverlay characters = new ButtonBuilder("characters").setText("New Character").build();
        characters.addStyle(appSettings.getGlobalTextSize());
        characters.setWidth(rootWidth);
        addElement(characters);
        characters.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new CharacterEditView(null).getRoot());
        });
    }

    public void setOnCollapseStateChange(Consumer<Boolean> onCollapseStateChange) {
        this.onCollapseStateChange = onCollapseStateChange;
    }

    private ButtonOverlay buildExpand() {
        ButtonOverlay buttonOverlay = new ButtonBuilder("expand").setIcon(new FontIcon(Material2AL.KEYBOARD_ARROW_RIGHT)).build();
        buttonOverlay.setWidth(32);
        buttonOverlay.setHeight(32);

        buttonOverlay.onClick(event1 -> {
            // Re-assemble sidebar.
            removeAllElements();
            setWidth(SIDEBAR_WIDTH);
            setHeight(SIDEBAR_HEIGHT);
            setMaxSize(50, SIDEBAR_HEIGHT);
            build();

            if (onCollapseStateChange != null) {
                onCollapseStateChange.accept(false);
            }
        });

        if (onCollapseStateChange != null) {
            onCollapseStateChange.accept(true);
        }


        return buttonOverlay;
    }
}
