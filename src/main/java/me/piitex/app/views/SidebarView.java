package me.piitex.app.views;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.updater.LLamaBackendUpdater;
import me.piitex.app.views.creator.CreatorView;
import me.piitex.app.views.models.ModelsView;
import me.piitex.app.views.settings.SettingsView;
import me.piitex.app.views.users.UserTemplateView;
import me.piitex.engine.containers.BorderContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.coreui.CoreUiBrands;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.util.concurrent.TimeUnit;

import static me.piitex.app.views.Positions.*;

public class SidebarView extends BorderContainer {

    // Testing out consumer. Hopefully it's more efficient than interfaces.
    double rootWidth = SIDEBAR_WIDTH - 30;

    private static final AppSettings appSettings = App.getInstance().getAppSettings();

    public SidebarView() {
        super(SIDEBAR_WIDTH, SIDEBAR_HEIGHT);
        setMaxSize(SIDEBAR_WIDTH, SIDEBAR_HEIGHT);
        addStyle(Styles.BG_INSET);
        addStyle(Styles.BORDER_DEFAULT);

        setCenter(buildTopLayout());
        setBottom(buildBottomLayout());
    }

    private VerticalLayout buildTopLayout() {
        VerticalLayout top = new VerticalLayout(SIDEBAR_WIDTH, -1);
        top.setMaxSize(top.getWidth(), top.getHeight());
        top.setAlignment(Pos.CENTER);
        top.setSpacing(25);

        ButtonOverlay home = new ButtonBuilder("home").setText("Home").setIcon(new IconOverlay(Material2AL.HOME)).addStyle(Styles.FLAT).build();
        home.addStyle(appSettings.getGlobalTextSize());
        home.setWidth(rootWidth);
        home.setAlignment(Pos.BASELINE_LEFT);
        top.addElement(home);
        home.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new HomeView());
        });

        ButtonOverlay settings = new ButtonBuilder("settings").setText("Settings").setIcon(new IconOverlay(Material2MZ.SETTINGS)).addStyle(Styles.FLAT).build();
        settings.addStyle(appSettings.getGlobalTextSize());
        settings.setWidth(rootWidth);
        settings.setAlignment(Pos.BASELINE_LEFT);
        top.addElement(settings);
        settings.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new SettingsView().getContainer());
        });

        ButtonOverlay models = new ButtonBuilder("models").setText("Models").setIcon(new IconOverlay(Material2AL.CODE)).addStyle(Styles.FLAT).build();
        models.addStyle(appSettings.getGlobalTextSize());
        models.setWidth(rootWidth);
        models.setAlignment(Pos.BASELINE_LEFT);
        top.addElement(models);
        models.onClick(event -> {
            App.window.getStage().getScene().setCursor(Cursor.WAIT);

            App.window.clearContainers();
            App.window.addContainer(new ModelsView("Settings"));
            App.window.getStage().getScene().setCursor(Cursor.DEFAULT);

        });

        ButtonOverlay users = new ButtonBuilder("users").setText("User Templates").setIcon(new IconOverlay(Material2AL.ACCOUNT_CIRCLE)).addStyle(Styles.FLAT).build();
        users.addStyle(appSettings.getGlobalTextSize());
        users.setWidth(rootWidth);
        users.setAlignment(Pos.BASELINE_LEFT);
        top.addElement(users);
        users.onClick(event -> {
            App.window.getStage().getScene().setCursor(Cursor.WAIT);

            App.window.clearContainers();
            App.window.addContainer(new UserTemplateView());
            App.window.getStage().getScene().setCursor(Cursor.DEFAULT);

        });

        ButtonOverlay create = new ButtonBuilder("create").setText("Create").setIcon(new IconOverlay(Material2AL.EDIT)).addStyle(Styles.FLAT).build();
        create.addStyle(appSettings.getGlobalTextSize());
        create.setWidth(rootWidth);
        create.setAlignment(Pos.BASELINE_LEFT);
        top.addElement(create);
        create.onClick(_ -> {
            // Put both character and user create in one menu.
            // This will open a new menu which will go through the creation process.
            App.window.clearContainers();
            App.window.addContainer(new CreatorView());
        });

        return top;
    }

    private VerticalLayout buildBottomLayout() {
        VerticalLayout layout = new VerticalLayout(0, -1);
        layout.setAlignment(Pos.BOTTOM_CENTER);
        layout.setSpacing(5);

        SeparatorOverlay separator = new SeparatorOverlay(Orientation.HORIZONTAL);
        separator.addStyle(Styles.ACCENT);
        layout.addElement(separator);

        // An update is available, display it.
        // To prevent a race condition, the task will be delayed.
        App.getThreadPoolManager().submitSchedule(() -> {
            Platform.runLater(() -> {
                LLamaBackendUpdater updater = App.getInstance().getBackendUpdater();
                if (updater != null && updater.isUpdateAvailable()) {
                    App.logger.info("Backend Versions: {},{}", updater.getCurrent().getVersion(), updater.getLatest().getVersion());
                    ButtonOverlay update = new ButtonBuilder("update").setText("Updates Available").setIcon(new IconOverlay(Material2MZ.SYSTEM_UPDATE_ALT)).addStyle(Styles.FLAT).build();
                    update.setWidth(rootWidth);
                    update.onClick(event -> {
                        if (!updater.startUpdate()) {
                            App.logger.error("Could not start backend update!");
                        }

                    });
                    layout.addElement(update, 2);
                }
            });

        }, 3, TimeUnit.SECONDS);


        layout.addElement(buildHelpLayout());
        layout.addElement(buildVersionLayout());

        return layout;
    }

    private HorizontalLayout buildHelpLayout() {
        HorizontalLayout layout = new HorizontalLayout(0, 0);
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(20);

        IconOverlay githubPage = new IconOverlay(CoreUiBrands.GITHUB);
        githubPage.setIconSize(24);
        githubPage.setTooltip("Checkout the project page.");
        layout.addElement(githubPage);
        githubPage.onClick(_ -> {
            App.getInstance().getHostServices().showDocument("https://github.com/HackusatePvP/character-chat-app");
        });

        IconOverlay bugReport = new IconOverlay(Material2AL.BUG_REPORT);
        bugReport.setColor(Color.rgb(255, 148, 122));
        bugReport.setIconSize(24);
        bugReport.setTooltip("Report issues you have.");
        layout.addElement(bugReport);
        bugReport.onClick(_ -> {
            App.getInstance().getHostServices().showDocument("https://github.com/HackusatePvP/character-chat-app/issues");
        });

        IconOverlay wiki = new IconOverlay(Material2AL.LOCAL_LIBRARY);
        wiki.setColor(Color.rgb(160, 255, 122));
        wiki.setIconSize(24);
        wiki.setTooltip("View documentation and guides.");
        layout.addElement(wiki);
        wiki.onClick(_ -> {
            App.getInstance().getHostServices().showDocument("https://github.com/HackusatePvP/character-chat-app/wiki");
        });

        return layout;
    }

    private VerticalLayout buildVersionLayout() {
        VerticalLayout layout = new VerticalLayout(0, 0);
        layout.setAlignment(Pos.CENTER);
        layout.addStyle(Styles.BORDER_DEFAULT);

        TextOverlay version = new TextOverlay("CCA " + App.getInstance().getVersion());
        version.addStyle(Styles.TEXT_BOLD);
        version.addStyle(Styles.TEXT_SMALL);
        layout.addElement(version);

        return layout;
    }
}