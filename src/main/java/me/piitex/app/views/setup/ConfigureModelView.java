package me.piitex.app.views.setup;

import atlantafx.base.theme.Styles;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import me.piitex.app.App;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.Positions;
import me.piitex.engine.containers.TileContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.File;

public class ConfigureModelView extends VerticalLayout {
    private final SetupView parent;
    private final AppSettings appSettings = App.getInstance().getAppSettings();
    private final ServerSettings settings = App.getInstance().getSettings();

    public ConfigureModelView(SetupView parent) {
        super(0, 0);
        this.parent = parent;
        setWidth(appSettings.getWidth() - Positions.SIDEBAR_WIDTH - 30);
        setHeight(appSettings.getHeight());
        addStyle(Styles.BG_DEFAULT);
        setAlignment(Pos.CENTER);

        init();
    }

    private void init() {
        TextOverlay header = new TextOverlay("Please select a model location.");
        header.addStyle(Styles.TITLE_1);
        addElement(header);

        SeparatorOverlay separatorOverlay = new SeparatorOverlay(Orientation.HORIZONTAL);
        separatorOverlay.setMaxWidth(400);
        addElement(separatorOverlay);

        TextFlowOverlay textFlowOverlay = new TextFlowOverlay("You can leave this as default, but it is recommended to change. You can view more information [url=https://github.com/HackusatePvP/character-chat-app/wiki/Server-settings#model-path]here.[/url]", 700, 15);
        textFlowOverlay.setMaxWidth(textFlowOverlay.getWidth());
        addElement(textFlowOverlay);

        HorizontalLayout layout = new HorizontalLayout(600, 50);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(15);
        addElement(layout);

        TextFieldOverlay currentPath = new TextFieldOverlay(settings.getModelPath(), "");
        currentPath.setEditable(false);
        layout.addElement(currentPath);
        currentPath.onClick(event -> {
            handleClick(currentPath);
        });

        IconOverlay iconOverlay = new IconOverlay(Material2AL.FOLDER);
        iconOverlay.setIconSize(24);
        layout.addElement(iconOverlay);
        iconOverlay.onClick(event -> {
            handleClick(currentPath);
        });

        layout.setClickEvent(event -> {
            handleClick(currentPath);
        });

        ButtonOverlay next = new ButtonBuilder("next").setText("Next").addStyle(Styles.BUTTON_OUTLINED).addStyle(Styles.SUCCESS).build();
        addElement(next);
        next.onClick(event -> {
            App.reloadModelList();
            parent.getRoot().removeAllElements();
            parent.getRoot().addElement(new GPUSetupView(parent));
        });
    }

    private void handleClick(TextFieldOverlay modelPath) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select a model location.");
        File directory = chooser.showDialog(App.window.getStage());
        if (directory != null && directory.isDirectory()) {
            settings.setModelPath(directory.getAbsolutePath());
            modelPath.setCurrentText(directory.getAbsolutePath());
        }
    }

}
