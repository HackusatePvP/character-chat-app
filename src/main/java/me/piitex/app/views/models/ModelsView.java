package me.piitex.app.views.models;

import atlantafx.base.theme.Styles;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.SidebarView;
import me.piitex.app.views.models.tabs.ConfigurationTab;
import me.piitex.app.views.models.tabs.DownloadTab;
import me.piitex.app.views.models.tabs.ListTab;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.tabs.TabsContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;

public class ModelsView extends EmptyContainer {
    private final AppSettings appSettings = App.getInstance().getAppSettings();

    private TabsContainer tabsContainer;

    public ModelsView() {
        super(800, 600);
        setWidth(appSettings.getWidth());
        setHeight(appSettings.getHeight());
        addStyle(Styles.BG_INSET);
        init();
    }

    public void init() {
        HorizontalLayout layout = new HorizontalLayout(appSettings.getWidth() - 100, appSettings.getHeight());
        layout.addElement(new SidebarView(layout, false).getRoot());
        addElement(layout);

        VerticalLayout main = new VerticalLayout(appSettings.getWidth() - 300, appSettings.getHeight());
        layout.addElement(main);

        // Add the views
        tabsContainer = new TabsContainer(0, 0, appSettings.getWidth() - 300, appSettings.getHeight());
        main.addElement(tabsContainer);

        buildTabs();
    }

    public void buildTabs() {
        tabsContainer.addTab(new ConfigurationTab(this, tabsContainer));
        tabsContainer.addTab(new ListTab());
        tabsContainer.addTab(new DownloadTab());
    }
}
