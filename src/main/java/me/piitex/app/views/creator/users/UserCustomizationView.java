package me.piitex.app.views.creator.users;

import atlantafx.base.theme.Styles;
import com.drew.imaging.ImageProcessingException;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.utils.UserCardImporter;
import me.piitex.app.utils.ImageCardExporter;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.TileContainer;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.image.BaseImageLoader;
import me.piitex.engine.loaders.image.ImageLoader;
import me.piitex.engine.overlays.*;
import me.piitex.os.configurations.InfoFile;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserCustomizationView extends EmptyContainer {
    private final VerticalLayout root;
    private final InfoFile infoFile;
    private final UserCreator parent;

    // Cached nodes for persistent states
    private TextFieldOverlay userIdInput;
    private TextFieldOverlay userDisplayInput;
    private RichTextAreaOverlay userPersonaInput;
    private ImageOverlay userImage;

    private final ScrollContainer scrollContainer;

    // Stores lore index with the string key+delimiter+value
    private final LinkedHashMap<Integer, String> tempLore = new LinkedHashMap<>();

    public UserCustomizationView(UserCreator parent, InfoFile infoFile, double width, double height) {
        super(width, height);
        addStyle(Styles.BG_DEFAULT);
        this.parent = parent;
        this.infoFile = infoFile;
        root = new VerticalLayout(width, height);
        root.setMaxSize(width, -1);
        root.addStyle(Styles.BORDER_DEFAULT);
        root.setAlignment(Pos.CENTER);
        addProperties("progress", "User");

        scrollContainer = new ScrollContainer(root, width - 5, height - 40);
        scrollContainer.setMaxSize(scrollContainer.getWidth(), scrollContainer.getHeight());
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setScrollWhenNeeded(false);
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

        TileContainer userId = new TileContainer(layout.getWidth(), 100);
        userId.setMaxSize(userId.getWidth(), userId.getHeight());
        userId.setTitle("User ID*");
        userId.setDescription("A unique name for your user. This name is not displayed.");
        layout.addElement(userId);

        userIdInput = new TextFieldOverlay((infoFile.hasKey("id") ? infoFile.get("id") : ""), "User Id", 100, 35);
        userId.setAction(userIdInput);
        userIdInput.setEnabled(parent.getUser() == null);

        TileContainer userDisplay = new TileContainer(layout.getWidth(), 100);
        userDisplay.setMaxSize(userId.getWidth(), userId.getHeight());
        userDisplay.setTitle("User Display*");
        userDisplay.setDescription("The display name for your user.");
        layout.addElement(userDisplay);

        userDisplayInput = new TextFieldOverlay((infoFile.hasKey("display-name") ? infoFile.get("display-name") : ""), "User Display", 100, 35);
        userDisplay.setAction(userDisplayInput);

        userIdInput.onInputSetEvent(event -> {
            infoFile.set("id", event.getInput());
            parent.revalidate();
        });

        userDisplayInput.onInputSetEvent(event -> {
            infoFile.set("display-name", event.getInput());
            parent.revalidate();
        });

        if (parent.getUser() != null) {
            User user = parent.getUser();
            ButtonOverlay exportUser = new ButtonBuilder("exp").setText("Export User").addStyle(Styles.ACCENT).build();
            exportUser.setY(-10);
            layout.addElement(exportUser);
            exportUser.onClick(_ -> {
                FileChooser chooser = new FileChooser();
                chooser.setInitialFileName(user.getId() + ".png");
                File file = chooser.showSaveDialog(App.window.getStage());
                if (file != null) {
                    String displayName = infoFile.get("display-name");
                    String persona = infoFile.get("persona");
                    String iconPath = infoFile.get("icon-path");
                    user.setDisplayName(displayName);
                    user.setPersona(persona);
                    user.setIconPath(iconPath);
                    //user.setLorebook(compileUserLore());
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
                        if (object != null) {
                            String id = UserCardImporter.getUserDisplay(object);
                            String displayName = UserCardImporter.getUserDisplay(object);
                            String persona = UserCardImporter.getUserPersona(object);
                            Map<String, String> loreItems = UserCardImporter.getLoreItems(object);
                            infoFile.set("id", id);
                            infoFile.set("display-name", displayName);
                            infoFile.set("persona", persona);
                            infoFile.set("icon-path", file.getAbsolutePath());
                            tempLore.clear();
                            userIdInput.setCurrentText(id);
                            userDisplayInput.setCurrentText(displayName);
                            userPersonaInput.setCurrentText(persona);
                            userImage.setImage(new BaseImageLoader(file));

                            parent.getUserLoreCustomizationView().getLoreLayout().removeAllElements();
                            loreItems.forEach((s, s2) -> parent.getUserLoreCustomizationView().getLoreLayout().addElement(parent.getUserLoreCustomizationView().buildLoreEntry(s, s2)));
                        }
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

        TextOverlay description = new TextOverlay("The description and lore of the character.");
        description.addStyle(Styles.TEXT_LIGHTER);
        wrapper.addElement(description);

        userPersonaInput = new RichTextAreaOverlay(infoFile.get("persona"), 720, -1);
        userPersonaInput.setMaxSize(userPersonaInput.getWidth(), userPersonaInput.getHeight());
        userPersonaInput.setBackgroundColor(App.getInstance().getAppSettings().getThemeDefaultColor(App.getInstance().getAppSettings().getTheme()));
        userPersonaInput.setBorderColor(App.getInstance().getAppSettings().getThemeBorderColor(App.getInstance().getAppSettings().getTheme()));
        userPersonaInput.setTextFill(App.getInstance().getAppSettings().getThemeTextColor(App.getInstance().getAppSettings().getTheme()));
        layout.addElement(userPersonaInput);

        userPersonaInput.onInputSetEvent(event -> {
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
            imageLoader = new BaseImageLoader(new File(infoFile.get("icon-path")));
        } else {
            imageLoader = new BaseImageLoader(new File(App.getExecutedDirectory(), "icons/character.png"));
        }
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
            if (file != null && file.isFile() && file.exists()) {

                App.logger.info("Updating image to '{}'", file.getAbsoluteFile());
                ImageLoader newImage = new BaseImageLoader(file);
                newImage.setWidth(imageSize);
                newImage.setHeight(imageSize);
                userImage.setImage(newImage);

                // Re-adjust the character image location during submission.
                infoFile.set("icon-path", file.getAbsolutePath());
            }
        });

        ButtonOverlay reset = new ButtonBuilder("rst").setText("Reset Image").addStyle(Styles.FLAT).build();
        layout.addElement(reset);
        reset.onClick(_ -> {
            ImageLoader loader = new BaseImageLoader(new File(App.getExecutedDirectory(), "icons/character.png"));
            userImage.setImage(loader);
            infoFile.set("icon-path", imageLoader.getFile().getAbsolutePath());
        });


        return layout;
    }

    public TextFieldOverlay getUserIdInput() {
        return userIdInput;
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
}
