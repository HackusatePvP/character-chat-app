package me.piitex.app.views.characters;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.HomeView;
import me.piitex.app.views.chats.ChatMobileView;
import me.piitex.app.views.chats.ChatView;
import me.piitex.engine.Container;
import me.piitex.engine.PopupPosition;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.DialogueContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.apache.commons.io.FileUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.IOException;

public class CharactersView {
    private final ScrollContainer root;

    private final int imageWidth, imageHeight;
    private final double cardWidth, cardHeight;

    private int spacing = 20;

    private AppSettings appSettings = App.getInstance().getAppSettings();

    public CharactersView() {
        VerticalLayout layout = new VerticalLayout(0, 0);
        layout.setSpacing(20);

        if (App.mobile) {
            root = new ScrollContainer(layout, 20, 20, 400, 0);
            root.setMaxSize(400, 1000);
            imageWidth = 128;
            imageHeight = 128;
            cardWidth = 125;
            cardHeight = 250;
            layout.setSpacing(70);
        } else {
            root = new ScrollContainer(layout, 20, 20, appSettings.getWidth() - 300, 0);
            root.setMaxSize(appSettings.getWidth() - 300, appSettings.getHeight() - 100);
            imageWidth = 256;
            imageHeight = 256;
            cardWidth = 220;
            cardHeight = 380;
        }
        root.setScrollWhenNeeded(false);
        root.setVerticalScroll(true);

        //TODO build boxes
        HorizontalLayout base = new HorizontalLayout(0, 0);
        base.setSpacing(20);
        layout.addElement(base);

        int i = 0;
        double scaleFactor = (double) appSettings.getWidth() / 1920.0;
        int max = (App.mobile ? 3 : (int) Math.round(6 * scaleFactor));
        for (Character character : App.getInstance().getCharacters().values()) {
            if (i == max) {
                // Start a new horizontal row
                base = new HorizontalLayout(0, 0);
                base.setSpacing(20);
                layout.addElement(base);
                i = 0;
            }

            CardContainer card = new CardContainer(0,0, cardWidth, cardHeight);
            card.setMaxSize(cardWidth, cardHeight);

            VerticalLayout displayBox = new VerticalLayout(0, 0);
            displayBox.setX(-10);
            displayBox.setY(-10);

            TextOverlay helper = new TextOverlay("Click to chat");
            helper.setUnderline(true);
            displayBox.addElement(helper);
            displayBox.setAlignment(Pos.BASELINE_CENTER);

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
                deleteCharacter(character);
            });

            contextMenu.getItems().add(edit);
            contextMenu.getItems().add(copy);
            contextMenu.getItems().add(delete);

            displayBox.setClickEvent(event -> {
                if (event.getFxClick().getButton() == MouseButton.PRIMARY) {
                    // Load chat window...
                    App.window.clearContainers();
                    if (App.mobile) {
                        App.window.addContainer(new ChatMobileView(character, character.getLastChat()).getContainer());
                    } else {
                        App.window.addContainer(new ChatView(character, character.getLastChat()).getContainer());
                    }
                    App.window.render();
                }

                if (event.getFxClick().getButton() == MouseButton.SECONDARY) {
                    if (contextMenu.isShowing()) return;
                    contextMenu.show(displayBox.getPane(), Side.BOTTOM, 60, 0);
                }

            });

            ImageOverlay icon = User.getUserAvatar(character.getIconPath(), imageWidth, imageHeight);
            if (icon != null && icon.getImage() != null) {
                displayBox.addElement(icon);
            }
            TextOverlay name = new TextOverlay(character.getId());
            displayBox.addElement(name);

            card.setBody(displayBox);

            card.setFooter(buildControlBox(character));

            base.addElement(card);

            i++;
        }
    }

    public HorizontalLayout buildControlBox(Character character) {
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
        edit.onClick(event -> {
            editCharacter(character);
        });
        root.addElement(edit);

        FontIcon duplicateIcon = new FontIcon(Material2AL.FILE_COPY);
        duplicateIcon.setIconSize(16);
        TextOverlay duplicate = new TextOverlay(duplicateIcon);
        duplicate.setTooltip("Duplicate the character.");
        duplicate.onClick(event -> {
            duplicateCharacter(character);
        });
        root.addElement(duplicate);

        FontIcon deleteIcon = new FontIcon(Material2AL.DELETE_FOREVER);
        TextOverlay delete = new TextOverlay(deleteIcon);

        delete.addStyle(Styles.DANGER);
        delete.setTooltip("Delete the character.");
        delete.onClick(event -> {
            deleteCharacter(character);
        });
        root.addElement(delete);
        return root;
    }

    private void editCharacter(Character character) {
        Container container;
        if (App.mobile) {
            container = new CharacterEditMobileView(character, false).getRoot();
        } else {
            container = new CharacterEditView(character, false).getRoot();
        }
        App.window.clearContainers();
        App.window.addContainer(container);
        App.window.render();

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
        App.window.render();
    }

    private void deleteCharacter(Character character) {
        DialogueContainer dialogueContainer = new DialogueContainer("Delete '" + character.getId() + "'?", 500, 500);

        ButtonOverlay cancel = new ButtonOverlay("cancel", "Keep");
        cancel.setWidth(150);
        cancel.addStyle(Styles.SUCCESS);
        cancel.onClick(event1 -> {
            App.window.removeContainer(dialogueContainer);
        });

        ButtonOverlay confirm = new ButtonOverlay("confirm", "Delete");
        confirm.setWidth(150);
        confirm.addStyle(Styles.DANGER);
        confirm.onClick(event1 -> {
            App.getInstance().getCharacters().remove(character.getId());
            try {
                FileUtils.deleteDirectory(character.getCharacterDirectory());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            App.window.clearContainers();
            App.window.addContainer(new HomeView().getContainer());
            App.window.render();
        });

        dialogueContainer.setCancelButton(cancel);
        dialogueContainer.setConfirmButton(confirm);

        // Render this on top
        App.window.renderPopup(dialogueContainer, PopupPosition.CENTER, 500, 500);
    }

    public ScrollContainer getRoot() {
        return root;
    }
}
