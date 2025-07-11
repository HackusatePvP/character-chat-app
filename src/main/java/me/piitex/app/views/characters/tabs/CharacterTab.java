package me.piitex.app.views.characters.tabs;

import atlantafx.base.theme.Styles;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.annotations.Nullable;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.configuration.InfoFile;
import me.piitex.app.utils.CharacterCardImporter;
import me.piitex.app.views.characters.CharacterEditView;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.*;
import me.piitex.app.backend.Character;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CharacterTab extends Tab {

    private final AppSettings appSettings;
    private final InfoFile infoFile;
    private final CharacterEditView parentView;

    private File characterIconPath;
    private String characterId;
    private String characterDisplay;
    private String characterPersona;
    private final Map<String, String> loreItems;

    private RichTextAreaOverlay charDescription;
    private InputFieldOverlay charIdInput;
    private InputFieldOverlay charDisplayName;

    public CharacterTab(AppSettings appSettings, InfoFile infoFile, @Nullable Character character, @Nullable User user, boolean duplicate, File characterIconPath, String characterId, String characterDisplay, String characterPersona, Map<String, String> loreItems, CharacterEditView parentView) {
        super("Character");
        this.appSettings = appSettings;
        this.infoFile = infoFile;
        this.parentView = parentView;

        this.characterIconPath = characterIconPath;
        this.characterId = characterId;
        this.characterDisplay = characterDisplay;
        this.characterPersona = characterPersona;
        this.loreItems = loreItems;

        buildCharacterTabContent(character, duplicate, user);
    }

    private void buildCharacterTabContent(@Nullable Character character, boolean duplicate, @Nullable User user) {
        this.setWidth(appSettings.getWidth() - 300);
        this.setHeight(appSettings.getHeight());

        VerticalLayout rootLayout = new VerticalLayout(appSettings.getWidth() - 300, appSettings.getHeight());
        rootLayout.setSpacing(50);
        rootLayout.setAlignment(Pos.TOP_CENTER);
        this.addElement(rootLayout);

        HorizontalLayout displayBox = new HorizontalLayout(500, 200);
        displayBox.setMaxSize(500, 200);
        displayBox.addStyle(Styles.BORDER_SUBTLE);
        displayBox.setSpacing(20);
        rootLayout.addElement(displayBox);

        CardContainer displayCard = buildCharacterDisplay(character, user);
        displayBox.addElement(displayCard);
        displayBox.addElement(buildCharacterInput(character, duplicate));

        double scaleFactor = (double) appSettings.getWidth() / 1920.0;

        charDescription = new RichTextAreaOverlay(characterPersona, 600, 400 * scaleFactor);
        charDescription.setBackgroundColor(appSettings.getThemeDefaultColor(appSettings.getTheme()));
        charDescription.setBorderColor(appSettings.getThemeBorderColor(appSettings.getTheme()));
        charDescription.setTextFill(appSettings.getThemeTextColor(appSettings.getTheme()));

        charDescription.setMaxWidth(600);
        charDescription.addStyle(appSettings.getTextSize());
        charDescription.addStyle(Styles.TEXT_ON_EMPHASIS);
        rootLayout.addElement(charDescription);

        ButtonOverlay importCard = new ButtonOverlay("import", "Import Character Card");
        if (character != null) {
            importCard.setEnabled(false);
        }
        importCard.addStyle(Styles.ACCENT);
        importCard.addStyle(Styles.BUTTON_OUTLINED);
        importCard.setWidth(400);

        FileChooserOverlay fileSelector = new FileChooserOverlay(App.window, importCard);
        rootLayout.addElement(fileSelector);
        fileSelector.onFileSelect(event -> {
            File file = event.getDirectory();
            try {
                JSONObject metadata = CharacterCardImporter.getImageMetaData(file);

                parentView.setCharacterId(CharacterCardImporter.getCharacterId(metadata));
                parentView.setCharacterDisplay(CharacterCardImporter.getCharacterDisplayName(metadata));
                parentView.setCharacterPersona(CharacterCardImporter.getCharacterPersona(metadata));
                parentView.setLoreItems(CharacterCardImporter.getLoreItems(metadata));
                parentView.setChatFirstMessage(CharacterCardImporter.getFirstMessage(metadata));
                parentView.setChatScenario(CharacterCardImporter.getChatScenario(metadata));
                parentView.setCharacterIconPath(file);

                App.window.clearContainers();
                App.window.addContainer(new CharacterEditView(parentView.getCharacter(), parentView.getUser(), infoFile, this).getRoot());
                App.window.render();

            } catch (ImageProcessingException | IOException e) {
                App.logger.error("Error importing character card: ", e);
                Platform.runLater(() -> {
                    MessageOverlay errorOverlay = new MessageOverlay(0, 0, 500, 50, "Import Failed", "Could not import character card: " + e.getMessage());
                    errorOverlay.addStyle(Styles.DANGER);
                    App.window.renderPopup(errorOverlay, 650, 870, 500, 50, false, null);
                });
            }
        });

        this.addElement(parentView.buildSubmitBox());
    }

    private CardContainer buildCharacterDisplay(@Nullable Character character, @Nullable User user) {
        CardContainer root = new CardContainer(0, 0, 200, 200);
        root.setMaxSize(200, 200);

        VerticalLayout layout = new VerticalLayout(200, 200);
        layout.setMaxSize(200, 200);

        layout.setX(-10);
        layout.setY(-10);
        root.setBody(layout);
        layout.setAlignment(Pos.BASELINE_CENTER);
        layout.setSpacing(25);

        File currentIconPath = parentView.getCharacterIconPath();
        if (currentIconPath == null || !currentIconPath.exists()) {
            currentIconPath = new File(App.getAppDirectory(), "icons/character.png");
        }

        ImageOverlay image = new ImageOverlay(new ImageLoader(currentIconPath));
        image.setWidth(128);
        image.setHeight(128);
        image.setPreserveRatio(false);
        layout.addElement(image, 2);

        TextOverlay upload = new TextOverlay("Click to upload image");
        upload.setTextFill(Color.WHITE);
        upload.setUnderline(true);
        layout.addElement(upload, 5);

        root.onClick(event -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Select an image.", "*.img", "*.png", "*.webp", "*.jpg"));
            File selectedFile = chooser.showOpenDialog(App.window.getStage());
            if (selectedFile != null) {
                parentView.setCharacterIconPath(selectedFile);
                App.window.clearContainers();
                App.window.addContainer(new CharacterEditView(character, user, infoFile, this).getRoot());
                App.window.render();
            }
        });
        return root;
    }

    private VerticalLayout buildCharacterInput(@Nullable Character character, boolean duplicate) {
        VerticalLayout root = new VerticalLayout(250, 150);
        root.setAlignment(Pos.BASELINE_CENTER);
        root.setMaxSize(250, 200);
        root.setSpacing(10);

        charIdInput = new InputFieldOverlay(parentView.getCharacterId(), 0, 0, 200, 50);
        if (character != null && !duplicate) {
            charIdInput.setEnabled(false);
        }
        charIdInput.setHintText("Character ID (Must be unique)");
        charIdInput.onInputSetEvent(event -> {
            parentView.setCharacterId(event.getInput());
        });
        root.addElement(charIdInput);

        charDisplayName = new InputFieldOverlay(parentView.getCharacterDisplay(), 0, 0, 200, 50);
        charDisplayName.setHintText("Display Name");
        charDisplayName.onInputSetEvent(event -> {
            parentView.setCharacterDisplay(event.getInput());
        });
        root.addElement(charDisplayName);

        return root;
    }

    public InputFieldOverlay getCharIdInput() {
        return charIdInput;
    }

    public InputFieldOverlay getCharDisplayName() {
        return charDisplayName;
    }

    public RichTextAreaOverlay getCharDescription() {
        return charDescription;
    }
}