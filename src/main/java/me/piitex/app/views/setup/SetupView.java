package me.piitex.app.views.setup;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;

public class SetupView extends EmptyContainer {
    private final AppSettings appSettings = App.getInstance().getAppSettings();
    private final SidebarView sidebarView;

    // Cached views
    private TOSView tosView;
    private VerticalLayout root;

    public SetupView(SidebarView sidebarView) {
        super(800, 600);
        this.sidebarView = sidebarView;
        setWidth(appSettings.getWidth());
        setHeight(appSettings.getHeight());
        addStyle(Styles.BG_INSET);

        init();
    }

    public void init() {
        // Initialize views
        tosView = new TOSView(this);

        HorizontalLayout layout = new HorizontalLayout(appSettings.getWidth() - 100, appSettings.getHeight());
        layout.addStyle(Styles.BG_INSET);
        layout.addElement(sidebarView);
        layout.setSpacing(15);
        addElement(layout);

        root = new VerticalLayout(appSettings.getWidth() - 265, appSettings.getHeight());
        root.setSpacing(20);
        root.setAlignment(Pos.CENTER);
        layout.addElement(root);

        // The quick setup will guide the user through required setups. TOS, GPU Device, GPU VRAM Allocation, ect
        root.addElement(tosView);
    }

    public VerticalLayout getRoot() {
        return root;
    }
}
