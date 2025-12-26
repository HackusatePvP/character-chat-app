package me.piitex.app.views.chats;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.text.TextAlignment;
import me.piitex.app.App;
import me.piitex.app.backend.ChatMessage;
import me.piitex.app.backend.Role;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.utils.Placeholder;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.Layout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.ImageOverlay;
import me.piitex.engine.overlays.TextFlowOverlay;
import me.piitex.engine.overlays.TextOverlay;

import java.io.File;

public class ChatPageView extends EmptyContainer {
    private final ChatView parent;
    private final VerticalLayout root;

    private static final AppSettings APP_SETTINGS = App.getInstance().getAppSettings();

    public ChatPageView(ChatView chatView, double width, double height) {
        super(width, height);
        setMaxSize(width, height);
        this.parent = chatView;

        root = new VerticalLayout(getWidth() - 20, getHeight());
        root.setMaxSize(root.getWidth(), root.getHeight());
        root.setAlignment(Pos.TOP_CENTER);
        root.addStyle(Styles.BG_INSET);

        ScrollContainer container = new ScrollContainer(root, root.getWidth(), APP_SETTINGS.getHeight() - 20);
        container.setMaxSize(root.getWidth(), root.getHeight());
        container.setScrollWhenNeeded(false);
        container.setHorizontalScroll(false);
        container.setVerticalScroll(true);
        container.setScrollToBottom(true);
        addElement(container);

        init();
    }

    public void init() {
        // Build chat messages
        for (ChatMessage chatMessage : parent.getChat().getMessages()) {
            root.addElement(buildMessageBox(chatMessage));
        }
    }

    public Layout buildMessageBox(ChatMessage chatMessage) {
        VerticalLayout layout = new VerticalLayout(root.getWidth(), -1);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());
        layout.setAlignment(Pos.TOP_CENTER);
        layout.addStyle(Styles.BG_DEFAULT);
        layout.addStyle(Styles.BORDER_DEFAULT);

        HorizontalLayout displayBox = new HorizontalLayout(layout.getWidth(), -1);
        displayBox.setSpacing(50);
        displayBox.setMaxSize(displayBox.getWidth(), displayBox.getHeight());
        layout.addElement(displayBox);

        //TODO: Make Avatar circular
        int avatarSize = 128;
        if (chatMessage.getSender() == Role.ASSISTANT) {
            displayBox.setAlignment(Pos.CENTER_RIGHT);

            String iconPath = (parent.getCharacter().getIconPath() != null && !parent.getCharacter().getIconPath().isEmpty() ? parent.getCharacter().getIconPath() : "");
            if (iconPath.isEmpty()) {
                iconPath = new File(App.getExecutedDirectory(), "icons/charater.png").getAbsolutePath();
            }

            ImageLoader imageLoader = new ImageLoader(new File(iconPath));
            imageLoader.setWidth(avatarSize);
            imageLoader.setHeight(avatarSize);
            ImageOverlay avatar = new ImageOverlay(imageLoader);
            avatar.setFitWidth(avatarSize);
            avatar.setFitHeight(avatarSize);

            TextOverlay displayName = new TextOverlay(parent.getCharacter().getDisplayName());
            displayName.addStyle(Styles.TITLE_3);

            displayBox.addElements(displayName, avatar);
        } else {
            // User message
            displayBox.setAlignment(Pos.CENTER_LEFT);

            String iconPath = (parent.getCharacter().getUser().getIconPath() != null && !parent.getCharacter().getUser().getIconPath().isEmpty() ? parent.getCharacter().getUser().getIconPath() : "");
            if (iconPath.isEmpty()) {
                iconPath = new File(App.getExecutedDirectory(), "icons/charater.png").getAbsolutePath();
            }

            ImageLoader imageLoader = new ImageLoader(new File(iconPath));
            imageLoader.setWidth(avatarSize);
            imageLoader.setHeight(avatarSize);
            ImageOverlay avatar = new ImageOverlay(imageLoader);
            avatar.setFitWidth(avatarSize);
            avatar.setFitHeight(avatarSize);

            TextOverlay displayName = new TextOverlay(parent.getCharacter().getUser().getDisplayName());
            displayName.addStyle(Styles.TITLE_3);

            displayBox.addElements(avatar, displayName);
        }

        String content = Placeholder.applyDynamicBBCode(chatMessage.getContent());
        TextFlowOverlay chatFlow = new TextFlowOverlay(content, root.getWidth() - 40, -1);
        chatFlow.setTextAlignment(TextAlignment.CENTER);
        chatFlow.addStyle(App.getInstance().getAppSettings().getChatTextSize());
        chatFlow.setMaxWidth(chatFlow.getWidth());
        layout.addElement(chatFlow);

        return layout;
    }
}
