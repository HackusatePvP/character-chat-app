package me.piitex.app.views.characters;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.Chat;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.utils.ImageCardExporter;
import me.piitex.app.views.LoadingView;
import me.piitex.app.views.Positions;
import me.piitex.app.views.chats.ChatView;
import me.piitex.app.views.creator.characters.CharacterCreator;
import me.piitex.engine.containers.*;
import me.piitex.engine.layouts.FlowLayout;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.image.ImageLoader;
import me.piitex.engine.overlays.*;
import org.apache.commons.io.FileUtils;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CharactersView {
    private final ScrollContainer root;

    public CharactersView() {
        AppSettings appSettings = App.getInstance().getAppSettings();

        VerticalLayout layout = new VerticalLayout(-1, -1);
        layout.setMaxSize(appSettings.getWidth() - Positions.SIDEBAR_WIDTH - 25, layout.getHeight());
        layout.setSpacing(20);

        int imageWidth;
        int imageHeight;
        double cardWidth;
        double cardHeight;
        if (App.mobile) {
            root = new ScrollContainer(layout, 0, 0, 400, -1);
            root.setMaxSize(400, 1000);
            imageWidth = 128;
            imageHeight = 128;
            cardWidth = 160;
            cardHeight = 250;
            layout.setSpacing(70);
        } else {
            root = new ScrollContainer(layout, 0, 0, layout.getMaxWidth(), appSettings.getHeight() - 50);
            root.setMaxSize(root.getWidth(), root.getHeight());
            imageWidth = 256;
            imageHeight = 256;
            cardWidth = 280;
            cardHeight = 380;
        }
        root.setScrollWhenNeeded(false);
        root.setHorizontalScroll(false);
        root.setVerticalScroll(true);


        FlowLayout base = new FlowLayout(root.getWidth(), -1);
        base.setVerticalSpacing(20);
        base.setHorizontalSpacing(20);

        layout.addElement(base);
        for (Character character : App.getInstance().getCharacters().values()) {

            CardContainer card = new CardContainer(0,0, cardWidth, cardHeight);
            card.setMaxSize(cardWidth, cardHeight);

            VerticalLayout displayBox = new VerticalLayout(0, 330);
            displayBox.setSpacing(15);
            displayBox.setAlignment(Pos.TOP_CENTER);

            TextOverlay helper = new TextOverlay("Click to chat");
            helper.setUnderline(true);
            displayBox.addElement(helper);

            ContextMenu contextMenu = new ContextMenu();

            MenuItem edit = new MenuItem("Edit");
            edit.setOnAction(_ -> editCharacter(character));
            MenuItem copy = new MenuItem("Copy");
            copy.setOnAction(_ -> duplicateCharacter(character));
            MenuItem delete = new MenuItem("Delete");
            delete.setOnAction(_ -> deleteCharacter(character, base, card));

            contextMenu.getItems().add(edit);
            contextMenu.getItems().add(copy);
            contextMenu.getItems().add(delete);

            displayBox.onClick(event -> {
                App.window.clearContainers();

                if (event.getHandler().getButton() == MouseButton.PRIMARY) {
                    // Display progress
                    Chat chat = character.getLastChat();
                    ChatView cachedView = character.getChatViewCachedNodes().get(chat);
                    if (chat != null && cachedView != null) {
                        Platform.runLater(() -> {
                            App.logger.info("Using cached chat view...");
                            App.window.addContainer(cachedView);
                        });
                    } else {
                        App.logger.info("Loading: {}", (chat == null) ? "New Chat" : chat.getFile().getName());
                        EmptyContainer progressContainer = new EmptyContainer(appSettings.getWidth(), appSettings.getHeight());
                        progressContainer.addElement(new LoadingView("Loading chat...", appSettings.getWidth(), appSettings.getHeight()));
                        App.window.addContainer(progressContainer);

                        App.getThreadPoolManager().submitTask(() -> {
                            ChatView chatView = new ChatView(character, chat);
                            character.getChatViewCachedNodes().put(chat, chatView);
                            Node assemble = chatView.assemble();
                            Platform.runLater(() -> {
                                App.window.addContainer(chatView, assemble);
                            });
                        });
                    }
                }
            });

            ImageOverlay icon = User.getUserAvatar(character.getIconPath(), imageWidth, imageHeight);
            if (icon != null && icon.getImage() != null) {
                icon.setPreserveRatio(false);
                displayBox.addElement(icon);
            }
            TextOverlay name = new TextOverlay(character.getId());
            displayBox.addElement(name);

            card.setBody(displayBox);

            card.setFooter(buildControlBox(base, card, character));

            base.addElement(card);
        }
    }

    public HorizontalLayout buildControlBox(FlowLayout base, CardContainer card, Character character) {
        HorizontalLayout root = new HorizontalLayout(200, 25);
        root.setIndex(10);

        int spacing = 20;
        root.setSpacing(spacing);
        if (!App.mobile) {
            root.setAlignment(Pos.BASELINE_CENTER);
        }

        IconOverlay edit = new IconOverlay(Material2AL.EDIT);
        edit.setTooltip("Edit the character");
        edit.setColor(Color.GREEN);
        edit.onClick(_ -> editCharacter(character));
        root.addElement(edit);

        IconOverlay duplicate = new IconOverlay(Material2AL.FILE_COPY);
        duplicate.setColor(Color.YELLOW);
        duplicate.setTooltip("Duplicate the character.");
        duplicate.onClick(_ -> duplicateCharacter(character));
        root.addElement(duplicate);

        IconOverlay delete = new IconOverlay(Material2AL.DELETE_FOREVER);
        delete.setColor(Color.RED);
        delete.setTooltip("Delete the character.");
        delete.onClick(event -> {
            deleteCharacter(base, card, character, event.getHandler().getSceneX(), event.getHandler().getSceneY());
        });
        root.addElement(delete);

        IconOverlay export = new IconOverlay(Material2AL.CLOUD_DOWNLOAD);
        export.setIconSize(16);
        export.setColor(Color.BLUE);
        export.setTooltip("Export the character.");
        export.onClick(_ -> {
            FileChooser chooser = new FileChooser();
            chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Save character card as.", "*.png"));
            chooser.setInitialFileName(character.getId() + ".png");

            File file = chooser.showSaveDialog(App.window.getStage());
            if (file != null) {
                try {
                    ImageCardExporter.exportCharacter(character, file);
                } catch (IOException e) {
                    App.logger.error("Could not save character card!", e);
                }
            } else {
                App.logger.error("Could not locate character file!");
            }
        });
        root.addElement(export);

        return root;
    }

    private void editCharacter(Character character) {
        App.window.clearContainers();
        EmptyContainer progressContainer = new EmptyContainer(App.getInstance().getAppSettings().getWidth(), App.getInstance().getAppSettings().getHeight());
        progressContainer.addElement(new LoadingView("Loading character data...", progressContainer.getWidth(), progressContainer.getHeight()));
        App.window.addContainer(progressContainer);

        App.getThreadPoolManager().submitTask(() -> {
            CharacterCreator characterCreator = new CharacterCreator(character);
            Node assemble = characterCreator.assemble();
            Platform.runLater(() -> {
                App.window.clearContainers();
                App.window.addContainer(characterCreator, assemble);
            });
        });
    }

    private void duplicateCharacter(Character character) {
        // Duplicate the character.
        String newId = character.getId() + " (copy)";
        while (App.getInstance().getCharacter(newId) != null) {
            newId += " (copy)";
        }

        // Edit the character in the edit view rather than duplicating the files
        // Allow the id to be edited and changed.

        // Create a copy of the character.
        App.window.clearContainers();
        EmptyContainer progressContainer = new EmptyContainer(App.getInstance().getAppSettings().getWidth(), App.getInstance().getAppSettings().getHeight());
        progressContainer.addElement(new LoadingView("Loading character data...", progressContainer.getWidth(), progressContainer.getHeight()));
        App.window.addContainer(progressContainer);

        App.getThreadPoolManager().submitTask(() -> {
            CharacterCreator characterCreator = new CharacterCreator(character, character.getUser());
            Node assemble = characterCreator.assemble();
            Platform.runLater(() -> {
                App.window.clearContainers();
                App.window.addContainer(characterCreator, assemble);
            });
        });
    }

    private void deleteCharacter(FlowLayout base, CardContainer card, Character character, double x, double y) {
        DialogueContainer dialogueContainer = new DialogueContainer("Delete '" + character.getId() + "'?", 500, 500);

        ButtonOverlay cancel = new ButtonBuilder("cancel").setText("Keep").build();
        cancel.setWidth(150);
        cancel.addStyle(Styles.SUCCESS);
        cancel.onClick(_ -> App.window.removeContainer(dialogueContainer));

        ButtonOverlay confirm = new ButtonBuilder("confirm").setText("Delete").build();
        confirm.setWidth(150);
        confirm.addStyle(Styles.DANGER);
        confirm.onClick(_ -> {
            App.getInstance().getCharacters().remove(character.getId());
            App.window.removeContainer(dialogueContainer);

            // Cleanup image usage
            VerticalLayout verticalLayout = (VerticalLayout) card.getBody();
            ImageOverlay imageOverlay = (ImageOverlay) verticalLayout.getElementAt(1);

            // When setting to null the engine will dispose of the image and the JVM will call gc.
            imageOverlay.setImage(null);

            base.removeElement(card);

            // Add a buffer to ensure image resources are disposed.
            App.getThreadPoolManager().submitSchedule(() -> {
                try {
                    App.logger.info("Removing image from cache '{}'", character.getIconPath());
                    ImageLoader.clearCache();
                    App.logger.info("Deleting Character: {}", character.getId());
                    FileUtils.deleteDirectory(character.getCharacterDirectory());
                } catch (IOException e) {
                    App.logger.error("Could not delete directory!", e);
                }
            }, 1, TimeUnit.SECONDS);
        });

        dialogueContainer.setCancelButton(cancel);
        dialogueContainer.setConfirmButton(confirm);

        // Render this on top
        App.window.renderPopup(dialogueContainer, x, y, 500, 500);
    }

    private void deleteCharacter(Character character, FlowLayout base, CardContainer card) {
        App.getInstance().getCharacters().remove(character.getId());

        App.getThreadPoolManager().submitSchedule(() -> {
            try {
                App.logger.info("Deleting: {}", character.getId());
                FileUtils.deleteDirectory(character.getCharacterDirectory());
            } catch (IOException e) {
                App.logger.error("Could not delete directory!", e);
            }
        }, 1, TimeUnit.SECONDS);

        base.removeElement(card);
    }

    public ScrollContainer getRoot() {
        return root;
    }
}
