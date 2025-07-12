package me.piitex.app.views.settings;

import atlantafx.base.theme.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.backend.server.*;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.Container;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsView {
    private final Container container;
    private final ServerSettings settings = App.getInstance().getSettings();

    // The amount of spacing between the description and the input.
    private final int layoutSpacing = 200;

    private ButtonOverlay start, stop, reload;

    private AppSettings appSettings = App.getInstance().getAppSettings();

    public SettingsView() {
        int maxWidth = appSettings.getWidth();
        container = new EmptyContainer(maxWidth - 300, 0);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setSpacing(35);
        container.addElement(root);

        root.addElement(new SidebarView().getRoot());

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

        layout.addElement(buildDangerZone());
        layout.addElement(buildResolution());
        layout.addElement(buildChatSize());
        layout.addElement(buildTheme());
        layout.addElement(buildBackend());
        layout.addElement(buildGpuDevice());

        // If the server is currently running but not active display progress bar
        handleServerLoad();
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
        root.addElement(description);

        List<String> items = new ArrayList<>();
        items.add("Small");
        items.add("Default");
        items.add("Large");
        items.add("Larger");
        items.add("Extra Large");
        items.add("Extreme Large Ultimate");

        AppSettings appSettings = App.getInstance().getAppSettings();;

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setMaxHeight(50);
        selection.setDefaultItem(getTextKey(appSettings.getTextSize()));
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
            appSettings.setTextSize(item);
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

    public CardContainer buildBackend() {
        // Testing out card designs
        CardContainer card = new CardContainer(0, 0, appSettings.getWidth() - 300, 120);
        card.setMaxSize(appSettings.getWidth() - 300, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);

        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("Select the compatible backend for your GPU device. Note, CPU only is not supported, this app requires an accelerated framework. Application may seem unresponsive when changing backend. This is normal behavior.", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        List<String> items = new ArrayList<>();
        items.add("Cuda");
        items.add("HIP");
        items.add("Vulkan");

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setMaxHeight(50);
        selection.setDefaultItem(settings.getBackend());
        root.addElement(selection);
        selection.onItemSelect(event -> {
            String newBackend = event.getItem();
            if (newBackend == null) return;
            if (App.vulkanDisable) {
                if (newBackend.equalsIgnoreCase("vulkan")) {
                    ComboBox<String> comboBox = (ComboBox<String>) selection.getNode();
                    comboBox.getSelectionModel().select(settings.getBackend());
                    MessageOverlay warning = new MessageOverlay(0, 0, 600, 100, "Vulkan Support", "Due to BSOD issues with Vulkan the backend is disabled. We are currently waiting on a fix. Thank you for your understanding.", new TextOverlay(new FontIcon(Material2MZ.OUTLINED_FLAG)));
                    warning.addStyle(Styles.WARNING);
                    App.window.renderPopup(warning, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                    return;
                }
            }

            try {
                // Thread blocks until done
                ComboBox<String> comboBox = (ComboBox<String>) selection.getNode();
                comboBox.getSelectionModel().select(newBackend);
                settings.setBackend(newBackend);
                new DeviceProcess(newBackend);

                settings.setDevice("Auto");

                // Once done re-render
                App.window.clearContainers();
                App.window.addContainer(new SettingsView().getContainer());
                App.window.render();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        card.setBody(root);

        return card;
    }

    public CardContainer buildGpuDevice() {
        CardContainer card = new CardContainer(0, 0, appSettings.getWidth() - 300, 120);
        card.setMaxSize(appSettings.getWidth() - 300, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("Select the compatible GPU for your backend. Auto will automatically choose the GPU for you. Please verify that there is one more option than Auto. You may need to install additional production drivers, like Cuda Toolkit, for example. AMD will need HIP.", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);


        ComboBoxOverlay selection = new ComboBoxOverlay(settings.getDevices(), 400, 50);
        selection.setDefaultItem(settings.getDevice());
        root.addElement(selection);
        selection.onItemSelect(event -> {
            settings.setDevice(event.getItem());
        });

        card.setBody(root);

        return card;
    }

    public CardContainer buildDangerZone() {
        CardContainer card = new CardContainer(0, 0, appSettings.getWidth() - 300, 150);
        card.setMaxSize(appSettings.getWidth() - 300, 150);

        TextOverlay text = new TextOverlay("Danger Zone");
        text.setTextFill(Color.RED);
        text.addStyle(Styles.DANGER);
        card.setHeader(text);

        TextOverlay desc = new TextOverlay("Any changes made to model settings will require a reload. Please wait until a notification appears to ensure everything worked properly.");
        card.setBody(desc);

        HorizontalLayout layout = new HorizontalLayout(0, 0);
        layout.setSpacing(20);
        layout.setAlignment(Pos.CENTER);
        card.setFooter(layout);

        start = new ButtonOverlay("start", "Start");
        start.addStyle(Styles.SUCCESS);
        start.addStyle(Styles.BUTTON_OUTLINED);

        reload = new ButtonOverlay("reload", "Reload");
        reload.setTextFill(Color.YELLOW);
        reload.addStyle(Styles.BUTTON_OUTLINED);

        stop = new ButtonOverlay("stop", "Stop");
        stop.addStyle(Styles.DANGER);
        stop.addStyle(Styles.BUTTON_OUTLINED);

        start.onClick(event -> {
            ServerProcess process = ServerProcess.getCurrentServer();
            if (process != null && process.isAlive()) return;
            Button startButton = (Button) start.getNode();
            Button reloadButton = (Button) reload.getNode();
            Button stopButton = (Button) stop.getNode();

            startButton.setDisable(true);
            reloadButton.setDisable(true);
            stopButton.setDisable(true);

            renderProgress();

            new Thread(() -> {
                ServerProcess newProcess = new ServerProcess((settings.getLastModel() == null ? App.getInstance().getDefaultModel() : settings.getLastModel()));
                Platform.runLater(() -> {
                    if (newProcess.isError()) {
                        if (settings.getLastModel() == null && App.getInstance().getDefaultModel() == null) {
                            MessageOverlay errorOverlay = new MessageOverlay(0, 0, 600, 100,"Error", "You do not have an active model. Set a model as default to start the server.");
                            errorOverlay.addStyle(Styles.DANGER);
                            App.window.renderPopup(errorOverlay, PopupPosition.BOTTOM_CENTER, 600, 100, false);

                        } else {
                            MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Error", "An error occurred while starting the server. Please revert changes. If issue persists, restart the application.");
                            error.addStyle(Styles.DANGER);
                            App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                        }
                    } else {
                        MessageOverlay started = new MessageOverlay(0, 0, 600, 100,"Success", "The server has been reloaded.");
                        started.addStyle(Styles.SUCCESS);
                        App.window.renderPopup(started, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                    }
                    startButton.setDisable(false);
                    reloadButton.setDisable(false);
                    stopButton.setDisable(false);
                });

            }).start();
        });

        reload.onClick(event -> {
            // Maybe attach a progress bar???
            if (ServerProcess.getCurrentServer() != null) {
                ServerProcess.getCurrentServer().stop();
            }
            if (settings.getLastModel() == null) {
                return;
            }

            Button startButton = (Button) start.getNode();
            Button reloadButton = (Button) reload.getNode();
            Button stopButton = (Button) stop.getNode();

            startButton.setDisable(true);
            reloadButton.setDisable(true);
            stopButton.setDisable(true);

            renderProgress();

            new Thread(() -> {
                ServerProcess process = new ServerProcess(settings.getLastModel());
                Platform.runLater(() -> {
                    if (process.isError()) {
                        MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Error", "An error occurred while starting the server. Please revert changes. If issue persists, restart the application.");
                        error.addStyle(Styles.DANGER);
                        App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                    } else {
                        MessageOverlay started = new MessageOverlay(0, 0, 600, 100,"Success", "The server has been reloaded.");
                        started.addStyle(Styles.SUCCESS);
                        App.window.renderPopup(started, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                    }
                    startButton.setDisable(false);
                    reloadButton.setDisable(false);
                    stopButton.setDisable(false);
                });

            }).start();
        });

        stop.onClick(event -> {
            if (ServerProcess.getCurrentServer() == null) {
                return;
            }

            ServerProcess.getCurrentServer().stop();

            // Lock buttons???
            new Thread(() -> {
                ServerProcess process = ServerProcess.getCurrentServer();
                Platform.runLater(() -> {
                    if (process.isAlive()) {
                        MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Error", "An error occurred while stopping the server. Please close or restart the app to shutdown the server.");
                        error.addStyle(Styles.DANGER);
                        App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                    } else {
                        MessageOverlay started = new MessageOverlay(0, 0, 600, 100,"Success", "The server was shutdown.");
                        started.addStyle(Styles.SUCCESS);
                        App.window.renderPopup(started, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                    }
                });


            }).start();
        });

        layout.addElements(start, reload, stop);

        return card;
    }

    private void handleServerLoad() {
        if (ServerProcess.getCurrentServer() != null) {
            ServerProcess serverProcess = ServerProcess.getCurrentServer();
            // Show progress bar only if still loading
            if (serverProcess.isLoading()) {
                start.setEnabled(false);
                stop.setEnabled(false);
                reload.setEnabled(false);
                renderProgress(); // Call renderProgress() to show your popup

                // Add a listener to be notified when loading is complete
                serverProcess.addServerLoadingListener(new ServerLoadingListener() {
                    @Override
                    public void onServerLoadingComplete(boolean success) {
                        // Ensure UI updates are on the JavaFX Application Thread
                        Platform.runLater(() -> {
                            if (App.window.getCurrentPopup() != null) { // Check if popup still exists
                                App.window.removeContainer(App.window.getCurrentPopup());
                                App.window.render();

                                start.getNode().setDisable(false);
                                stop.getNode().setDisable(false);
                                reload.getNode().setDisable(false);
                            }
                        });
                        // Crucial: Remove the listener if it's a one-time event, to prevent memory leaks
                        serverProcess.removeServerLoadingListener(this);
                    }
                });
            }
        }
    }

    private void renderProgress() {
        // Display progress bar for backend loading
        ProgressBarOverlay progress = new ProgressBarOverlay();
        progress.setWidth(120);
        progress.setMaxHeight(50);
        progress.setY(10);
        TextOverlay label = new TextOverlay("Starting backend...");
        App.window.renderPopup(progress, PopupPosition.BOTTOM_CENTER, 200, 100, false, label);
    }

    public Container getContainer() {
        return container;
    }
}
