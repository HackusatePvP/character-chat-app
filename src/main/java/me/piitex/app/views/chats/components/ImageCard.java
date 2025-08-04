package me.piitex.app.views.chats.components;

import me.piitex.app.backend.ChatMessage;
import me.piitex.engine.layouts.TitledLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.ImageOverlay;

import java.io.File;

public class ImageCard extends TitledLayout {
    private final ChatMessage chatMessage;

    public ImageCard(ChatMessage chatMessage, String title, double width, double height) {
        super(title, width, height);
        this.chatMessage = chatMessage;
        buildImageCard();
    }

    public void buildImageCard() {
        setCollapse(true);
        setExpanded(false);
        setMaxSize(getWidth(), getHeight());

        ImageLoader imageLoader = new ImageLoader(new File(chatMessage.getImageUrl()));
        ImageOverlay imageOverlay = new ImageOverlay(imageLoader);

        addElement(imageOverlay);
    }
}
