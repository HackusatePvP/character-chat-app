package me.piitex.app.views.models.tabs;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.server.*;
import me.piitex.app.configuration.AppSettings;
import me.piitex.engine.Element;
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
import me.piitex.os.OSUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static me.piitex.app.views.Positions.*;

public class ConfigurationTab extends Tab {
    private final TabsContainer tabsContainer;
    private final AppSettings appSettings;
    private final VerticalLayout layout;
    private TextFieldOverlay runningModel;
    private ButtonOverlay start, stop, reload;

    private final ServerSettings settings = App.getInstance().getSettings();

    public ConfigurationTab(TabsContainer tabsContainer) {
        super("Settings");
        setPrefSize(MODEL_CONFIGURATION_LAYOUT_WIDTH, MODEL_CONFIGURATION_LAYOUT_HEIGHT);
        setMaxSize(getWidth(), getHeight());
        this.tabsContainer = tabsContainer;
        appSettings = App.getInstance().getAppSettings();

        // Build the list view for the models.
        layout = new VerticalLayout(MODEL_CONFIGURATION_LAYOUT_WIDTH, 0);
        layout.setSpacing(MODEL_CONFIGURATION_LAYOUT_SPACING);

        ScrollContainer scrollContainer = new ScrollContainer(layout, 0, 0, MODEL_CONFIGURATION_LAYOUT_WIDTH, MODEL_CONFIGURATION_SCROLL_HEIGHT);
        scrollContainer.setMaxSize(MODEL_CONFIGURATION_LAYOUT_WIDTH, MODEL_CONFIGURATION_SCROLL_HEIGHT);
        scrollContainer.setVerticalScroll(true);
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setScrollWhenNeeded(false);
        addElement(scrollContainer); // Adds the scroll container

        // TODO: Allow remote server routing.
        //       When enabled it allows the user to remotely connect to an endpoint.
        //       Not sure how control of the server would work.
        layout.addElement(buildServerZone());
        layout.addElement(buildHostTile());
        layout.addElement(buildRemoteModeTile());
        layout.addElement(buildBackend());
        layout.addElement(buildGpuDevice());
        layout.addElement(buildRunningModel());
        layout.addElement(buildModelPathTile());
        layout.addElement(buildCurrentModel());
        layout.addElement(buildGpuLayers());
        layout.addElement(buildMemoryLock());
        layout.addElement(buildFlashAttention());

        Platform.runLater(this::handleServerLoad);
    }

    public TileContainer buildHostTile() {
        TileContainer container = new TileContainer(0, -1);
        container.setMaxSize(layout.getWidth(), 180);
        container.setTitle("Set device as host.");
        container.setDescription("Allows other devices to connect to this devices backend server.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);
        container.addStyle(appSettings.getGlobalTextSize());

        VerticalLayout configLayout = new VerticalLayout(400, 150);
        configLayout.setSpacing(10);
        configLayout.setAlignment(Pos.CENTER_RIGHT);

        ToggleSwitchOverlay switchOverlay = new ToggleSwitchOverlay(settings.isHost());
        switchOverlay.onToggle(event -> settings.setHost(event.getNewValue()));

        configLayout.addElements(switchOverlay);
        container.setAction(configLayout);

        return container;
    }

    public TileContainer buildRemoteModeTile() {
        TileContainer container = new TileContainer(0, -1);
        container.setMaxSize(layout.getWidth(), 180);
        container.setTitle("Remote Server Mode");
        container.setDescription("Setup a remote connection to use a different device to run models.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);
        container.addStyle(appSettings.getGlobalTextSize());

        VerticalLayout configLayout = new VerticalLayout(400, 150);
        configLayout.setSpacing(10);
        configLayout.setAlignment(Pos.CENTER_RIGHT);

        ToggleSwitchOverlay switchOverlay = new ToggleSwitchOverlay(settings.isRemoteMode());
        TextFieldOverlay urlInput = new TextFieldOverlay(settings.getRemoteUrl(), 0, 0, 400, 40);
        urlInput.setHintText("Remote URL (e.g., http://192.168.1.2:8187)");

        TextFieldOverlay keyInput = new TextFieldOverlay(settings.getApiKey(), 0, 0, 400, 40);
        keyInput.setHintText("API Key (Optional)");

        switchOverlay.onToggle(event -> {
            settings.setRemoteMode(event.getNewValue());
        });
        urlInput.onInputSetEvent(event -> {
            settings.setRemoteUrl(event.getInput());
        });
        keyInput.onInputSetEvent(event -> {
            settings.setApiKey(event.getInput());
        });

        configLayout.addElements(switchOverlay, urlInput, keyInput);
        container.setAction(configLayout);

        return container;
    }

    public TileContainer buildModelPathTile() {
        TileContainer container = new TileContainer(0, -1);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("Model Path");
        container.setDescription("Select the folder for your models.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);
        container.addStyle(appSettings.getGlobalTextSize());
        container.setAction(actionButton(container));

        return container;
    }

    private ButtonOverlay actionButton(TileContainer container) {
        ButtonOverlay button = new ButtonBuilder("location").setText("Set Location").build();
        button.setTooltip(settings.getModelPath());
        container.setAction(button);
        button.onClick(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            if (OSUtil.getOS().contains("Linux") || OSUtil.getOS().contains("Ubuntu")) {
                File media = new File("/media/");
                if (media.exists() && media.isDirectory()) {
                    chooser.setInitialDirectory(media);
                } else {
                    chooser.setInitialDirectory(new File(System.getProperty("user.home")));
                }
            } else {
                File currentPath = new File(settings.getModelPath());
                if (currentPath.exists() && currentPath.isDirectory()) {
                    chooser.setInitialDirectory(currentPath);
                }
            }
            File file = chooser.showDialog(App.window.getStage());
            if (file == null) return;
            App.logger.info("Updating model path to '{}'", file.getAbsolutePath());
            settings.setModelPath(file.getAbsolutePath());

            // Updates button tooltip
            container.setAction(actionButton(container));
            tabsContainer.replaceTab(tabsContainer.getTabs().get("List"), new ListTab(tabsContainer));

            layout.replaceElement(5, buildCurrentModel());

        });

        return button;
    }


    public TileContainer buildCurrentModel() {
        TileContainer container = new TileContainer(0, -1);
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
            if (event.getNewValue().startsWith("Default /")) {
                Model model = App.getDefaultModel();
                if (model != null) {
                    settings.setGlobalModel(model.getFile().getAbsolutePath());
                }
                return;
            }
            String dir = event.getNewValue().split("/")[0];
            String file = event.getNewValue().split("/")[1];
            settings.setGlobalModel(App.getModelByName(dir, file).getFile().getAbsolutePath());
        });

        return container;
    }

    public TileContainer buildGpuLayers() {
        TileContainer container = new TileContainer(0, -1);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("GPU Usage");
        container.setDescription("Percentage of total VRAM to use. Recommended to keep below 80%.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);
        container.addStyle(appSettings.getGlobalTextSize());

        VerticalLayout action = new VerticalLayout(200, 100);
        action.setAlignment(Pos.CENTER);

        SliderOverlay input = new SliderOverlay(0, 100, settings.getGpuUsage());
        input.getSlider().setShowTickLabels(true);
        input.getSlider().setShowTickMarks(true);
        input.getSlider().setMinorTickCount(4);
        input.getSlider().getStyleClass().add(Styles.LARGE);
        input.getSlider().setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double value) {
                return String.format("%.0f%%", value);
            }

            @Override
            public Double fromString(String string) {
                return 0.0;
            }
        });
        action.addElement(input);
        long currentValue = (long) (appSettings.getTotalGpuVram() * (input.getSlider().getValue() / 100));
        TextOverlay textOverlay = new TextOverlay(String.format("%d", (long) input.getSlider().getValue()) + "%: " + String.format("%d", currentValue) + "MiB");
        action.addElement(textOverlay);
        container.setAction(action);

        input.onSliderMove(event -> {
            settings.setGpuUsage(event.getNewValue());
            long value = (long) (appSettings.getTotalGpuVram() * (event.getNewValue() / 100));
            textOverlay.setText(String.format("%d", (long) input.getSlider().getValue()) + "%: " + String.format("%d", value) + "MiB");
        });

        return container;
    }

    public TileContainer buildMemoryLock() {
        TileContainer container = new TileContainer(0, -1);
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
        TileContainer container = new TileContainer(0, -1);
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
        TileContainer container = new TileContainer(0, -1);
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

        runningModel = new TextFieldOverlay(input, 0, 0, 400, 50);
        runningModel.setEnabled(false);

        container.setAction(runningModel);

        return container;
    }

    public CardContainer buildServerZone() {
        CardContainer card = new CardContainer(0, 0, layout.getWidth(), 200);
        card.setMaxSize(layout.getWidth(), 200);

        TextOverlay text = new TextOverlay("Server Zone");
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
            if (process != null && process.isAlive()) {
                App.logger.info("Server is already running.");
                return;
            }

            start.setEnabled(true);
            reload.setEnabled(true);
            stop.setEnabled(true);

            renderProgress();
            Model model = settings.getGlobalModel();
            if (model == null) {
                model = App.getDefaultModel();
            }
            startServer(model);

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
                App.logger.info("Falling back to default model.");
                model = App.getModels("exclude").stream().filter(model1 -> model1.getSettings().isDefault()).findFirst().orElse(null);
            }

            if (model == null) {
                MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Error", "No model was detected. Please set a default model.");
                error.addStyle(Styles.DANGER);
                error.addStyle(Styles.BG_DEFAULT);
                App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                return;
            }

            if (model.getFile().setExecutable(true)) {
                App.logger.info("Updated file permission for model: {}", model.getFile().getAbsolutePath());
            }

            App.logger.info("Calculated model layers: {}", model.getSettings().getTotalLayers());

            start.setEnabled(false);
            reload.setEnabled(false);
            stop.setEnabled(false);

            renderProgress();

            Model finalModel = model;
            startServer(finalModel);

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
        boolean loading = false;
        if (ServerProcess.getCurrentServer() == null) {
            loading = true;
        } else if (ServerProcess.getCurrentServer().isLoading()) {
            loading = true;
        }

        if (loading) {
            start.setEnabled(false);
            stop.setEnabled(false);
            reload.setEnabled(false);
            renderProgress();

            ServerProcess serverProcess = ServerProcess.getCurrentServer();

            if (serverProcess == null) {
                App.getThreadPoolManager().submitSchedule(() -> {
                    if (ServerProcess.getCurrentServer() != null) { // Check again to see if it is null
                        handleServerEvent(ServerProcess.getCurrentServer());
                    } else {
                        Platform.runLater(() -> {
                            App.window.removeContainer(App.window.getCurrentPopup());

                            start.getNode().setDisable(false);
                            stop.getNode().setDisable(false);
                            reload.getNode().setDisable(false);
                        });
                    }
                }, 500, TimeUnit.MILLISECONDS);
            } else {
                if (serverProcess.isLoading()) {
                    handleServerEvent(serverProcess);
                }
            }
        }
    }

    private void handleServerEvent(ServerProcess serverProcess) {
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

    private void startServer(Model model) {
        if (settings.isRemoteMode()) {
            Platform.runLater(() -> {
                MessageOverlay started = new MessageOverlay(0, 0, 600, 100,"Success", "Connected to remote server at " + settings.getRemoteUrl());
                started.addStyle(Styles.SUCCESS);
                started.addStyle(Styles.BG_DEFAULT);
                App.window.renderPopup(started, PopupPosition.BOTTOM_CENTER, 600, 100, false);
                runningModel.setCurrentText("Remote Node Active");

                start.setEnabled(true);
                reload.setEnabled(true);
                stop.setEnabled(true);
            });
            return;
        }

        App.getThreadPoolManager().submitTask(() -> {
            ServerProcess process = new ServerProcess(model);
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
                start.setEnabled(true);
                reload.setEnabled(true);
                stop.setEnabled(true);
            });

        });
    }

    public TileContainer buildBackend() {
        TileContainer container = new TileContainer(0, -1);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("Backend Server");
        container.setDescription("Select the compatible backend for your GPU device. Application may freeze while searching for devices.");
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
            String newBackend = event.getNewValue();
            if (newBackend == null) return;

            try {
                // Thread blocks until done
                ComboBox<String> comboBox = (ComboBox<String>) selection.getNode();
                comboBox.getSelectionModel().select(newBackend);
                settings.setBackend(newBackend);
                new DeviceProcess(newBackend);

                settings.setDevice("Auto");

                // Re-build devices
                Element element = layout.getElementAt(2);
                if (element != null) {
                    layout.removeElement(2);
                    layout.addElement(buildGpuDevice(), 2);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        container.setAction(selection);

        return container;
    }

    public TileContainer buildGpuDevice() {
        TileContainer container = new TileContainer(0, -1);
        container.setMaxSize(layout.getWidth(), 100);
        container.setTitle("GPU Device");
        container.setDescription("Select the compatible GPU for your backend. Auto will automatically choose the GPU for you. Please verify that there is one more option than Auto.");
        container.addStyle(Styles.BG_DEFAULT);
        container.addStyle(Styles.BORDER_DEFAULT);
        container.addStyle(appSettings.getGlobalTextSize());

        ComboBoxOverlay selection = new ComboBoxOverlay(settings.getDevices(), 400, 50);
        selection.setDefaultItem(settings.getDevice());
        selection.onItemSelect(event -> {
            settings.setDevice(event.getNewValue());
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
