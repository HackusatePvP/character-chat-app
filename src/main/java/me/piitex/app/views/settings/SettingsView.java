package me.piitex.app.views.settings;

import atlantafx.base.theme.*;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.backend.server.*;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.Positions;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.Container;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;

import java.util.ArrayList;
import java.util.List;

public class SettingsView {
    private final Container container;
    private final ServerSettings settings = App.getInstance().getSettings();

    // The amount of spacing between the description and the input.
    private final int layoutSpacing = 200;

    private final AppSettings appSettings = App.getInstance().getAppSettings();

    public SettingsView() {
        int maxWidth = appSettings.getWidth();
        container = new EmptyContainer(maxWidth - 300, 0);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setSpacing(35);
        container.addElement(root);

        root.addElement(new SidebarView(root, false).getRoot());

        VerticalLayout layout = new VerticalLayout(0, 0);
        layout.setOffsetX(20);
        layout.setSpacing(20);

        //FIXME: If the scroller breaks it's probably because of changes to VerticalLayout. setPrefSize() does not work with the scroller and will break it. Only use setMinSize.
        ScrollContainer scrollContainer = new ScrollContainer(layout, 0, 20, maxWidth - 250, appSettings.getHeight() - 100);
        scrollContainer.setMaxSize(maxWidth - 250, appSettings.getHeight() - 100);

        scrollContainer.setVerticalScroll(true);
        scrollContainer.setScrollWhenNeeded(true);
        scrollContainer.setHorizontalScroll(false);
        root.addElement(scrollContainer);

        layout.addElement(buildResolution());
        layout.addElement(buildGlobalChatSize());
        layout.addElement(buildChatSize());
        layout.addElement(buildTheme());
    }

    public CardContainer buildResolution() {
        CardContainer card = new CardContainer(0, 0, appSettings.getWidth() - 300, 120);
        card.setMaxSize(appSettings.getWidth() - 300, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);

        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("Set the base resolution for the application. This is still in development, some pages may not support different resolutions.", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        description.addStyle(appSettings.getGlobalTextSize());
        root.addElement(description);

        List<String> items = new ArrayList<>();
        items.add("1280x720");
        items.add("1920x1080");
        items.add("2560x1440");
        items.add("3840x2160");

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setMaxHeight(50);
        //String current = (infoFile.hasKey("width") && infoFile.hasKey("height") ? infoFile.get("width") + "x" + infoFile.get("height") : "");
        AppSettings appSettings = App.getInstance().getAppSettings();
        String current = appSettings.getWidth() + "x" + appSettings.getHeight();
        selection.setDefaultItem(current);
        root.addElement(selection);
        selection.onItemSelect(event -> {
            String item = event.getItem();
            int width = Integer.parseInt(item.split("x")[0]);
            int height = Integer.parseInt(item.split("x")[1]);
            appSettings.setWidth(width);
            appSettings.setHeight(height);

            App.window.setWidth(width);
            App.window.setHeight(height);

            Positions.initialize();

            App.window.clearContainers();
            App.window.addContainer(new SettingsView().getContainer());
            App.window.render();
        });
        card.setBody(root);

        return card;
    }

    public CardContainer buildChatSize() {
        CardContainer card = new CardContainer(0, 0, appSettings.getWidth() - 300, 120);
        card.setMaxSize(appSettings.getWidth() - 300, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);

        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("Set the text size for the chats. Can be helpful to those with visual impairments.", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        description.addStyle(appSettings.getGlobalTextSize());
        root.addElement(description);

        List<String> items = new ArrayList<>();
        items.add("Small");
        items.add("Default");
        items.add("Large");
        items.add("Larger");
        items.add("Extra Large");
        items.add("Extreme Large Ultimate");

        AppSettings appSettings = App.getInstance().getAppSettings();

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setMaxHeight(50);
        selection.setDefaultItem(getTextKey(appSettings.getChatTextSize()));
        root.addElement(selection);
        selection.onItemSelect(event -> {
            String item = event.getItem();
            if (item.equalsIgnoreCase("small")) {
                item = Styles.TEXT_SMALL;
            } else if (item.equalsIgnoreCase("Default")) {
                item = Styles.TEXT;
            } else if (item.equalsIgnoreCase("large")) {
                item = Styles.TITLE_4;
            } else if (item.equalsIgnoreCase("larger")) {
                item = Styles.TITLE_3;
            } else if (item.equalsIgnoreCase("extra large")) {
                item = Styles.TITLE_2;
            } else if (item.equalsIgnoreCase("extreme large ultimate")) {
                item = Styles.TITLE_1;
            } else {
                item = Styles.TEXT;
            }
            appSettings.setChatTextSize(item);
        });
        card.setBody(root);

        return card;
    }

    public CardContainer buildGlobalChatSize() {
        CardContainer card = new CardContainer(0, 0, appSettings.getWidth() - 300, 120);
        card.setMaxSize(appSettings.getWidth() - 300, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);

        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("Set the text size for the general UI. This is still in development, may cause issues or incompatible with some components.", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        description.addStyle(appSettings.getGlobalTextSize());
        root.addElement(description);

        List<String> items = new ArrayList<>();
        items.add("Small");
        items.add("Default");
        items.add("Large");
        items.add("Larger");
        items.add("Extra Large");
        items.add("Extreme Large Ultimate");

        AppSettings appSettings = App.getInstance().getAppSettings();

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setMaxHeight(50);
        selection.setDefaultItem(getTextKey(appSettings.getGlobalTextSize()));
        root.addElement(selection);
        selection.onItemSelect(event -> {
            String item = event.getItem();
            if (item.equalsIgnoreCase("small")) {
                item = Styles.TEXT_SMALL;
            } else if (item.equalsIgnoreCase("Default")) {
                item = Styles.TEXT;
            } else if (item.equalsIgnoreCase("large")) {
                item = Styles.TITLE_4;
            } else if (item.equalsIgnoreCase("larger")) {
                item = Styles.TITLE_3;
            } else if (item.equalsIgnoreCase("extra large")) {
                item = Styles.TITLE_2;
            } else if (item.equalsIgnoreCase("extreme large ultimate")) {
                item = Styles.TITLE_1;
            } else {
                item = Styles.TEXT;
            }
            appSettings.setGlobalTextSize(item);
        });
        card.setBody(root);

        return card;
    }

    public CardContainer buildTheme() {
        CardContainer card = new CardContainer(0, 0, appSettings.getWidth() - 300, 120);
        card.setMaxSize(appSettings.getWidth() - 300, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);

        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("Change the theme of the application.", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        description.addStyle(appSettings.getGlobalTextSize());
        root.addElement(description);

        List<String> items = new ArrayList<>();
        items.add("Primer Light");
        items.add("Primer Dark");
        items.add("Nord Light");
        items.add("Nord Dark");
        items.add("Cupertino Light");
        items.add("Cupertino Dark");
        items.add("Dracula");

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setMaxHeight(50);
        //String current = (infoFile.hasKey("width") && infoFile.hasKey("height") ? infoFile.get("width") + "x" + infoFile.get("height") : "");
        AppSettings appSettings = App.getInstance().getAppSettings();
        selection.setDefaultItem(appSettings.getTheme());
        root.addElement(selection);
        selection.onItemSelect(event -> {
            String item = event.getItem();
            appSettings.setTheme(item);
            App.logger.info("Switching theme to " + item);

            Application.setUserAgentStylesheet(appSettings.getStyleTheme(item).getUserAgentStylesheet());
        });
        card.setBody(root);

        return card;
    }

    private String getTextKey(String item) {
        if (item.contains("small")) {
            return "Small";
        } else if (item.equalsIgnoreCase("text")) {
            return "Default";
        } else if (item.contains("title-4")) {
            return "Large";
        } else if (item.contains("title-3")) {
            return "Larger";
        } else if (item.contains("title-2")) {
            return "Extra Large";
        } else if (item.contains("title-1")) {
            return "Extreme Large Ultimate";
        } else {
            return "Default";
        }
    }

    public Container getContainer() {
        return container;
    }
}
