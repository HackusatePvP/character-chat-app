package me.piitex.app.views.creator.characters;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import me.piitex.os.configurations.InfoFile;
import java.io.File;
import java.util.List;

public class ChatCustomizationView extends EmptyContainer  {
    private final VerticalLayout root;
    private final InfoFile infoFile;
    private final CharacterCreator parent;


    public ChatCustomizationView(CharacterCreator parent, InfoFile infoFile, double width, double height) {
        super(width, height);
        addStyle(Styles.BG_DEFAULT);
        this.parent = parent;
        this.infoFile = infoFile;
        root = new VerticalLayout(width, height);
        root.setMaxSize(width, -1);
        root.addStyle(Styles.BORDER_DEFAULT);
        root.setAlignment(Pos.CENTER);
        root.setSpacing(50);
        addProperties("progress", "User");

        ScrollContainer scrollContainer = new ScrollContainer(root, width - 15, height - 40);
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setScrollWhenNeeded(false);
        scrollContainer.setMaxSize(width, height);
        addElement(scrollContainer);

        init();
    }

    private void init() {

        if (infoFile == null) {
            App.logger.error("Could not initialize chat data!", new RuntimeException());
        }

        root.addElement(buildChatSettings());
        root.addElement(buildChatImporting());
    }

    private VerticalLayout buildChatSettings() {
        VerticalLayout layout = new VerticalLayout(720, 400);
        layout.addStyle(Styles.BORDER_DEFAULT);
        layout.setSpacing(50);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());
        layout.setAlignment(Pos.TOP_CENTER);

        TextOverlay disclaimer = new TextOverlay("These fields are optional.");
        disclaimer.addStyle(Styles.TEXT_ITALIC);
        disclaimer.setY(5); // Offset y position.
        layout.addElement(disclaimer);

        layout.addElement(buildChatScenario());
        layout.addElement(buildAssistantFirstMessage());

        return layout;
    }

    private VerticalLayout buildChatScenario() {
        VerticalLayout layout = new VerticalLayout(720, 0);
        layout.setMaxSize(layout.getWidth(), -1);
        layout.setSpacing(5);
        layout.setAlignment(Pos.CENTER);

        VerticalLayout wrapper = new VerticalLayout(650, 0);
        wrapper.setMaxSize(wrapper.getWidth(), wrapper.getHeight());
        wrapper.setSpacing(5);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        layout.addElement(wrapper);

        TextOverlay header = new TextOverlay("Chat Scenario");
        header.addStyle(Styles.TEXT_BOLDER);
        wrapper.addElement(header);

        TextOverlay description = new TextOverlay("Summarization of the current scenario.");
        description.addStyle(Styles.TEXT_LIGHTER);
        wrapper.addElement(description);

        RichTextAreaOverlay chatScenarioInput = new RichTextAreaOverlay(infoFile.get("chat-scenario"), 650, -1);
        chatScenarioInput.setMaxSize(chatScenarioInput.getWidth(), chatScenarioInput.getHeight());
        chatScenarioInput.setBackgroundColor(App.getInstance().getAppSettings().getThemeDefaultColor(App.getInstance().getAppSettings().getTheme()));
        chatScenarioInput.setBorderColor(App.getInstance().getAppSettings().getThemeBorderColor(App.getInstance().getAppSettings().getTheme()));
        chatScenarioInput.setTextFill(App.getInstance().getAppSettings().getThemeTextColor(App.getInstance().getAppSettings().getTheme()));
        layout.addElement(chatScenarioInput);

        chatScenarioInput.onInputSetEvent(event -> {
            infoFile.set("chat-scenario", event.getInput());
        });

        return layout;
    }

    private VerticalLayout buildAssistantFirstMessage() {
        VerticalLayout layout = new VerticalLayout(720, 0);
        layout.setMaxSize(layout.getWidth(), -1);
        layout.setSpacing(5);
        layout.setAlignment(Pos.CENTER);

        VerticalLayout wrapper = new VerticalLayout(650, 0);
        wrapper.setMaxSize(wrapper.getWidth(), wrapper.getHeight());
        wrapper.setSpacing(5);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        layout.addElement(wrapper);

        TextOverlay header = new TextOverlay("First Message");
        header.addStyle(Styles.TEXT_BOLDER);
        wrapper.addElement(header);

        TextOverlay description = new TextOverlay("Configure the assistant to start the chat.");
        description.addStyle(Styles.TEXT_LIGHTER);
        wrapper.addElement(description);

        RichTextAreaOverlay chatScenarioInput = new RichTextAreaOverlay(infoFile.get("chat-first-message"), 650, -1);
        chatScenarioInput.setMaxSize(chatScenarioInput.getWidth(), chatScenarioInput.getHeight());
        chatScenarioInput.setBackgroundColor(App.getInstance().getAppSettings().getThemeDefaultColor(App.getInstance().getAppSettings().getTheme()));
        chatScenarioInput.setBorderColor(App.getInstance().getAppSettings().getThemeBorderColor(App.getInstance().getAppSettings().getTheme()));
        chatScenarioInput.setTextFill(App.getInstance().getAppSettings().getThemeTextColor(App.getInstance().getAppSettings().getTheme()));
        layout.addElement(chatScenarioInput);

        chatScenarioInput.onInputSetEvent(event -> {
            infoFile.set("chat-first-message", event.getInput());
        });

        return layout;
    }

    private VerticalLayout buildChatImporting() {
        VerticalLayout layout = new VerticalLayout(720, VBox.USE_COMPUTED_SIZE);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());
        layout.setAlignment(Pos.TOP_CENTER);

        TextOverlay header = new TextOverlay("Import Chat Data");
        layout.addElement(header);

        TextOverlay description = new TextOverlay("Import existing chat files for the character.");
        layout.addElement(description);

        ButtonOverlay importer = new ButtonBuilder("imp").setText("Import Chats").build();
        layout.addElement(importer);

        importer.onClick(_ -> {
            FileChooser chooser = new FileChooser();
            chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Select chat files.", "*.*"));

            List<File> files = chooser.showOpenMultipleDialog(App.window.getStage());
            if (files == null) return;

            for (File file : files) {
                if (file.exists() && file.isFile()) {
                    // Add files to be imported upon creation.
                    parent.getImportedChatFiles().add(file);
                }
            }

        });

        return layout;
    }


}