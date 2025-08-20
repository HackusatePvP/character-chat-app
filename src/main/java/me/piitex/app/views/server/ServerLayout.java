package me.piitex.app.views.server;

import atlantafx.base.theme.Styles;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.overlays.TextOverlay;

public class ServerLayout extends HorizontalLayout {
    private final App app = App.getInstance();

    public ServerLayout(double width, double height) {
        super(width, height);
        setSpacing(200);
        setY(app.getAppSettings().getHeight() - 90);
        build();
    }

    public void build() {
        Model model = (ServerProcess.getCurrentServer() != null && ServerProcess.getCurrentServer().getModel() != null ? ServerProcess.getCurrentServer().getModel() : app.getSettings().getGlobalModel());
        if (model == null) {
            model = App.getDefaultModel();
        }
        String currentModel = (model != null ? model.getFile().getAbsolutePath() : "null");

        TextOverlay text = new TextOverlay("Server loaded: " + currentModel);
        text.addStyle(Styles.TEXT_SMALL);
        addElement(text);

        addStyle(Styles.BORDER_DEFAULT);
        addStyle(Styles.BG_DEFAULT);
    }

}
