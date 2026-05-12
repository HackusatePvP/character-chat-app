package me.piitex.app.views.chats;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import me.piitex.app.App;
import me.piitex.app.backend.Chat;
import me.piitex.app.backend.ChatMessage;
import me.piitex.app.backend.Response;
import me.piitex.app.backend.Role;
import me.piitex.app.backend.server.Server;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.utils.Placeholder;
import me.piitex.app.views.chats.components.ControlBarView;
import me.piitex.app.views.chats.components.GlobalControlBar;
import me.piitex.engine.containers.BorderContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.image.BaseImageLoader;
import me.piitex.engine.loaders.image.ImageLoader;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;
import java.io.IOException;

import static me.piitex.app.views.Positions.CHAT_SEND_BOX_HEIGHT;
import static me.piitex.app.views.Positions.CHAT_SEND_BOX_WIDTH;

public class ChatPageView extends BorderContainer {
    private final ChatView parent;
    private final VerticalLayout chatRoot;
    private RichTextAreaOverlay sendTextBox;
    private GlobalControlBar controlBox;
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
        container.setPannable(true);
        container.setScrollWhenNeeded(false);
        container.setHorizontalScroll(false);
        container.setVerticalScroll(true);
        container.setScrollToBottom(true);
        setCenter(container);

        buildChatBoxes();

        VerticalLayout bottom = new VerticalLayout(getWidth(), 150);
        bottom.setMaxSize(bottom.getWidth(), bottom.getHeight());
        bottom.setAlignment(Pos.CENTER);
        bottom.setSpacing(0);
        setBottom(bottom);

        controlBox = new GlobalControlBar(this, 100, -1);
        bottom.addElement(controlBox);

        // Horizontal layout to add text box and send button
        // TODO: Add send button
        HorizontalLayout sendLayout = new HorizontalLayout(getWidth(), bottom.getHeight() - 50);
        sendLayout.setAlignment(Pos.CENTER);
        sendLayout.setMaxSize(sendLayout.getWidth(), sendLayout.getHeight());
        bottom.addElement(sendLayout);

        sendTextBox = new RichTextAreaOverlay("", "", CHAT_SEND_BOX_WIDTH - 50, CHAT_SEND_BOX_HEIGHT - 50);
        sendTextBox.setMaxSize(sendTextBox.getWidth(), sendTextBox.getHeight());
        sendTextBox.setBackgroundColor(APP_SETTINGS.getThemeDefaultColor(APP_SETTINGS.getTheme()));
        sendTextBox.setBorderColor(APP_SETTINGS.getThemeBorderColor(APP_SETTINGS.getTheme()));
        sendTextBox.setTextFill(APP_SETTINGS.getThemeTextColor(APP_SETTINGS.getTheme()));
        sendTextBox.addStyle(Styles.BG_DEFAULT);
        sendTextBox.addStyle(APP_SETTINGS.getChatTextSize());
        sendTextBox.addStyle(Styles.TEXT_ON_EMPHASIS);
        sendLayout.addElement(sendTextBox);

        sendTextBox.onSubmit(_ -> {
            generateResponse(sendTextBox.getCurrentText(), false);
            sendTextBox.setCurrentText("");
        });
    }

    public void buildChatBoxes() {
        // Build chat messages
        for (ChatMessage chatMessage : parent.getChat().getMessages()) {
            chatRoot.addElement(buildMessageBox(chatMessage));
        }
    }

    public VerticalLayout buildMessageBox(ChatMessage chatMessage) {
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
        separator.setMaxWidth(displayBox.getWidth() - 100);
        layout.addElement(separator);

        //TODO: Make Avatar circular
        int avatarSize = 196;
        if (chatMessage.getSender() == Role.ASSISTANT) {
            displayBox.setAlignment(Pos.CENTER_RIGHT);

            String iconPath = (parent.getCharacter().getIconPath() != null && !parent.getCharacter().getIconPath().isEmpty() ? parent.getCharacter().getIconPath() : "");
            if (iconPath.isEmpty()) {
                iconPath = new File(App.getExecutedDirectory(), "icons/charater.png").getAbsolutePath();
            }

            ImageLoader imageLoader = new BaseImageLoader(new File(iconPath));
            imageLoader.setWidth(avatarSize);
            imageLoader.setHeight(avatarSize);
            imageLoader.setSmoothing(true);
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

            ImageLoader imageLoader = new BaseImageLoader(new File(iconPath));
            imageLoader.setWidth(avatarSize);
            imageLoader.setHeight(avatarSize);
            imageLoader.setSmoothing(true);
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

        layout.addElement(new ControlBarView(this, layout, chatMessage, layout.getWidth(), 50));

        return layout;
    }

    public void generateResponse(String prompt, boolean update) {
        Chat chat = parent.getChat();

        // Remove regenerate button from last message
        if (!chatRoot.getElements().isEmpty()) {
            VerticalLayout lastMessageBox = (VerticalLayout) chatRoot.getLastElement();
            if (lastMessageBox != null) {
                HorizontalLayout controlBox = (HorizontalLayout) lastMessageBox.getLastElement();
                if (controlBox != null && controlBox.getElements().size() > 3) {
                    controlBox.removeLastElement();
                }
            }
        }

        ChatMessage charMessage;
        VerticalLayout currentCharBox;
        if (!update) {
            ChatMessage userMessage = new ChatMessage(Role.USER, prompt, null, null);
            chat.addLine(userMessage);
            chatRoot.addElement(buildMessageBox(userMessage));
        } else {
            chat.removeMessage(chatRoot.getElements().size());
        }

        charMessage = new ChatMessage(Role.ASSISTANT, "", null, null);
        chat.addLine(charMessage);

        currentCharBox = buildMessageBox(charMessage);
        chatRoot.addElement(currentCharBox);
        Response response = new Response(chat.getMessages().size(), prompt, parent.getCharacter(), parent.getCharacter().getUser(), chat);

        // Add halt button to global controls box
        IconOverlay stop = new IconOverlay(Material2MZ.STOP_CIRCLE);
        stop.setIconSize(18);
        stop.setColor(Color.RED);
        HorizontalLayout controlLayout = (HorizontalLayout) controlBox.getElementAt(0);
        controlLayout.addElement(stop);
        stop.onClick(_ -> {
            response.setHalt(true);
            controlLayout.removeElement(stop);
        });

        VerticalLayout finalCurrentCharBox = currentCharBox;
        App.getThreadPoolManager().submitTask(() -> {
            try {
                String content = Server.generateResponseOAIStream(finalCurrentCharBox, response);
                response.setResponse(content);
                charMessage.setContent(content);
                chat.update();

                // Remove stop button if present
                Platform.runLater(() -> {
                    if (controlLayout.containsElement(stop)) {
                        controlLayout.removeElement(stop);
                    }
                });
            } catch (Exception e) {
                App.logger.error("Could not generate response!", e);
            }
        });

    }

    public ChatView getParent() {
        return parent;
    }

    public VerticalLayout getChatRoot() {
        return chatRoot;
    }

    public RichTextAreaOverlay getSendTextBox() {
        return sendTextBox;
    }
}
