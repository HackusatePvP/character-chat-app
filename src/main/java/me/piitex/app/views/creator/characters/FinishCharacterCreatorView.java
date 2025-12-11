package me.piitex.app.views.creator.characters;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.User;
import me.piitex.app.views.HomeView;
import me.piitex.engine.Element;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import me.piitex.os.configurations.InfoFile;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.TreeMap;

public class FinishCharacterCreatorView extends EmptyContainer {
    private final VerticalLayout root;
    private final InfoFile infoFile;
    private final CharacterCreator parent;


    public FinishCharacterCreatorView(CharacterCreator parent, InfoFile infoFile, double width, double height) {
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

        ScrollContainer scrollContainer = new ScrollContainer(root, width, height);
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

        root.addElement(buildDisplayBox());
        ButtonOverlay submit = new ButtonBuilder("submit").setGraphic(buildFinishButton(400)).addStyle(Styles.SUCCESS).addStyle(Styles.BUTTON_OUTLINED).build();
        root.addElement(submit);
        submit.onClick(event -> {
            System.out.println("Character: " + parent.getCharacter());

            if (!validate()) {
                return;
            }
            
            Character character;
            if (parent.getCharacter() != null) {
                // Character is being updated.
                character = parent.getCharacter();
            } else {
                character = new Character(infoFile.get("id"));
            }
            character.setDisplayName(infoFile.get("display-name"));
            character.setPersona(infoFile.getOrDefault("persona", ""));

            if (!infoFile.hasKey("icon-path")) {
                infoFile.set("icon-path", new File(App.getExecutedDirectory(), "icons/character.png").getAbsolutePath());
            }
            if (!infoFile.hasKey("user-icon-path")) {
                infoFile.set("user-icon-path", new File(App.getExecutedDirectory(), "icons/character.png").getAbsolutePath());
            }

            File characterImage = new File(character.getCharacterDirectory(), "character.png");
            File newCharacterImage = new File(infoFile.get("icon-path"));
            try {
                Files.copy(newCharacterImage.toPath(), characterImage.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                App.logger.error("Could not copy new character image!", e);
            }
            character.setIconPath(characterImage.getAbsolutePath());
            character.setLorebook(compileCharacterLore());

            User user = new User(infoFile.get("user-display-name"), new InfoFile(new File(character.getUserDirectory(), "user.info"), true));
            user.setDisplayName(infoFile.get("user-display-name"));
            user.setPersona(infoFile.getOrDefault("user-persona", ""));

            File userImage = new File(character.getCharacterDirectory(), "user/user.png");
            File newUserImage = new File(infoFile.get("user-icon-path"));
            try {
                Files.copy(newUserImage.toPath(), userImage.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                App.logger.error("Could not copy new user image!", e);
            }
            user.setIconPath(userImage.getAbsolutePath());
            user.setLorebook(compileUserLore());
            character.setUser(user);

            character.setChatScenario(infoFile.getOrDefault("chat-scenario", ""));
            character.setFirstMessage(infoFile.getOrDefault("first-message", ""));

            App.getInstance().getCharacters().putIfAbsent(character.getId(), character);

            App.window.clearContainers();
            App.window.addContainer(new HomeView());
        });
    }

    private TreeMap<String, String> compileCharacterLore() {
        TreeMap<String, String> toReturn = new TreeMap<>();

        for (Element element : parent.getCharacterCustomizationView().getLoreLayout().getElements().values()) {
            // All entires are vertical layouts
            VerticalLayout root = (VerticalLayout) element;

            // The key is in the first horizontal layout at the first index.
            HorizontalLayout horizontalLayout = (HorizontalLayout) root.getElements().firstEntry().getValue();

            TextFieldOverlay loreKey = (TextFieldOverlay) horizontalLayout.getElements().firstEntry().getValue();
            String key = loreKey.getCurrentText();
            if (key == null || key.isEmpty()) {
                continue;
            }

            // The value is the second index of the root
            RichTextAreaOverlay loreValue = (RichTextAreaOverlay) root.getElements().lastEntry().getValue();
            String value = loreValue.getCurrentText();
            if (value == null || value.isEmpty()) {
                continue;
            }

            toReturn.put(key, value);
        }

        return toReturn;
    }

    private TreeMap<String, String> compileUserLore() {
        TreeMap<String, String> toReturn = new TreeMap<>();

        for (Element element : parent.getUserCustomizationView().getLoreLayout().getElements().values()) {
            // All entires are vertical layouts
            VerticalLayout root = (VerticalLayout) element;

            // The key is in the first horizontal layout at the first index.
            HorizontalLayout horizontalLayout = (HorizontalLayout) root.getElements().firstEntry().getValue();

            TextFieldOverlay loreKey = (TextFieldOverlay) horizontalLayout.getElements().firstEntry().getValue();
            String key = loreKey.getCurrentText();
            if (key == null || key.isEmpty()) {
                continue;
            }

            // The value is the second index of the root
            RichTextAreaOverlay loreValue = (RichTextAreaOverlay) root.getElements().lastEntry().getValue();
            String value = loreValue.getCurrentText();
            if (value == null || value.isEmpty()) {
                continue;
            }

            toReturn.put(key, value);
        }

        return toReturn;
    }

    private HorizontalLayout buildDisplayBox() {
        HorizontalLayout layout = new HorizontalLayout(527, 360);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());
        layout.addStyle(Styles.BORDER_DEFAULT);
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(20);

        layout.addElement(buildCharacterImage());
        layout.addElement(buildCharacterInputs());

        return layout;
    }

    private VerticalLayout buildCharacterImage() {
        VerticalLayout layout = new VerticalLayout(256, -1);
        layout.addStyle(Styles.BORDER_DEFAULT);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());
        layout.setAlignment(Pos.CENTER);

        ImageOverlay imageOverlay;
        String path;
        if (infoFile.hasKey("icon-path")) {
            path = infoFile.get("icon-path");
            imageOverlay = User.getUserAvatar(path, 256, 256);
        } else {
            path = "icons/character.png";
            imageOverlay = User.getUserAvatar("", 256, 256);
        }
        layout.addElement(imageOverlay);

        TextOverlay textOverlay = new TextOverlay(path);
        textOverlay.addStyle(Styles.TEXT_SMALL);
        layout.addElement(textOverlay);


        return layout;
    }

    private VerticalLayout buildCharacterInputs() {
        VerticalLayout layout = new VerticalLayout(256, -1);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(20);

        TextFieldOverlay idField = new TextFieldOverlay(infoFile.get("id"), "");
        idField.setMaxSize(250, 50);
        idField.setEditable(false);
        layout.addElement(idField);

        TextFieldOverlay displayField = new TextFieldOverlay(infoFile.get("display-name"), "");
        displayField.setMaxSize(250, 50);
        displayField.setEditable(false);
        layout.addElement(displayField);

        return layout;
    }

    private HorizontalLayout buildFinishButton(double width) {
        HorizontalLayout root = new HorizontalLayout(width, 0);
        root.setSpacing(20);

        IconOverlay icon = new IconOverlay(Material2AL.CHECK_CIRCLE_OUTLINE);
        icon.setColor(Color.rgb(50, 50, 50,0.5));
        icon.setIconSize(26);
        root.addElement(icon);

        TextOverlay textOverlay = new TextOverlay("Finalize");
        textOverlay.addStyle(Styles.TITLE_4);
        root.addElement(textOverlay);

        root.getPane().addEventHandler(MouseEvent.MOUSE_ENTERED, mouseEvent -> {
            textOverlay.setText("Submit Character Changes");
        });

        root.getPane().addEventHandler(MouseEvent.MOUSE_EXITED, mouseEvent -> {
            textOverlay.setText("Finalize");
        });

        return root;
    }

    private boolean validate() {
        if (parent.getCharacter() == null && (parent.getCharacterCustomizationView().getCharacterIdInput() == null || parent.getCharacterCustomizationView().getCharacterIdInput().getCurrentText().isEmpty())) {
            // Display and select the field with a popover
            parent.getDisplayContent().removeElement(parent.getDisplayContent().getElements().lastKey());
            parent.getDisplayContent().addElement(parent.getCharacterCustomizationView());
            parent.setCurrentView(parent.getCharacterCustomizationView());

            // Popover
            parent.getCharacterCustomizationView().getCharacterIdInput().getNode().requestFocus();
            Popover popover = new Popover(new Text("Character id must be provided."));
            popover.setAutoHide(true);
            popover.show(parent.getCharacterCustomizationView().getCharacterIdInput().getNode());

            return false;
        }
        if (parent.getCharacter() == null && App.getInstance().getCharacter(parent.getCharacterCustomizationView().getCharacterIdInput().getCurrentText()) != null) {
            parent.getDisplayContent().removeElement(parent.getDisplayContent().getElements().lastKey());
            parent.getDisplayContent().addElement(parent.getCharacterCustomizationView());
            parent.setCurrentView(parent.getCharacterCustomizationView());

            // Popover
            parent.getCharacterCustomizationView().getCharacterIdInput().getNode().requestFocus();
            Popover popover = new Popover(new Text("A character with this id already exists."));
            popover.setAutoHide(true);
            popover.show(parent.getCharacterCustomizationView().getCharacterIdInput().getNode());

            return false;
        }
        if (parent.getCharacterCustomizationView().getCharacterDisplayInput() == null || parent.getCharacterCustomizationView().getCharacterDisplayInput().getCurrentText().isEmpty()) {
            // Display and select the field with a popover
            parent.getDisplayContent().removeElement(parent.getDisplayContent().getElements().lastKey());
            parent.getDisplayContent().addElement(parent.getCharacterCustomizationView());
            parent.setCurrentView(parent.getCharacterCustomizationView());

            // Popover
            parent.getCharacterCustomizationView().getCharacterDisplayInput().getNode().requestFocus();
            Popover popover = new Popover(new Text("Character display must be provided."));
            popover.setAutoHide(true);
            popover.show(parent.getCharacterCustomizationView().getCharacterDisplayInput().getNode());
            return false;
        }

        if (parent.getUserCustomizationView().getUserDisplayInput() == null || parent.getUserCustomizationView().getUserDisplayInput().getCurrentText().isEmpty()) {
            // Display and select the field with a popover
            parent.getDisplayContent().removeElement(parent.getDisplayContent().getElements().lastKey());
            parent.getDisplayContent().addElement(parent.getUserCustomizationView());
            parent.setCurrentView(parent.getUserCustomizationView());

            // Popover
            parent.getUserCustomizationView().getUserDisplayInput().getNode().requestFocus();
            Popover popover = new Popover(new Text("User display must be provided."));
            popover.setAutoHide(true);
            popover.show(parent.getUserCustomizationView().getUserDisplayInput().getNode());
            return false;
        }

        return true;
    }


}