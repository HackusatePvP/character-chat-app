package me.piitex.app.views.models.tabs;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.geometry.Pos;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.models.ModelEditView;
import me.piitex.app.views.models.ModelsView;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.TitledContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.Layout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.CheckBoxOverlay;
import me.piitex.engine.overlays.InputFieldOverlay;
import me.piitex.engine.overlays.TextOverlay;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

public class ListTab extends Tab {
    private final AppSettings appSettings;
    private final ScrollContainer scrollContainer;

    public ListTab() {
        super("List");
        appSettings = App.getInstance().getAppSettings();

        // Build the list view for the models.
        VerticalLayout layout = new VerticalLayout(0, 0);
        layout = new VerticalLayout(0, 0);
        layout.setSpacing(0);
        layout.setPrefSize(appSettings.getWidth() - 500, 0);

        scrollContainer = new ScrollContainer(layout, 0, 20, appSettings.getWidth() - 300, appSettings.getHeight() - 200);
        scrollContainer.setMaxSize(appSettings.getWidth() - 300, appSettings.getHeight() - 200);
        scrollContainer.setVerticalScroll(true);
        scrollContainer.setScrollWhenNeeded(true);
        scrollContainer.setHorizontalScroll(false);
        addElement(scrollContainer); // Adds the scroll container

        buildModelCards(layout); // Adds the models to the view

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
            root.setX(20);
            root.addStyle(Styles.DENSE);
            root.setMaxSize(900, 250);
            root.setSpacing(30);
            root.setAlignment(Pos.TOP_CENTER);
            root.addStyle(Tweaks.ALT_ICON);
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
                App.window.addContainer(new ModelsView().getContainer());
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
                App.window.addContainer(new ModelEditView(model.getSettings()).getContainer());
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
}
