package me.piitex.app.views.creator.characters;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import me.piitex.app.App;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.*;
import me.piitex.os.configurations.InfoFile;

import java.io.File;

public class CharacterCustomizationView extends EmptyContainer {
    private final VerticalLayout root;
    private final InfoFile infoFile;

    public CharacterCustomizationView(InfoFile infoFile, int width, int height) {
        super(width, height);
        this.infoFile = infoFile;
        root = new VerticalLayout(width, height);
        root.setMaxSize(width, height);
        root.addStyle(Styles.BORDER_DEFAULT);
        root.setAlignment(Pos.TOP_CENTER);
        addElement(root);
        init();
    }

    private void init() {

        if (infoFile == null) {
            App.logger.error("Could not initialize character data!", new RuntimeException());
        }

        root.addElement(buildCharacterImageBox());
        root.addElement(buildCharacterInputs());

    }

    private VerticalLayout buildCharacterImageBox() {
        VerticalLayout imageBox = new VerticalLayout(256, 320);
        imageBox.setSpacing(5);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.addStyle(Styles.BORDER_SUBTLE);
        imageBox.setMaxSize(imageBox.getWidth(), imageBox.getHeight());

        VerticalLayout wrapperBox = new VerticalLayout(256, 256);
        wrapperBox.setMaxSize(wrapperBox.getWidth(), wrapperBox.getHeight());
        imageBox.setAlignment(Pos.CENTER);
        imageBox.addElement(wrapperBox);

        ImageLoader imageLoader;
        if (infoFile.get("icon-path") != null) {
            imageLoader = new ImageLoader(new File(infoFile.get("icon-path")));
        } else {
            imageLoader = new ImageLoader(new File(App.getExecutedDirectory(), "icons/character.png"));
        }
        imageLoader.setHeight(256);
        imageLoader.setWidth(256);
        ImageOverlay imageOverlay = new ImageOverlay(imageLoader);
        imageOverlay.setFitWidth(256);
        imageOverlay.setFitHeight(256);
        wrapperBox.addElement(imageOverlay);

        wrapperBox.setClickEvent(_ -> {
            System.out.println("Clicking image...");
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Select character image.", "*.png", "*.jpg"));

            File file = fileChooser.showOpenDialog(App.window.getStage());
            if (file != null && file.exists()) {
                infoFile.set("icon-path", file.getAbsolutePath());
                ImageLoader newImage = new ImageLoader(file);
                newImage.setWidth(256);
                newImage.setHeight(256);
                imageOverlay.setImage(newImage);
            }
        });

        ButtonOverlay reset = new ButtonBuilder("rst").setText("Reset Image").addStyle(Styles.FLAT).build();
        reset.onClick(_ -> {
            imageOverlay.setImage(new ImageLoader(new File(App.getExecutedDirectory(), "icons/character.png")));
        });
        imageBox.addElement(reset);

        TextOverlay overlay = new TextOverlay("Images should be sized to 256x256");
        overlay.addStyle(Styles.TEXT_SMALL);
        imageBox.addElement(overlay);

        return imageBox;
    }

    private VerticalLayout buildCharacterInputs() {
        VerticalLayout inputBox = new VerticalLayout(getWidth(), 0);
        inputBox.setAlignment(Pos.CENTER);

        TextFieldOverlay characterInputId = new TextFieldOverlay((infoFile.hasKey("id") ? infoFile.get("id") : ""), "Character Id", 0, 0, 200, 35);
        characterInputId.setMaxSize(characterInputId.getWidth(), characterInputId.getHeight());
        inputBox.addElement(characterInputId);
        characterInputId.onInputSetEvent(event -> {
            infoFile.set("id", event.getInput());
        });

        return inputBox;
    }

}