package me.piitex.app.views.chats;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import com.drew.lang.annotations.Nullable;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
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
import me.piitex.app.views.chats.components.*;
import me.piitex.engine.Container;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.*;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.javafx.FontIcon;
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
        container.addStyle(Styles.BG_INSET);

        HorizontalLayout main = new HorizontalLayout(appSettings.getWidth(), 0);
        main.setSpacing(35);
        main.addElement(new SidebarView().getRoot());

        container.addElement(main);

        VerticalLayout chatView = new VerticalLayout(0, 0);
        chatView.setAlignment(Pos.TOP_CENTER);
        chatView.setY(70);
        chatView.setSpacing(40);
        chatView.addStyle(Styles.BG_INSET);
        main.addElement(chatView);

        layout = new VerticalLayout(appSettings.getWidth() - 300, 0);
        layout.addStyle(Styles.BG_INSET);
        layout.setAlignment(Pos.TOP_CENTER);

        double scrollHeight = appSettings.getHeight() - 350;
        scrollContainer = new ScrollContainer(layout, 0, 0, appSettings.getWidth() - 250, scrollHeight);
        scrollContainer.addStyle(Styles.BG_INSET);
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
            buildChatBox(message, index, false);
            index++;
        }
    }

    public CardContainer buildChatBox(ChatMessage chatMessage, int index, boolean render) {
        CardContainer cardContainer = new ChatBoxCard(character, chat, chatMessage, index, this, appSettings.getWidth() - 350, 0);
        containerMap.put(index, cardContainer);
        layout.addElement(cardContainer);

        if (render) {
            layout.getPane().getChildren().add(cardContainer.build().getKey());
        }

        // If there is an image attached to the response build the image card
        if (chatMessage.getSender() == Role.USER && chatMessage.hasImage()) {
            File file = new File(chatMessage.getImageUrl());
            ImageCard imageCard = new ImageCard(chatMessage, file.getName(), appSettings.getWidth() - 350, 0);
            layout.addElement(imageCard);

            if (render) {
                layout.getPane().getChildren().add(imageCard.build().getKey());
            }

        }

        return cardContainer;
    }

    public HorizontalLayout buildButtonBox(ChatMessage chatMessage, int index) {
        HorizontalLayout buttonBox = new ButtonBoxLayout(character, chatMessage, chat, index, this, 900, 50);
        return buttonBox;
    }

    public VerticalLayout buildSendBox() {
        send = new RichTextAreaOverlay("", "Type your response.", 600, 150);
        submit = new ButtonOverlay("submit", "Send");

        VerticalLayout sendBox = new SendBox(send, submit, this, 0, 0);
        sendBox.setMaxSize(700, 250);
        return sendBox;
    }

    public HorizontalLayout buildTopControls() {
        topControls = new TopControlBox(chat, send, this,0, 0);
        topControls.setX(50);
        topControls.setSpacing(20);
        topControls.setMaxSize(1000, 50);

       return topControls;
    }

    public void checkServer() {
        ServerProcess serverProcess = ServerProcess.getCurrentServer();

        // Check if model override is active.
        // If it is, shutdown the current server if it's not running the same model.
        String model = character.getModel();
        if (model != null && !model.isEmpty() && character.isOverride()) {
            Model m = App.getModelByName(model);
            if (m != null) {
                if (serverProcess != null) {
                    Model check = serverProcess.getModel();
                    if (!check.getFile().getName().equalsIgnoreCase(m.getFile().getName())) {
                        serverProcess.stop();
                        new Thread(() -> {
                            new ServerProcess(m);
                        }).start();

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        handleServerLoad(serverProcess);
                        return;
                    }
                }

            }
        }
        // Show progress bar only if still loading
        serverProcess = ServerProcess.getCurrentServer();
        handleServerLoad(serverProcess);
    }

    private void handleServerLoad(ServerProcess serverProcess) {
        // Show progress bar only if still loading
        serverProcess = ServerProcess.getCurrentServer();
        if (serverProcess != null && serverProcess.isLoading()) {
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
                    ServerProcess.getCurrentServer().removeServerLoadingListener(this);
                }
            });
        }
    }


    private void renderProgress() {
        send.getNode().setDisable(true);
        submit.getNode().setDisable(true);

        // Display progress bar for backend loading
        ProgressBarOverlay progress = new ProgressBarOverlay();
        progress.setWidth(120);
        progress.setMaxHeight(50);
        progress.setY(10);
        TextOverlay label = new TextOverlay("Starting backend...");
        App.window.renderPopup(progress, 800, 885, 200, 100, false, label);
    }

    public void handleSubmit(String message) {
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

        buildChatBox(chatMessage, chat.getMessages().size(), true);

        int assistantIndex = chat.getMessages().size();
        ChatMessage newMsg = new ChatMessage(Role.ASSISTANT, "", (image != null ? image.getAbsolutePath() : null));
        System.out.println("Image Vali: " + newMsg.hasImage());
        CardContainer responseBox = buildChatBox(newMsg, assistantIndex, true); // Set content later

        // Gen response
        Response response = new Response(chat.getMessages().size(), message, character, character.getUser(), chat);

        chat.setResponse(response);

        generateResponse(response, chatMessage, responseBox);
    }

    public void generateResponse(Response response, ChatMessage chatMessage, CardContainer responseBox) {
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

            chat.addLine(Role.ASSISTANT, received, (chatMessage.hasImage() ? chatMessage.getImageUrl() : null));
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

    public VerticalLayout getLayout() {
        return layout;
    }

    public RichTextAreaOverlay getSend() {
        return send;
    }

    public ButtonOverlay getSubmit() {
        return submit;
    }

    public void setImage(File image) {
        this.image = image;
    }

    public Map<Integer, CardContainer> getContainerMap() {
        return containerMap;
    }

    public static TextFlowOverlay buildTextFlow(ChatMessage chatMessage, Chat chat, int index) {
        String content = Placeholder.applyDynamicBBCode(chatMessage.getContent());

        TextFlowOverlay chatBox;
        if (App.mobile) {
            chatBox = new TextFlowOverlay(content, 500, 0);
            chatBox.setMaxWidth(500);
        } else {
            chatBox = new TextFlowOverlay(content, App.getInstance().getAppSettings().getWidth() - 370, 0);
            chatBox.setMaxWidth(App.getInstance().getAppSettings().getWidth() - 370); // Set a little less than chatLayout
        }

        chatBox.addStyle(App.getInstance().getAppSettings().getChatTextSize());

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
