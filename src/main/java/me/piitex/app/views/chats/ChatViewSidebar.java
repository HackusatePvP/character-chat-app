package me.piitex.app.views.chats;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.Chat;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.HomeView;
import me.piitex.app.views.LoadingView;
import me.piitex.engine.containers.BorderContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.StackContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.Layout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static me.piitex.app.views.Positions.SIDEBAR_HEIGHT;

public class ChatViewSidebar extends EmptyContainer {
    private final ChatView parent;
    private final VerticalLayout root;

    private static final AppSettings APP_SETTINGS = App.getInstance().getAppSettings();

    public ChatViewSidebar(ChatView chatView) {
        super(200, SIDEBAR_HEIGHT);
        setMaxSize(getWidth(), getHeight());
        addStyle(Styles.BG_INSET);
        addStyle(Styles.BORDER_DEFAULT);
        this.parent = chatView;

        root = new VerticalLayout(getWidth(), SIDEBAR_HEIGHT);
        root.setMaxSize(root.getWidth(), root.getHeight());
        root.setAlignment(Pos.TOP_CENTER);
        addElement(root);

        init();
    }

    private void init() {
        BorderContainer container = new BorderContainer(getWidth(), SIDEBAR_HEIGHT);
        container.setMaxSize(container.getWidth(), container.getHeight());
        root.addElement(container);

        container.setTop(buildTopControls());
        container.setCenter(buildMiddleControls());
    }

    private Layout buildTopControls() {
        VerticalLayout layout = new VerticalLayout(getWidth(), 50);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());

        IconOverlay homeIcon = new IconOverlay(Material2AL.HOME);
        ButtonOverlay home = new ButtonBuilder("home").setText("Home").setIcon(homeIcon).addStyle(Styles.FLAT).build();
        layout.addElement(home);
        home.onClick(_ -> {
            App.window.clearContainers();
            App.window.addContainer(new HomeView());
        });

        return layout;
    }

    private VerticalLayout buildMiddleControls() {
        VerticalLayout layout = new VerticalLayout(getWidth(), 200);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());
        layout.addElement(buildCurrentChatSelection(layout));
        return layout;
    }

    private StackContainer buildCurrentChatSelection(Layout layout) {
        StackContainer container = new StackContainer(layout.getWidth(), 50);
        container.setMaxSize(container.getWidth(), container.getHeight());

        TextFieldOverlay currentChat = new TextFieldOverlay((parent.getChat() != null ? parent.getChatName() : ""), "", layout.getWidth(), container.getHeight());
        container.setMaxSize(container.getWidth(), currentChat.getHeight());
        container.addElement(currentChat);

        IconOverlay downloadIcon = new IconOverlay(Material2AL.CLOUD_DOWNLOAD);
        downloadIcon.setColor(Color.LIGHTBLUE);
        ButtonOverlay downloadChat = new ButtonBuilder("download").addStyle(Styles.FLAT).setIcon(downloadIcon).build();
        downloadChat.setTooltip("Download Chat");

        IconOverlay importIcon = new IconOverlay(Material2AL.IMPORT_EXPORT);
        importIcon.setColor(Color.LIGHTYELLOW);
        ButtonOverlay importChat = new ButtonBuilder("import").addStyle(Styles.FLAT).setIcon(importIcon).build();
        importChat.setTooltip("Import Chat");

        IconOverlay renameIcon = new IconOverlay(Material2AL.EDIT);
        renameIcon.setColor(Color.LIGHTGREEN);
        ButtonOverlay renameChat = new ButtonBuilder("rename").addStyle(Styles.FLAT).setIcon(renameIcon).build();
        renameChat.setTooltip("Rename Chat");
        renameIcon.onClick(_ -> {
            Character character = parent.getCharacter();
            Chat chat = parent.getChat();

            String name = (currentChat.getCurrentText().endsWith(".bin") ? currentChat.getCurrentText() : currentChat.getCurrentText() + ".bin");

            if (name.equalsIgnoreCase(chat.getFile().getName())) {
                currentChat.getTextField().requestFocus();
                return;
            }

            // Rename the chat and reload the character.
            File newChatFile = new File(character.getChatDirectory(), name);
            try {
                Files.copy(chat.getFile().toPath(), newChatFile.toPath());

                // Release chat allocations
                character.getChatViewCachedNodes().remove(chat);
                character.getChats().remove(chat);
                Files.delete(chat.getFile().toPath());

                // Re-create chat with new file
                Chat newChat = new Chat(newChatFile);
                character.getChats().add(newChat);
                character.setLastChat(newChat);

                App.window.clearContainers();
                App.window.addContainer(new ChatView(character, null));
            } catch (IOException e) {
                App.logger.error("Could not rename chat!", e);
            }
        });

        HorizontalLayout buttonLayout = new HorizontalLayout(layout.getWidth(), container.getHeight());
        buttonLayout.setAlignment(Pos.BOTTOM_RIGHT);
        buttonLayout.setMaxSize(buttonLayout.getWidth(), buttonLayout.getHeight());
        buttonLayout.addElements(downloadChat, importChat, renameChat);
        container.addElement(buttonLayout);

        List<String> chats = new ArrayList<>();
        for (Chat chat : parent.getCharacter().getChats()) {
            System.out.println("Adding " + chat.getFile().getName() + " to selection");
            chats.add(chat.getFile().getName());
        }

        ChoiceBoxOverlay chatSelection = new ChoiceBoxOverlay(chats);
        chatSelection.setWidth(layout.getWidth());
        chatSelection.setDefaultItem(parent.getChat().getFile().getName());
        layout.addElement(chatSelection);

        chatSelection.onItemSelect(event -> {
            Chat chat = parent.getCharacter().getChat(event.getNewValue());
            if (chat != null) {
                App.window.clearContainers();
                EmptyContainer progressContainer = new EmptyContainer(APP_SETTINGS.getWidth(), APP_SETTINGS.getHeight());
                progressContainer.addElement(new LoadingView("Loading chat...", progressContainer.getWidth(), progressContainer.getHeight()));
                App.window.addContainer(progressContainer);
            }

            App.getThreadPoolManager().submitTask(() -> {
                ChatView chatView = new ChatView(parent.getCharacter(), chat);
                Node assemble = chatView.assemble();
                Platform.runLater(() -> {
                    App.window.clearContainers();
                    App.window.addContainer(chatView, assemble);
                });
            });
        });

        return container;
    }

}
