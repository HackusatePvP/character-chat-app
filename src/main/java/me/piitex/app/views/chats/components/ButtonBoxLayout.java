package me.piitex.app.views.chats.components;

import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;
import me.piitex.app.App;
import me.piitex.app.backend.*;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.utils.Placeholder;
import me.piitex.app.views.chats.ChatView;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.ModalContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.TextAreaOverlay;
import me.piitex.engine.overlays.TextOverlay;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;


public class ButtonBoxLayout extends HorizontalLayout {
    private final Character character;
    private final ChatMessage chatMessage;
    private final Chat chat;
    private final int index;
    private final ChatView parentView;


    public ButtonBoxLayout(Character character, ChatMessage chatMessage, Chat chat, int index, ChatView parentView, double width, double height) {
        super(width, height);
        this.character = character;
        this.chatMessage = chatMessage;
        this.chat = chat;
        this.index = index;
        this.parentView = parentView;
        buildButtonBox();
    }

    public void buildButtonBox() {
        setY(20);
        setSpacing(20);
        setMaxSize(getWidth(), getHeight());

        TextOverlay copy = new TextOverlay(new FontIcon(Material2AL.CONTENT_COPY));
        copy.setTooltip("Copy text to clipboard.");
        addElement(copy);
        copy.onClick(event -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(Placeholder.retrieveOriginalText(chatMessage.getContent()));
            clipboard.setContent(clipboardContent);

            // Maybe add green to the icon and set it back??
            copy.getNode().getStyleClass().add(Styles.SUCCESS);

            // After 3 seconds remove the style?
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(1), // Delay for 1 seconds
                    event1 -> copy.getNode().getStyleClass().remove(Styles.SUCCESS) // Action to perform after delay
            ));
            timeline.play();
        });

        TextOverlay edit = new TextOverlay(new FontIcon(Material2AL.EDIT));
        edit.addStyle(Styles.ACCENT);
        edit.setTooltip("Edit the message.");
        addElement(edit);

        edit.onClick(event -> {
            if (ServerProcess.getCurrentServer().isLoading()) return;

            ChatMessage originalMessage = chat.getMessage(index);

            if (originalMessage == null) {
                App.logger.warn("Attempted to edit message at index {} but no message found.", index);
                return;
            }

            String contentForEdit = originalMessage.getContent();

            VerticalLayout verticalLayout = new VerticalLayout(0, 0);

            ModalContainer modalContainer = new ModalContainer(verticalLayout, 400, 400);
            modalContainer.addStyle(Styles.ELEVATED_1);

            verticalLayout.addElement(new TextOverlay("Edit Message"));
            TextAreaOverlay area = new TextAreaOverlay(Placeholder.retrieveOriginalText(contentForEdit), 0, 0, 400, 300);
            verticalLayout.addElement(area);

            ButtonOverlay submit = new ButtonOverlay("submit", "Submit");
            submit.addStyle(Styles.SUCCESS);
            verticalLayout.addElement(submit);

            submit.onClick(event1 -> {
                String newContentFromUser = area.getCurrentText();
                String contentToStore = newContentFromUser.replace("\n", "!@!");

                chat.replaceMessageContent(index, contentToStore);

                parentView.getLayout().getPane().getChildren().removeLast(); // Re-evaluate this line for correctness in your UI setup

                ChatMessage updatedChatMessage = chat.getMessage(index);
                if (updatedChatMessage != null) {
                    parentView.buildChatBox(updatedChatMessage, index, true);
                }

                App.window.removeContainer(modalContainer);
            });

            App.window.renderPopup(modalContainer, PopupPosition.CENTER, 400, 400);

            if (modalContainer.getView() != null) {
                ModalBox modalBox = (ModalBox) modalContainer.getView();
                modalBox.setOnClose(event1 -> {
                    App.window.removeContainer(modalContainer);
                });
            }
        });

        TextOverlay delete = new TextOverlay(new FontIcon(Material2AL.DELETE_FOREVER));
        delete.addStyle(Styles.DANGER);
        delete.setTooltip("Delete the message.");
        addElement(delete);
        delete.onClick(event -> {
            // Delete the message
            //TODO: Add confirm for deleting
            parentView.getLayout().getPane().getChildren().removeLast();
            chat.removeMessage(index);
            chat.update();
        });

        // Doesn't make sense to regenerate the configured message.
        String firstMsg = (character.getFirstMessage() == null || character.getFirstMessage().isEmpty() ? "null" : character.getFirstMessage());
        if (chatMessage.getSender() == Role.ASSISTANT && !firstMsg.equalsIgnoreCase(chatMessage.getContent())) {
            TextOverlay regenerate = new TextOverlay(new FontIcon(Material2MZ.REFRESH));
            regenerate.addStyle(Styles.WARNING);
            regenerate.setTooltip("Regenerate the response.");
            addElement(regenerate);
            regenerate.onClick(event -> {
                if (ServerProcess.getCurrentServer() == null || ServerProcess.getCurrentServer().isLoading() || ServerProcess.getCurrentServer().isError()) return;

                if (index + 1 != chat.getMessages().size()) {
                    return;
                }

                // Remove the line which is the last line of the chat
                chat.removeMessage(index);
                parentView.getLayout().getPane().getChildren().removeLast();

                // Generate new box with
                CardContainer responseBox = parentView.buildChatBox(chatMessage, chat.getMessages().size(), true); // Set content later

                // Gen response
                Response response = new Response(index, chat.getLastLine(index).getContent(), character, character.getUser(), chat);
                chat.setResponse(response);

                // Disable buttons to prevent spamming
                parentView.getSend().getNode().setDisable(true);
                parentView.getSubmit().getNode().setDisable(true);
                parentView.generateResponse(response, chatMessage, responseBox);

            });
        }
    }
}
