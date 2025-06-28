package me.piitex.app.views;

import atlantafx.base.theme.Styles;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import me.piitex.app.App;
import me.piitex.app.views.characters.CharactersView;
import me.piitex.engine.Container;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;

public class HomeView {
    private final Container container;

    public HomeView() {
        HorizontalLayout root;
        if (App.mobile) {
            container = new EmptyContainer(600, 1080);
            root = new HorizontalLayout(600, 1080);
        } else {
            container = new EmptyContainer(1920, 1080);
            root = new HorizontalLayout(1920, 1080);
        }

        root.setSpacing(35);
        container.addElement(root);

        root.addElement(new SidebarView().getRoot());
        if (!App.getInstance().getCharacters().isEmpty()) {
            root.addElement(new CharactersView().getRoot());
        } else {
            root.addElement(buildInstructions());
        }

        // Prompt warning with Vulkan
        if (App.getInstance().getSettings().getBackend().equalsIgnoreCase("vulkan")) {
            MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Warning", "Vulkan is known to cause BSOD (Blue screen). To minimize this issue, do not max out gpu layers. The issue is known and reported to LLamaCPP.");
            error.addStyle(Styles.WARNING);
            App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, false);
        }


    }

    public CardContainer buildInstructions() {
        CardContainer card = new CardContainer(400, 50,0, 0);
        card.setMaxSize(600, 800);

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
        body.add(new TextOverlay("\n\n"));
        body.add(new TextOverlay("1. Navigate to settings.\n\n"));
        body.add(new TextOverlay("2. Set a compatible backend. If you do not want to download drivers select Vulkan.\n\n"));
        body.add(new TextOverlay("3. Set a GPU. Ensure there is another option besides auto.\n\n"));
        body.add(new TextOverlay("3a. (Optional) Configure the rest of the settings on the page.\n\n"));
        body.add(new TextOverlay("4. Navigate to Models.\n\n"));
        body.add(new TextOverlay("5. Set the directory for your models.\n\n"));
        body.add(new TextOverlay("6. Download a GGUF model and place it inside the folder.\n\n"));
        body.add(new TextOverlay("7. Set the model as a default (REQUIRED).\n\n"));
        body.add(new TextOverlay("7a. (Optional) Configure the model settings by clicking the blue gear.\n\n"));
        body.add(new TextOverlay("8. Go back to settings and start the server.\n\n"));
        body.add(new TextOverlay("9. (Optional) Create a User Template.\n\n"));
        body.add(new TextOverlay("10. Create a character.\n\n"));
        bodyLayout.addElement(body);

        card.setBody(bodyLayout);

        VerticalLayout footerLayout = new VerticalLayout(600, 50);
        footerLayout.setAlignment(Pos.BOTTOM_CENTER);

        TextOverlay footer = new TextOverlay("These instructions will remain on this page until a character is created.");
        footer.setX(200);
        footerLayout.addElement(footer);

        card.setFooter(footerLayout);

        return card;

    }

    public Container getContainer() {
        return container;
    }
}
