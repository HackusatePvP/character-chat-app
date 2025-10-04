package me.piitex.app.views.settings;

import atlantafx.base.theme.*;
import javafx.application.Application;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.backend.server.*;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.Positions;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.Container;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.TileContainer;
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
        container = new EmptyContainer(appSettings.getWidth() - 300, 0);
        container.addStyle(Styles.BG_INSET);
        build();
    }

    public void build() {

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setSpacing(35);
        container.addElement(root);

        root.addElement(new SidebarView(root, false));

        VerticalLayout layout = new VerticalLayout(0, 0);
        layout.setOffsetX(20);
        layout.setSpacing(20);

        ScrollContainer scrollContainer = new ScrollContainer(layout, 0, 20, appSettings.getWidth() - 250, appSettings.getHeight() - 100);
        scrollContainer.setMaxSize(appSettings.getWidth() - 250, appSettings.getHeight() - 100);

        scrollContainer.setVerticalScroll(true);
        scrollContainer.setScrollWhenNeeded(true);
        scrollContainer.setHorizontalScroll(false);
        root.addElement(scrollContainer);

        layout.addElement(buildResolution());
        layout.addElement(buildGlobalChatSize());
        layout.addElement(buildChatSize());
        layout.addElement(buildTheme());
        layout.addElement(buildQuotesColor());
        layout.addElement(buildAstrixColor());
    }

    public TileContainer buildResolution() {
        TileContainer tileContainer = new TileContainer(0, 0, appSettings.getWidth() - 300, 120);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 120);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());

        tileContainer.setTitle("Resolution");
        tileContainer.setDescription("Set the base resolution for the application. This is still in development, some pages may not support different resolutions.");

        List<String> items = new ArrayList<>();
        items.add("1280x720");
        items.add("1920x1080");
        items.add("2560x1440");
        items.add("3840x2160");

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setMaxHeight(50);
        String current = appSettings.getWidth() + "x" + appSettings.getHeight();
        selection.setDefaultItem(current);
        selection.onItemSelect(event -> {
            String item = event.getNewValue();
            int width = Integer.parseInt(item.split("x")[0]);
            int height = Integer.parseInt(item.split("x")[1]);
            appSettings.setWidth(width);
            appSettings.setHeight(height);

            App.window.setWidth(width);
            App.window.setHeight(height);

            Positions.initialize();

            App.window.clearContainers();
            App.window.addContainer(new SettingsView().getContainer());
        });

        tileContainer.setAction(selection);

        return tileContainer;
    }

    public TileContainer buildChatSize() {
        TileContainer tileContainer = new TileContainer(appSettings.getWidth() - 300, 120);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 120);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());

        tileContainer.setTitle("Chat Text Size");
        tileContainer.setDescription("Set the text size for the chats. Can be helpful to those with visual impairments.");

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
        selection.onItemSelect(event -> {
            String item = event.getNewValue();
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

        tileContainer.setAction(selection);

        return tileContainer;
    }

    public TileContainer buildGlobalChatSize() {
        TileContainer tileContainer = new TileContainer(appSettings.getWidth() - 300, 120);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 120);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());

        tileContainer.setTitle("Global Text Size");
        tileContainer.setDescription("Set the text size for the general UI. This is still in development, some components are incompatible.");

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
        selection.onItemSelect(event -> {
            String item = event.getNewValue();
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

            // Refresh view to reflect changes.
            container.getElements().clear();
            build();
            Pane pane = (Pane) container.getNode();
            pane.getChildren().clear();
            pane.getChildren().addAll(container.build());

        });

        tileContainer.setAction(selection);

        return tileContainer;
    }

    public TileContainer buildTheme() {
        TileContainer tileContainer = new TileContainer(appSettings.getWidth() - 300, 120);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 120);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());

        tileContainer.setTitle("Theme");
        tileContainer.setDescription("Change the theme of the application.");

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
        AppSettings appSettings = App.getInstance().getAppSettings();
        selection.setDefaultItem(appSettings.getTheme());
        selection.onItemSelect(event -> {
            String item = event.getNewValue();
            appSettings.setTheme(item);
            App.logger.info("Switching theme to " + item);

            Application.setUserAgentStylesheet(appSettings.getStyleTheme(item).getUserAgentStylesheet());
        });
        tileContainer.setAction(selection);

        return tileContainer;
    }

    public TileContainer buildQuotesColor() {
        TileContainer tileContainer = new TileContainer(appSettings.getWidth() - 300, 120);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 120);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle("color-fg-default");
        tileContainer.addStyle(appSettings.getGlobalTextSize());

        Color currentColor = Color.web(appSettings.getQuoteColor());
        ColorPickerOverlay colorPickerOverlay = new ColorPickerOverlay();
        colorPickerOverlay.setValue(currentColor);

        tileContainer.setTitle("Quote Tag Color");
        tileContainer.setDescription("Change the color of text surrounded by quotations. ([color=rgb(" +
                (int)(currentColor.getRed() * 255) + "," +
                (int)(currentColor.getGreen() * 255) + "," +
                (int)(currentColor.getBlue() * 255) +
                ")]\"This is quoted text.\"[/color]).");


        colorPickerOverlay.onColorSelect(event -> {
            Color color = event.getColorPickerOverlay().getValue();
            appSettings.setQuoteColor(colorPickerOverlay.getValue().toString());
            tileContainer.setDescription("Change the theme of text surrounded by quotations. ([color=rgb(" +
                    (int)(color.getRed() * 255) + "," +
                    (int)(color.getGreen() * 255) + "," +
                    (int)(color.getBlue() * 255) +
                    ")]\"This is quoted text.\"[/color]).");
        });

        tileContainer.setAction(colorPickerOverlay);

        return tileContainer;
    }

    public TileContainer buildAstrixColor() {
        TileContainer tileContainer = new TileContainer(appSettings.getWidth() - 300, 120);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 120);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());

        Color currentColor = Color.web(appSettings.getAstrixColor());
        ColorPickerOverlay colorPickerOverlay = new ColorPickerOverlay();
        colorPickerOverlay.setValue(currentColor);

        tileContainer.setTitle("Astrix Tag Color");
        tileContainer.setDescription("Change the color of text surrounded by astrix. ([color=rgb(" +
                (int)(currentColor.getRed() * 255) + "," +
                (int)(currentColor.getGreen() * 255) + "," +
                (int)(currentColor.getBlue() * 255) +
                ")]*This is astrix text.*[/color]).");


        colorPickerOverlay.onColorSelect(event -> {
            Color color = event.getColorPickerOverlay().getValue();
            appSettings.setAstrixColor(colorPickerOverlay.getValue().toString());
            tileContainer.setDescription("Change the theme of text surrounded by quotations. ([color=rgb(" +
                    (int)(color.getRed() * 255) + "," +
                    (int)(color.getGreen() * 255) + "," +
                    (int)(color.getBlue() * 255) +
                    ")]*This is astrix text.*[/color]).");
        });
        tileContainer.setAction(colorPickerOverlay);

        return tileContainer;
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
