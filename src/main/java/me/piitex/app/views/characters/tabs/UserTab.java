package me.piitex.app.views.characters.tabs;

import atlantafx.base.theme.Styles;
import com.drew.imaging.ImageProcessingException;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.utils.ImageCardExporter;
import me.piitex.app.utils.UserCardImporter;
import me.piitex.os.configurations.InfoFile;
import me.piitex.app.views.characters.CharacterEditView;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.*;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserTab extends Tab {

    private final AppSettings appSettings;
    private final InfoFile infoFile;
    private final CharacterEditView parentView;

    private TextFieldOverlay userDisplayNameInput;
    private RichTextAreaOverlay userDescription;

    private ImageOverlay image;

    public UserTab(AppSettings appSettings, InfoFile infoFile, CharacterEditView parentView) {
        super("User");
        this.appSettings = appSettings;
        this.infoFile = infoFile;
        this.parentView = parentView;

        buildUserTabContent();
    }

    private void buildUserTabContent() {
        this.setWidth(appSettings.getWidth() - 300);
        this.setHeight(appSettings.getHeight());

        VerticalLayout rootLayout = new VerticalLayout(appSettings.getWidth() - 315, 0);
        rootLayout.setSpacing(40);
        rootLayout.setAlignment(Pos.TOP_CENTER);

        ScrollContainer scrollContainer = new ScrollContainer(rootLayout, 0, 0, appSettings.getWidth() - 300, appSettings.getHeight() - 200);
        scrollContainer.setMaxSize(scrollContainer.getWidth(), scrollContainer.getHeight());
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setPannable(true);
        addElement(scrollContainer);

        HorizontalLayout displayBox = new HorizontalLayout(600, 320);
        displayBox.setMaxSize(600, 320);
        displayBox.addStyle(Styles.BORDER_SUBTLE);
        displayBox.setSpacing(20);
        rootLayout.addElement(displayBox);

        CardContainer displayCard = buildUserDisplay();
        displayBox.addElement(displayCard);
        displayBox.addElement(buildUserInput());

        double scaleFactor = (double) appSettings.getWidth() / 1920.0;
        userDescription = new RichTextAreaOverlay(parentView.getUserPersona(), 600, 400 * scaleFactor);
        userDescription.setBackgroundColor(appSettings.getThemeDefaultColor(appSettings.getTheme()));
        userDescription.setBorderColor(appSettings.getThemeBorderColor(appSettings.getTheme()));
        userDescription.setTextFill(appSettings.getThemeTextColor(appSettings.getTheme()));
        userDescription.setMaxHeight(400 * scaleFactor);
        userDescription.setMaxWidth(600);
        userDescription.onInputSetEvent(event -> {
            parentView.setUserPersona(event.getInput());
            parentView.warnTokens();
        });
        userDescription.addStyle(Styles.BG_DEFAULT);
        userDescription.addStyle(appSettings.getChatTextSize());
        userDescription.addStyle(Styles.TEXT_ON_EMPHASIS);
        rootLayout.addElement(userDescription);

        addElement(parentView.buildSubmitBox());
    }

    private CardContainer buildUserDisplay() {
        CardContainer root = new CardContainer(0, 0, 300, 320);

        VerticalLayout layout = new VerticalLayout(300, 320);
        root.setBody(layout);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setSpacing(25);

        // Use parentView's userIconPath
        File currentIconPath = parentView.getUserIconPath();
        if (currentIconPath == null || !currentIconPath.exists() || currentIconPath.isDirectory()) {
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
        upload.setTextFill(javafx.scene.paint.Color.WHITE);
        upload.setUnderline(true);
        layout.addElement(upload);

        root.onClick(_ -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Select an image.", "*.img", "*.png", "*.webp", "*.jpg", "*.gif"));
            if (appSettings.getImagesPath() != null && !appSettings.getImagesPath().isEmpty()) {
                chooser.setInitialDirectory(new File(appSettings.getImagesPath()));
            }
            File selectedFile = chooser.showOpenDialog(App.window.getStage());
            if (selectedFile != null) {
                parentView.setUserIconPath(selectedFile);
                appSettings.setImagesPath(selectedFile.getParent());
                infoFile.set("icon-path-user", selectedFile.getAbsolutePath());

                parentView.updateInfoData();

                ImageLoader imageLoader = new ImageLoader(selectedFile);
                imageLoader.setWidth(256);
                imageLoader.setHeight(256);

                image.setImage(imageLoader);
            }
        });

        return root;
    }

    private VerticalLayout buildUserInput() {
        VerticalLayout root = new VerticalLayout(250, 150);
        root.setAlignment(Pos.BASELINE_CENTER);
        root.setMaxSize(250, 200);
        root.setSpacing(10);

        List<String> users = new ArrayList<>();
        users.add("None");
        users.addAll(App.getInstance().getUserTemplates().keySet());

        ChoiceBoxOverlay templates = new ChoiceBoxOverlay(users, 200, 50);
        root.addElement(templates);

        userDisplayNameInput = new TextFieldOverlay(parentView.getUserDisplay(), 0, 0, 200, 50);
        userDisplayNameInput.setEnabled(true);
        userDisplayNameInput.setHintText("Display Name");
        userDisplayNameInput.onInputSetEvent(event -> {
            parentView.setUserDisplay(event.getInput());
        });
        root.addElement(userDisplayNameInput);

        // Set default selection for templates if a user is already set
        if (parentView.getUser() != null) {
            templates.setDefaultItem(parentView.getUser().getId()); // Assuming user ID is the template name
        } else {
            templates.setDefaultItem("None");
        }

        templates.onItemSelect(event -> {
            String item = event.getNewValue();
            if (item.isEmpty()) return;
            User template = null;
            if (!item.equalsIgnoreCase("none")) {
                template = App.getInstance().getUser(item); // Set user object in parentView
            } else {
                userDescription.setCurrentText("");
                userDisplayNameInput.setCurrentText("");
            }

            if (template != null) {
                userDisplayNameInput.setCurrentText(template.getDisplayName());
                userDescription.setCurrentText(template.getPersona());

                if (template.getIconPath() != null && !template.getIconPath().isEmpty()) {
                    parentView.setUserIconPath(new File(template.getIconPath()));
                    image.setImage(new ImageLoader(parentView.getUserIconPath()));
                }

                if (parentView.getUser() != null) {
                    parentView.getUser().getLorebook().keySet().forEach(s -> parentView.getLoreBookTabInstance().getItems().remove(s));
                }

                parentView.getLoreBookTabInstance().getItems().putAll(template.getLorebook());
                parentView.getLoreBookTabInstance().buildLorebookTabContent();
            }

            parentView.updateInfoData();
        });

        ButtonOverlay importCard = new ButtonBuilder("import").setText("Import User Card").build();
        importCard.addStyle(Styles.ACCENT);
        importCard.addStyle(Styles.BUTTON_OUTLINED);
        importCard.setWidth(200);
        importCard.setHeight(50);

        FileChooserOverlay fileSelector = new FileChooserOverlay(App.window, importCard);
        fileSelector.setText("Import user card.");
        fileSelector.setFileExtensions(new String[]{"*.png"});
        root.addElement(fileSelector);
        fileSelector.onFileSelect(event -> {
            File file = event.getDirectory();
            try {
                JSONObject metadata = UserCardImporter.getImageMetaData(file);
                userDisplayNameInput.setCurrentText(UserCardImporter.getUserDisplay(metadata));
                userDescription.setCurrentText(UserCardImporter.getUserPersona(metadata));

                if (parentView.getUser() != null) {
                    parentView.getUser().getLorebook().keySet().forEach(s -> parentView.getLoreBookTabInstance().getItems().remove(s));
                }
                parentView.getLoreBookTabInstance().getItems().putAll(UserCardImporter.getLoreItems(metadata));
                parentView.getLoreBookTabInstance().buildLorebookTabContent();

                parentView.setUserIconPath(file);
                parentView.getInfoFile().set("icon-path-user", file.getAbsolutePath());
                image.setImage(new ImageLoader(file));

                parentView.updateInfoData();

            } catch (ImageProcessingException | IOException e) {
                App.logger.error("Error importing user card: ", e);
                Platform.runLater(() -> {
                    MessageOverlay errorOverlay = new MessageOverlay(0, 0, 500, 50, "Import Failed", "Could not import user card: " + e.getMessage());
                    errorOverlay.addStyle(Styles.DANGER);
                    errorOverlay.addStyle(Styles.BG_DEFAULT);
                    App.window.renderPopup(errorOverlay, 650, 870, 500, 50, false, null);
                });
            }
        });

        ButtonOverlay exportCard = new ButtonBuilder("export").setText("Export User Card").build();
        if (parentView.getUser() == null) {
            exportCard.setEnabled(false);
        }
        exportCard.addStyle(Styles.ACCENT);
        exportCard.addStyle(Styles.BUTTON_OUTLINED);
        exportCard.setWidth(200);
        exportCard.setHeight(50);

        FileChooserOverlay exportSelector = new FileChooserOverlay(App.window, exportCard);
        exportSelector.setText("Export user card as.");
        exportSelector.setFileExtensions(new String[]{"*.png"});
        root.addElement(exportSelector);
        exportSelector.onFileSelect(event -> {
            File file = event.getDirectory();
            try {
                // User cannot be null as the button is disabled if it is.
                ImageCardExporter.exportUser(parentView.getUser(), file);
            } catch (IOException e) {
                App.logger.error("Error importing character card: ", e);
                Platform.runLater(() -> {
                    MessageOverlay errorOverlay = new MessageOverlay(0, 0, 500, 50, "Import Failed", "Could not import user card: " + e.getMessage());
                    errorOverlay.addStyle(Styles.DANGER);
                    errorOverlay.addStyle(Styles.BG_DEFAULT);
                    App.window.renderPopup(errorOverlay, 650, 870, 500, 50, false, null);
                });
            }
        });

        return root;
    }

    public TextFieldOverlay getUserDisplayNameInput() {
        return userDisplayNameInput;
    }

    public RichTextAreaOverlay getUserDescription() {
        return userDescription;
    }
}