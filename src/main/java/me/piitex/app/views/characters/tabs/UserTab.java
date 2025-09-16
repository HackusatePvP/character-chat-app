package me.piitex.app.views.characters.tabs;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.configuration.InfoFile;
import me.piitex.app.views.characters.CharacterEditView;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UserTab extends Tab {

    private final AppSettings appSettings;
    private final InfoFile infoFile;
    private final CharacterEditView parentView;

    private InputFieldOverlay userDisplayNameInput;
    private RichTextAreaOverlay userDescription;

    private ImageOverlay image;

    public UserTab(AppSettings appSettings, InfoFile infoFile, Character character, CharacterEditView parentView) {
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

        //this.addElement(rootLayout);
        ScrollContainer scrollContainer = new ScrollContainer(rootLayout, 0, 0, appSettings.getWidth() - 300, appSettings.getHeight() - 200);
        scrollContainer.setMaxSize(scrollContainer.getWidth(), scrollContainer.getHeight());
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setPannable(true);
        this.addElement(scrollContainer);

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

        this.addElement(parentView.buildSubmitBox());
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

        root.onClick(event -> {
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

        userDisplayNameInput = new InputFieldOverlay(parentView.getUserDisplay(), 0, 0, 200, 50);
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
                }
            }

            parentView.updateInfoData();
        });

        return root;
    }

    public InputFieldOverlay getUserDisplayNameInput() {
        return userDisplayNameInput;
    }

    public RichTextAreaOverlay getUserDescription() {
        return userDescription;
    }
}