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
import javafx.scene.layout.Pane;
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
import java.util.concurrent.Future;

import static me.piitex.app.views.Positions.*;

public class ChatView extends EmptyContainer {
    private final Character character;
    private Chat chat;

    private VerticalLayout layout;
    private ScrollContainer scrollContainer;
    private HorizontalLayout topControls;

    private RichTextAreaOverlay send;

    private ButtonOverlay submit;

    private File image = null;

    // Not sure if this is smart but for better control map the layout to the index.
    private final Map<Integer, VerticalLayout> messageMap = new HashMap<>();

    private AppSettings appSettings = App.getInstance().getAppSettings();

    public ChatView(Character character, @Nullable Chat chat) {
        super(800, 600);

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
        chat.loadChat();
        character.setLastChat(chat);
        init();
    }

    public ChatView(Character character, @Nullable Chat chat, boolean create) {
        super(800, 600);

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
        chat.loadChat();
        character.setLastChat(chat);
        init();
    }

    public void init() {
        setWidth(appSettings.getWidth());
        setHeight(0);
        addStyle(Styles.BG_INSET);

        HorizontalLayout main = new HorizontalLayout(appSettings.getWidth(), 0);
        main.setSpacing(5);

        SidebarView sidebarView = new SidebarView(main, false);
        main.addElement(sidebarView.getRoot());

        addElement(main);

        VerticalLayout chatView = new VerticalLayout(0, 0);
        chatView.setAlignment(Pos.TOP_CENTER);
        //chatView.setY(70);
        chatView.setSpacing(40);
        chatView.addStyle(Styles.BG_INSET);
        main.addElement(chatView);

        layout = new VerticalLayout(0, 0);
        layout.addStyle(Styles.BG_INSET);
        layout.setAlignment(Pos.TOP_CENTER);

        scrollContainer = new ScrollContainer(layout, CHAT_VIEW_SCROLL_X, CHAT_VIEW_SCROLL_Y, CHAT_VIEW_SCROLL_WIDTH, CHAT_VIEW_SCROLL_HEIGHT);
        scrollContainer.addStyle(Styles.BG_INSET);
        scrollContainer.setMaxSize(CHAT_VIEW_SCROLL_WIDTH, CHAT_VIEW_SCROLL_HEIGHT);
        scrollContainer.setVerticalScroll(true);
        scrollContainer.setScrollWhenNeeded(true);
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setPannable(true);
        scrollContainer.setScrollPosition(10000);

        chatView.addElement(scrollContainer);
        chatView.addElement(buildSendBox());

        addElement(buildSelection());

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

        sidebarView.setOnCollapseStateChange((aBoolean) -> {
            if (aBoolean) {
                scrollContainer.getScrollPane().setPrefWidth(CHAT_VIEW_SCROLL_WIDTH + 50);
                scrollContainer.getScrollPane().setMaxWidth(CHAT_VIEW_SCROLL_WIDTH + 50);
                scrollContainer.getScrollPane().setMinWidth(CHAT_VIEW_SCROLL_WIDTH + 50);
            }
        });

    }

    public ChoiceBoxOverlay buildSelection() {
        List<String> items = new ArrayList<>();
        items.add("New Chat");
        items.addAll(character.getChatFileNames());

        ChoiceBoxOverlay selection = new ChoiceBoxOverlay(items, CHAT_VIEW_SELECTION_WIDTH, CHAT_VIEW_SELECTION_HEIGHT);
        selection.setDefaultItem(chat.getFile().getName());
        selection.setX(CHAT_VIEW_SELECTION_X);
        selection.setMaxWidth(CHAT_VIEW_SELECTION_WIDTH);
        selection.setMaxHeight(CHAT_VIEW_SELECTION_HEIGHT);

        selection.onClick(event -> {
            selection.onItemSelect(event1 -> {
                String item = event1.getItem();
                Chat next = character.getChat(item);
                if (next != null && !next.getFile().getName().equalsIgnoreCase(chat.getFile().getName())) {
                    next.loadChat();
                }

                App.window.clearContainers();
                App.window.addContainer(new ChatView(character, next, true));

            });
        });

        return selection;
    }

    public void loadMessages() {
        int index = 0;
        for (ChatMessage message : chat.getMessages()) {
            buildChatBox(message, index);
            index++;
        }
    }

    public VerticalLayout buildChatBox(ChatMessage chatMessage, int index) {
        return new ChatMessageBox(character, chat, chatMessage, index, this, CHAT_BOX_WIDTH, CHAT_BOX_HEIGHT);
    }

    public HorizontalLayout buildButtonBox(VerticalLayout messageBox, ChatMessage chatMessage, int index) {
        return new ButtonBoxLayout(messageBox, character, chatMessage, chat, index, this, CHAT_BOX_BUTTON_BOX_WIDTH, CHAT_BOX_BUTTON_BOX_HEIGHT);
    }

    public VerticalLayout buildSendBox() {
        send = new RichTextAreaOverlay("", "Type your response.", CHAT_SEND_BOX_WIDTH, CHAT_SEND_BOX_HEIGHT);
        send.setMaxHeight(CHAT_SEND_BOX_HEIGHT);
        submit = new ButtonOverlay("submit", "Send");

        VerticalLayout sendBox = new SendBox(send, submit, this, CHAT_SEND_BOX_WIDTH, CHAT_SEND_BOX_HEIGHT);
        sendBox.setMaxSize(CHAT_SEND_BOX_WIDTH, CHAT_SEND_BOX_HEIGHT);
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
            String dir = model.split("/")[0];
            String file = model.split("/")[1];
            Model m = App.getModelByName(dir, file);
            if (m != null) {
                if (serverProcess != null) {
                    Model check = serverProcess.getModel();
                    if (!check.getFile().getName().equalsIgnoreCase(m.getFile().getName())) {
                        serverProcess.stop();
                        App.getThreadPoolManager().submitTask(() -> {
                            new ServerProcess(m);
                        });

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        handleServerLoad();
                        return;
                    }
                }

            }
        }
        handleServerLoad();
    }

    private void handleServerLoad() {
        // Show progress bar only if still loading
        ServerProcess serverProcess = ServerProcess.getCurrentServer();
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
        send.setEnabled(false);
        submit.setEnabled(false);

        // Remove regen from previous
        int lastKey;
        if (scrollContainer.getLayout().getElements().isEmpty()) {
            lastKey = -1;
        } else {
            lastKey = scrollContainer.getLayout().getElements().lastKey();
        }
        if (lastKey != -1) {
            VerticalLayout previous = (VerticalLayout) scrollContainer.getLayout().getElementAt(lastKey);
            if (previous != null && !chat.getMessages().isEmpty()) {
                Role previousSender = chat.getMessages().getLast().getSender();
                if (previousSender == Role.ASSISTANT) {
                    CardContainer card = (CardContainer) previous.getElements().values().stream().filter(element -> element instanceof CardContainer).findAny().orElse(null);
                    if (card != null) {
                        HorizontalLayout buttonLayout = (HorizontalLayout) card.getFooterLayout();
                        buttonLayout.removeElement(buttonLayout.getElements().lastKey());
                    }
                }
            }
        }

        message = Placeholder.formatPlaceholders(message, character, character.getUser());
        ChatMessage chatMessage = new ChatMessage(Role.USER, message, (image != null ? image.getAbsolutePath() : null));

        buildChatBox(chatMessage, chat.getMessages().size());
        chat.addLine(chatMessage);

        int assistantIndex = chat.getMessages().size();
        ChatMessage newMsg = new ChatMessage(Role.ASSISTANT, "", (image != null ? image.getAbsolutePath() : null));
        VerticalLayout responseBox = buildChatBox(newMsg, assistantIndex); // Set content later

        // Gen response
        Response response = new Response(chat.getMessages().size(), message, character, character.getUser(), chat);

        chat.setResponse(response);

        generateResponse(response, chatMessage, responseBox);
    }

    public void generateResponse(Response response, ChatMessage chatMessage, VerticalLayout responseBox) {
        CardContainer card = (CardContainer) responseBox.getElements().values().stream().filter(element -> element instanceof CardContainer).findAny().orElse(null);

        if (image != null) {
            response.setImage(image);
        }

        Future<?> thread = App.getThreadPoolManager().submitTask(() -> {
            // Generate response!
            response.setPrompt(chatMessage.getContent());
            String received;
            try {
                received = Server.generateResponseOAIStream(scrollContainer.getScrollPane(), card, response);
            } catch (IOException e) {
                Platform.runLater(() -> {
                    MessageOverlay error = new MessageOverlay(0, 0, 600, 100,"Response Error", "Could not generate a response! Check backend status and settings.");
                    error.addStyle(Styles.DANGER);
                    error.addStyle(Styles.BG_DEFAULT);
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

            thread.cancel(true); // Does not stop input stream
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

    public Map<Integer, VerticalLayout> getMessageMap() {
        return messageMap;
    }

    public static TextFlowOverlay buildTextFlow(ChatMessage chatMessage, Chat chat, int index) {
        String content = Placeholder.applyDynamicBBCode(chatMessage.getContent());
        AppSettings appSettings = App.getInstance().getAppSettings();

        TextFlowOverlay chatBox;
        int width = CHAT_BOX_WIDTH;
        // Set a little less than chatLayout
        chatBox = new TextFlowOverlay(content, width - 30, 0);
        chatBox.setMaxWidth(width - 30);

        chatBox.addStyle(appSettings.getChatTextSize());

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
