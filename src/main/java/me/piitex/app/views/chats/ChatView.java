package me.piitex.app.views.chats;

import atlantafx.base.controls.Card;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import com.drew.lang.annotations.Nullable;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import me.piitex.app.App;
import me.piitex.app.backend.*;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.server.Server;
import me.piitex.app.backend.server.ServerLoadingListener;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.utils.Placeholder;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.Container;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.*;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.*;
import org.fxmisc.richtext.StyledTextArea;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ChatView {
    private final Character character;
    private Chat chat;
    private Container container;

    private VerticalLayout layout;
    private ScrollContainer scrollContainer;
    private HorizontalLayout topControls;

    private RichTextAreaOverlay send;

    private ButtonOverlay submit;

    private File image = null;

    // Not sure if this is smart but for better control map the container to the index.
    private final Map<Integer, CardContainer> containerMap = new HashMap<>();

    private AppSettings appSettings = App.getInstance().getAppSettings();

    public ChatView(Character character, @Nullable Chat chat) {
        this.character = character;
        if (chat == null) {
            if (character.getLastChat() != null) {
                chat = character.getLastChat();
            } else {
                chat = new Chat(new File(character.getChatDirectory(), "untitled-" + character.getChatDirectory().listFiles().length));
                character.getChats().add(chat);

                // Add first message to chat if it exists.
                if (character.getFirstMessage() != null && !character.getFirstMessage().isEmpty()) {
                    chat.addLine(Role.ASSISTANT, character.getFirstMessage());
                }
            }
        }
        this.chat = chat;
        character.setLastChat(chat);
        build();
    }

    public ChatView(Character character, @Nullable Chat chat, boolean create) {
        this.character = character;
        if (create) {
            if (chat == null) {

                chat = new Chat(new File(character.getChatDirectory(), "untitled-" + character.getChatDirectory().listFiles().length));
                character.getChats().add(chat);
                // Add first message to chat if it exists.
                if (character.getFirstMessage() != null && !character.getFirstMessage().isEmpty()) {
                    chat.addLine(Role.ASSISTANT, character.getFirstMessage());

                }
            }
            this.chat = chat;
        }
        character.setLastChat(chat);
        build();
    }

    public void build() {
        container = new EmptyContainer(appSettings.getWidth(), 0);

        HorizontalLayout main = new HorizontalLayout(appSettings.getWidth(), 0);
        main.setSpacing(35);
        main.addElement(new SidebarView().getRoot());

        container.addElement(main);

        VerticalLayout chatView = new VerticalLayout(0, 0);
        chatView.setAlignment(Pos.TOP_CENTER);
        chatView.setY(70);
        chatView.setSpacing(40);
        main.addElement(chatView);

        layout = new VerticalLayout(appSettings.getWidth() - 300, 0);
        //layout.setX(100);
        layout.setAlignment(Pos.TOP_CENTER);

        double scrollHeight = appSettings.getHeight() - 350;
        scrollContainer = new ScrollContainer(layout, 0, 0, appSettings.getWidth() - 300, scrollHeight);
        scrollContainer.setMaxSize(appSettings.getWidth(), scrollHeight);
        scrollContainer.setVerticalScroll(true);
        scrollContainer.setScrollWhenNeeded(true);
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setScrollPosition(10000);
        chatView.addElement(scrollContainer);
        chatView.addElement(buildSendBox());

        container.addElement(buildSelection());

        // Layout is the chat window.
        loadMessages();

        Platform.runLater(() -> {
            if (!character.isShownDisclaimer()) {
                AlertOverlay alert = new AlertOverlay("DISCLAIMER", Alert.AlertType.WARNING);
                alert.setWidth(600);
                alert.setHeight(250);
                alert.setContent("Everything the character says is made up. Do not use AI for mental or medical health assistance.");
                alert.addButton(new ButtonType("I Understand", ButtonBar.ButtonData.YES));

                alert.onConfirm(event -> {
                    character.setShownDisclaimer(true);
                });

                App.window.renderAlert(alert);
            }
        });

    }

    public ChoiceBoxOverlay buildSelection() {
        List<String> items = new ArrayList<>();
        items.add("New Chat");
        items.addAll(character.getChatFileNames());

        ChoiceBoxOverlay selection = new ChoiceBoxOverlay(items);
        selection.setDefaultItem(chat.getFile().getName());
        selection.setX(appSettings.getWidth() / 2 - 100);
        selection.setY(20);
        selection.setMaxWidth(400);
        selection.setWidth(400);
        selection.setHeight(50);

        selection.onItemSelect(event -> {
            String item = event.getItem();
            Chat next = character.getChat(item);

            App.window.clearContainers();
            App.window.addContainer(new ChatView(character, next, true).getContainer());
            App.window.render();

        });

        return selection;
    }

    public void loadMessages() {
        int index = 0;
        for (ChatMessage message : chat.getMessages()) {
            CardContainer cardContainer = buildChatBox(message, index);
            containerMap.put(index, cardContainer);
            layout.addElement(cardContainer);
            index++;
        }
    }

    public CardContainer buildChatBox(ChatMessage chatMessage, int index) {
        CardContainer cardContainer = new CardContainer(appSettings.getWidth() - 300, 0); // Width, height
        cardContainer.setMaxSize(appSettings.getWidth() - 300, 0);
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
        avatar.setWidth(64);
        avatar.setHeight(64);

        HorizontalLayout header = new HorizontalLayout(900 ,0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(50);
        header.addElement(avatar);
        cardContainer.setHeader(header);

        TextOverlay display = new TextOverlay(displayName);
        display.addStyle(Styles.TITLE_4);
        header.addElement(display);

        cardContainer.setBody(buildTextFlow(chatMessage, chat, index));

        cardContainer.setFooter(buildButtonBox(chatMessage, index));

        return cardContainer;
    }

    public HorizontalLayout buildButtonBox(ChatMessage chatMessage, int index) {
        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setSpacing(20);
        root.setMaxSize(900, 50);

        TextOverlay copy = new TextOverlay(new FontIcon(Material2AL.CONTENT_COPY));
        copy.setTooltip("Copy text to clipboard.");
        root.addElement(copy);
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
        root.addElement(edit);

        edit.onClick(event -> {
            if (ServerProcess.getCurrentServer().isLoading()) return;

            ChatMessage originalMessage = chat.getMessage(index);

            if (originalMessage == null) {
                App.logger.warn("Attempted to edit message at index " + index + " but no message found.");
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

                layout.getPane().getChildren().removeLast(); // Re-evaluate this line for correctness in your UI setup

                ChatMessage updatedChatMessage = chat.getMessage(index);
                if (updatedChatMessage != null) {
                    CardContainer responseBox = buildChatBox(updatedChatMessage, index);
                    containerMap.put(index, responseBox);
                    layout.addElement(responseBox);
                    layout.getPane().getChildren().add(responseBox.build().getKey());
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
        root.addElement(delete);
        delete.onClick(event -> {
            // Delete the message
            //TODO: Add confirm for deleting
            layout.getPane().getChildren().removeLast();
            chat.removeMessage(index);
            chat.update();
        });

        // Doesn't make sense to regenerate the configured message.
        String firstMsg = (character.getFirstMessage() == null || character.getFirstMessage().isEmpty() ? "null" : character.getFirstMessage());
        if (chatMessage.getSender() == Role.ASSISTANT && !firstMsg.equalsIgnoreCase(chatMessage.getContent())) {
            TextOverlay regenerate = new TextOverlay(new FontIcon(Material2MZ.REFRESH));
            regenerate.setTooltip("Regenerate the response.");
            root.addElement(regenerate);
            regenerate.onClick(event -> {
                if (ServerProcess.getCurrentServer() == null || ServerProcess.getCurrentServer().isLoading() || ServerProcess.getCurrentServer().isError()) return;

                if (index + 1 != chat.getMessages().size()) {
                    return;
                }

                // Remove the line which is the last line of the chat
                chat.removeMessage(index);
                layout.getPane().getChildren().removeLast();

                // Generate new box with
                CardContainer responseBox = buildChatBox(chatMessage, chat.getMessages().size()); // Set content later
                containerMap.put(chat.getMessages().size(), responseBox);
                // Gen response

                Response response = new Response(index, chat.getLastLine(index).getContent(), character, character.getUser(), chat);
                chat.setResponse(response);

                layout.addElement(responseBox);
                layout.getPane().getChildren().add(responseBox.build().getKey());
                // Disable buttons to prevent spamming
                send.getNode().setDisable(true);
                submit.getNode().setDisable(true);
                generateResponse(response, chatMessage, responseBox);

            });
        }


        return root;
    }

    // Tile is really nice not but styleable. Might switch back to it though.
//    public TileContainer buildChatBox(Role role, String content) {
//        TileContainer tileContainer = new TileContainer(800, 0);
//        tileContainer.getStyles().add(Styles.BORDERED);
//        String iconPath = "";
//        String displayName = "";
//        if (role == Role.ASSISTANT) {
//            iconPath = character.getUser().getIconPath();
//            displayName = character.getUser().getDisplayName();
//        } else {
//            iconPath = character.getIconPath();
//            displayName = character.getDisplayName();
//        }
//        System.out.println("Icon Path: " + iconPath);
//
//        ImageOverlay avatar = new ImageOverlay(new ImageLoader(new File(iconPath)));
//        avatar.setWidth(64);
//        avatar.setHeight(64);
//        tileContainer.setGraphic(avatar);
//
//        tileContainer.setTitle(displayName);
//        tileContainer.setDescription(content);
//
//        return tileContainer;
//    }

    public VerticalLayout buildSendBox() {
        VerticalLayout root = new VerticalLayout(0, 0);
        root.setMaxSize(700, 250);

        root.addElement(buildTopControls());

        HorizontalLayout bottom = new HorizontalLayout(0, 0);
        //bottom.setX(200);
       //bottom.setY(100);
        bottom.setSpacing(20);
        bottom.setMaxSize(700, 150);

        root.addElement(bottom);

        send = new RichTextAreaOverlay("", "Type your response.", 600, 150);
        send.setBackgroundColor(appSettings.getThemeDefaultColor(appSettings.getTheme()));
        send.setBorderColor(appSettings.getThemeBorderColor(appSettings.getTheme()));
        send.setTextFill(appSettings.getThemeTextColor(appSettings.getTheme()));

        send.addStyle(Styles.BG_DEFAULT);
        send.addStyle(appSettings.getTextSize());
        send.addStyle(Styles.TEXT_ON_EMPHASIS);
        bottom.addElement(send);

        submit = new ButtonOverlay("submit", "Send");
        submit.addStyle(Styles.ACCENT);
        submit.setY(25);
        submit.setWidth(100);
        submit.setHeight(50);
        bottom.addElement(submit);

        send.onSubmit(event -> {
            // Handle submit action. Send the input to the model and generate a response
            StyledTextArea textArea = (StyledTextArea) send.getNode();
            handleSubmit(textArea.getText());
            textArea.replaceText("");
        });

        submit.onClick(event -> {
            StyledTextArea textArea = (StyledTextArea) send.getNode();
            handleSubmit(textArea.getText());
            textArea.replaceText("");
        });


        Platform.runLater(this::checkServer);

        return root;
    }

    public HorizontalLayout buildTopControls() {
        topControls = new HorizontalLayout(0, 0);
        topControls.setX(50);
        topControls.setSpacing(20);
        topControls.setMaxSize(1000, 50);

        TextOverlay undo = new TextOverlay(new FontIcon(Material2MZ.UNDO));
        undo.setTooltip("Undo the last message.");
        topControls.addElement(undo);
        undo.onClick(event -> {
            if (ServerProcess.getCurrentServer().isLoading()) return;

            // Copy user message to clipboard
            // Delete both last assistant and user messages
            int index = chat.getMessages().size();
            if (index == 0) return;

            ChatMessage lastLine = chat.getLastLine(index);
            int layoutSize = layout.getPane().getChildren().size();

            Role role = lastLine.getSender();
            if (role == Role.ASSISTANT) {
                layout.getPane().getChildren().remove(layoutSize - 1);
                layout.getPane().getChildren().remove(layoutSize - 2);

                // Remove assistant
                chat.removeMessage(index - 1);

                // Copy user message and then remove it
                ChatMessage content = chat.getMessage(index - 2);
                ((StyledTextArea) send.getNode()).replaceText(content.getContent());
                chat.removeMessage(index - 2);

                chat.update();
            } else {
                // The last message is user
                // Just remove the message from the chat and put it back into the box.
                layout.getPane().getChildren().remove(layoutSize - 1);
                ((TextArea) send.getNode()).setText(lastLine.getContent());
                chat.removeMessage(index - 1);
                chat.update();
            }


        });

        TextOverlay impersonate = new TextOverlay(new FontIcon(Material2AL.BRUSH));
        impersonate.setTooltip("Generate your response.");
        topControls.addElement(impersonate);
        impersonate.onClick(event -> {

        });

        TextOverlay addMedia = new TextOverlay(new FontIcon(Material2AL.ADD));
        addMedia.setTooltip("Attach an image to your prompt.");
        topControls.addElement(addMedia);
        addMedia.onClick(event -> {
            if (ServerProcess.getCurrentServer() == null || ServerProcess.getCurrentServer().isLoading() || ServerProcess.getCurrentServer().isError()) return;
            Model model = ServerProcess.getCurrentServer().getModel();
            if (model.getSettings().getMmProj().equalsIgnoreCase("None / Disabled")) {
                MessageOverlay error = new MessageOverlay(-100, 0, 600, 100,"Image Error", "This model is not configured to support images. Set the MM-Proj file if it's a vision supported model.");
                error.addStyle(Styles.DANGER);
                error.addStyle(Styles.ELEVATED_4);
                App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, true);
                return;
            }

            // Attach an image to the response
            FileChooser fileChooser = new FileChooser();
            if (appSettings.getImagesPath() != null && !appSettings.getImagesPath().isEmpty()) {
                fileChooser.setInitialDirectory(new File(appSettings.getImagesPath()));
            }
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image", "*.png"));
            image = fileChooser.showOpenDialog(App.window.getStage());
            if (image == null) return;
            appSettings.setImagesPath(image.getParent());

            HorizontalLayout imgBox = new HorizontalLayout(100, 40);
            imgBox.setMaxSize(100, 40);
            imgBox.setSpacing(20);

            imgBox.addElement(new TextOverlay(image.getName()));
            TextOverlay remove = new TextOverlay(new FontIcon(Material2AL.DELETE_FOREVER));
            remove.addStyle(Styles.DANGER);
            imgBox.addElement(remove);

            Node node = imgBox.render();
            remove.onClick(event1 -> {
                topControls.getPane().getChildren().remove(node);
                image = null;
            });
            topControls.getPane().getChildren().add(node);

        });


        return topControls;
    }

    private void checkServer() {
        // If the server is currently running but not active display progress bar
        if (ServerProcess.getCurrentServer() != null) {
            ServerProcess serverProcess = ServerProcess.getCurrentServer();
            // Show progress bar only if still loading
            if (serverProcess.isLoading()) {
                send.getNode().setDisable(true);
                submit.getNode().setDisable(true);
                renderProgress(); // Call renderProgress() to show your popup

                // Add a listener to be notified when loading is complete
                serverProcess.addServerLoadingListener(new ServerLoadingListener() {
                    @Override
                    public void onServerLoadingComplete(boolean success) {
                        // Ensure UI updates are on the JavaFX Application Thread
                        Platform.runLater(() -> {
                            if (App.window.getCurrentPopup() != null) { // Check if popup still exists
                                App.window.removeContainer(App.window.getCurrentPopup());
                                App.window.render();

                                send.getNode().setDisable(false);
                                submit.getNode().setDisable(false);
                            }
                        });
                        // Crucial: Remove the listener if it's a one-time event, to prevent memory leaks
                        serverProcess.removeServerLoadingListener(this);
                    }
                });
            }
        }
    }

    private void renderProgress() {
        // Display progress bar for backend loading
        ProgressBarOverlay progress = new ProgressBarOverlay();
        progress.setWidth(120);
        progress.setMaxHeight(50);
        progress.setY(10);
        TextOverlay label = new TextOverlay("Starting backend...");
        App.window.renderPopup(progress, 800, 885, 200, 100, false, label);
    }

    private void handleSubmit(String message) {
        if (message.isEmpty()) return;

        // Disable buttons to prevent spamming
        send.getNode().setDisable(true);
        submit.getNode().setDisable(true);

        // Remove regen from previous card
        CardContainer previous = containerMap.get(chat.getMessages().size() - 1);
        if (!chat.getMessages().isEmpty()) {
            Role previousSender = chat.getMessages().getLast().getSender();
            if (previous != null && previousSender == Role.ASSISTANT) {
                Card card = (Card) previous.getView();
                if (card != null) {
                    HBox buttonLayout = (HBox) card.getFooter();
                    buttonLayout.getChildren().removeLast();
                }
            }
        }

        message = Placeholder.formatPlaceholders(message, character, character.getUser());
        ChatMessage chatMessage = chat.addLine(Role.USER, message, (image != null ? image.getAbsolutePath() : null));
        chat.update();

        CardContainer userBox = buildChatBox(chatMessage, chat.getMessages().size());
        containerMap.put(chat.getMessages().size(), userBox);


        layout.addElement(userBox); // This doesn't render the user box, but it does add it to the layout for reference.
        layout.getPane().getChildren().add(userBox.build().getKey()); // Force render the new box
        //chat.addLine(role, message);

        int assistantIndex = chat.getMessages().size();
        ChatMessage newMsg = new ChatMessage(Role.ASSISTANT, "", null);
        CardContainer responseBox = buildChatBox(newMsg, assistantIndex); // Set content later
        containerMap.put(chat.getMessages().size(), responseBox);

        // Gen response

        Response response = new Response(chat.getMessages().size(), message, character, character.getUser(), chat);
        chat.setResponse(response);

        layout.addElement(responseBox);
        layout.getPane().getChildren().add(responseBox.build().getKey());

        generateResponse(response, chatMessage, responseBox);
    }

    private void generateResponse(Response response, ChatMessage chatMessage, CardContainer responseBox) {
        Card card = (Card) responseBox.build().getKey();

        if (image != null) {
            response.setImage(image);
        }

        Thread thread = new Thread(() -> {
            // Generate response!
            response.setPrompt(chatMessage.getContent());
            String received;
            try {
                received = Server.generateResponseOAIStream(scrollContainer.getScrollPane(), card, response);
            } catch (IOException e) {
                Platform.runLater(() -> {
                    MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Response Error", "Could not generate a response! Check backend status and settings.");
                    error.addStyle(Styles.DANGER);
                    App.window.renderPopup(error, PopupPosition.BOTTOM_CENTER, 600, 100, true);

                    topControls.getPane().getChildren().removeLast();
                });
                return;
            } catch (InterruptedException e) {
                // Get the current response
                received = response.getResponse();
                Platform.runLater(() -> {
                    if (App.window.getCurrentPopup() != null) {
                        App.window.removeContainer(App.window.getCurrentPopup());
                    }
                });
            }

            chat.addLine(Role.ASSISTANT, received);
            chat.getResponse().getResponses().put(chat.getResponse().getResponses().size(), received);
            chat.getResponse().update();
            send.getNode().setDisable(false);
            submit.getNode().setDisable(false);

            Platform.runLater(() -> {
                topControls.getPane().getChildren().removeLast();
            });
        });

        // Since this is being manually added to JavaFX pane, RenEngine API is useless.
        TextOverlay stop = new TextOverlay(new FontIcon(Material2MZ.STOP_CIRCLE));
        stop.addStyle(Styles.DANGER);

        Tooltip tooltip = new Tooltip("Stop the response stream.");
        tooltip.setShowDelay(Duration.millis(250));
        Node stopNode = stop.render();
        Tooltip.install(stopNode, tooltip);

        stop.onClick(event -> {
            App.logger.info("Force stopping response...");
            stopNode.setDisable(true);

            thread.interrupt(); // Does not stop input stream
            Platform.runLater(() -> {
                ProgressBarOverlay progress = new ProgressBarOverlay();
                progress.setWidth(120);
                progress.setMaxHeight(50);
                progress.setY(10);
                TextOverlay label = new TextOverlay("Stopping Response...");
                App.window.renderPopup(progress, 800, 885, 200, 100, false, label);
            });
        });

        topControls.getPane().getChildren().add(stopNode);

        thread.start();
    }

    public Container getContainer() {
        return container;
    }

    public static TextFlowOverlay buildTextFlow(ChatMessage chatMessage, Chat chat, int index) {
        String content = Placeholder.applyDynamicBBCode(chatMessage.getContent());

        TextFlowOverlay chatBox;
        if (App.mobile) {
            chatBox = new TextFlowOverlay(content, 500, 0);
            chatBox.setMaxWidth(500);
        } else {
            chatBox = new TextFlowOverlay(content, App.getInstance().getAppSettings().getWidth() - 320, 0);
            chatBox.setMaxWidth(App.getInstance().getAppSettings().getWidth() - 320); // Set a little less than chatLayout
        }

        chatBox.addStyle(App.getInstance().getAppSettings().getTextSize());

        // Right click menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copy = new MenuItem("Copy");
        contextMenu.getItems().add(copy);

        chatBox.onClick(event -> {
            if (event.getHandler().getButton() == MouseButton.SECONDARY) {
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                    return;
                }
                copy.setOnAction(event1 -> {
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(Placeholder.retrieveOriginalText(content));
                    clipboard.setContent(clipboardContent);
                });

                contextMenu.show(chatBox.getNode(), Side.BOTTOM, 300, 0);
            }
        });

        return chatBox;
    }
}
