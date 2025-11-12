package me.piitex.app.views.users.tabs;

import atlantafx.base.theme.Styles;
import com.drew.imaging.ImageProcessingException;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.utils.UserCardImporter;
import me.piitex.app.views.users.UserEditView;
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

public class UserTab extends Tab {
    private final UserEditView userEditView;

    private InputFieldOverlay userIdInput;
    private InputFieldOverlay userDisplayNameInput;
    private RichTextAreaOverlay userDescription;

    private ImageOverlay image;

    private static final AppSettings appSettings = App.getInstance().getAppSettings();

    public UserTab(String text, UserEditView userEditView) {
        super(text);
        this.userEditView = userEditView;
        init();
    }

    public void init() {
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
        userDescription = new RichTextAreaOverlay(userEditView.getUserPersona(), 600, 400 * scaleFactor);
        userDescription.setBackgroundColor(appSettings.getThemeDefaultColor(appSettings.getTheme()));
        userDescription.setBorderColor(appSettings.getThemeBorderColor(appSettings.getTheme()));
        userDescription.setTextFill(appSettings.getThemeTextColor(appSettings.getTheme()));
        userDescription.setMaxHeight(400 * scaleFactor);
        userDescription.setMaxWidth(600);
        userDescription.onInputSetEvent(event -> {
            userEditView.setUserPersona(event.getInput());
        });
        userDescription.addStyle(Styles.BG_DEFAULT);
        userDescription.addStyle(appSettings.getChatTextSize());
        userDescription.addStyle(Styles.TEXT_ON_EMPHASIS);

        rootLayout.addElement(userDescription);

        addElement(userEditView.buildSubmitBox());
    }

    private CardContainer buildUserDisplay() {
        CardContainer root = new CardContainer(0, 0, 300, 320);

        VerticalLayout layout = new VerticalLayout(300, 320);
        root.setBody(layout);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setSpacing(25);

        // Use parentView's userIconPath
        File currentIconPath = userEditView.getUserIconPath();
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

        root.onClick(event -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Select an image.", "*.img", "*.png", "*.webp", "*.jpg", "*.gif"));
            if (appSettings.getImagesPath() != null && !appSettings.getImagesPath().isEmpty()) {
                chooser.setInitialDirectory(new File(appSettings.getImagesPath()));
            }
            File selectedFile = chooser.showOpenDialog(App.window.getStage());
            if (selectedFile != null) {
                userEditView.setUserIconPath(selectedFile);
                appSettings.setImagesPath(selectedFile.getParent());
                userEditView.setUserIconPath(selectedFile.getAbsoluteFile());

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

        userIdInput = new InputFieldOverlay(userEditView.getUserId(), 0, 0, 200, 50);
        userIdInput.setEnabled(true);
        userIdInput.setHintText("Unique Identifier");
        userIdInput.onInputSetEvent(event -> {
            userEditView.setUserId(event.getInput());
        });
        if (userEditView.getUser() != null) {
            userIdInput.setEnabled(false);
        }
        root.addElement(userIdInput);

        userDisplayNameInput = new InputFieldOverlay(userEditView.getUserDisplay(), 0, 0, 200, 50);
        userDisplayNameInput.setEnabled(true);
        userDisplayNameInput.setHintText("Display Name");
        userDisplayNameInput.onInputSetEvent(event -> {
            userEditView.setUserDisplay(event.getInput());
        });
        root.addElement(userDisplayNameInput);

        ButtonOverlay importCard = new ButtonBuilder("import").setText("Import Character Card").build();
        if (userEditView.getUser() != null) {
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
                JSONObject metadata = UserCardImporter.getImageMetaData(file);
                userDisplayNameInput.setCurrentText(UserCardImporter.getUserDisplay(metadata));
                userDescription.setCurrentText(UserCardImporter.getUserPersona(metadata));

                userEditView.getLoreBook().clear();
                userEditView.getLoreBook().putAll(UserCardImporter.getLoreItems(metadata));
                userEditView.getUserLoreBookTab().buildLorebookTabContent();

                userEditView.setUserIconPath(file);
                image.setImage(new ImageLoader(file));
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

    public InputFieldOverlay getUserIdInput() {
        return userIdInput;
    }

    public InputFieldOverlay getUserDisplayNameInput() {
        return userDisplayNameInput;
    }

    public RichTextAreaOverlay getUserDescription() {
        return userDescription;
    }
}
