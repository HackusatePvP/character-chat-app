package me.piitex.app.views.models.tabs;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.server.DeviceProcess;
import me.piitex.app.backend.server.ServerLoadingListener;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.models.ModelsView;
import me.piitex.app.views.settings.SettingsView;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.EmptyContainer;
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

import static me.piitex.app.views.Positions.*;

public class ConfigurationTab extends Tab {
    private final TabsContainer tabsContainer;
    private final ModelsView modelsView;
    private final AppSettings appSettings;
    private final ScrollContainer scrollContainer;
    private VerticalLayout layout;
    private InputFieldOverlay runningModel;
    private ButtonOverlay start, stop, reload;

    private final ServerSettings settings = App.getInstance().getSettings();

    public ConfigurationTab(ModelsView parent, TabsContainer tabsContainer) {
        super("Settings");
        this.tabsContainer = tabsContainer;
        this.modelsView = parent;
        appSettings = App.getInstance().getAppSettings();

        // Build the list view for the models.
        layout = new VerticalLayout(MODEL_CONFIGURATION_LAYOUT_WIDTH, 0);
        layout.setSpacing(MODEL_CONFIGURATION_LAYOUT_SPACING);
        layout.setX(20);

        scrollContainer = new ScrollContainer(layout, 0, 20, MODEL_CONFIGURATION_SCROLL_WIDTH, MODEL_CONFIGURATION_SCROLL_HEIGHT);
        scrollContainer.setMaxSize(MODEL_CONFIGURATION_SCROLL_WIDTH, MODEL_CONFIGURATION_SCROLL_HEIGHT);
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
        container.addStyle(appSettings.getGlobalTextSize());

        ButtonOverlay button = new ButtonBuilder("location").setText("Set Location").build();
        button.setTooltip(settings.getModelPath());
        container.setAction(button);

        button.onClick(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setInitialDirectory(new File(settings.getModelPath()));
            File file = chooser.showDialog(App.window.getStage());
            if (file == null) return;
            App.logger.info("Updating model path to '{}'", file.getAbsolutePath());
            settings.setModelPath(file.getAbsolutePath());

            // Will refresh the entire view.
            modelsView.getElements().clear();
            modelsView.build();

            Pane pane = (Pane) modelsView.getView();
            pane.getChildren().clear();
            pane.getChildren().addAll(modelsView.build().getValue());

        });


        return container;
    }


    public TileContainer buildCurrentModel() {
        TileContainer container = new TileContainer(0, 0);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("Model Selection");
        container.setDescription("Select a model to use. Will require a \"reload\".");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);
        container.addStyle(appSettings.getGlobalTextSize());

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
        container.addStyle(appSettings.getGlobalTextSize());

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
        container.addStyle(appSettings.getGlobalTextSize());

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
        container.addStyle(appSettings.getGlobalTextSize());

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
        container.addStyle(appSettings.getGlobalTextSize());

        Model model = (ServerProcess.getCurrentServer() != null && ServerProcess.getCurrentServer().getModel() != null ? ServerProcess.getCurrentServer().getModel() : settings.getGlobalModel());
        if (model == null) {
            model = App.getDefaultModel();
        }
        String input = (model != null ? model.getFile().getAbsolutePath() : "null");

        runningModel = new InputFieldOverlay(input, 0, 0, 400, 50);
        runningModel.setEnabled(false);

        container.setAction(runningModel);

        return container;
    }

    public CardContainer buildDangerZone() {
        CardContainer card = new CardContainer(0, 0, layout.getWidth(), 200);
        card.setMaxSize(layout.getWidth(), 200);

        TextOverlay text = new TextOverlay("Danger Zone");
        text.addStyle(Styles.TITLE_3);
        text.setTextFill(Color.RED);
        text.addStyle(Styles.DANGER);
        card.setHeader(text);

        TextFlowOverlay desc = new TextFlowOverlay("Any changes made to model settings will require a reload. Please wait until a notification appears to ensure everything worked properly.", (int) card.getWidth() - 50, 0);
        desc.addStyle(appSettings.getGlobalTextSize());
        card.setBody(desc);

        HorizontalLayout layout = new HorizontalLayout(0, 0);
        layout.setSpacing(20);
        layout.setAlignment(Pos.CENTER);
        card.setFooter(layout);

        start = new ButtonBuilder("start").setText("Start").build();
        start.setEnabled(true);
        start.addStyle(Styles.SUCCESS);
        start.addStyle(Styles.BUTTON_OUTLINED);

        reload = new ButtonBuilder("reload").setText("Reload").build();
        reload.setEnabled(true);
        reload.setTextFill(Color.YELLOW);
        reload.addStyle(Styles.BUTTON_OUTLINED);

        stop = new ButtonBuilder("stop").setText("Stop").build();
        stop.setEnabled(true);
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

            App.getThreadPoolManager().submitTask(() -> {
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
                        runningModel.setCurrentText(ServerProcess.getCurrentServer().getModel().getFile().getAbsolutePath());
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
            App.getThreadPoolManager().submitTask(() -> {
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
                        runningModel.setCurrentText(ServerProcess.getCurrentServer().getModel().getFile().getAbsolutePath());
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

            App.getThreadPoolManager().submitTask(() -> {
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
                        runningModel.setCurrentText("null");
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
        container.addStyle(appSettings.getGlobalTextSize());

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
        container.setTitle("GPU Device");
        container.setDescription("Select the compatible GPU for your backend. Auto will automatically choose the GPU for you. Please verify that there is one more option than Auto.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);
        container.addStyle(appSettings.getGlobalTextSize());

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
        progress.setWidth(200);
        progress.setMaxHeight(50);
        progress.setY(10);
        TextOverlay label = new TextOverlay("Starting backend...");

        EmptyContainer container = new EmptyContainer(300, 100);
        container.addStyle(Styles.BORDER_DEFAULT);
        container.addStyle(Styles.BG_DEFAULT);

        VerticalLayout layout = new VerticalLayout(300, 100);
        layout.setAlignment(Pos.CENTER);
        container.addElement(layout);
        layout.addElement(label);
        layout.addElement(progress);

        App.window.renderPopup(container, PopupPosition.BOTTOM_CENTER, 300, 100);
        //App.window.renderPopup(progress, PopupPosition.BOTTOM_CENTER, 200, 100, false, label);
    }

}
