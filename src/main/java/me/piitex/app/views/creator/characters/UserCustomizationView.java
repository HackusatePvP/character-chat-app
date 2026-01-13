package me.piitex.app.views.creator.characters;

import atlantafx.base.theme.Styles;
import com.drew.imaging.ImageProcessingException;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.utils.ImageCardExporter;
import me.piitex.app.utils.UserCardImporter;
import me.piitex.engine.Element;
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
import java.util.TreeMap;

public class UserCustomizationView extends EmptyContainer  {
    private final VerticalLayout root;
    private final InfoFile infoFile;
    private final CharacterCreator parent;

    private TextFieldOverlay userDisplayInput;
    private RichTextAreaOverlay userPersonaInput;
    private ImageOverlay userImage;
    private VerticalLayout loreLayout;
    private final LinkedHashMap<Integer, String> tempLore = new LinkedHashMap<>();

    public UserCustomizationView(CharacterCreator parent, InfoFile infoFile, double width, double height) {
        super(width, height);
        addStyle(Styles.BG_DEFAULT);
        this.parent = parent;
        this.infoFile = infoFile;
        root = new VerticalLayout(width, height);
        root.setMaxSize(width, -1);
        root.addStyle(Styles.BORDER_DEFAULT);
        root.setAlignment(Pos.CENTER);
        addProperties("progress", "User");

        ScrollContainer scrollContainer = new ScrollContainer(root, width, height);
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setScrollWhenNeeded(false);
        scrollContainer.setMaxSize(width, height);
        addElement(scrollContainer);

        init();
    }

    private void init() {

        if (infoFile == null) {
            App.logger.error("Could not initialize user data!", new RuntimeException());
        }

        root.addElement(buildUserSettings());
    }

    public VerticalLayout buildUserSettings() {
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


        TileContainer userDisplay = new TileContainer(layout.getWidth(), 100);
        userDisplay.setMaxSize(userDisplay.getWidth(), userDisplay.getHeight());
        userDisplay.setTitle("User Display*");
        userDisplay.setDescription("The display name for your user.");
        layout.addElement(userDisplay);

        userDisplayInput = new TextFieldOverlay((infoFile.hasKey("user-display-name") ? infoFile.get("user-display-name") : ""), "User Display", 100, 35);
        userDisplay.setAction(userDisplayInput);

        userDisplayInput.onInputSetEvent(event -> {
            infoFile.set("user-display-name", event.getInput());
            parent.revalidate();
        });

        if (parent.getCharacter() != null && parent.getCharacter().getUser() != null) {
            User user = parent.getCharacter().getUser();

            ButtonOverlay exportUser = new ButtonBuilder("exp").setText("Export User").addStyle(Styles.ACCENT).build();
            exportUser.setY(-10);
            layout.addElement(exportUser);
            exportUser.onClick(_ -> {
                FileChooser chooser = new FileChooser();
                chooser.setInitialFileName(user.getId() + ".png");
                File file = chooser.showSaveDialog(App.window.getStage());
                if (file != null) {
                    String displayName = infoFile.get("user-display-name");
                    String persona = infoFile.get("user-persona");
                    String iconPath = infoFile.get("user-icon-path");
                    user.setDisplayName(displayName);
                    user.setPersona(persona);
                    user.setIconPath(iconPath);
                    user.setLorebook(compileUserLore());
                    try {
                        ImageCardExporter.exportUser(user, file);
                    } catch (IOException e) {
                        App.logger.error("Could not export user!", e);
                    }
                }
            });
        } else {
            ButtonOverlay importUser = new ButtonBuilder("imp").setText("Import User").addStyle(Styles.ACCENT).build();
            importUser.setY(-10);
            layout.addElement(importUser);
            importUser.onClick(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Select a user card.", "*.png"));
                File file = fileChooser.showOpenDialog(App.window.getStage());
                if (file != null && file.exists() && file.isFile()) {
                    try {
                        JSONObject object = UserCardImporter.getImageMetaData(file);
                        String displayName = UserCardImporter.getUserDisplay(object);
                        String persona = UserCardImporter.getUserPersona(object);
                        Map<String, String> loreItems = UserCardImporter.getLoreItems(object);
                        infoFile.set("user-display-name", displayName);
                        infoFile.set("user-persona", persona);
                        infoFile.set("user-lorebook", loreItems);
                        tempLore.clear();
                        userDisplayInput.setCurrentText(displayName);
                        userPersonaInput.setCurrentText(persona);
                        userImage.setImage(new ImageLoader(file));
                        loreLayout.removeAllElements();
                        loreItems.forEach((s, s2) -> loreLayout.addElement(buildLoreEntry(s, s2)));
                    } catch (ImageProcessingException | IOException e) {
                        App.logger.error("Could not process user card!", e);
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

        TextOverlay description = new TextOverlay("The description and lore of the user.");
        description.addStyle(Styles.TEXT_LIGHTER);
        wrapper.addElement(description);

        userPersonaInput = new RichTextAreaOverlay(infoFile.getOrDefault("user-persona", ""), 720, -1);
        userPersonaInput.setMaxSize(userPersonaInput.getWidth(), userPersonaInput.getHeight());
        userPersonaInput.setBackgroundColor(App.getInstance().getAppSettings().getThemeDefaultColor(App.getInstance().getAppSettings().getTheme()));
        userPersonaInput.setBorderColor(App.getInstance().getAppSettings().getThemeBorderColor(App.getInstance().getAppSettings().getTheme()));
        userPersonaInput.setTextFill(App.getInstance().getAppSettings().getThemeTextColor(App.getInstance().getAppSettings().getTheme()));
        layout.addElement(userPersonaInput);

        userPersonaInput.onInputSetEvent(event -> {
            infoFile.set("user-persona", event.getInput());
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

        File image = new File(infoFile.getOrDefault("user-icon-path", new File(App.getExecutedDirectory(), "icons/character.png").getAbsolutePath()));

        if (!image.exists() || image.isDirectory()) {
            image = new File(App.getExecutedDirectory(), "icons/character.png");
        }

        ImageLoader imageLoader = new ImageLoader(image);

        imageLoader.setWidth(imageSize);
        imageLoader.setHeight(imageSize);

        userImage = new ImageOverlay(imageLoader);
        userImage.setFitWidth(imageSize);
        userImage.setFitHeight(imageSize);
        imageWrapper.addElement(userImage);

        imageWrapper.onClick(_ -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Select an image.", "*.png", "*.jpg"));

            File file = chooser.showOpenDialog(App.window.getStage());
            if (file != null && !file.isDirectory() && file.exists()) {

                App.logger.info("Updating image to '{}'", file.getAbsoluteFile());
                ImageLoader newImage = new ImageLoader(file);
                newImage.setWidth(imageSize);
                newImage.setHeight(imageSize);
                userImage.setImage(newImage);

                // Re-adjust the user image location during submission.
                infoFile.set("user-icon-path", file.getAbsolutePath());
            }
        });

        ButtonOverlay reset = new ButtonBuilder("rst").setText("Reset Image").addStyle(Styles.FLAT).build();
        layout.addElement(reset);
        reset.onClick(_ -> {
            ImageLoader loader = new ImageLoader(new File(App.getExecutedDirectory(), "icons/character.png"));
            userImage.setImage(loader);
            infoFile.set("user-icon-path", loader.getFile().getAbsolutePath());
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

        if (infoFile.hasKey("user-lore")) {
            infoFile.getSortedStringMap("user-lore").forEach((key, value) -> {
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

    public TextFieldOverlay getUserDisplayInput() {
        return userDisplayInput;
    }

    public RichTextAreaOverlay getUserPersonaInput() {
        return userPersonaInput;
    }

    public ImageOverlay getUserImage() {
        return userImage;
    }

    public VerticalLayout getLoreLayout() {
        return loreLayout;
    }

    public TreeMap<String, String> compileUserLore() {
        TreeMap<String, String> toReturn = new TreeMap<>();

        for (Element element : getLoreLayout().getElements().values()) {
            // All entires are vertical layouts
            VerticalLayout root = (VerticalLayout) element;

            // The key is in the first horizontal layout at the first index.
            HorizontalLayout horizontalLayout = (HorizontalLayout) root.getElements().firstEntry().getValue();

            TextFieldOverlay loreKey = (TextFieldOverlay) horizontalLayout.getElements().firstEntry().getValue();
            String key = loreKey.getCurrentText();
            if (key == null || key.isEmpty()) {
                continue;
            }

            // The value is the second index of the root
            RichTextAreaOverlay loreValue = (RichTextAreaOverlay) root.getElements().lastEntry().getValue();
            String value = loreValue.getCurrentText();
            if (value == null || value.isEmpty()) {
                continue;
            }

            toReturn.put(key, value);
        }

        return toReturn;
    }
}