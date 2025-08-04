package me.piitex.app.views.characters.tabs;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.configuration.InfoFile;
import me.piitex.app.views.characters.CharacterEditView;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.util.Map;

public class LorebookTab extends Tab {

    private final AppSettings appSettings;
    private final InfoFile infoFile;
    private final CharacterEditView parentView;

    private InputFieldOverlay addKeyInput;
    private TextAreaOverlay addValueInput;
    private ScrollContainer scrollLoreContainer;
    private final Map<String, String> loreItems ;


    public LorebookTab(AppSettings appSettings, InfoFile infoFile, CharacterEditView parentView) {
        super("Lorebook");
        this.appSettings = appSettings;
        this.infoFile = infoFile;
        this.parentView = parentView;
        this.loreItems = parentView.getLoreItems();
        buildLorebookTabContent();
    }

    private void buildLorebookTabContent() {
        VerticalLayout rootLayout = new VerticalLayout(appSettings.getWidth() - 300, appSettings.getHeight());
        rootLayout.setSpacing(50);
        rootLayout.setAlignment(Pos.TOP_CENTER);
        this.addElement(rootLayout);

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Use the following placeholders; {char}, {{char}}, {chara}, {{chara}}, {character}, {{character}}, {user}, {{user}}, {usr}, {{usr}}");
        info.setX(200);
        info.setY(10);
        rootLayout.addElement(info);

        HorizontalLayout displayBox = new HorizontalLayout(0, 0);
        displayBox.setSpacing(100);
        displayBox.setMaxSize(600, 0);
        rootLayout.addElement(displayBox);

        CardContainer addContainer = new CardContainer(0, 0, 400, 400);
        addContainer.setMaxSize(400, 400);

        addKeyInput = new InputFieldOverlay("", "Separate multiple keys with a comma (,)", 0, 0, 200, 50);
        addKeyInput.setId("key");
        addContainer.setHeader(addKeyInput);

        addValueInput = new TextAreaOverlay("", "Enter the lore info", 0, 0, 400, 200);
        addValueInput.setId("value");
        addContainer.setBody(addValueInput);

        HorizontalLayout buttonBox = new HorizontalLayout(200, 50);
        buttonBox.setAlignment(Pos.CENTER);

        ButtonOverlay add = new ButtonBuilder("add").setText("Add").build();
        add.addStyle(Styles.SUCCESS);
        add.addStyle(Styles.BUTTON_OUTLINED);
        buttonBox.addElement(add);
        addContainer.setFooter(buttonBox);

        scrollLoreContainer = getLoreItems();
        add.onClick(event -> {
            String keyText = addKeyInput.getCurrentText();
            String valueText = addValueInput.getCurrentText();

            if (keyText.isEmpty() || valueText.isEmpty()) {
                return;
            }

            loreItems.put(keyText, valueText);

            VerticalLayout scrollLayout = (VerticalLayout) scrollLoreContainer.getLayout();
            CardContainer newLoreEntryCard = buildLoreEntry(keyText, scrollLayout);
            scrollLayout.addElement(newLoreEntryCard);

            ScrollPane pane = scrollLoreContainer.getScrollPane();
            if (pane != null) {
                VBox vBox = (VBox) pane.getContent();
                if (vBox != null) {
                    vBox.getChildren().add(newLoreEntryCard.build().getKey());
                }
            }

            ((TextField) addKeyInput.getNode()).setText("");
            ((TextArea) addValueInput.getNode()).setText("");

            parentView.warnTokens();
        });

        displayBox.addElement(addContainer);
        displayBox.addElement(scrollLoreContainer);

        this.addElement(parentView.buildSubmitBox());
    }

    private ScrollContainer getLoreItems() {
        VerticalLayout scrollLayout = new VerticalLayout(0, 0);

        ScrollContainer root = new ScrollContainer(scrollLayout, 0, 0, 450, 0);
        root.setMaxSize(450, appSettings.getHeight() - 300);
        root.setVerticalScroll(true);
        root.setScrollWhenNeeded(false);
        root.setHorizontalScroll(false);

        App.logger.info("Building lore cache...");
        for (Map.Entry<String, String> entry : loreItems.entrySet()) {
            scrollLayout.addElement(buildLoreEntry(entry.getKey(), scrollLayout));
        }
        return root;
    }

    private CardContainer buildLoreEntry(String key, VerticalLayout scrollContainer) {
        CardContainer card = new CardContainer(0, 0, 400, 300);
        card.setId(key);

        InputFieldOverlay entryKey = new InputFieldOverlay(key, 0, 0, 400, 50);
        entryKey.setEnabled(false); // Make key read-only
        card.setHeader(entryKey);

        TextAreaOverlay entryValue = new TextAreaOverlay(loreItems.get(key), 0, 0, 400, 200);
        entryValue.onInputSetEvent(event -> {
            loreItems.put(key, event.getInput());
            parentView.warnTokens();
        });
        card.setBody(entryValue);

        ButtonOverlay remove = new ButtonBuilder("remove").setText("Remove").build();
        remove.setX(170);
        remove.addStyle(Styles.DANGER);
        remove.addStyle(Styles.BUTTON_OUTLINED);
        card.setFooter(remove);

        remove.onClick(event -> {
            loreItems.remove(key);

            if (scrollContainer != null) {
                ScrollPane pane = scrollLoreContainer.getScrollPane();
                if (pane != null) {
                    VBox vBox = (VBox) pane.getContent();
                    if (vBox != null) {
                        Node toRemove = scrollContainer.getNode(key);
                        if (toRemove != null) {
                            vBox.getChildren().remove(toRemove);
                        }
                    }
                }
            }
            parentView.warnTokens();
        });

        return card;
    }
}
