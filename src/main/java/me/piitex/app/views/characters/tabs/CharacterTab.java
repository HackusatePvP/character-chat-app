package me.piitex.app.views.characters.tabs;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.annotations.Nullable;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.utils.CharacterCardImporter;
import me.piitex.app.views.characters.CharacterEditView;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.*;
import me.piitex.app.backend.Character;
import org.json.JSONObject;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.File;
import java.io.IOException;

public class CharacterTab extends Tab {

    private final AppSettings appSettings;
    private final CharacterEditView parentView;

    private RichTextAreaOverlay charDescription;
    private InputFieldOverlay charIdInput;
    private InputFieldOverlay charDisplayName;
    private ImageOverlay image;

    public CharacterTab(AppSettings appSettings, @Nullable Character character, @Nullable User user, boolean duplicate, CharacterEditView parentView) {
        super("Character");
        this.appSettings = appSettings;
        this.parentView = parentView;

        buildCharacterTabContent(character, duplicate, user);
    }

    private void buildCharacterTabContent(@Nullable Character character, boolean duplicate, @Nullable User user) {
        this.setWidth(appSettings.getWidth() - 300);
        this.setHeight(appSettings.getHeight());

        VerticalLayout rootLayout = new VerticalLayout(appSettings.getWidth() - 315, 0);
        rootLayout.setSpacing(40);
        rootLayout.setAlignment(Pos.TOP_CENTER);

        //this.addElement(rootLayout);
        ScrollContainer scrollContainer = new ScrollContainer(rootLayout, 0, 0, appSettings.getWidth() - 300, appSettings.getHeight() - 200);
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setPannable(true);
        this.addElement(scrollContainer);

        HorizontalLayout displayBox = new HorizontalLayout(600, 320);
        displayBox.setMaxSize(600, 320);
        displayBox.addStyle(Styles.BORDER_SUBTLE);
        displayBox.setSpacing(20);
        rootLayout.addElement(displayBox);

        CardContainer displayCard = buildCharacterDisplay();
        displayBox.addElement(displayCard);
        displayBox.addElement(buildCharacterInput(character, duplicate));

        double scaleFactor = (double) appSettings.getWidth() / 1920.0;

        charDescription = new RichTextAreaOverlay(parentView.getCharacterPersona(), 800, 400 * scaleFactor);
        charDescription.setBackgroundColor(appSettings.getThemeDefaultColor(appSettings.getTheme()));
        charDescription.setBorderColor(appSettings.getThemeBorderColor(appSettings.getTheme()));
        charDescription.setTextFill(appSettings.getThemeTextColor(appSettings.getTheme()));
        charDescription.onInputSetEvent(event -> {
            parentView.setCharacterPersona(event.getInput());
        });

        charDescription.setMaxWidth(800);
        charDescription.addStyle(appSettings.getChatTextSize());
        charDescription.addStyle(Styles.TEXT_ON_EMPHASIS);

        rootLayout.addElement(charDescription);

        //rootLayout.addElement(buildExampleDialogue());

        this.addElement(parentView.buildSubmitBox());
    }

    private CardContainer buildCharacterDisplay() {
        CardContainer root = new CardContainer(0, 0, 300, 320);

        VerticalLayout layout = new VerticalLayout(300, 320);
        layout.setAlignment(Pos.BASELINE_CENTER);
        root.setBody(layout);

        File currentIconPath = parentView.getCharacterIconPath();
        if (currentIconPath == null || !currentIconPath.exists()) {
            currentIconPath = new File(App.getAppDirectory(), "icons/character.png");
        }

        ImageLoader loader = new ImageLoader(currentIconPath);
        loader.setWidth(256);
        loader.setHeight(256);

        image = new ImageOverlay(loader);
        image.setFitWidth(256);
        image.setFitHeight(256);
        image.setPreserveRatio(false);

        layout.addElement(image);

        TextOverlay upload = new TextOverlay("Click to upload image");
        upload.setTextFill(Color.WHITE);
        upload.setUnderline(true);
        layout.addElement(upload);

        root.onClick(event -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Select an image.", "*.img", "*.png", "*.webp", "*.jpg", "*.gif"));
            if (appSettings.getImagesPath() != null && !appSettings.getImagesPath().isEmpty()) {
                chooser.setInitialDirectory(new File(appSettings.getImagesPath()));
            }
            File selectedFile = chooser.showOpenDialog(App.window.getStage());
            if (selectedFile != null) {
                parentView.setCharacterIconPath(selectedFile);
                parentView.getInfoFile().set("icon-path", selectedFile.getAbsolutePath());
                appSettings.setImagesPath(selectedFile.getParent());

                parentView.updateInfoData();

                image.setImage(new ImageLoader(selectedFile));
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
            charIdInput.setEditable(false);
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

        ButtonOverlay importCard = new ButtonBuilder("import").setText("Import Character Card").build();
        if (character != null) {
            importCard.setEnabled(false);
        }
        importCard.addStyle(Styles.ACCENT);
        importCard.addStyle(Styles.BUTTON_OUTLINED);
        importCard.setWidth(200);
        importCard.setHeight(50);

        FileChooserOverlay fileSelector = new FileChooserOverlay(App.window, importCard);
        root.addElement(fileSelector);
        fileSelector.onFileSelect(event -> {
            File file = event.getDirectory();
            try {
                JSONObject metadata = CharacterCardImporter.getImageMetaData(file);
                charIdInput.setCurrentText(CharacterCardImporter.getCharacterId(metadata));
                charDisplayName.setCurrentText(CharacterCardImporter.getCharacterDisplayName(metadata));
                charDescription.setCurrentText(CharacterCardImporter.getCharacterPersona(metadata));

                parentView.getLoreBookTabInstance().getItems().clear();
                parentView.getLoreBookTabInstance().getItems().putAll(CharacterCardImporter.getLoreItems(metadata));
                parentView.getLoreBookTabInstance().buildLorebookTabContent();

                parentView.getChatTabInstance().getFirstMessageInput().setCurrentText(CharacterCardImporter.getFirstMessage(metadata));
                parentView.getChatTabInstance().getChatScenarioInput().setCurrentText(CharacterCardImporter.getChatScenario(metadata));

                parentView.setCharacterIconPath(file);
                parentView.getInfoFile().set("icon-path", file.getAbsolutePath());
                image.setImage(new ImageLoader(file));

                parentView.updateInfoData();

            } catch (ImageProcessingException | IOException e) {
                App.logger.error("Error importing character card: ", e);
                Platform.runLater(() -> {
                    MessageOverlay errorOverlay = new MessageOverlay(0, 0, 500, 50, "Import Failed", "Could not import character card: " + e.getMessage());
                    errorOverlay.addStyle(Styles.DANGER);
                    errorOverlay.addStyle(Styles.BG_DEFAULT);
                    App.window.renderPopup(errorOverlay, 650, 870, 500, 50, false, null);
                });
            }
        });

        return root;
    }

    public CardContainer buildExampleDialogue() {
        CardContainer container = new CardContainer(800, -1);
        container.setMaxSize(800, -1);
        container.addStyle(Styles.BORDER_DEFAULT);
        container.addStyle(Styles.BG_DEFAULT);

        HorizontalLayout header = new HorizontalLayout(-1, 0);
        header.setAlignment(Pos.TOP_CENTER);
        container.setBody(header);

        TextOverlay textHeader = new TextOverlay("Example Dialogue");
        textHeader.addStyle(Styles.TITLE_4);
        header.addElement(textHeader);

        VerticalLayout body = new VerticalLayout(800, -1);

        // Load current examples
        parentView.getExampleDialogue().forEach((s, s2) -> {
            body.addElement(addDialogueEntry(body, s2));
        });

        body.addElement(addDialogueEntry(body, null));
        container.setBody(body);

        return container;
    }

    public HorizontalLayout addDialogueEntry(VerticalLayout root, @Nullable String value) {
        HorizontalLayout dialogueBox = new HorizontalLayout(800, -1);
        dialogueBox.setSpacing(50);

        InputFieldOverlay add = new InputFieldOverlay((value != null ? value : ""), "{character}: Example dialogue for {character}", 0, 0, 500, 50);
        dialogueBox.addElement(add);

        if (value != null) {
            add.setEnabled(false);

            ButtonOverlay delete = new ButtonBuilder("delete").setIcon(new FontIcon(Material2AL.DELETE)).build();
            delete.addStyle(Styles.DANGER);
            delete.onClick(event1 -> {
                root.removeElement(dialogueBox);
            });
            dialogueBox.addElement(delete);
        } else {
            ButtonOverlay insert = new ButtonBuilder("insert").setIcon(new FontIcon(Material2AL.ADD)).build();
            dialogueBox.addElement(insert);

            insert.onClick(event -> {
                if (!add.getCurrentText().startsWith("{character}:") && !add.getCurrentText().startsWith("{user}:")) {
                    Popover popover = new Popover(new Text("Dialogue must start with literal {character}: or {user}:"));
                    popover.show(add.getNode());
                    return;
                }

                add.setEnabled(false);
                dialogueBox.removeElement(insert);

                ButtonOverlay delete = new ButtonBuilder("delete").setIcon(new FontIcon(Material2AL.DELETE)).build();
                delete.addStyle(Styles.DANGER);
                delete.onClick(event1 -> {
                    root.removeElement(dialogueBox);
                });
                dialogueBox.addElement(delete);

                root.addElement(addDialogueEntry(root, null));

                parentView.getExampleDialogue().put(parentView.getExampleDialogue().size() + "", add.getCurrentText());
            });
        }


        return dialogueBox;
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