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
        int height = App.getInstance().getAppSettings().getHeight() - 50;

        super(600, height);
        if (App.mobile) {
            root = new HorizontalLayout(600, height);
        } else {
            setWidth(appSettings.getWidth());
            setHeight(height);
            root = new HorizontalLayout(appSettings.getWidth() - 200, height);
        }

        addElement(root);

        init();
    }

    public void init() {
        root.addStyle(Styles.BG_INSET);
        root.addElement(new SidebarView(root, false));
        root.setSpacing(35);

        if (App.getInstance().isLoading()) {
            root.addElement(new LoadingView("Loading data...", root.getWidth(), 650));
        } else {
            buildBody();
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
                    root.removeElement(1);
                    buildBody();
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
        body.add(new TextOverlay("4. Select the models directory (Recommended).\n\n"));
        body.add(new TextOverlay("5. Download a GGUF model from the \"Download\" tab.\n\n"));
        body.add(new TextOverlay("6. Select the model or set as a default. (REQUIRED).\n\n"));
        body.add(new TextOverlay("7. (Optional) Configure the model settings by clicking the blue gear.\n\n"));
        body.add(new TextOverlay("8. Go back to settings and start the server.\n\n"));
        body.add(new TextOverlay("9. (Optional) Create a User Template.\n\n"));
        body.add(new TextOverlay("10. Create a character.\n"));

        card.setBody(bodyLayout);

        VerticalLayout footerLayout = new VerticalLayout(600, 50);
        footerLayout.setAlignment(Pos.BOTTOM_CENTER);

        TextOverlay footer = new TextOverlay("These instructions will remain on this page until a character is created.");
        footerLayout.addElement(footer);

        card.setFooter(footerLayout);

        return layout;

    }

    public void buildBody() {
        if (!App.getInstance().getCharacters().isEmpty()) {
            CharactersView charactersView = new CharactersView();
            root.addElement(charactersView.getRoot());
        } else {
            root.addElement(buildInstructions());
        }
    }
}
