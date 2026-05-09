package me.piitex.app.views.creator.users;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;

import me.piitex.app.views.HomeView;
import me.piitex.engine.containers.Container;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonBuilder;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.IconOverlay;
import me.piitex.engine.overlays.TextOverlay;
import me.piitex.os.configurations.InfoFile;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.material2.Material2AL;

public class UserCreator extends EmptyContainer {
    private final AppSettings appSettings = App.getInstance().getAppSettings();
    private final User user;
    private final InfoFile infoFile;

    private IconOverlay userIcon;
    private IconOverlay loreIcon;
    private IconOverlay finishIcon;
    private ButtonOverlay submission;

    private UserCustomizationView userCustomizationView;
    private UserLoreCustomizationView userLoreCustomizationView;
    private UserFinishView userFinishView;

    private HorizontalLayout displayContent;
    private Container currentView;

    private final double contentWidth = appSettings.getWidth() - 300;
    private final double contentHeight = appSettings.getHeight();

    /**
     * Constructor for editing or creating a user
     * @param user If null a new user will be created. Else it will be editing an existing user.
     */
    public UserCreator(@Nullable User user) {
        // Two buttons that will guide through either the character or user view.
        super(800, 600);
        this.user = user;
        if (user != null) {
            infoFile = InfoFile.copy(user.getInfoFile());
        } else {
            infoFile = new InfoFile();
        }
        init();
    }

    public void init() {
        setWidth(appSettings.getWidth());
        setHeight(appSettings.getHeight());
        addStyle(Styles.BG_INSET);

//        HorizontalLayout layout = new HorizontalLayout(appSettings.getWidth() - 100, appSettings.getHeight());
//        layout.addStyle(Styles.BG_INSET);
//        layout.addElement(new SidebarView(false));
//        addElement(layout);

        VerticalLayout main = new VerticalLayout(appSettings.getWidth() - 265, appSettings.getHeight());
        main.setSpacing(20);
        addElement(main);

        displayContent = new HorizontalLayout(appSettings.getWidth(), appSettings.getHeight());
        displayContent.setSpacing(20);
        main.addElement(displayContent);

        userCustomizationView = new UserCustomizationView(this, infoFile, contentWidth, contentHeight);
        // Lore customization
        userLoreCustomizationView = new UserLoreCustomizationView(this, infoFile, contentWidth, contentHeight);
        userFinishView = new UserFinishView(this, infoFile, contentWidth, contentHeight);

        currentView = userCustomizationView;

        displayContent.addElement(buildChecklist());

        // Character Customization will be displayed first.
        displayContent.addElement(userCustomizationView);

        // Check validations, if this is a new character all the indicators will be red.
        // If this is an existing character it will validate each section.
        revalidate();
    }

    private VerticalLayout buildChecklist() {
        VerticalLayout root = new VerticalLayout(250, appSettings.getHeight());
        root.setSpacing(40);
        root.addStyle(Styles.BORDER_SUBTLE);
        root.setAlignment(Pos.TOP_CENTER);

        ButtonOverlay homeButton = new ButtonBuilder("home").setText("Home").addStyle(Styles.FLAT).build();
        root.addElement(homeButton);
        homeButton.onClick(_ -> {
            App.window.clearContainers();
            App.window.addContainer(new HomeView());
        });
        ButtonOverlay userButton = new ButtonBuilder("uc").setGraphic(buildUserButton(root.getWidth())).addStyle(Styles.FLAT).build();
        root.addElement(userButton);
        userButton.onClick(_ -> {
            displayContent.removeElement(displayContent.getElements().lastKey());
            displayContent.addElement(userCustomizationView);
            currentView = userCustomizationView;
        });

        ButtonOverlay loreButton = new ButtonBuilder("ccc").setGraphic(buildLoreButton(root.getWidth())).addStyle(Styles.FLAT).build();
        root.addElement(loreButton);
        loreButton.onClick(_ -> {
            displayContent.removeElement(displayContent.getElements().lastKey());
            displayContent.addElement(userLoreCustomizationView);
            currentView = userCustomizationView;
        });

        ButtonOverlay finishButton = new ButtonBuilder("finish").setGraphic(buildFinishButton(root.getWidth())).addStyle(Styles.FLAT).build();
        root.addElement(finishButton);
        finishButton.onClick(_ -> {
            displayContent.removeElement(displayContent.getElements().lastKey());
            displayContent.addElement(userFinishView);
            currentView = userCustomizationView;
        });

        if (currentView != null && currentView.hasProperty("progress")) {
            submission = new ButtonBuilder("fin").setText("Next").addStyle(Styles.SUCCESS).addStyle(Styles.BUTTON_OUTLINED).setWidth(root.getWidth()).build();
            submission.onClick(event -> {
                if (currentView == userCustomizationView) {
                    // View user
                    displayContent.removeElement(displayContent.getElements().lastKey());
                    displayContent.addElement(userLoreCustomizationView);
                    currentView = userLoreCustomizationView;

                    // Lore is next
                } else if (currentView == userLoreCustomizationView) {
                    // View chat
                    displayContent.removeElement(displayContent.getElements().lastKey());
                    displayContent.addElement(userFinishView);
                    currentView = userLoreCustomizationView;
                }
            });
            root.addElement(submission);
        }

        return root;
    }

    private HorizontalLayout buildUserButton(double width) {
        HorizontalLayout root = new HorizontalLayout(width, 0);
        root.setSpacing(20);

        userIcon = new IconOverlay(Material2AL.CHECK_CIRCLE_OUTLINE);
        userIcon.setColor(Color.rgb(50, 50, 50,0.5));
        userIcon.setIconSize(26);
        root.addElement(userIcon);

        TextOverlay textOverlay = new TextOverlay("User Customization");
        textOverlay.addStyle(Styles.TITLE_4);
        root.addElement(textOverlay);

        return root;
    }


    private HorizontalLayout buildLoreButton(double width) {
        HorizontalLayout root = new HorizontalLayout(width, 0);
        root.setSpacing(20);

        loreIcon = new IconOverlay(Material2AL.CHECK_CIRCLE_OUTLINE);
        loreIcon.setColor(Color.rgb(50, 50, 50,0.5));
        loreIcon.setIconSize(26);
        root.addElement(loreIcon);

        TextOverlay textOverlay = new TextOverlay("User Lore");
        textOverlay.addStyle(Styles.TITLE_4);
        root.addElement(textOverlay);

        return root;
    }

    private HorizontalLayout buildFinishButton(double width) {
        HorizontalLayout root = new HorizontalLayout(width, 0);
        root.setSpacing(20);

        finishIcon = new IconOverlay(Material2AL.CHECK_CIRCLE_OUTLINE);
        finishIcon.setColor(Color.rgb(50, 50, 50,0.5));
        finishIcon.setIconSize(26);
        root.addElement(finishIcon);

        TextOverlay textOverlay = new TextOverlay("Finish Character");
        textOverlay.addStyle(Styles.TITLE_4);
        root.addElement(textOverlay);

        return root;
    }

    public void revalidate() {
        boolean userFailed = false;
        if (userCustomizationView.getUserIdInput().isEnabled() && userCustomizationView.getUserIdInput().getTextField().isEditable() && userCustomizationView.getUserIdInput().getCurrentText().isEmpty()) {
            userFailed = true;
        }
        if (userCustomizationView.getUserDisplayInput().getCurrentText().isEmpty()) {
            userFailed = true;
        }

        if (userFailed) {
            userIcon.setColor(Color.RED);
        } else {
            userIcon.setColor(Color.GREEN);
        }


        // Lore will always be green
        loreIcon.setColor(Color.GREEN);

        if (userFailed) {
            submission.getButton().getStyleClass().removeAll(Styles.SUCCESS);
            submission.getButton().getStyleClass().add(Styles.DANGER);
            finishIcon.setColor(Color.RED);
        } else {
            submission.getButton().getStyleClass().removeAll(Styles.DANGER);
            submission.getButton().getStyleClass().add(Styles.SUCCESS);
            finishIcon.setColor(Color.GREEN);
        }
    }

    public User getUser() {
        return user;
    }

    public UserCustomizationView getUserCustomizationView() {
        return userCustomizationView;
    }

    public UserLoreCustomizationView getUserLoreCustomizationView() {
        return userLoreCustomizationView;
    }

    public HorizontalLayout getDisplayContent() {
        return displayContent;
    }

    public void setCurrentView(Container currentView) {
        this.currentView = currentView;
    }
}
