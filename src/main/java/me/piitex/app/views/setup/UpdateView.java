package me.piitex.app.views.setup;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.Positions;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonBuilder;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.TextOverlay;

public class UpdateView extends VerticalLayout {
    private final SetupView parent;
    private final AppSettings appSettings = App.getInstance().getAppSettings();

    public UpdateView(SetupView parent) {
        super(0, 0);
        this.parent = parent;
        setWidth(appSettings.getWidth() - Positions.SIDEBAR_WIDTH - 30);
        setHeight(appSettings.getHeight());
        addStyle(Styles.BG_DEFAULT);
        setAlignment(Pos.CENTER);
        setSpacing(50);


        init();
    }

    private void init() {
        TextOverlay content = new TextOverlay("Updates are needed to continue. Once the update completes re-open the application.");
        addElement(content);

        ButtonOverlay update = new ButtonBuilder("update").setText("Update").addStyle(Styles.BUTTON_OUTLINED).addStyle(Styles.SUCCESS).build();
        addElement(update);
        update.onClick(event -> {
            if (!App.getInstance().getBackendUpdater().startUpdate()) {
                content.setTextFill(Color.RED);
                content.setText("Could not update! Check your network connection.");
            }
        });

    }
}
