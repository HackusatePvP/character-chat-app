package me.piitex.app.views.setup;

import atlantafx.base.theme.Styles;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import me.piitex.app.App;
import me.piitex.app.backend.server.DeviceProcess;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.Positions;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;

import java.io.IOException;
import java.util.LinkedList;

public class GPUSetupView extends VerticalLayout {
    private final SetupView parent;
    private final AppSettings appSettings = App.getInstance().getAppSettings();
    private final ServerSettings settings = App.getInstance().getSettings();

    private TextOverlay progressText;

    public GPUSetupView(SetupView parent) {
        super(0, 0);
        this.parent = parent;
        setWidth(appSettings.getWidth() - Positions.SIDEBAR_WIDTH - 30);
        setHeight(appSettings.getHeight());
        addStyle(Styles.BG_DEFAULT);
        setAlignment(Pos.CENTER);


        init();
    }

    private void init() {
        TextOverlay header = new TextOverlay("Setup your GPU.");
        header.addStyle(Styles.TITLE_1);
        addElement(header);

        SeparatorOverlay separatorOverlay = new SeparatorOverlay(Orientation.HORIZONTAL);
        separatorOverlay.setMaxWidth(400);
        addElement(separatorOverlay);

        VerticalLayout backendBox = new VerticalLayout(-1, -1);
        backendBox.setMaxSize(backendBox.getWidth(), backendBox.getHeight());
        backendBox.setAlignment(Pos.CENTER);
        addElement(backendBox);

        TextFlowOverlay backendDesc = new TextFlowOverlay("Select Vulkan if you are unsure. You can learn more information about backends [url=]here[/url]", 650, 15);
        backendDesc.setMaxWidth(backendDesc.getWidth());
        backendBox.addElement(backendDesc);

        LinkedList<String> backends = new LinkedList<>();
        backends.add("Vulkan");
        backends.add("Cuda");
        backends.add("HIP");

        ChoiceBoxOverlay backendSelection = new ChoiceBoxOverlay(backends);
        backendSelection.setItems(backends);
        backendSelection.setSelected("Vulkan");
        backendSelection.setMaxSize(200, 50);
        backendBox.addElement(backendSelection);
        backendSelection.onItemSelect(event -> {
            settings.setBackend(event.getNewValue());
        });

        VerticalLayout gpuBox = new VerticalLayout(-1, -1);
        gpuBox.setMaxSize(gpuBox.getWidth(), gpuBox.getHeight());
        gpuBox.setAlignment(Pos.CENTER);
        addElement(gpuBox);

        TextFlowOverlay gpuDesc = new TextFlowOverlay("A GPU is required to use this application. You can learn more about system requirements [url=]here[/url].", 650, 15);
        gpuDesc.setMaxWidth(gpuDesc.getWidth());
        gpuBox.addElement(gpuDesc);

        try {
            new DeviceProcess(settings.getBackend());
        } catch (IOException e) {
            App.logger.error("Could not load devices for Vulkan!", e);
        }

        LinkedList<String> gpus = settings.getDevices();
        ChoiceBoxOverlay gpuSelection = new ChoiceBoxOverlay(gpus);
        gpuSelection.setItems(gpus);
        gpuSelection.setMaxSize(200, 50);
        gpuSelection.setSelected(settings.getDevice());
        gpuBox.addElement(gpuSelection);

        gpuSelection.onItemSelect(event -> {
            settings.setDevice(event.getNewValue());
        });

        ButtonOverlay next = new ButtonBuilder("next").setText("Next").addStyle(Styles.SUCCESS).addStyle(Styles.BUTTON_OUTLINED).build();
        addElement(next);

        next.onClick(event -> {
            parent.getRoot().removeAllElements();
            parent.getRoot().addElement(new SetupRunnerView(parent));
        });

    }


}
