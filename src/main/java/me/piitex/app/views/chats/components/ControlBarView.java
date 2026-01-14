package me.piitex.app.views.chats.components;

import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.backend.ChatMessage;
import me.piitex.app.backend.Role;
import me.piitex.app.utils.Placeholder;
import me.piitex.app.views.chats.ChatPageView;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.ModalContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

public class ControlBarView extends HorizontalLayout {
    private final ChatPageView parent;
    private final VerticalLayout root;
    private final ChatMessage chatMessage;

    public ControlBarView(ChatPageView parent, VerticalLayout root, ChatMessage chatMessage, double width, double height) {
        super(width, height);
        this.parent = parent;
        this.root = root;
        this.chatMessage = chatMessage;
        setMaxSize(width, height);
        setAlignment(Pos.CENTER_LEFT);
        setX(20);
        setSpacing(20);

        init();
    }

    private void init() {
        IconOverlay copy = new IconOverlay(Material2AL.FILE_COPY);
        copy.setColor(Color.GREEN);
        copy.setTooltip("Copy the chat text.");
        copy.onClick(_ -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(Placeholder.retrieveOriginalText(chatMessage.getContent()));
            clipboard.setContent(clipboardContent);
        });
        addElement(copy);

        IconOverlay edit = new IconOverlay(Material2AL.EDIT);
        edit.setColor(Color.CYAN);
        edit.setTooltip("Edit the chat text.");
        addElement(edit);

        int index = parent.getParent().getChat().getMessages().indexOf(chatMessage);
        edit.onClick(_ -> {
            ChatMessage originalMessage = parent.getParent().getChat().getMessage(index);
            TextFlowOverlay textFlowOverlay = (TextFlowOverlay) root.getElementAt(2);

            if (originalMessage == null) {
                App.logger.warn("Attempted to edit message at index {} but no message found.", index);
                return;
            }

            ModalContainer modalContainer = new ModalContainer(400, 400);
            modalContainer.addStyle(Styles.ELEVATED_1);

            VerticalLayout verticalLayout = new VerticalLayout(0, 0);
            verticalLayout.addStyle(Styles.BG_INSET);

            verticalLayout.addElement(new TextOverlay("Edit Message"));

            TextAreaOverlay area;
            if (textFlowOverlay != null) {
                area = new TextAreaOverlay(Placeholder.retrieveOriginalText(textFlowOverlay.getText()), "", 400, 300);
                verticalLayout.addElement(area);
            } else {
                area = new TextAreaOverlay(Placeholder.retrieveOriginalText(originalMessage.getContent()), "", 400, 300);
            }

            ButtonOverlay submit = new ButtonBuilder("submit").setText("Submit").build();
            submit.addStyle(Styles.SUCCESS);
            verticalLayout.addElement(submit);

            submit.onClick(event1 -> {
                String newContentFromUser = area.getCurrentText();
                String contentToStore = newContentFromUser.replace("\n", "!@!");

                parent.getParent().getChat().replaceMessageContent(index, contentToStore);

                ChatMessage updatedChatMessage = parent.getParent().getChat().getMessage(index);

                    if (textFlowOverlay != null) {
                        textFlowOverlay.setText(Placeholder.applyDynamicBBCode(updatedChatMessage.getContent()));
                    }
                App.window.removeContainer(modalContainer);
            });

            modalContainer.setContent(verticalLayout);
            App.window.renderPopup(modalContainer, PopupPosition.CENTER, 400, 400);

            ModalBox modalBox = modalContainer.getModalBox();
            modalBox.setOnClose(event1 -> {
                App.window.removeContainer(modalContainer);
            });
        });

        IconOverlay delete = new IconOverlay(Material2AL.DELETE_FOREVER);
        delete.setColor(Color.RED);
        delete.setTooltip("Delete the chat text.");
        addElement(delete);
        delete.onClick(_ -> {
            parent.getParent().getChat().removeMessage(index);
            parent.getChatRoot().removeElement(root);
        });

        if (chatMessage.getSender() == Role.ASSISTANT) {
            IconOverlay regenerate = new IconOverlay(Material2MZ.REFRESH);
            regenerate.setColor(Color.YELLOW);
            regenerate.setTooltip("Regenerate the chat text.");
            addElement(regenerate);
            regenerate.onClick(_ -> {
                String userPrompt = "";

                ChatMessage previousMessage = parent.getParent().getChat().getMessage(index - 1);
                if (previousMessage != null) {
                    userPrompt = previousMessage.getContent();
                }

                parent.getChatRoot().removeElement(index);
                parent.generateResponse(userPrompt, true);
            });
        }
    }
}
