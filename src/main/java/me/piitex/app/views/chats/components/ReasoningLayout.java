package me.piitex.app.views.chats.components;

import me.piitex.app.backend.ChatMessage;
import me.piitex.app.views.Positions;
import me.piitex.engine.layouts.TitledLayout;
import me.piitex.engine.overlays.TextFlowOverlay;

public class ReasoningLayout extends TitledLayout {
    private final ChatMessage chatMessage;

    public ReasoningLayout(ChatMessage chatMessage, double width, double height) {
        super("Reasoning", width, height);
        getPane().setMinSize(width, height);
        setMaxSize(width, height);
        this.chatMessage = chatMessage;
        buildReasoning();
    }

    public void buildReasoning() {
        setCollapse(true);
        setExpanded(false);
        setMaxSize(getWidth(), getHeight());

        double width = Positions.CHAT_TEXTFLOW_WIDTH;

        TextFlowOverlay textFlowOverlay = new TextFlowOverlay(chatMessage.getReasoning(), (int) width, (int) getHeight());
        addElement(textFlowOverlay);
    }
}
