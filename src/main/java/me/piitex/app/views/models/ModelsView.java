package me.piitex.app.views.models;

import atlantafx.base.theme.Styles;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.SidebarView;
import me.piitex.app.views.models.tabs.ConfigurationTab;
import me.piitex.app.views.models.tabs.DownloadTab;
import me.piitex.app.views.models.tabs.ListTab;
import me.piitex.engine.Container;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.tabs.TabsContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;

public class ModelsView {
    private final AppSettings appSettings = App.getInstance().getAppSettings();
    private final Container container;

    private TabsContainer tabsContainer;

    public ModelsView() {
        container = new EmptyContainer(appSettings.getWidth(), appSettings.getHeight());
        container.addStyle(Styles.BG_INSET);
        build();
    }

    public void build() {
        HorizontalLayout layout = new HorizontalLayout(appSettings.getWidth() - 100, appSettings.getHeight());
        layout.addElement(new SidebarView(layout, false).getRoot());
        container.addElement(layout);

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

    public Container getContainer() {
        return container;
    }
}
