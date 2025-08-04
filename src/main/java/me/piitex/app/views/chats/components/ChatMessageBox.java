package me.piitex.app.views.chats.components;

import me.piitex.app.backend.Character;
import me.piitex.app.backend.Chat;
import me.piitex.app.backend.ChatMessage;
import me.piitex.app.backend.Role;
import me.piitex.app.views.chats.ChatView;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.layouts.VerticalLayout;

import java.io.File;

import static me.piitex.app.views.Positions.*;
import static me.piitex.app.views.Positions.CHAT_BOX_IMAGE_HEIGHT;

public class ChatMessageBox extends VerticalLayout {
    private final Character character;
    private final Chat chat;
    private final ChatMessage chatMessage;
    private final int index;
    private final ChatView parentView;


    public ChatMessageBox(Character character, Chat chat, ChatMessage chatMessage, int index, ChatView parentView, double width, double height) {
        super(width, height);
        this.character = character;
        this.chat = chat;
        this.chatMessage = chatMessage;
        this.index = index;
        this.parentView = parentView;

        initialize();
    }

    public void initialize() {
        // This will create a vertical layout which contains the components for the message.
        // 1. Thought tags if applicable
        // 2. Chat card
        // 3. Image card

        CardContainer cardContainer = new ChatBoxCard(this, character, chat, chatMessage, index, parentView, CHAT_BOX_WIDTH, CHAT_BOX_HEIGHT);
        addElement(cardContainer);

        // If there is an image attached to the response build the image card
        if (chatMessage.getSender() == Role.USER && chatMessage.hasImage()) {
            File file = new File(chatMessage.getImageUrl());
            if (file.exists()) {
                ImageCard imageCard = new ImageCard(chatMessage, file.getName(), CHAT_BOX_IMAGE_WIDTH, CHAT_BOX_IMAGE_HEIGHT);
                addElement(imageCard);
                parentView.getLayout().removeElement(imageCard);
            }
        }
        parentView.getLayout().addElement(this);

    }

}
