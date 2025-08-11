package me.piitex.app.views;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.characters.CharactersView;
import me.piitex.app.views.server.ServerLayout;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;

public class HomeView extends EmptyContainer {
    private final HorizontalLayout root;

    private final AppSettings appSettings = App.getInstance().getAppSettings();

    public HomeView() {
        super(600, 1080);
        if (App.mobile) {
            root = new HorizontalLayout(600, 1080);
        } else {
            setWidth(appSettings.getWidth());
            setHeight(appSettings.getHeight());
            root = new HorizontalLayout(appSettings.getWidth(), appSettings.getHeight());
        }

        addElement(root);

        init();
    }

    public void init() {
        root.addStyle(Styles.BG_INSET);
        root.addElement(new SidebarView(root, false).getRoot());
        root.setSpacing(35);

        if (App.getInstance().isLoading()) {
            VerticalLayout layout = new VerticalLayout(1920, 0);

            TextOverlay text = new TextOverlay("Loading data...");
            layout.addElement(text);

            ProgressBarOverlay load = new ProgressBarOverlay();
            layout.addElement(load);

            root.addElement(layout);
        } else {
            buildBody(root);
        }

        addElement(new ServerLayout(appSettings.getWidth(), 50));
        if (App.getInstance().isLoading()) {
            addRenderEvent(event -> App.getThreadPoolManager().submitTask(() -> {
                boolean loading = App.getInstance().isLoading();
                while (loading) {
                    loading = App.getInstance().isLoading();
                    if (!loading) break;
                }
                Platform.runLater(() -> {
                    buildBody(root);
                });
            }));
        }
    }

    public VerticalLayout buildInstructions() {
        VerticalLayout layout = new VerticalLayout(appSettings.getWidth() - 300, -1);
        layout.setAlignment(Pos.TOP_CENTER);

        CardContainer card = new CardContainer(0, 0,0, 0);
        card.setMaxSize(600, appSettings.getHeight() - 100);
        layout.addElement(card);

        VerticalLayout headerLayout = new VerticalLayout(600, 50);
        headerLayout.setAlignment(Pos.TOP_CENTER);

        TextOverlay header = new TextOverlay("Setup Instructions");
        header.addStyle(Styles.TITLE_1);
        headerLayout.addElement(header);

        SeparatorOverlay separator = new SeparatorOverlay(Orientation.HORIZONTAL);
        separator.setX(-12);
        separator.addStyle(Styles.MEDIUM);
        headerLayout.addElement(separator);

        card.setHeader(headerLayout);

        VerticalLayout bodyLayout = new VerticalLayout(600, -1);

        TextFlowOverlay body = new TextFlowOverlay("", 600, -1);
        bodyLayout.addElement(body);

        body.add(new TextOverlay("1. Navigate to \"Models / Backend\".\n\n"));
        body.add(new TextOverlay("2. Set a compatible backend. If you do not want to download drivers select Vulkan.\n\n"));
        body.add(new TextOverlay("3. Set a GPU. Ensure there is another option besides auto.\n\n"));
        body.add(new TextOverlay("4. Navigate to Models.\n\n"));
        body.add(new TextOverlay("5. Set the directory for your models.\n\n"));
        body.add(new TextOverlay("6. Download a GGUF model from the \"Download\" tab.\n\n"));
        body.add(new TextOverlay("7. Set the model as a default in the \"List\" tab. (REQUIRED).\n\n"));
        body.add(new TextOverlay("8. (Optional) Configure the model settings by clicking the blue gear.\n\n"));
        body.add(new TextOverlay("9. Go back to settings and start the server.\n\n"));
        body.add(new TextOverlay("10. (Optional) Create a User Template.\n\n"));
        body.add(new TextOverlay("11. Create a character.\n"));

        card.setBody(bodyLayout);

        VerticalLayout footerLayout = new VerticalLayout(600, 50);
        footerLayout.setAlignment(Pos.BOTTOM_CENTER);

        TextOverlay footer = new TextOverlay("These instructions will remain on this page until a character is created.");
        footerLayout.addElement(footer);

        card.setFooter(footerLayout);

        return layout;

    }

    public void buildBody(HorizontalLayout root) {
        if (!App.getInstance().getCharacters().isEmpty()) {
            root.addElement(new CharactersView().getRoot());
        } else {
            root.addElement(buildInstructions());
        }
    }
}
