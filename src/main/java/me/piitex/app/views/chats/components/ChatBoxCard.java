package me.piitex.app.views.chats.components;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.Chat;
import me.piitex.app.backend.ChatMessage;
import me.piitex.app.backend.Role;
import me.piitex.app.views.chats.ChatView;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.ImageOverlay;
import me.piitex.engine.overlays.TextOverlay;

import java.io.File;

import static me.piitex.app.views.chats.ChatView.buildTextFlow;

public class ChatBoxCard extends CardContainer {
    private final Character character;
    private final Chat chat;
    private final ChatMessage chatMessage;
    private final int index;
    private final ChatView parentView;

    public ChatBoxCard(Character character, Chat chat, ChatMessage chatMessage, int index, ChatView parentView, double width, double height) {
        super(width, height);
        this.character = character;
        this.chat = chat;
        this.chatMessage = chatMessage;
        this.index = index;
        this.parentView = parentView;
        buildCard();
    }

    public void buildCard() {
        /*CardContainer cardContainer = new CardContainer(appSettings.getWidth() - 350, 0); // Width, height
        cardContainer.setMaxSize(appSettings.getWidth() - 350, 0);*/
        this.setMaxSize(getWidth(), getHeight());
        String iconPath = "";
        String displayName = "";
        Role role = chatMessage.getSender();
        if (role == Role.USER) {
            iconPath = (character.getUser() != null && character.getUser().getIconPath() != null && !character.getUser().getIconPath().isEmpty()? character.getUser().getIconPath() :  new File(App.getAppDirectory(), "icons/character.png").getAbsolutePath());
            displayName = character.getUser().getDisplayName();
        } else {
            iconPath = (character != null && character.getIconPath() != null && !character.getIconPath().isEmpty() ? character.getIconPath() : new File(App.getAppDirectory(), "icons/character.png").getAbsolutePath());
            displayName = character.getDisplayName();
        }

        ImageOverlay avatar = new ImageOverlay(new ImageLoader(new File(iconPath)));
        avatar.setPreserveRatio(false);
        avatar.setWidth(128);
        avatar.setHeight(128);

        HorizontalLayout header = new HorizontalLayout(900 ,0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(50);
        header.addElement(avatar);
        this.setHeader(header);

        TextOverlay display = new TextOverlay(displayName);
        display.addStyle(Styles.TITLE_3);
        header.addElement(display);

        this.setBody(buildTextFlow(chatMessage, chat, index));
        this.setFooter(parentView.buildButtonBox(this, chatMessage, index));
    }

}
