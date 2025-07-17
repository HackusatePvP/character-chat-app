package me.piitex.app.views.characters.tabs;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.Model;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.configuration.InfoFile;
import me.piitex.app.views.characters.CharacterEditView;
import me.piitex.app.views.models.ModelEditView;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.DialogueContainer;
import me.piitex.engine.containers.TileContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.layouts.Layout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.ComboBoxOverlay;
import me.piitex.engine.overlays.MessageOverlay;
import me.piitex.engine.overlays.ToggleSwitchOverlay;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import java.util.ArrayList;
import java.util.List;

public class ModelTab extends Tab {

    private final AppSettings appSettings;
    private final InfoFile infoFile;
    private final CharacterEditView parentView;
    private final Character character;

    private ToggleSwitchOverlay modelOverride;
    private ComboBoxOverlay modelSelection;
    private Layout layout;

    public ModelTab(AppSettings appSettings, InfoFile infoFile, Character character, CharacterEditView parentView) {
        super("Model");
        this.appSettings = appSettings;
        this.infoFile = infoFile;
        this.parentView = parentView;
        this.character = character;

        buildModelTabContent();
    }

    private void buildModelTabContent() {
        this.setWidth(appSettings.getWidth() - 300);
        this.setHeight(appSettings.getHeight());

        layout = new VerticalLayout(appSettings.getWidth() - 300, appSettings.getHeight() - 100);
        layout.setAlignment(Pos.TOP_CENTER);
        this.addElement(layout);

        layout.addElement(buildOverrideSwitch());
        layout.addElement(buildModelOverride());
        layout.addElement(buildModelEdit());

        this.addElement(parentView.buildSubmitBox());

    }

    public TileContainer buildOverrideSwitch() {
        TileContainer container = new TileContainer(layout.getWidth() / 2, 200);
        container.addStyle(Styles.BORDER_DEFAULT);
        container.setMaxSize(layout.getWidth() / 2, 200);
        container.setTitle("Model Override");
        container.setDescription("When toggled the server will switch the configured model on this page.");

        modelOverride = new ToggleSwitchOverlay((character != null && character.isOverride()));
        modelOverride.onToggle(event -> {
            infoFile.set("override", event.getNewValue());
        });
        container.setAction(modelOverride);

        return container;
    }

    public TileContainer buildModelOverride() {
        TileContainer container = new TileContainer(layout.getWidth() / 2, 100);
        container.addStyle(Styles.BORDER_DEFAULT);
        container.setMaxSize(layout.getWidth() / 2, 100);
        container.setTitle("Model Selection");
        container.setDescription("This will override all other model configurations.");

        List<String> items = new ArrayList<>();
        items.add("Default / Last Model");
        items.addAll(App.getModelNames("exclude"));

        modelSelection = new ComboBoxOverlay(items, 300, 50);
        modelSelection.setMaxWidth(300);
        modelSelection.setMaxHeight(50);
        modelSelection.setDefaultItem((character != null && character.getModel() != null && !character.getModel().isEmpty() ? character.getModel() : "Default / Last Model"));
        container.setAction(modelSelection);

        modelSelection.onItemSelect(event -> {
            if (event.getItem().startsWith("Default /")) {
                infoFile.set("model", "");
                return;
            }
            Model m = App.getModelByName(event.getItem());
            if (m != null) {
                infoFile.set("model", m.getFile().getName());
            } else {
                App.logger.error("Could not find model '{}'", event.getItem());
            }
        });
        container.setAction(modelSelection);

        return container;
    }

    public TileContainer buildModelEdit() {
        TileContainer container = new TileContainer(layout.getWidth() / 2, 200);
        container.addStyle(Styles.BORDER_DEFAULT);
        container.setMaxSize(layout.getWidth() / 2, 200);
        container.setTitle("Model Settings");
        container.setDescription("Click on the gear icon to edit character specific model settings. This will override global model settings.");

        ButtonOverlay buttonOverlay = new ButtonOverlay("", new FontIcon(Material2MZ.SETTINGS));
        buttonOverlay.onClick(event -> {
            if (character == null) {
                MessageOverlay messageOverlay = new MessageOverlay("Warning", "You must create the character before editing the model settings.");
                messageOverlay.addStyle(Styles.WARNING);
                messageOverlay.addStyle(Styles.BG_DEFAULT);
                App.window.renderPopup(messageOverlay, PopupPosition.BOTTOM_CENTER, 400, 100, false);
                return;
            }

            DialogueContainer dialogueContainer = new DialogueContainer("To edit the model settings you must exit the page. Ensure you have saved any changes.", 500, 500);

            ButtonOverlay stay = new ButtonOverlay("stay", "Save");
            stay.setWidth(150);
            stay.addStyle(Styles.SUCCESS);
            stay.onClick(event1 -> {
                App.window.removeContainer(dialogueContainer);
            });

            ButtonOverlay leave = new ButtonOverlay("leave", "Configure Model Settings");
            leave.setWidth(150);
            leave.addStyle(Styles.DANGER);
            leave.onClick(event1 -> {
                App.window.clearContainers();
                App.window.addContainer(new ModelEditView(character.getModelSettings()).getContainer());
                App.window.render();
            });

            dialogueContainer.setCancelButton(stay);
            dialogueContainer.setConfirmButton(leave);

            App.window.renderPopup(dialogueContainer, PopupPosition.CENTER, 500, 500);

        });
        container.setAction(buttonOverlay);

        return container;
    }

    public ToggleSwitchOverlay getModelOverride() {
        return modelOverride;
    }

    public ComboBoxOverlay getModelSelection() {
        return modelSelection;
    }
}