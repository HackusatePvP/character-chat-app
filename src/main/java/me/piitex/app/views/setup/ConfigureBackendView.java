package me.piitex.app.views.setup;

import atlantafx.base.theme.Styles;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;
import me.piitex.app.App;
import me.piitex.app.configuration.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.Positions;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;

public class ConfigureBackendView extends VerticalLayout {
    private final SetupView parent;
    private final AppSettings appSettings = App.getInstance().getAppSettings();
    private final ServerSettings settings = App.getInstance().getSettings();

    private TextOverlay progressText;

    public ConfigureBackendView(SetupView parent) {
        super(0, 0);
        this.parent = parent;
        setWidth(appSettings.getWidth() - Positions.SIDEBAR_WIDTH - 30);
        setHeight(appSettings.getHeight());
        addStyle(Styles.BG_DEFAULT);
        setAlignment(Pos.CENTER);

        init();
    }

    private void init() {
        String device;
        if (settings.getDevice().equals("Auto")) {
            if (settings.getDevices().size() > 1) {
                device = settings.getDevices().get(1).split(":")[1].replaceAll("\\(.*?\\)", "");
            } else {
                device = "Auto";
            }
        } else {
            device = settings.getDevice().split(":")[1].replaceAll("\\(.*?\\)", "");
        }

        TextOverlay header = new TextOverlay(device + ": Configuration");
        header.addStyle(Styles.TITLE_1);
        addElement(header);

        SeparatorOverlay separatorOverlay = new SeparatorOverlay(Orientation.HORIZONTAL);
        separatorOverlay.setMaxWidth(400);
        addElement(separatorOverlay);

        TextOverlay vramHeader = new TextOverlay("VRAM Configuration");
        vramHeader.addStyle(Styles.TITLE_2);
        addElement(vramHeader);

        VerticalLayout vramLayout = new VerticalLayout(0, -1);
        vramLayout.setMaxSize(vramLayout.getWidth(), vramLayout.getHeight());
        vramLayout.setAlignment(Pos.CENTER);
        addElement(vramLayout);

        TextOverlay vramDesc = new TextOverlay("Set the max allocated amount of VRAM to use. Leave at least 1GB for the operating system.");
        vramDesc.setTextAlignment(TextAlignment.CENTER);
        vramLayout.addElement(vramDesc);

        SliderOverlay vramInput = new SliderOverlay(0, 100, settings.getGpuUsage());
        vramInput.setWidth(400);
        vramInput.getSlider().setShowTickLabels(true);
        vramInput.getSlider().setShowTickMarks(true);
        vramInput.getSlider().setMinorTickCount(4);
        vramInput.getSlider().getStyleClass().add(Styles.LARGE);
        vramLayout.addElement(vramInput);

        long currentValue = (long) (appSettings.getTotalGpuVram() * (vramInput.getSlider().getValue() / 100));
        TextOverlay currentVram = new TextOverlay(String.format("%d", (long) vramInput.getSlider().getValue()) + "%: " + String.format("%d", currentValue) + "MiB");
        currentVram.addStyle(Styles.TITLE_4);
        vramLayout.addElement(currentVram);

        vramInput.getSlider().setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double value) {
                return String.format("%.0f%%", value);
            }

            @Override
            public Double fromString(String string) {
                return 0.0;
            }
        });
        vramInput.onSliderMove(event -> {
            long value = (long) (appSettings.getTotalGpuVram() * (event.getNewValue() / 100));
            currentVram.setText(String.format("%d", (long) vramInput.getSlider().getValue()) + "%: " + String.format("%d", value) + "MiB");
            settings.setGpuUsage(event.getNewValue());
        });

        ButtonOverlay next = new ButtonBuilder("next").setText("Next").addStyle(Styles.SUCCESS).addStyle(Styles.BUTTON_OUTLINED).build();
        addElement(next);
        next.onClick(event -> {
            parent.getRoot().removeAllElements();
            parent.getRoot().addElement(new SetupModelView(parent));
        });


    }
}
