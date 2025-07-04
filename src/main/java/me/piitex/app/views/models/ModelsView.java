package me.piitex.app.views.models;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.Container;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.TitledContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.Layout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

public class ModelsView {
    private final Container container;
    private ScrollContainer scrollContainer;

    private AppSettings appSettings = App.getInstance().getAppSettings();

    public ModelsView(double scrollPosition) {
        container = new EmptyContainer(appSettings.getWidth() - 300, 1500);
        HorizontalLayout root = new HorizontalLayout(appSettings.getWidth() - 100, 0);
        root.setSpacing(35);
        container.addElement(root);

        root.addElement(new SidebarView().getRoot());

        VerticalLayout layout = new VerticalLayout(0, 0);
        layout.setSpacing(0);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPrefSize(appSettings.getWidth() - 300, 0);

        //FIXME: If the scroller breaks it's probably because of changes to VerticalLayout. setPrefSize() does not work with the scroller and will break it. Only use setMinSize.
        scrollContainer = new ScrollContainer(layout, 0, 20, appSettings.getWidth() - 250, appSettings.getHeight() - 100);
        scrollContainer.setMaxSize(appSettings.getWidth() - 250, appSettings.getHeight() - 100);
        scrollContainer.setVerticalScroll(true);
        scrollContainer.setScrollWhenNeeded(true);
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setScrollPosition(scrollPosition);
        root.addElement(scrollContainer);

        layout.addElement(buildModelLocation());
        // Add models to layout
        buildModelCards(layout);
    }


    public CardContainer buildModelLocation() {
        CardContainer root = new CardContainer(800, 100);
        root.setMaxSize(800, 200);

        HorizontalLayout body = new HorizontalLayout(400, 100);
        body.setAlignment(Pos.CENTER_LEFT);
        body.setSpacing(50);
        root.setBody(body);

        ButtonOverlay directoryButton = new ButtonOverlay("select", "Select models location");
        directoryButton.setWidth(200);
        directoryButton.setHeight(50);
        DirectoryOverlay directory = new DirectoryOverlay(App.window, directoryButton);
        body.addElement(directory);

        InputFieldOverlay currentDirectory = new InputFieldOverlay(App.getInstance().getSettings().getModelPath(), 0, 0, 400, 50);
        currentDirectory.setEnabled(false);
        body.addElement(currentDirectory);

        directory.onDirectorySelect(event -> {
            File file = event.getDirectory();
            if (file != null && file.exists() && file.isDirectory()) {
                App.getInstance().getSettings().setModelPath(file.getAbsolutePath());

                App.window.clearContainers();
                App.window.addContainer(new ModelsView(scrollContainer.getScrollPane().getVvalue()).getContainer());
                App.window.render();
            }
        });

        return root;
    }

    public void buildModelCards(Layout layout) {

        boolean def = false;

        for (Model model : App.getModels("exclude")) {
            if (model.getSettings().isDefault()) {
                if (!def) {
                    def = true;
                } else {
                    model.getSettings().setDefault(false);
                }
            }
            File modelFile = model.getFile();
            long fileSizeInBytes = modelFile.length();
            double fileSizeInGB = (double) fileSizeInBytes / (1024 * 1024 * 1024);

            // Format to two decimal places
            DecimalFormat df = new DecimalFormat("#.##");
            String formattedFileSize = df.format(fileSizeInGB);

            TitledContainer root = new TitledContainer(model.getFile().getName() + " (" + formattedFileSize + "GB)", 0, 0);
            root.setMaxSize(900, 250);
            root.setSpacing(30);
            root.setAlignment(Pos.TOP_CENTER);
            root.setExpanded(model.getSettings().isDefault());

            HorizontalLayout body = new HorizontalLayout(800, 50);
            body.setSpacing(10);
            root.addElement(body);

            TextOverlay folder = new TextOverlay(new FontIcon(Material2AL.FOLDER));
            folder.setY(15);
            folder.setTooltip("Open file location.");
            body.addElement(folder);
            folder.onClick(event -> {
                try {
                    Runtime.getRuntime().exec("explorer.exe /select,\"" + model.getFile().getAbsolutePath() + "\"");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            InputFieldOverlay location = new InputFieldOverlay(model.getFile().getAbsolutePath(), 0, 0, 500, 50);
            location.setEnabled(false);
            body.addElement(location);

            HorizontalLayout footer = new HorizontalLayout(800, 50);
            root.addElement(footer);

            CheckBoxOverlay defaultModel = new CheckBoxOverlay(model.getSettings().isDefault(),"Set as default");
            footer.addElement(defaultModel);
            defaultModel.onSet(event -> {
                // If set to true scan all models and set default to false
                // Then set this one to true
                // Also, re-render the view

                // Reloop models and disable any defaults
                for (Model m : App.getModels("exclude")) {
                    if (m == model) continue;
                    if (m.getSettings().isDefault()) {
                        m.getSettings().setDefault(false);
                    }
                }
                model.getSettings().setDefault(event.getNewValue());

                App.window.clearContainers();
                App.window.addContainer(new ModelsView(scrollContainer.getScrollPane().getVvalue()).getContainer());
                App.window.render();
            });

            HorizontalLayout subFooter = new HorizontalLayout(400, 50);
            subFooter.setX(20);
            subFooter.setSpacing(40);
            footer.addElement(subFooter);

            TextOverlay settings = new TextOverlay(new FontIcon(Material2MZ.SETTINGS));
            settings.setTooltip("Go to model settings.");
            subFooter.addElement(settings);
            settings.addStyle(Styles.ACCENT);
            settings.addStyle(Styles.LARGE);
            settings.onClick(event -> {
                App.window.clearContainers();
                App.window.addContainer(new ModelEditView(model).getContainer());
                App.window.render();
            });

            TextOverlay delete = new TextOverlay(new FontIcon(Material2AL.DELETE_FOREVER));
            delete.setTooltip("Delete the model.");
            delete.addStyle(Styles.DANGER);
            subFooter.addElement(delete);
            delete.onClick(event -> {
                // Confirm the model deletion.
            });

            layout.addElement(root);
        }
    }

    public Container getContainer() {
        return container;
    }
}
