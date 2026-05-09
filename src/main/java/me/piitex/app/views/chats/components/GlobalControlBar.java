package me.piitex.app.views.chats.components;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import me.piitex.app.backend.Chat;
import me.piitex.app.backend.ChatMessage;
import me.piitex.app.backend.Role;
import me.piitex.app.utils.Placeholder;
import me.piitex.app.views.chats.ChatPageView;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.overlays.IconOverlay;
import org.kordamp.ikonli.material2.Material2MZ;

public class GlobalControlBar extends EmptyContainer {
    private final ChatPageView parent;

    public GlobalControlBar(ChatPageView parent, double width, double height) {
        super(width, height);
        this.parent = parent;
        setMaxSize(getWidth(), getHeight());
        init();
    }

    private void init() {
        HorizontalLayout root = new HorizontalLayout(getWidth(), getHeight());
        root.setMaxSize(getWidth(), getHeight());
        root.setAlignment(Pos.CENTER);
        root.setSpacing(10);
        addElement(root);

        IconOverlay undo = new IconOverlay(Material2MZ.UNDO);
        undo.setTooltip("Undo the previous prompt.");
        undo.setIconSize(18);
        undo.setColor(Color.WHITE);
        root.addElement(undo);
        undo.onClick(_ -> {
            // Remove the last two messages
            Chat chat = parent.getParent().getChat();
            if (chat.getMessages().size() > 1) {
                // There are two messages in the index.
                ChatMessage previous = chat.getMessage(chat.getMessages().size() - 1);
                if (previous.getSender() != Role.USER) {
                    previous = chat.getMessage(chat.getMessages().size() - 2);
                } else {
                    previous = null;
                }
                if (previous != null) {
                    String copy = previous.getContent();
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(Placeholder.retrieveOriginalText(copy));
                    clipboard.setContent(clipboardContent);

                    // Insert content into text box.
                    parent.getSendTextBox().setCurrentText(copy);
                }
                chat.removeMessage(chat.getMessages().size() - 1);
                chat.removeMessage(chat.getMessages().size() - 1);
                parent.getChatRoot().removeLastElement();
                parent.getChatRoot().removeLastElement();
            } else {
                // There is only one message.
                // If it is a user copy the contents
                ChatMessage previous = chat.getMessage(chat.getMessages().size() - 1);
                if (previous != null && previous.getSender() == Role.USER) {
                    String copy = previous.getContent();
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(Placeholder.retrieveOriginalText(copy));
                    clipboard.setContent(clipboardContent);

                    // Insert content into text box.
                    parent.getSendTextBox().setCurrentText(copy);
                }
                chat.removeMessage(chat.getMessages().size() - 1);
                if (!parent.getChatRoot().getElements().isEmpty()) {
                    parent.getChatRoot().removeLastElement();
                }
            }


        });
    }
}
