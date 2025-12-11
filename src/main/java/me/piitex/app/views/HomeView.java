package me.piitex.app.views;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.input.MouseEvent;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.characters.CharactersView;
import me.piitex.app.views.setup.SetupView;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;

import java.util.concurrent.TimeUnit;

public class HomeView extends EmptyContainer {
    private final HorizontalLayout root;

    private final AppSettings appSettings = App.getInstance().getAppSettings();
    private final SidebarView sidebarView;

    public HomeView() {
        int height = App.getInstance().getAppSettings().getHeight() - 50;
        super(600, height);
        this.sidebarView = new SidebarView(false);
        if (App.mobile) {
            root = new HorizontalLayout(600, height);
        } else {
            setWidth(appSettings.getWidth());
            setHeight(height);
            root = new HorizontalLayout(appSettings.getWidth() - 20, height);
        }
        root.setSpacing(10);
        addElement(root);

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
                    buildBody();
                });
            }, 1, TimeUnit.SECONDS);
        } else {
            buildBody();
        }
    }

    public VerticalLayout buildInstructions() {
        VerticalLayout layout = new VerticalLayout(appSettings.getWidth() - 265, appSettings.getHeight());
        layout.setSpacing(20);
        layout.setAlignment(Pos.CENTER);

        TextOverlay header = new TextOverlay("Guided Setup");
        header.addStyle(Styles.TITLE_2);
        layout.addElement(header);

        SeparatorOverlay separatorOverlay = new SeparatorOverlay(Orientation.HORIZONTAL);
        separatorOverlay.setMaxWidth(1000);
        layout.addElement(separatorOverlay);

        layout.addElement(getSetupButon());

        return layout;
    }

    public ButtonOverlay getSetupButon() {
        ButtonOverlay button = new ButtonBuilder("cc").setGraphic(buildSetupGraphic()).build();
        button.addStyle(Styles.ACCENT);
        button.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new SetupView(sidebarView));

        });

        return button;
    }


    public VerticalLayout buildSetupGraphic() {
        VerticalLayout root = new VerticalLayout(400, 250);
        root.setMaxSize(root.getWidth(), root.getHeight());
        root.setSpacing(50);
        root.setAlignment(Pos.CENTER);

        TextOverlay textOverlay = new TextOverlay("Quick Setup");
        textOverlay.addStyle(Styles.TITLE_2);
        root.addElement(textOverlay);

        root.getPane().addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            textOverlay.setText("Guided setup instructions.");
        });

        root.getPane().addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            textOverlay.setText("Get Started");
        });

        return root;
    }

    public void buildBody() {
        if (appSettings.isSetup()) {
            CharactersView charactersView = new CharactersView();
            root.addElement(charactersView.getRoot());
        } else {
            sidebarView.setEnabled(false);
            root.addElement(buildInstructions());
        }
    }
}
