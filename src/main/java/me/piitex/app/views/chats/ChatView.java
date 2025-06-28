package me.piitex.app.views.chats;

import atlantafx.base.controls.Card;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.BBCodeParser;
import com.drew.lang.annotations.Nullable;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.Chat;
import me.piitex.app.backend.Response;
import me.piitex.app.backend.Role;
import me.piitex.app.backend.server.Server;
import me.piitex.app.backend.server.ServerLoadingListener;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.utils.Placeholder;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.Container;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.*;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.Layout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatView {
    private final Character character;
    private Chat chat;
    private Container container;

    private VerticalLayout layout;
    private ScrollContainer scrollContainer;
    private HorizontalLayout topControls;

    private TextAreaOverlay send;

    private ButtonOverlay submit;

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
        container = new EmptyContainer(1920, 0);

        HorizontalLayout main = new HorizontalLayout(1900, 0);
        main.setSpacing(35);
        main.addElement(new SidebarView().getRoot());

        container.addElement(main);

        VerticalLayout chatView = new VerticalLayout(0, 0);
        chatView.setY(70);
        main.addElement(chatView);

        layout = new VerticalLayout(1000, 0);
        layout.setX(100);
        layout.setAlignment(Pos.TOP_CENTER);

        double scrollHeight = 760;
        scrollContainer = new ScrollContainer(layout, 0, 0, 1500, scrollHeight);
        scrollContainer.setMaxSize(1920, scrollHeight);
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
        selection.setX(750);
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
        for (String line : chat.getRawLines()) {
            Role role = chat.getSender(line);
            String content = chat.getContent(line);
            layout.addElement(buildChatBox(role, content, index));
            index++;
        }
    }

    public CardContainer buildChatBox(Role role, String content, int index) {
        CardContainer cardContainer = new CardContainer(1000, 0); // Width, height
        cardContainer.setMaxSize(1300, 0);
        String iconPath = "";
        String displayName = "";
        if (role == Role.USER) {
            iconPath = character.getUser().getIconPath();
            displayName = character.getUser().getDisplayName();
        } else {
            iconPath = character.getIconPath();
            displayName = character.getDisplayName();
        }

        ImageOverlay avatar = new ImageOverlay(new ImageLoader(new File(iconPath)));
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

        content = Placeholder.applyDynamicBBCode(content);

        VerticalLayout chatLayout = new VerticalLayout(1300, 0);

        TextFlowOverlay chatBox = new TextFlowOverlay(content, 1100, 0);
        chatBox.setMaxWidth(1100); // Set a little less than chatLayout
        chatLayout.addElement(chatBox);

        cardContainer.setBody(chatBox);

        cardContainer.setFooter(buildButtonBox(role, content, index));

        return cardContainer;
    }

    public HorizontalLayout buildButtonBox(Role role, String message, int index) {
        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setSpacing(20);
        root.setMaxSize(900, 50);

        TextOverlay copy = new TextOverlay(new FontIcon(Material2AL.CONTENT_COPY));
        copy.setTooltip("Copy text to clipboard.");
        root.addElement(copy);
        copy.onClick(event -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(Placeholder.retrieveOriginalText(message));
            clipboard.setContent(clipboardContent);

            // Maybe add green to the icon and set it back??
            copy.getNode().getStyleClass().add(Styles.SUCCESS);

            // After 3 seconds remove the style?
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(3), // Delay for 3 seconds
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

            // Open edit dialogue or something
            VerticalLayout verticalLayout = new VerticalLayout(0, 0);

            ModalContainer modalContainer = new ModalContainer(verticalLayout, 400, 400);
            modalContainer.addStyle(Styles.ELEVATED_1);

            verticalLayout.addElement(new TextOverlay("Edit Message"));
            TextAreaOverlay area = new TextAreaOverlay(message, 0, 0, 400, 300);
            verticalLayout.addElement(area);

            ButtonOverlay submit = new ButtonOverlay("submit", "Submit");
            submit.addStyle(Styles.SUCCESS);
            verticalLayout.addElement(submit);
            submit.onClick(event1 -> {
                String old = chat.getLine(index);

                String replace = area.getCurrentText();
                replace = Role.ASSISTANT.name().toLowerCase() + ":" + replace;

                chat.replaceLine(old, replace);

                layout.getPane().getChildren().removeLast();

                CardContainer responseBox = buildChatBox(Role.ASSISTANT, chat.getContent(replace), chat.getRawLines().size());
                layout.addElement(responseBox);
                layout.getPane().getChildren().add(responseBox.build().getKey());

                App.window.removeContainer(modalContainer);

                chat.update();
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
            chat.removeLine(index);
            chat.update();
        });

        if (role == Role.ASSISTANT && !Placeholder.retrieveOriginalText(message).equalsIgnoreCase(character.getFirstMessage())) {
            TextOverlay regenerate = new TextOverlay(new FontIcon(Material2MZ.REFRESH));
            regenerate.setTooltip("Regenerate the response.");
            root.addElement(regenerate);
            regenerate.onClick(event -> {
                if (ServerProcess.getCurrentServer() == null || ServerProcess.getCurrentServer().isLoading() || ServerProcess.getCurrentServer().isError()) return;

                if (index + 1 != chat.getRawLines().size()) {
                    return;
                }

                String remove = chat.getLine(index);

                // Remove the line which is the last line of the chat
                chat.removeLine(chat.getIndex(remove));
                layout.getPane().getChildren().removeLast();

                // Generate new box with
                CardContainer responseBox = buildChatBox(Role.ASSISTANT, "", chat.getRawLines().size()); // Set content later
                // Gen response

                Response response = new Response(chat.getLines().size() + 1, message, character, character.getUser(), chat);
                chat.setResponse(response);

                layout.addElement(responseBox);
                layout.getPane().getChildren().add(responseBox.build().getKey());
                // Disable buttons to prevent spamming
                send.getNode().setDisable(true);
                submit.getNode().setDisable(true);
                generateResponse(response, message, responseBox);

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
        root.setMaxSize(1000, 200);
        root.setX(200);
        root.setY(50);

        root.addElement(buildTopControls());

        HorizontalLayout bottom = new HorizontalLayout(0, 0);
        //bottom.setX(200);
       //bottom.setY(100);
        bottom.setSpacing(20);
        bottom.setMaxSize(1000, 100);

        root.addElement(bottom);

        send = new TextAreaOverlay("", "Type your response.", 0, 0, 800, 100);
        bottom.addElement(send);

        submit = new ButtonOverlay("submit", "Send");
        submit.addStyle(Styles.ACCENT);
        submit.setY(25);
        submit.setWidth(100);
        submit.setHeight(50);
        bottom.addElement(submit);

        send.onSubmit(event -> {
            // Handle submit action. Send the input to the model and generate a response
            TextArea textArea = (TextArea) send.getNode();
            handleSubmit(textArea.getText());
            textArea.setText("");
        });

        submit.onClick(event -> {
            TextArea textArea = (TextArea) send.getNode();
            handleSubmit(textArea.getText());
            textArea.setText("");
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
            int index = chat.getRawLines().size();
            String lastLine = chat.getLastLine(index);
            int layoutSize = layout.getPane().getChildren().size();

            Role role = chat.getSender(lastLine);
            if (role == Role.ASSISTANT) {
                layout.getPane().getChildren().remove(layoutSize - 1);
                layout.getPane().getChildren().remove(layoutSize - 2);

                // Remove assistant
                chat.removeLine(index - 1);

                // Copy user message and then remove it
                String content = chat.getLine(index - 2);
                ((TextArea) send.getNode()).setText(chat.getContent(content));
                chat.removeLine(index - 2);

                chat.update();
            } else {
                // The last message is user
                // Just remove the message from the chat and put it back into the box.
                layout.getPane().getChildren().remove(layoutSize - 1);
                ((TextArea) send.getNode()).setText(chat.getContent(lastLine));
                chat.removeLine(index - 1);
                chat.update();
            }


        });

        TextOverlay impersonate = new TextOverlay(new FontIcon(Material2AL.BRUSH));
        impersonate.setTooltip("Generate your response.");
        topControls.addElement(impersonate);
        impersonate.onClick(event -> {

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

        // First add the user box then generate the ai box and response
        Role role = Role.USER;

        int userIndex = chat.getRawLines().size();
        message = Placeholder.formatPlaceholders(message, character, character.getUser());
        CardContainer userBox = buildChatBox(role, message, chat.getRawLines().size());
        chat.addLine(role, message);
        layout.addElement(userBox); // This doesn't render the user box, but it does add it to the layout for reference.
        layout.getPane().getChildren().add(userBox.build().getKey()); // Force render the new box
        //chat.addLine(role, message);

        int assistantIndex = chat.getRawLines().size();
        CardContainer responseBox = buildChatBox(Role.ASSISTANT, "", assistantIndex); // Set content later

        // Gen response

        Response response = new Response(chat.getLines().size() + 1, message, character, character.getUser(), chat);
        chat.setResponse(response);

        layout.addElement(responseBox);
        layout.getPane().getChildren().add(responseBox.build().getKey());

        generateResponse(response, message, responseBox);
    }

    private void generateResponse(Response response, String message, CardContainer responseBox) {
        Card card = (Card) responseBox.build().getKey();

        Thread thread = new Thread(() -> {
            // Generate response!
            response.setPrompt(message);
            String received;
            try {
                received = Server.generateResponseOAIStream(scrollContainer.getScrollPane(), card, response);
                //TODO: Add stopping box
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
}
