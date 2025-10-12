package me.piitex.app.views;

import javafx.scene.layout.VBox;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.engine.Window;

// Static fields to specify various positions of elements.
// Must be initialized before GUI is rendered.
public class Positions {

    /*
    ###########################
        Sidebar positions
    ############################
    */
    public static int SIDEBAR_WIDTH;
    public static int SIDEBAR_WIDTH_COLLAPSE;
    public static int SIDEBAR_HEIGHT;

    /*
    ###########################
        ChatView positions
    ############################
    */
    public static int CHAT_VIEW_SCROLL_X;
    public static int CHAT_VIEW_SCROLL_Y;
    public static double CHAT_VIEW_SCROLL_WIDTH;
    public static double CHAT_VIEW_SCROLL_HEIGHT;
    public static double CHAT_VIEW_SELECTION_X;
    public static double CHAT_VIEW_SELECTION_WIDTH;
    public static double CHAT_VIEW_SELECTION_HEIGHT;
    public static double CHAT_BOX_WIDTH;
    public static double CHAT_BOX_HEIGHT;
    public static double CHAT_BOX_IMAGE_WIDTH;
    public static double CHAT_BOX_IMAGE_HEIGHT;
    public static double CHAT_BOX_BUTTON_BOX_WIDTH;
    public static double CHAT_BOX_BUTTON_BOX_HEIGHT;
    public static double CHAT_SEND_BOX_WIDTH;
    public static double CHAT_SEND_BOX_HEIGHT;
    public static double CHAT_TEXTFLOW_WIDTH;

    /*
    ###########################
        Model Tabs positions
    ############################
    */
    public static double MODEL_CONFIGURATION_SCROLL_WIDTH;
    public static double MODEL_CONFIGURATION_SCROLL_HEIGHT;
    public static double MODEL_CONFIGURATION_LAYOUT_WIDTH;
    public static double MODEL_CONFIGURATION_LAYOUT_HEIGHT;
    public static double MODEL_CONFIGURATION_LAYOUT_SPACING;


    public static void initialize() {
        if (App.mobile) {
            initializeMobile();
        } else {
            initializeDesktop();
        }
    }

    private static void initializeDesktop() {
        Window window = App.window;

        // -1 is computed size
        CHAT_VIEW_SCROLL_X = 0;
        CHAT_VIEW_SCROLL_Y = 10; // Small y offset
        CHAT_VIEW_SCROLL_WIDTH = window.getWidth() - 225;
        CHAT_VIEW_SCROLL_HEIGHT = window.getHeight() - 250;
        CHAT_VIEW_SELECTION_X = window.getWidth() / 2;
        CHAT_VIEW_SELECTION_WIDTH = 100;
        CHAT_VIEW_SELECTION_HEIGHT = 50;
        CHAT_BOX_WIDTH = CHAT_VIEW_SCROLL_WIDTH - 10;
        CHAT_BOX_HEIGHT = VBox.USE_COMPUTED_SIZE;
        CHAT_BOX_IMAGE_WIDTH = window.getWidth() - (window.getWidth() / 3);
        CHAT_BOX_IMAGE_HEIGHT = -1;
        CHAT_BOX_BUTTON_BOX_WIDTH = 900;
        CHAT_BOX_BUTTON_BOX_HEIGHT = 50;
        CHAT_SEND_BOX_WIDTH =  500;
        CHAT_SEND_BOX_HEIGHT = -1;
        CHAT_TEXTFLOW_WIDTH = CHAT_BOX_WIDTH - 50;

        SIDEBAR_WIDTH = 200;
        SIDEBAR_WIDTH_COLLAPSE = 50;
        SIDEBAR_HEIGHT = (int) window.getHeight();

        MODEL_CONFIGURATION_SCROLL_WIDTH = window.getWidth() - 265;
        MODEL_CONFIGURATION_SCROLL_HEIGHT = window.getHeight() - 200;
        MODEL_CONFIGURATION_LAYOUT_WIDTH = MODEL_CONFIGURATION_SCROLL_WIDTH - 20;
        MODEL_CONFIGURATION_LAYOUT_HEIGHT = -1;
        MODEL_CONFIGURATION_LAYOUT_SPACING = 10;
    }

    private static void initializeMobile() {
        // Assuming the view is 600 x 1200 (Vertical display)
        // Should work for other values but designed for that resolution
        AppSettings appSettings = App.getInstance().getAppSettings();
        CHAT_VIEW_SCROLL_HEIGHT = App.window.getHeight() - 300;
        CHAT_VIEW_SCROLL_WIDTH = App.window.getWidth() - 100;
        CHAT_BOX_WIDTH = CHAT_VIEW_SCROLL_WIDTH - 10;
        CHAT_BOX_HEIGHT = -1;
        CHAT_VIEW_SELECTION_X = App.window.getWidth() / 3;
        CHAT_VIEW_SELECTION_WIDTH = 100;
        CHAT_VIEW_SELECTION_HEIGHT = 30;
        CHAT_BOX_BUTTON_BOX_WIDTH = CHAT_BOX_WIDTH;
        CHAT_BOX_BUTTON_BOX_HEIGHT = 50;
        CHAT_SEND_BOX_WIDTH = 400;
        CHAT_SEND_BOX_HEIGHT = 100;
        CHAT_BOX_IMAGE_WIDTH = CHAT_BOX_WIDTH - 10;
        CHAT_TEXTFLOW_WIDTH = CHAT_BOX_WIDTH - 50;

        SIDEBAR_WIDTH = 100;
        SIDEBAR_WIDTH_COLLAPSE = 50;
        SIDEBAR_HEIGHT = 1200;
    }

}
