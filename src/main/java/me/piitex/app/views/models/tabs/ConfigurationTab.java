package me.piitex.app.views.models.tabs;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.server.DeviceProcess;
import me.piitex.app.backend.server.ServerLoadingListener;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.settings.SettingsView;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.TileContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.containers.tabs.TabsContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationTab extends Tab {
    private final AppSettings appSettings;
    private final ScrollContainer scrollContainer;
    private VerticalLayout layout;
    private final TabsContainer tabsContainer;

    private ButtonOverlay start, stop, reload;

    private final ServerSettings settings = App.getInstance().getSettings();

    public ConfigurationTab(TabsContainer tabsContainer) {
        super("Settings");
        this.tabsContainer = tabsContainer;
        appSettings = App.getInstance().getAppSettings();

        // Build the list view for the models.
        layout = new VerticalLayout(0, 0);
        layout.setSpacing(0);
        layout.setX(20);
        layout.setPrefSize(appSettings.getWidth() - 500, 0);

        scrollContainer = new ScrollContainer(layout, 0, 20, appSettings.getWidth() - 300, appSettings.getHeight() - 200);
        scrollContainer.setMaxSize(appSettings.getWidth() - 300, appSettings.getHeight() - 200);
        scrollContainer.setVerticalScroll(true);
        scrollContainer.setScrollWhenNeeded(true);
        scrollContainer.setHorizontalScroll(false);
        addElement(scrollContainer); // Adds the scroll container

        layout.addElement(buildDangerZone());
        layout.addElement(buildBackend());
        layout.addElement(buildGpuDevice());
        layout.addElement(buildModelPathTile());
        layout.addElement(buildCurrentModel());
        layout.addElement(buildGpuLayers());
        layout.addElement(buildMemoryLock());
        layout.addElement(buildFlashAttention());
        layout.addElement(buildRunningModel());

    }

    public TileContainer buildModelPathTile() {
        TileContainer container = new TileContainer(0, 0);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("Model Path");
        container.setDescription("Select the folder for your models.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);

        ButtonOverlay button = new ButtonOverlay("location", "Select Location");
        button.setTooltip(settings.getModelPath());
        container.setAction(button);

        button.onClick(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setInitialDirectory(new File(settings.getModelPath()));
            File file = chooser.showDialog(App.window.getStage());
            if (file == null) return;
            App.logger.info("Updating model path to '{}'", file.getAbsolutePath());
            settings.setModelPath(file.getAbsolutePath());

            javafx.scene.control.Tab tab = tabsContainer.getTabPane().getTabs().stream().filter(tab1 -> tab1.getText().equalsIgnoreCase("list")).findAny().orElse(null);
            int index = tabsContainer.getTabPane().getTabs().indexOf(tab);
            tabsContainer.getTabPane().getTabs().set(index, new ListTab().render());

        });


        return container;
    }


    public TileContainer buildCurrentModel() {
        TileContainer container = new TileContainer(0, 0);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("Current Model");
        container.setDescription("Set the current model. This will override the default model.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);

        List<String> items = new ArrayList<>();
        items.add("Default / Last Model");
        items.addAll(App.getModelNames("exclude"));

        ServerSettings settings = App.getInstance().getSettings();

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setMaxHeight(50);
        String defaultModel = "Default / Last Model";
        if (settings.getGlobalModel() != null) {
            defaultModel = new File(settings.getGlobalModel().getFile().getParent()).getName() + "/" + settings.getGlobalModel().getFile().getName();
        }
        selection.setDefaultItem(defaultModel);
        container.setAction(selection);

        selection.onItemSelect(event -> {
            if (event.getItem().startsWith("Default /")) {
                Model model = App.getDefaultModel();
                if (model != null) {
                    settings.setGlobalModel(model.getFile().getAbsolutePath());
                }
                return;
            }
            String dir = event.getItem().split("/")[0];
            String file = event.getItem().split("/")[1];
            settings.setGlobalModel(App.getModelByName(dir, file).getFile().getAbsolutePath());
        });

        return container;
    }

    public TileContainer buildGpuLayers() {
        TileContainer container = new TileContainer(0, 0);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("GPU Layers");
        container.setDescription("The amount of layers to store in VRam, the higher the better generation speed. Can cause server errors if you run out of VRam.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(-1, 200, settings.getGpuLayers());
        input.onValueChange(event -> {
            settings.setGpuLayers((int) event.getNewValue());
        });
        container.setAction(input);

        return container;
    }

    public TileContainer buildMemoryLock() {
        TileContainer container = new TileContainer(0, 0);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("Memory Lock");
        container.setDescription("Locks model in RAM. Can improve generation times. Disables model swapping.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);

        ToggleSwitchOverlay switchOverlay = new ToggleSwitchOverlay(settings.isMemoryLock());
        switchOverlay.onToggle(event -> {
            settings.setMemoryLock(!settings.isMemoryLock());
        });
        container.setAction(switchOverlay);

        return container;
    }

    public TileContainer buildFlashAttention() {
        TileContainer container = new TileContainer(0, 0);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("Flash Attention");
        container.setDescription("Toggles flash attention. Designed to speed up training and inference while reducing memory usage. In some rare cases it can greatly reduce quality.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);

        ToggleSwitchOverlay switchOverlay = new ToggleSwitchOverlay(settings.isFlashAttention());
        switchOverlay.onToggle(event -> {
            settings.setFlashAttention(!settings.isFlashAttention());
        });
        container.setAction(switchOverlay);

        return container;
    }

    public TileContainer buildRunningModel() {
        TileContainer container = new TileContainer(0, 0);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("Current Model");
        container.setDescription("The current running model that is loaded. Will be null if no model is active.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);

        Model model = (ServerProcess.getCurrentServer() != null && ServerProcess.getCurrentServer().getModel() != null ? ServerProcess.getCurrentServer().getModel() : settings.getGlobalModel());
        if (model == null) {
            model = App.getDefaultModel();
        }
        String input = (model != null ? model.getFile().getAbsolutePath() : "null");

        InputFieldOverlay inputFieldOverlay = new InputFieldOverlay(input, 0, 0, 400, 50);
        inputFieldOverlay.setEnabled(false);

        container.setAction(inputFieldOverlay);

        return container;
    }

    public CardContainer buildDangerZone() {
        CardContainer card = new CardContainer(0, 0, layout.getWidth(), 150);
        card.setMaxSize(layout.getWidth(), 150);

        TextOverlay text = new TextOverlay("Danger Zone");
        text.setTextFill(Color.RED);
        text.addStyle(Styles.DANGER);
        card.setHeader(text);

        TextOverlay desc = new TextOverlay("Any changes made to model settings will require a reload. Please wait until a notification appears to ensure everything worked properly.");
        desc.addStyle(appSettings.getGlobalTextSize());
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

            App.getInstance().getThreadPoolManager().submitTask(() -> {
                Model model = settings.getGlobalModel();
                if (model == null) {
                    model = App.getDefaultModel();
                }
                ServerProcess newProcess = new ServerProcess(model);
                Platform.runLater(() -> {
                    if (newProcess.isError()) {
                        if (settings.getGlobalModel() == null && App.getDefaultModel() == null) {
                            MessageOverlay errorOverlay = new MessageOverlay(0, 0, 600, 100,"Error", "You do not have an active model. Set a model as default to start the server.");
                            errorOverlay.addStyle(Styles.DANGER);
                            errorOverlay.addStyle(Styles.BG_DEFAULT);
                            App.window.renderPopup(errorOverlay, PopupPosition.BOTTOM_CENTER, 600, 100, false);

                        } else {
                            MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Error", "An error occurred while starting the server. Please revert changes. If issue persists, restart the application.");
                            error.addStyle(Styles.DANGER);
                            error.addStyle(Styles.BG_DEFAULT);
                            App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                        }
                    } else {
                        MessageOverlay started = new MessageOverlay(0, 0, 600, 100,"Success", "The server has been reloaded.");
                        started.addStyle(Styles.SUCCESS);
                        started.addStyle(Styles.BG_DEFAULT);
                        App.window.renderPopup(started, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                    }
                    startButton.setDisable(false);
                    reloadButton.setDisable(false);
                    stopButton.setDisable(false);
                });

            });
        });

        reload.onClick(event -> {
            // Maybe attach a progress bar???
            if (ServerProcess.getCurrentServer() != null) {
                ServerProcess.getCurrentServer().stop();
            }

            if (settings.getGlobalModel() == null && ServerProcess.getCurrentServer() == null) {
                MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Error", "No model was detected. Please set a default model.");
                error.addStyle(Styles.DANGER);
                error.addStyle(Styles.BG_DEFAULT);
                App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                return;
            }

            Model model = (settings.getGlobalModel() != null ? settings.getGlobalModel() : ServerProcess.getCurrentServer().getModel());
            if (model == null) {
                // Lastly, look for the default model.
                model = App.getModels("exlude").stream().filter(model1 -> model1.getSettings().isDefault()).findFirst().orElse(null);
            }

            if (model == null) {
                MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Error", "No model was detected. Please set a default model.");
                error.addStyle(Styles.DANGER);
                error.addStyle(Styles.BG_DEFAULT);
                App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, false);
            }

            Button startButton = (Button) start.getNode();
            Button reloadButton = (Button) reload.getNode();
            Button stopButton = (Button) stop.getNode();

            startButton.setDisable(true);
            reloadButton.setDisable(true);
            stopButton.setDisable(true);

            renderProgress();

            Model finalModel = model;
            App.getInstance().getThreadPoolManager().submitTask(() -> {
                ServerProcess process = new ServerProcess(finalModel);
                Platform.runLater(() -> {
                    if (process.isError()) {
                        MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Error", "An error occurred while starting the server. Please revert changes. If issue persists, restart the application.");
                        error.addStyle(Styles.DANGER);
                        error.addStyle(Styles.BG_DEFAULT);
                        App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                    } else {
                        MessageOverlay started = new MessageOverlay(0, 0, 600, 100,"Success", "The server has been reloaded.");
                        started.addStyle(Styles.SUCCESS);
                        started.addStyle(Styles.BG_DEFAULT);
                        App.window.renderPopup(started, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                    }
                    startButton.setDisable(false);
                    reloadButton.setDisable(false);
                    stopButton.setDisable(false);
                });

            });
        });

        stop.onClick(event -> {
            if (ServerProcess.getCurrentServer() == null) {
                return;
            }

            ServerProcess.getCurrentServer().stop();

            App.getInstance().getThreadPoolManager().submitTask(() -> {
                ServerProcess process = ServerProcess.getCurrentServer();
                Platform.runLater(() -> {
                    if (process.isAlive()) {
                        MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Error", "An error occurred while stopping the server. Please close or restart the app to shutdown the server.");
                        error.addStyle(Styles.DANGER);
                        error.addStyle(Styles.BG_DEFAULT);
                        App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                    } else {
                        MessageOverlay started = new MessageOverlay(0, 0, 600, 100,"Success", "The server was shutdown.");
                        started.addStyle(Styles.SUCCESS);
                        started.addStyle(Styles.BG_DEFAULT);
                        App.window.renderPopup(started, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                    }
                });


            });
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

    public TileContainer buildBackend() {
        TileContainer container = new TileContainer(0, 0);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("Backend Server");
        container.setDescription("Select the compatible backend for your GPU device..");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);

        List<String> items = new ArrayList<>();
        items.add("Cuda");
        items.add("HIP");
        items.add("Vulkan");

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setMaxHeight(50);
        String item = settings.getBackend();
        item = item.replace(item.substring(0, 1), item.substring(0, 1).toUpperCase());
        selection.setDefaultItem(item);

        selection.onItemSelect(event -> {
            String newBackend = event.getItem();
            if (newBackend == null) return;
            if (App.vulkanDisable) {
                if (newBackend.equalsIgnoreCase("vulkan")) {
                    ComboBox<String> comboBox = (ComboBox<String>) selection.getNode();
                    comboBox.getSelectionModel().select(settings.getBackend());
                    MessageOverlay warning = new MessageOverlay(0, 0, 600, 100, "Vulkan Support", "Due to BSOD issues with Vulkan the backend is disabled. We are currently waiting on a fix. Thank you for your understanding.", new TextOverlay(new FontIcon(Material2MZ.OUTLINED_FLAG)));
                    warning.addStyle(Styles.WARNING);
                    warning.addStyle(Styles.BG_DEFAULT);
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

        container.setAction(selection);

        return container;
    }

    public TileContainer buildGpuDevice() {
        TileContainer container = new TileContainer(0, 0);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("Backend Server");
        container.setDescription("Select the compatible GPU for your backend. Auto will automatically choose the GPU for you. Please verify that there is one more option than Auto.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);

        ComboBoxOverlay selection = new ComboBoxOverlay(settings.getDevices(), 400, 50);
        selection.setDefaultItem(settings.getDevice());
        selection.onItemSelect(event -> {
            settings.setDevice(event.getItem());
        });
        container.setAction(selection);

        return container;
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

}
