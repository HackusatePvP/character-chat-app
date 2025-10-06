package me.piitex.app.views.characters;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.HomeView;
import me.piitex.app.views.LoadingView;
import me.piitex.app.views.chats.ChatView;
import me.piitex.engine.Container;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.DialogueContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.FlowLayout;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.apache.commons.io.FileUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CharactersView {
    private final ScrollContainer root;

    private final int imageWidth, imageHeight;
    private final double cardWidth, cardHeight;

    private final int spacing = 20;

    public CharactersView() {
        VerticalLayout layout = new VerticalLayout(0, -1);
        AppSettings appSettings = App.getInstance().getAppSettings();
        layout.setMaxSize(appSettings.getWidth() - 400, 0);
        layout.setSpacing(20);

        if (App.mobile) {
            root = new ScrollContainer(layout, 0, 0, 400, -1);
            root.setMaxSize(400, 1000);
            imageWidth = 128;
            imageHeight = 128;
            cardWidth = 160;
            cardHeight = 250;
            layout.setSpacing(70);
        } else {
            root = new ScrollContainer(layout, 10, 10, appSettings.getWidth() - 310, -1);
            root.setMaxSize(appSettings.getWidth() - 300, appSettings.getHeight() - 100);
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
        base.addStyle(Styles.BORDER_DEFAULT);

        layout.addElement(base);
        for (Character character : App.getInstance().getCharacters().values()) {

            CardContainer card = new CardContainer(0,0, cardWidth, cardHeight);
            card.setMaxSize(cardWidth, cardHeight);

            VerticalLayout displayBox = new VerticalLayout(0, 0);
            displayBox.setAlignment(Pos.BASELINE_CENTER);

            TextOverlay helper = new TextOverlay("Click to chat");
            helper.setUnderline(true);
            displayBox.addElement(helper);

            ContextMenu contextMenu = new ContextMenu();

            MenuItem edit = new MenuItem("Edit");
            edit.setOnAction(event -> {
                editCharacter(character);
            });
            MenuItem copy = new MenuItem("Copy");
            copy.setOnAction(event -> {
                duplicateCharacter(character);
            });
            MenuItem delete = new MenuItem("Delete");
            delete.setOnAction(event -> {
                deleteCharacter(character, base, card);
            });

            contextMenu.getItems().add(edit);
            contextMenu.getItems().add(copy);
            contextMenu.getItems().add(delete);

            displayBox.setClickEvent(event -> {
                if (event.getFxClick().getButton() == MouseButton.SECONDARY) {
                    if (contextMenu.isShowing()) return;
                    contextMenu.show(displayBox.getPane(), Side.BOTTOM, 60, 0);
                }

                if (event.getFxClick().getButton() == MouseButton.PRIMARY) {
                    // Display progress
                    App.window.clearContainers();

                    EmptyContainer progressContainer = new EmptyContainer(appSettings.getWidth(), appSettings.getHeight());
                    progressContainer.addElement(new LoadingView("Loading chat...", appSettings.getWidth(), appSettings.getHeight()));
                    App.window.addContainer(progressContainer);

                    App.getThreadPoolManager().submitTask(() -> {
                        ChatView chatView = new ChatView(character, character.getLastChat());

                        Platform.runLater(() -> {
                            Node assemble = chatView.assemble();
                            App.window.clearContainers();
                            App.window.addContainer(chatView, assemble);
                        });

                    });

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
        root.setSpacing(spacing);
        if (!App.mobile) {
            root.setAlignment(Pos.BASELINE_CENTER);
        }

        FontIcon editIcon = new FontIcon(Material2AL.EDIT);
        editIcon.setIconSize(16);
        TextOverlay edit = new TextOverlay(editIcon);
        edit.setTooltip("Edit the character");
        edit.addStyle(Styles.ACCENT);
        edit.onClick(event -> {
            editCharacter(character);
        });
        root.addElement(edit);

        FontIcon duplicateIcon = new FontIcon(Material2AL.FILE_COPY);
        duplicateIcon.setIconSize(16);
        TextOverlay duplicate = new TextOverlay(duplicateIcon);
        duplicate.setTooltip("Duplicate the character.");
        duplicate.addStyle(Styles.WARNING);
        duplicate.onClick(event -> {
            duplicateCharacter(character);
        });
        root.addElement(duplicate);

        FontIcon deleteIcon = new FontIcon(Material2AL.DELETE_FOREVER);
        TextOverlay delete = new TextOverlay(deleteIcon);

        delete.addStyle(Styles.DANGER);
        delete.setTooltip("Delete the character.");
        delete.onClick(event -> {
            deleteCharacter(base, card, character, event.getHandler().getSceneX(), event.getHandler().getSceneY());
        });
        root.addElement(delete);
        return root;
    }

    private void editCharacter(Character character) {
        App.window.getStage().getScene().setCursor(Cursor.WAIT);

        Container container = new CharacterEditView(character, false).getRoot();

        Platform.runLater(() -> {
            App.window.clearContainers();
            App.window.addContainer(container);
            App.window.getStage().getScene().setCursor(Cursor.DEFAULT);
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
        Character duplicated = new Character(newId, null);
        duplicated.copy(character);

        CharacterEditView editView = new CharacterEditView(duplicated, true);
        App.window.clearContainers();
        App.window.addContainer(editView.getRoot());
    }

    private void deleteCharacter(FlowLayout base, CardContainer card, Character character, double x, double y) {
        DialogueContainer dialogueContainer = new DialogueContainer("Delete '" + character.getId() + "'?", 500, 500);

        ButtonOverlay cancel = new ButtonBuilder("cancel").setText("Keep").build();
        cancel.setWidth(150);
        cancel.addStyle(Styles.SUCCESS);
        cancel.onClick(event1 -> {
            App.window.removeContainer(dialogueContainer);
        });

        ButtonOverlay confirm = new ButtonBuilder("confirm").setText("Delete").build();
        confirm.setWidth(150);
        confirm.addStyle(Styles.DANGER);
        confirm.onClick(event1 -> {
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
                    App.logger.info("Deleting: {}", character.getId());
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
