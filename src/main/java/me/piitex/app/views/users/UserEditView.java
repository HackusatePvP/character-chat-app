package me.piitex.app.views.users;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import me.piitex.app.App;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.SidebarView;
import me.piitex.app.views.users.tabs.UserLoreBookTab;
import me.piitex.app.views.users.tabs.UserTab;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.tabs.TabsContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonBuilder;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.MessageOverlay;
import me.piitex.os.configurations.InfoFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.TreeMap;

public class UserEditView extends EmptyContainer {
    private User user;
    private final InfoFile infoFile;
    private HorizontalLayout root;
    private VerticalLayout mainPage;
    private String userId = "";
    private String userDisplay = "";
    private String userPersona = "";
    private File userIconPath;

    // Lore Map
    private TreeMap<String, String> loreBook;

    private TabsContainer tabsContainer;
    private UserTab userTab;
    private UserLoreBookTab userLoreBookTab;

    private static final AppSettings appSettings = App.getInstance().getAppSettings();

    public UserEditView() {
        super(appSettings.getWidth(), appSettings.getHeight());
        this.infoFile = new InfoFile();
        this.loreBook = new TreeMap<>();
        init();
    }

    public UserEditView(User user) {
        super(appSettings.getWidth(), appSettings.getHeight());
        this.user = user;
        this.infoFile = user.getInfoFile();
        this.userId = user.getId();
        this.userDisplay = user.getDisplayName();
        this.userPersona = user.getPersona();
        this.userIconPath = new File(user.getIconPath());
        this.loreBook = user.getLorebook();
        init();
    }

    public void init() {
        root = new HorizontalLayout(getWidth(), getHeight());
        root.setMaxSize(root.getWidth(), root.getHeight());
        addElement(root);

        SidebarView sidebarView = new SidebarView(this, false);
        root.addElement(sidebarView);

        mainPage = new VerticalLayout(appSettings.getWidth() - 200, 0);
        mainPage.setMaxSize(mainPage.getWidth(), mainPage.getHeight());
        root.addElement(mainPage);

        tabsContainer = new TabsContainer(0, 0, mainPage.getWidth(), appSettings.getHeight());
        mainPage.addElement(tabsContainer);

        userTab = new UserTab("User", this);
        tabsContainer.addTab(userTab);

        userLoreBookTab = new UserLoreBookTab("Lorebook", this);
        tabsContainer.addTab(userLoreBookTab);
    }

    public HorizontalLayout buildSubmitBox() {
        HorizontalLayout layout = new HorizontalLayout(appSettings.getWidth() - 300, 50);
        layout.setY(appSettings.getHeight() - 150);
        layout.setSpacing(20);
        layout.setAlignment(Pos.CENTER);

        ButtonOverlay cancel = new ButtonBuilder("cancel").setText("Cancel").build();
        cancel.addStyle(Styles.DANGER);
        cancel.addStyle(Styles.BUTTON_OUTLINED);
        layout.addElement(cancel);

        ButtonOverlay submit = new ButtonBuilder("submit").setText("Submit").build();
        submit.addStyle(Styles.SUCCESS);
        submit.addStyle(Styles.BUTTON_OUTLINED);
        layout.addElements(submit);


        submit.onClick(event -> {
            if (!validate()) {
                return;
            }

            if (user == null) {
                user = new User(userId);
            }
            user.setDisplayName(userDisplay);
            user.setPersona(userPersona);
            user.setLorebook(loreBook);
            if (userIconPath == null || !userIconPath.exists()) {
                userIconPath = new File(App.getAppDirectory(), "icons/character.png");
            }
            try {
                File output = new File(user.getUserDirectory(), "user.png");
                Files.copy(userIconPath.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                user.setIconPath(output.getAbsolutePath());
            } catch (IOException e) {
                App.logger.error("Failed to move image to user.", e);
            }

            App.getInstance().getUserTemplates().putIfAbsent(userId, user);

            App.window.clearContainers();
            App.window.addContainer(new UsersView());
        });

        return layout;
    }

    public boolean validate() {
        if (userId.isEmpty() || ((TextField) userTab.getUserIdInput().getNode()).getText().isEmpty()) {
            tabsContainer.getTabPane().getSelectionModel().select(userTab.getJfxTab());
            MessageOverlay required = new MessageOverlay(0, 0, 600, 100, "User ID", "ID is required.");
            required.addStyle(Styles.WARNING);
            required.addStyle(Styles.BG_DEFAULT);
            App.window.renderPopup(required, PopupPosition.CENTER, 600, 100, true);
            userTab.getUserIdInput().getNode().requestFocus();
            return false;
        } else if (userDisplay.isEmpty() || ((TextField) userTab.getUserDisplayNameInput().getNode()).getText().isEmpty()) {
            tabsContainer.getTabPane().getSelectionModel().select(userTab.getJfxTab());
            MessageOverlay required = new MessageOverlay(0, 0, 600, 100, "Display Name", "Display name is required.");
            required.addStyle(Styles.WARNING);
            required.addStyle(Styles.BG_DEFAULT);
            App.window.renderPopup(required, PopupPosition.CENTER, 600, 100, true);
            userTab.getUserIdInput().getNode().requestFocus();
            return false;
        } else {
            return true;
        }
    }

    public User getUser() {
        return user;
    }

    public String getUserId() {
        return userId;
    }

    public File getUserIconPath() {
        return userIconPath;
    }

    public String getUserDisplay() {
        return userDisplay;
    }

    public String getUserPersona() {
        return userPersona;
    }

    public TreeMap<String, String> getLoreBook() {
        return loreBook;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserDisplay(String userDisplay) {
        this.userDisplay = userDisplay;
    }

    public void setUserPersona(String userPersona) {
        this.userPersona = userPersona;
    }

    public void setUserIconPath(File userIconPath) {
        this.userIconPath = userIconPath;
    }

    public void setLoreBook(TreeMap<String, String> loreBook) {
        this.loreBook = loreBook;
    }
}
