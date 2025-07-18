package me.piitex.app.views;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.text.Text;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.characters.CharactersView;
import me.piitex.app.views.server.ServerLayout;
import me.piitex.engine.Container;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;

public class HomeView {
    private final Container container;

    private final AppSettings appSettings = App.getInstance().getAppSettings();

    public HomeView() {
        HorizontalLayout root;
        if (App.mobile) {
            container = new EmptyContainer(600, 1080);
            root = new HorizontalLayout(600, 1080);
        } else {
            container = new EmptyContainer(appSettings.getWidth(), appSettings.getHeight());
            root = new HorizontalLayout(appSettings.getWidth(), appSettings.getHeight());
        }
        root.addStyle(Styles.BG_INSET);

        root.addElement(new SidebarView(root).getRoot());

        root.setSpacing(35);
        container.addElement(root);

        if (App.getInstance().isLoading()) {
            VerticalLayout layout = new VerticalLayout(1920, 0);

            TextOverlay text = new TextOverlay("Loading data...");
            layout.addElement(text);

            ProgressBarOverlay load = new ProgressBarOverlay();
            layout.addElement(load);

            root.addElement(layout);
        } else {
            if (!App.getInstance().getCharacters().isEmpty()) {
                root.addElement(new CharactersView().getRoot());
            } else {
                root.addElement(buildInstructions());
            }
        }

        container.addElement(new ServerLayout(appSettings.getWidth(), 50));

        // Not thread efficient of safe. To be fair the entire application is not efficient or thread safe so why care now.
        if (App.getInstance().isLoading()) {
            container.addRenderEvent(event -> App.getInstance().getThreadPoolManager().submitTask(() -> {
                boolean loading = App.getInstance().isLoading();
                while (loading) {
                    loading = App.getInstance().isLoading();
                    if (!loading) break;
                }

                Platform.runLater(() -> {
                    App.window.clearContainers();
                    App.window.addContainer(new HomeView().getContainer());
                    App.window.render();
                });
            }));
        }

        // Prompt warning with Vulkan
        if (App.getInstance().getSettings().getBackend().equalsIgnoreCase("vulkan")) {
            MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Warning", "Vulkan is known to cause BSOD (Blue screen). To minimize this issue, do not max out gpu layers. The issue is known and reported to LLamaCPP.");
            error.addStyle(Styles.DANGER);
            error.addStyle(Styles.BG_DEFAULT);
            App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, false);
        }


    }

    public CardContainer buildInstructions() {
        CardContainer card = new CardContainer(20, 20,0, 0);
        card.setMaxSize(600, appSettings.getHeight() - 100);

        VerticalLayout headerLayout = new VerticalLayout(600, 50);
        headerLayout.setAlignment(Pos.TOP_CENTER);

        TextOverlay header = new TextOverlay("Setup Instructions");
        header.addStyle(Styles.TITLE_1);
        headerLayout.addElement(header);

        SeparatorOverlay separator = new SeparatorOverlay(Orientation.HORIZONTAL);
        separator.addStyle(Styles.MEDIUM);
        headerLayout.addElement(separator);

        card.setHeader(headerLayout);

        VerticalLayout bodyLayout = new VerticalLayout(600, 500);

        TextFlowOverlay body = new TextFlowOverlay("", 600, 500);
        body.addStyle(Styles.TITLE_4);
        body.add(new TextOverlay("1. Navigate to settings.\n\n"));
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
        bodyLayout.addElement(body);

        card.setBody(bodyLayout);

        VerticalLayout footerLayout = new VerticalLayout(600, 50);
        footerLayout.setAlignment(Pos.BOTTOM_CENTER);

        TextOverlay footer = new TextOverlay("These instructions will remain on this page until a character is created.");
        footerLayout.addElement(footer);

        card.setFooter(footerLayout);

        return card;

    }

    public Container getContainer() {
        return container;
    }
}
