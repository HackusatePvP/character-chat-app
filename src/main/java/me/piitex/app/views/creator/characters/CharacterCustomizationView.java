package me.piitex.app.views.creator.characters;

import atlantafx.base.theme.Styles;
import com.drew.imaging.ImageProcessingException;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.app.utils.CharacterCardImporter;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.TileContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.*;
import me.piitex.os.configurations.InfoFile;
import org.json.JSONObject;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterCustomizationView extends EmptyContainer {
    private final VerticalLayout root;
    private final InfoFile infoFile;
    private final CharacterCreator parent;

    // Cached nodes for persistent states
    private TextFieldOverlay characterIdInput;
    private TextFieldOverlay characterDisplayInput;
    private RichTextAreaOverlay characterPersonaInput;
    private ImageOverlay characterImage;
    private VerticalLayout loreLayout;

    // Stores lore index with the string key+delimiter+value
    private final LinkedHashMap<Integer, String> tempLore = new LinkedHashMap<>();

    public CharacterCustomizationView(CharacterCreator parent, InfoFile infoFile, double width, double height) {
        super(width, height);
        addStyle(Styles.BG_DEFAULT);
        this.parent = parent;
        this.infoFile = infoFile;
        root = new VerticalLayout(width, height);
        root.setMaxSize(width, -1);
        root.addStyle(Styles.BORDER_DEFAULT);
        root.setAlignment(Pos.CENTER);
        addProperties("progress", "Character");

        ScrollContainer scrollContainer = new ScrollContainer(root, width - 25, height - 40);
        scrollContainer.setMaxSize(scrollContainer.getWidth(), scrollContainer.getHeight());
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setScrollWhenNeeded(false);
        addElement(scrollContainer);

        init();
    }

    private void init() {

        if (infoFile == null) {
            App.logger.error("Could not initialize character data!", new RuntimeException());
        }

        root.addElement(buildCharacterSettings());
    }

    public VerticalLayout buildCharacterSettings() {
        VerticalLayout layout = new VerticalLayout(720, 0);
        layout.setSpacing(20);
        layout.setAlignment(Pos.TOP_CENTER);

        TextOverlay header = new TextOverlay("General Settings");
        header.addStyle(Styles.TITLE_3);
        layout.addElement(header);

        layout.addElement(buildSettingsLayout());
        layout.addElement(buildPersonaLayout());
        layout.addElement(buildImagesLayout());
        layout.addElement(buildLorebookLayout());

        return layout;
    }

    private VerticalLayout buildSettingsLayout() {
        VerticalLayout layout = new VerticalLayout(720, VBox.USE_COMPUTED_SIZE);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());
        layout.setAlignment(Pos.TOP_CENTER);
        layout.addStyle(Styles.BORDER_DEFAULT);

        TextOverlay disclaimer = new TextOverlay("These fields are required.");
        disclaimer.addStyle(Styles.TEXT_ITALIC);
        disclaimer.setY(5); // Offset y position.
        layout.addElement(disclaimer);

        TileContainer characterId = new TileContainer(layout.getWidth(), 100);
        characterId.setMaxSize(characterId.getWidth(), characterId.getHeight());
        characterId.setTitle("Character ID*");
        characterId.setDescription("A unique name for your character. This name is not displayed.");
        layout.addElement(characterId);

        characterIdInput = new TextFieldOverlay((infoFile.hasKey("id") ? infoFile.get("id") : ""), "Character Id", 100, 35);
        characterId.setAction(characterIdInput);

        TileContainer characterDisplay = new TileContainer(layout.getWidth(), 100);
        characterDisplay.setMaxSize(characterId.getWidth(), characterId.getHeight());
        characterDisplay.setTitle("Character Display*");
        characterDisplay.setDescription("The display name for your character.");
        layout.addElement(characterDisplay);

        characterDisplayInput = new TextFieldOverlay((infoFile.hasKey("id") ? infoFile.get("id") : ""), "Character Display", 100, 35);
        characterDisplay.setAction(characterDisplayInput);

        characterIdInput.onInputSetEvent(event -> {
            infoFile.set("id", event.getInput());
            parent.revalidate();
        });

        characterDisplayInput.onInputSetEvent(event -> {
            infoFile.set("display-name", event.getInput());
            parent.revalidate();
        });

        if (parent.getCharacter() != null) {
            ButtonOverlay exportCharacter = new ButtonBuilder("exp").setText("Export Character").addStyle(Styles.ACCENT).build();
            exportCharacter.setY(-10);
            layout.addElement(exportCharacter);
        } else {
            ButtonOverlay importCharacter = new ButtonBuilder("imp").setText("Import Character").addStyle(Styles.ACCENT).build();
            importCharacter.setY(-10);
            layout.addElement(importCharacter);
            importCharacter.onClick(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Select a character card.", "*.png"));
                File file = fileChooser.showOpenDialog(App.window.getStage());
                if (file != null && file.exists() && file.isFile()) {
                    try {
                        JSONObject object = CharacterCardImporter.getImageMetaData(file);
                        String id = CharacterCardImporter.getCharacterId(object);
                        String displayName = CharacterCardImporter.getCharacterDisplayName(object);
                        String persona = CharacterCardImporter.getCharacterPersona(object);
                        Map<String, String> loreItems = CharacterCardImporter.getLoreItems(object);
                        infoFile.set("id", id);
                        infoFile.set("display-name", displayName);
                        infoFile.set("persona", persona);
                        tempLore.clear();
                        characterIdInput.setCurrentText(id);
                        characterDisplayInput.setCurrentText(displayName);
                        characterPersonaInput.setCurrentText(persona);
                        characterImage.setImage(new ImageLoader(file));
                        loreLayout.removeAllElements();
                        loreItems.forEach((s, s2) -> loreLayout.addElement(buildLoreEntry(s, s2)));
                    } catch (ImageProcessingException | IOException e) {
                        App.logger.error("Could not process character card!", e);
                    }
                }
            });
        }

        return layout;
    }

    private VerticalLayout buildPersonaLayout() {
        VerticalLayout layout = new VerticalLayout(720, 0);
        layout.setMaxSize(layout.getWidth(), -1);
        layout.setSpacing(5);
        layout.setAlignment(Pos.CENTER);

        VerticalLayout wrapper = new VerticalLayout(0, 0);
        wrapper.setSpacing(5);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        layout.addElement(wrapper);

        TextOverlay header = new TextOverlay("Persona");
        header.addStyle(Styles.TEXT_BOLDER);
        wrapper.addElement(header);

        TextOverlay description = new TextOverlay("The description and lore of the character.");
        description.addStyle(Styles.TEXT_LIGHTER);
        wrapper.addElement(description);

        characterPersonaInput = new RichTextAreaOverlay("", 720, -1);
        characterPersonaInput.setMaxSize(characterPersonaInput.getWidth(), characterPersonaInput.getHeight());
        characterPersonaInput.setBackgroundColor(App.getInstance().getAppSettings().getThemeDefaultColor(App.getInstance().getAppSettings().getTheme()));
        characterPersonaInput.setBorderColor(App.getInstance().getAppSettings().getThemeBorderColor(App.getInstance().getAppSettings().getTheme()));
        characterPersonaInput.setTextFill(App.getInstance().getAppSettings().getThemeTextColor(App.getInstance().getAppSettings().getTheme()));
        layout.addElement(characterPersonaInput);

        characterPersonaInput.onInputSetEvent(event -> {
            infoFile.set("persona", event.getInput());
        });

        return layout;
    }

    private VerticalLayout buildImagesLayout() {
        VerticalLayout layout = new VerticalLayout(720, 0);
        layout.addStyle(Styles.BORDER_DEFAULT);
        layout.setMaxSize(layout.getWidth(), -1);
        layout.setSpacing(10);
        layout.setAlignment(Pos.CENTER);

        VerticalLayout wrapper = new VerticalLayout(0, 0);
        wrapper.setSpacing(5);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        layout.addElement(wrapper);

        TextOverlay header = new TextOverlay("Image");
        header.setX(10);
        header.setY(10);
        header.addStyle(Styles.TEXT_BOLDER);
        wrapper.addElement(header);

        TextOverlay description = new TextOverlay("The image size should be sized to 256x256");
        description.setX(10);
        description.setY(10);
        description.addStyle(Styles.TEXT_LIGHTER);
        wrapper.addElement(description);

        double imageSize = 256;
        VerticalLayout imageWrapper = new VerticalLayout(256, 256);
        imageWrapper.addStyle(Styles.BORDER_DEFAULT); // A border will be applied to add a visual confirmation that the image is the correct size.
        imageWrapper.setMaxSize(imageWrapper.getWidth(), imageWrapper.getHeight());
        imageWrapper.setAlignment(Pos.CENTER);
        layout.addElement(imageWrapper);

        ImageLoader imageLoader;
        if (infoFile.hasKey("icon-path")) {
            imageLoader = new ImageLoader(new File(infoFile.get("icon-path")));
        } else {
            imageLoader = new ImageLoader(new File(App.getExecutedDirectory(), "icons/character.png"));
        }
        imageLoader.setWidth(imageSize);
        imageLoader.setHeight(imageSize);

        characterImage = new ImageOverlay(imageLoader);
        characterImage.setFitWidth(imageSize);
        characterImage.setFitHeight(imageSize);
        imageWrapper.addElement(characterImage);

        imageWrapper.setClickEvent(_ -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Select an image.", "*.png", "*.jpg"));

            File file = chooser.showOpenDialog(App.window.getStage());
            if (file != null && !file.isDirectory() && file.exists()) {

                App.logger.info("Updating image to '{}'", file.getAbsoluteFile());
                ImageLoader newImage = new ImageLoader(file);
                newImage.setWidth(imageSize);
                newImage.setHeight(imageSize);
                characterImage.setImage(newImage);

                // Re-adjust the character image location during submission.
                infoFile.set("icon-path", file.getAbsolutePath());
            }
        });

        ButtonOverlay reset = new ButtonBuilder("rst").setText("Reset Image").addStyle(Styles.FLAT).build();
        layout.addElement(reset);
        reset.onClick(_ -> {
            ImageLoader loader = new ImageLoader(new File(App.getExecutedDirectory(), "icons/character.png"));
            characterImage.setImage(loader);
            infoFile.set("icon-path", imageLoader.getFile().getAbsolutePath());
        });


        return layout;
    }

    private VerticalLayout buildLorebookLayout() {
        VerticalLayout layout = new VerticalLayout(720, 0);
        layout.setMaxSize(layout.getWidth(), -1);
        layout.setSpacing(5);
        layout.setAlignment(Pos.CENTER);

        VerticalLayout wrapper = new VerticalLayout(0, 0);
        wrapper.setSpacing(5);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        layout.addElement(wrapper);

        TextOverlay header = new TextOverlay("Lorebook");
        header.addStyle(Styles.TEXT_BOLDER);
        wrapper.addElement(header);

        TextOverlay description = new TextOverlay("Context that is dynamically added to the AI. The context is triggered by the appearance of the key.");
        description.addStyle(Styles.TEXT_LIGHTER);
        wrapper.addElement(description);

        ButtonOverlay addEntry = new ButtonBuilder("add").setText("Add Entry").addStyle(Styles.BUTTON_OUTLINED).addStyle(Styles.ACCENT).setWidth(layout.getWidth()).build();
        layout.addElement(addEntry);

        loreLayout = new VerticalLayout(root.getWidth(), -1);
        loreLayout.setMaxSize(loreLayout.getWidth(), -1);
        layout.addElement(loreLayout);

        addEntry.onClick(_ -> {
            loreLayout.addElement(buildLoreEntry("", ""), 0);
        });

        if (infoFile.hasKey("lorebook")) {
            infoFile.getSortedStringMap("lorebook").forEach((key, value) -> {
                loreLayout.addElement(buildLoreEntry(key, value));
            });
        }


        return layout;
    }

    public VerticalLayout buildLoreEntry(String key, String value) {
        VerticalLayout layout = new VerticalLayout(root.getWidth(), 300);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());

        // Index this entry
        int index = loreLayout.getElements().size();
        tempLore.put(index, key + "-" + value);

        HorizontalLayout wrapper = new HorizontalLayout(root.getWidth(), 35);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxSize(wrapper.getWidth(), wrapper.getHeight());
        layout.addElement(wrapper);

        TextFieldOverlay textFieldOverlay = new TextFieldOverlay(key, "Keys (Use comma to separate).", 700, 35);
        textFieldOverlay.setMaxSize(textFieldOverlay.getWidth(), textFieldOverlay.getHeight());
        wrapper.addElement(textFieldOverlay);

        IconOverlay delete = new IconOverlay(Material2AL.DELETE_FOREVER);
        delete.setIconSize(24);
        delete.setColor(Color.RED);
        wrapper.addElement(delete);

        delete.onClick(event -> {
            loreLayout.removeElement(layout);
            tempLore.remove(index);
        });


        RichTextAreaOverlay richTextAreaOverlay = new RichTextAreaOverlay(value, 720, 200);
        richTextAreaOverlay.setMaxSize(richTextAreaOverlay.getWidth(), richTextAreaOverlay.getHeight());
        richTextAreaOverlay.setBackgroundColor(App.getInstance().getAppSettings().getThemeDefaultColor(App.getInstance().getAppSettings().getTheme()));
        richTextAreaOverlay.setBorderColor(App.getInstance().getAppSettings().getThemeBorderColor(App.getInstance().getAppSettings().getTheme()));
        richTextAreaOverlay.setTextFill(App.getInstance().getAppSettings().getThemeTextColor(App.getInstance().getAppSettings().getTheme()));
        layout.addElement(richTextAreaOverlay);

        textFieldOverlay.onInputSetEvent(event -> {
            if (!richTextAreaOverlay.getCurrentText().isBlank()) {
                tempLore.put(index, textFieldOverlay.getCurrentText() + "-" + event.getInput());
            }
        });

        richTextAreaOverlay.onInputSetEvent(event -> {
            if (!textFieldOverlay.getCurrentText().isBlank()) {
                tempLore.put(index, event.getInput() + "-" + richTextAreaOverlay.getCurrentText());
            }
        });


        return layout;
    }

    public TextFieldOverlay getCharacterIdInput() {
        return characterIdInput;
    }

    public TextFieldOverlay getCharacterDisplayInput() {
        return characterDisplayInput;
    }

    public RichTextAreaOverlay getCharacterPersonaInput() {
        return characterPersonaInput;
    }

    public ImageOverlay getCharacterImage() {
        return characterImage;
    }

    public LinkedHashMap<Integer, String> getTempLore() {
        return tempLore;
    }

    public VerticalLayout getLoreLayout() {
        return loreLayout;
    }
}