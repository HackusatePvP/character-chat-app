package me.piitex.app.views.settings;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.server.*;
import me.piitex.app.views.SidebarView;
import me.piitex.app.views.characters.CharactersView;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsView {
    private final Container container;
    private final ServerSettings settings = App.getInstance().getSettings();

    // The amount of spacing between the description and the input.
    private int layoutSpacing = 200;

    private ButtonOverlay start, stop, reload;

    public SettingsView() {
        container = new EmptyContainer(1670, 0);
        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setSpacing(35);
        container.addElement(root);

        root.addElement(new SidebarView().getRoot());

        VerticalLayout layout = new VerticalLayout(0, 0);
        layout.setOffsetX(20);
        layout.setSpacing(20);

        //FIXME: If the scroller breaks it's probably because of changes to VerticalLayout. setPrefSize() does not work with the scroller and will break it. Only use setMinSize.
        ScrollContainer scrollContainer = new ScrollContainer(layout, 0, 20, 1670, 850);
        scrollContainer.setMaxSize(1670, 850);

        scrollContainer.setVerticalScroll(true);
        scrollContainer.setScrollWhenNeeded(true);
        scrollContainer.setHorizontalScroll(false);
        root.addElement(scrollContainer);

        layout.addElement(buildDangerZone());
        layout.addElement(buildModelSelection());
        layout.addElement(buildBackend());
        layout.addElement(buildGpuDevice());
        layout.addElement(buildGpuLayers());
        layout.addElement(buildMemoryLock());
        layout.addElement(buildFlashAttention());

        // If the server is currently running but not active display progress bar
        handleServerLoad();
    }

    public CardContainer buildModelSelection() {
        // Testing out card designs
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);

        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("Select the model to use. Keeping it default will use the model configured as default or the last model used.", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        List<String> items = new ArrayList<>();
        items.add("Default / Last Model");
        items.addAll(App.getModelNames());

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setMaxHeight(50);
        selection.setDefaultItem((settings.getLastModel() != null ? settings.getLastModel().getFile().getName() : "Default / Last Model"));
        root.addElement(selection);
        selection.onItemSelect(event -> {
            if (event.getItem().startsWith("Default /")) {
                for (Model model : App.getModels()) {
                    if (model.getSettings().isDefault()) {
                        App.getInstance().getSettings().setLastModel(model.getFile().getAbsolutePath());
                        break;
                    }
                }
                return;
            }
            App.getInstance().getSettings().setLastModel(App.getModelByName(event.getItem()).getFile().getAbsolutePath());
        });
        card.setBody(root);
        return card;
    }

    public CardContainer buildBackend() {
        // Testing out card designs
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

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
            if (newBackend.equalsIgnoreCase("vulkan")) {
                ComboBox<String> comboBox = (ComboBox<String>) selection.getNode();
                comboBox.getSelectionModel().select(settings.getBackend());
                MessageOverlay warning = new MessageOverlay(0, 0, 600, 100, "Vulkan Support", "Due to BSOD issues with Vulkan the backend is disabled. We are currently waiting on a fix. Thank you for your understanding.", new TextOverlay(new FontIcon(Material2MZ.OUTLINED_FLAG)));
                warning.addStyle(Styles.WARNING);
                App.window.renderPopup(warning, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                return;
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
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

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

    public CardContainer buildGpuLayers() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("The amount of layers to store in VRam, the higher the better generation speed. Can cause server errors if you run out of VRam.", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(-1, 200, settings.getGpuLayers());
        input.onValueChange(event -> {
            settings.setGpuLayers((int) event.getNewValue());
        });
        root.addElement(input);

        card.setBody(root);

        return card;
    }

    public CardContainer buildMemoryLock() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("Locks model in RAM. Can improve generation times. Disables model swapping.", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        ToggleSwitchOverlay switchOverlay = new ToggleSwitchOverlay(settings.isMemoryLock());
        switchOverlay.onToggle(event -> {
            settings.setMemoryLock(!settings.isMemoryLock());
        });
        root.addElement(switchOverlay);

        card.setBody(root);

        return card;
    }

    public CardContainer buildFlashAttention() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("Toggles flash attention. Designed to speed up training and inference while reducing memory usage. In some rare cases it can greatly reduce quality.", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        ToggleSwitchOverlay switchOverlay = new ToggleSwitchOverlay(settings.isFlashAttention());
        switchOverlay.onToggle(event -> {
            settings.setFlashAttention(!settings.isFlashAttention());
        });
        root.addElement(switchOverlay);
        card.setBody(root);

        return card;
    }


    public CardContainer buildDangerZone() {
        CardContainer card = new CardContainer(0, 0, 1600, 150);

        TextOverlay text = new TextOverlay("Danger Zone");
        text.setTextFill(Color.RED);
        text.addStyle(Styles.DANGER);
        card.setHeader(text);

        TextOverlay desc = new TextOverlay("Any changes made to the settings will require a reload. Please wait until a notification appears to ensure everything worked properly.");
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
