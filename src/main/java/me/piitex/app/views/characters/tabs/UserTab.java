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
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserTab extends Tab {

    private final AppSettings appSettings;
    private final InfoFile infoFile;
    private final CharacterEditView parentView;

    // Fields to be managed by CharacterEditView
    private File userIconPath;
    private String userDisplay;
    private String userPersona;
    private User user; // Reference to the User object in parentView

    // UI elements
    private InputFieldOverlay userDisplayNameInput;
    private TextAreaOverlay userDescription;

    public UserTab(AppSettings appSettings, InfoFile infoFile, Character character, User user, File userIconPath, String userDisplay, String userPersona, Map<String, String> loreItems, CharacterEditView parentView) {
        super("User");
        this.appSettings = appSettings;
        this.infoFile = infoFile;
        this.parentView = parentView;

        // Initialize local fields from parentView's state
        this.user = user; // This is the User object from parentView
        this.userIconPath = userIconPath;
        this.userDisplay = userDisplay;
        this.userPersona = userPersona;

        buildUserTabContent(character);
    }

    private void buildUserTabContent(Character character) {
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

        CardContainer displayCard = buildUserDisplay(character);
        displayBox.addElement(displayCard);
        displayBox.addElement(buildUserInput(character));

        double scaleFactor = (double) appSettings.getWidth() / 1920.0;
        userDescription = new TextAreaOverlay(userPersona, 0, 0, 600, 400 * scaleFactor);
        userDescription.setHintText("Describe the user and provide key lore.");
        rootLayout.addElement(userDescription);
        userDescription.onInputSetEvent(event -> {
            parentView.setUserPersona(event.getInput());
            parentView.warnTokens(); // Call warnTokens on parent view
        });

        this.addElement(parentView.buildSubmitBox());
    }

    private CardContainer buildUserDisplay(Character character) {
        CardContainer root = new CardContainer(0, 0, 200, 200);
        root.setMaxSize(200, 200);

        VerticalLayout layout = new VerticalLayout(200, 200);
        layout.setX(-10);
        layout.setY(-10);
        root.setBody(layout);
        layout.setAlignment(Pos.BASELINE_CENTER);
        layout.setSpacing(25);

        // Use parentView's userIconPath
        File currentIconPath = parentView.getUserIconPath();
        if (currentIconPath == null || !currentIconPath.exists()) {
            currentIconPath = new File(App.getAppDirectory(), "icons/character.png");
        }

        ImageOverlay image = new ImageOverlay(new ImageLoader(currentIconPath));
        image.setWidth(128);
        image.setHeight(128);
        image.setPreserveRatio(false);
        layout.addElement(image, 2);

        TextOverlay upload = new TextOverlay("Click to upload image");
        upload.setTextFill(javafx.scene.paint.Color.WHITE);
        upload.setUnderline(true);
        layout.addElement(upload, 5);

        root.onClick(event -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Select an image.", "*.img", "*.png", "*.webp", "*.jpg"));
            File selectedFile = chooser.showOpenDialog(App.window.getStage());
            if (selectedFile != null) {
                parentView.setUserIconPath(selectedFile);
                App.window.clearContainers();
                App.window.addContainer(new CharacterEditView(character, parentView.getUser(), infoFile, this).getRoot());
                App.window.render();
            }
        });

        return root;
    }

    private VerticalLayout buildUserInput(Character character) {
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
            String item = event.getItem();
            if (item.isEmpty()) return;
            User template = null;
            if (!item.equalsIgnoreCase("none")) {
                System.out.println("Item: " + item);
                System.out.println("User: " + App.getInstance().getUser(item));
                template = App.getInstance().getUser(item); // Set user object in parentView
            }
            // Re-render the CharacterEditView to reflect the selected user template
            App.window.clearContainers();
            App.window.addContainer(new CharacterEditView(character, template, infoFile, this).getRoot());
            App.window.render();
        });

        return root;
    }

    // Getters for validation in CharacterEditView
    public InputFieldOverlay getUserDisplayNameInput() {
        return userDisplayNameInput;
    }

    public TextAreaOverlay getUserDescription() {
        return userDescription;
    }
}
