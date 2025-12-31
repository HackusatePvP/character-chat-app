package me.piitex.app.views.chats;

import atlantafx.base.theme.Styles;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.text.TextAlignment;
import me.piitex.app.App;
import me.piitex.app.backend.Chat;
import me.piitex.app.backend.ChatMessage;
import me.piitex.app.backend.Role;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.utils.Placeholder;
import me.piitex.engine.containers.BorderContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.Layout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.*;

import java.io.File;

import static me.piitex.app.views.Positions.CHAT_SEND_BOX_HEIGHT;
import static me.piitex.app.views.Positions.CHAT_SEND_BOX_WIDTH;

public class ChatPageView extends BorderContainer {
    private final ChatView parent;
    private final VerticalLayout chatRoot;

    private static final AppSettings APP_SETTINGS = App.getInstance().getAppSettings();

    public ChatPageView(ChatView chatView, double width, double height) {
        super(width, height);
        setMaxSize(width, height);
        addStyle(Styles.BG_INSET);

        this.parent = chatView;
        chatRoot = new VerticalLayout(getWidth() - 20, -1);
        chatRoot.setMaxSize(chatRoot.getWidth(), chatRoot.getHeight());
        chatRoot.setAlignment(Pos.TOP_CENTER);

        ScrollContainer container = new ScrollContainer(chatRoot, chatRoot.getWidth(), -1);
        container.setMaxSize(chatRoot.getWidth(), container.getHeight());
        container.setScrollWhenNeeded(false);
        container.setHorizontalScroll(false);
        container.setVerticalScroll(true);
        container.setScrollToBottom(true);
        setCenter(container);

        buildChatBoxes();

        HorizontalLayout bottom = new HorizontalLayout(getWidth(), CHAT_SEND_BOX_HEIGHT);
        bottom.setAlignment(Pos.CENTER);
        bottom.setMaxSize(bottom.getWidth(), bottom.getHeight());
        setBottom(bottom);

        RichTextAreaOverlay send = new RichTextAreaOverlay("", "", CHAT_SEND_BOX_WIDTH - 50, CHAT_SEND_BOX_HEIGHT - 50);
        send.setMaxSize(send.getWidth(), send.getHeight());
        send.setBackgroundColor(APP_SETTINGS.getThemeDefaultColor(APP_SETTINGS.getTheme()));
        send.setBorderColor(APP_SETTINGS.getThemeBorderColor(APP_SETTINGS.getTheme()));
        send.setTextFill(APP_SETTINGS.getThemeTextColor(APP_SETTINGS.getTheme()));
        send.addStyle(Styles.BG_DEFAULT);
        send.addStyle(APP_SETTINGS.getChatTextSize());
        send.addStyle(Styles.TEXT_ON_EMPHASIS);
        bottom.addElement(send);

        send.onSubmit(event -> {
            System.out.println("Submitting response...");
            generateResponse(send.getCurrentText());
        });
        send.onOverlaySubmit(event -> {
            System.out.println("Submitted!");
        });
    }

    public void buildChatBoxes() {
        // Build chat messages
        for (ChatMessage chatMessage : parent.getChat().getMessages()) {
            chatRoot.addElement(buildMessageBox(chatMessage));
        }
    }

    public Layout buildMessageBox(ChatMessage chatMessage) {
        VerticalLayout layout = new VerticalLayout(chatRoot.getWidth(), -1);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());
        layout.setAlignment(Pos.TOP_CENTER);
        layout.addStyle(Styles.BG_DEFAULT);
        layout.addStyle(Styles.BORDER_DEFAULT);

        HorizontalLayout displayBox = new HorizontalLayout(layout.getWidth(), -1);
        displayBox.setSpacing(50);
        displayBox.setMaxSize(displayBox.getWidth(), displayBox.getHeight());
        layout.addElement(displayBox);

        SeparatorOverlay separator = new SeparatorOverlay(Orientation.HORIZONTAL);
        separator.setMaxWidth(displayBox.getWidth() - 20);
        layout.addElement(separator);

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
        TextFlowOverlay chatFlow = new TextFlowOverlay(content, chatRoot.getWidth() - 40, -1);
        chatFlow.setTextAlignment(TextAlignment.CENTER);
        chatFlow.addStyle(App.getInstance().getAppSettings().getChatTextSize());
        chatFlow.setMaxWidth(chatFlow.getWidth());
        layout.addElement(chatFlow);

        return layout;
    }

    public void generateResponse(String prompt) {

        Chat chat = parent.getChat();
        // TODO: Add image url and reasoning
        ChatMessage userMessage = new ChatMessage(Role.USER, prompt, null, null);

        // Add the user message and render the chat box
        chat.addLine(userMessage);
        chatRoot.addElement(buildMessageBox(userMessage));

    }
}
