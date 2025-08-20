package me.piitex.app.views.chats.components;

import atlantafx.base.theme.Styles;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.app.backend.Chat;
import me.piitex.app.backend.ChatMessage;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.Role;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.chats.ChatView;
import me.piitex.engine.Element;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.MessageOverlay;
import me.piitex.engine.overlays.RichTextAreaOverlay;
import me.piitex.engine.overlays.TextOverlay;
import org.fxmisc.richtext.StyledTextArea;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;

public class TopControlBox extends HorizontalLayout {
    private final Chat chat;
    private final RichTextAreaOverlay send;
    private final ChatView parentView;

    private final AppSettings appSettings = App.getInstance().getAppSettings();

    public TopControlBox(Chat chat, RichTextAreaOverlay send, ChatView parentView, double width, double height) {
        super(width, height);
        this.chat = chat;
        this.send = send;
        this.parentView = parentView;
        buildTopControls();
    }

    public void buildTopControls() {
        TextOverlay undo = new TextOverlay(new FontIcon(Material2MZ.UNDO));
        undo.addStyle(Styles.DANGER);
        undo.setTooltip("Undo the last message.");
        addElement(undo);
        undo.onClick(event -> {
            if (ServerProcess.getCurrentServer().isLoading()) return;

            // Copy user message to clipboard
            // Delete both last assistant and user messages
            int index = chat.getMessages().size();
            if (index == 0) return;

            ChatMessage lastLine = chat.getLastLine(index);

            Role role = lastLine.getSender();
            if (role == Role.ASSISTANT) {
                parentView.getLayout().removeElement(parentView.getLayout().getElements().lastKey());
                parentView.getLayout().removeElement(parentView.getLayout().getElements().lastKey());

                if (lastLine.hasImage()) {
                    parentView.getLayout().removeElement(parentView.getLayout().getElements().lastKey());
                }

                // Remove assistant
                chat.removeMessage(index - 1);

                // Copy user message and then remove it
                ChatMessage content = chat.getMessage(index - 2);
                send.setCurrentText(content.getContent());
                chat.removeMessage(index - 2);

                ChatMessage last = chat.getMessages().getLast();
                if (last.getSender() == Role.ASSISTANT) {
                    Element element = parentView.getLayout().getElements().lastEntry().getValue();
                    if (element instanceof VerticalLayout messageBox) {
                        for (Element e : messageBox.getElements().values()) {
                            if (e instanceof CardContainer cardContainer) {
                                cardContainer.setFooter(parentView.buildButtonBox(messageBox, last, chat.getMessages().lastIndexOf(chat.getMessages().getLast())));
                                break;
                            }
                        }
                    }
                }

                chat.update();
            } else {
                // The last message is user
                // Just remove the message from the chat and put it back into the box.
                parentView.getLayout().removeElement(parentView.getLayout().getElements().lastKey());

                if (lastLine.hasImage()) {
                    parentView.getLayout().removeElement(parentView.getLayout().getElements().lastKey());
                }

                send.setCurrentText(lastLine.getContent());
                chat.removeMessage(index - 1);
                chat.update();
            }

        });

        TextOverlay impersonate = new TextOverlay(new FontIcon(Material2AL.BRUSH));
        impersonate.addStyle(Styles.ACCENT);
        impersonate.setTooltip("Generate your response.");
        addElement(impersonate);
        impersonate.onClick(event -> {

        });

        TextOverlay addMedia = new TextOverlay(new FontIcon(Material2AL.ADD));
        addMedia.addStyle(Styles.WARNING);
        addMedia.setTooltip("Attach an image to your prompt.");
        addElement(addMedia);
        addMedia.onClick(event -> {
            if (ServerProcess.getCurrentServer() == null || ServerProcess.getCurrentServer().isLoading() || ServerProcess.getCurrentServer().isError()) return;
            Model model = ServerProcess.getCurrentServer().getModel();
            if (model.getSettings().getMmProj().equalsIgnoreCase("None / Disabled")) {
                MessageOverlay error = new MessageOverlay(-100, 0, 600, 100,"Image Error", "This model is not configured to support images. Set the MM-Proj file if it's a vision supported model.");
                error.addStyle(Styles.DANGER);
                error.addStyle(Styles.BG_DEFAULT);
                App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, true);
                return;
            }

            // Attach an image to the response
            FileChooser fileChooser = new FileChooser();
            if (appSettings.getImagesPath() != null && !appSettings.getImagesPath().isEmpty()) {
                fileChooser.setInitialDirectory(new File(appSettings.getImagesPath()));
            }
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image", "*.png"));
            File image = fileChooser.showOpenDialog(App.window.getStage());
            if (image == null) return;
            appSettings.setImagesPath(image.getParent());
            parentView.setImage(image);

            HorizontalLayout imgBox = new HorizontalLayout(100, 40);
            imgBox.setMaxSize(100, 40);
            imgBox.setSpacing(20);

            imgBox.addElement(new TextOverlay(image.getName()));
            TextOverlay remove = new TextOverlay(new FontIcon(Material2AL.DELETE_FOREVER));
            remove.addStyle(Styles.DANGER);
            imgBox.addElement(remove);

            Node node = imgBox.render();
            remove.onClick(event1 -> {
                getPane().getChildren().remove(node);
                parentView.setImage(null);
            });
            getPane().getChildren().add(node);
        });
    }
}
