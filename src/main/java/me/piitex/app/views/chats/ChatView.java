package me.piitex.app.views.chats;

import org.jetbrains.annotations.Nullable;
import atlantafx.base.theme.Styles;
import com.drew.lang.annotations.NotNull;

import me.piitex.app.App;
import me.piitex.app.backend.Chat;
import me.piitex.app.backend.Character;
import me.piitex.app.configuration.AppSettings;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.layouts.HorizontalLayout;

import java.io.File;

public class ChatView extends EmptyContainer {
    private final Character character;
    private final Chat chat;
    private HorizontalLayout layout;

    private static final AppSettings APP_SETTINGS = App.getInstance().getAppSettings();

    public ChatView(@NotNull Character character, @Nullable Chat chat) {
        super(APP_SETTINGS.getWidth(), APP_SETTINGS.getHeight());
        this.character = character;
        if (chat == null) {
            // Create a new chat
            chat = new Chat(new File(character.getChatDirectory(), "untitled-" + character.getChatDirectory().listFiles().length + ".bin"));
        }
        this.chat = chat;
        init();
    }
    
    public void init() {
        layout = new HorizontalLayout(APP_SETTINGS.getWidth() - 100, APP_SETTINGS.getHeight());
        layout.setMaxSize(layout.getWidth(), layout.getHeight());
        layout.setSpacing(5);
        layout.addStyle(Styles.BG_INSET);
        layout.addElement(new ChatViewSidebar(this));
        layout.addElement(new ChatPageView(this, layout.getWidth() - layout.getSpacing() - 100, layout.getHeight()));
        addElement(layout);
    }

    public HorizontalLayout getLayout() {
        return layout;
    }

    public Character getCharacter() {
        return character;
    }

    public Chat getChat() {
        return chat;
    }

    public String getChatName() {
        return chat.getFile().getName().replace(".bin", "");
    }
}
