package me.piitex.app.views.models.tabs;

import atlantafx.base.theme.Styles;
import javafx.stage.DirectoryChooser;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.TileContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.containers.tabs.TabsContainer;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.ComboBoxOverlay;
import me.piitex.engine.overlays.SpinnerNumberOverlay;
import me.piitex.engine.overlays.ToggleSwitchOverlay;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationTab extends Tab {
    private final AppSettings appSettings;
    private final ScrollContainer scrollContainer;
    private VerticalLayout layout;
    private final TabsContainer tabsContainer;

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

        layout.addElement(buildModelPathTile());
        layout.addElement(buildCurrentModel());
        layout.addElement(buildGpuLayers());
        layout.addElement(buildMemoryLock());
        layout.addElement(buildFlashAttention());

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

}
