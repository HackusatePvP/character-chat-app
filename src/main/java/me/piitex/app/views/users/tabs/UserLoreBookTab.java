package me.piitex.app.views.users.tabs;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.users.UserEditView;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.util.Map;

public class UserLoreBookTab extends Tab {
    private final UserEditView userEditView;
    private TextFieldOverlay addKeyInput;
    private TextAreaOverlay addValueInput;
    private ScrollContainer scrollLoreContainer;

    private static final AppSettings appSettings = App.getInstance().getAppSettings();

    public UserLoreBookTab(String text, UserEditView userEditView) {
        super(text);
        this.userEditView = userEditView;
        buildLorebookTabContent();
    }

    public void buildLorebookTabContent() {
        removeAllElements();

        VerticalLayout rootLayout = new VerticalLayout(appSettings.getWidth() - 300, appSettings.getHeight());
        rootLayout.setSpacing(50);
        rootLayout.setAlignment(Pos.TOP_CENTER);
        this.addElement(rootLayout);

        IconOverlay info = new IconOverlay(Material2AL.INFO);
        info.setTooltip("Use the following placeholders; {char}, {{char}}, {chara}, {{chara}}, {character}, {{character}}, {user}, {{user}}, {usr}, {{usr}}");
        rootLayout.addElement(info);

        HorizontalLayout displayBox = new HorizontalLayout(0, -1);
        displayBox.setAlignment(Pos.TOP_CENTER);
        displayBox.setSpacing(100);
        rootLayout.addElement(displayBox);

        CardContainer addContainer = new CardContainer(0, 0, 400, 400);
        addContainer.setMaxSize(400, 400);

        addKeyInput = new TextFieldOverlay("", "Separate multiple keys with a comma (,)", 0, 0, 200, 50);
        addContainer.setHeader(addKeyInput);

        addValueInput = new TextAreaOverlay("", "Enter the lore info", 0, 0, 400, 200);
        addContainer.setBody(addValueInput);

        HorizontalLayout buttonBox = new HorizontalLayout(200, 50);
        buttonBox.setAlignment(Pos.CENTER);

        ButtonOverlay add = new ButtonBuilder("add").setText("Add").build();
        add.addStyle(Styles.SUCCESS);
        add.addStyle(Styles.BUTTON_OUTLINED);
        buttonBox.addElement(add);
        addContainer.setFooter(buttonBox);

        scrollLoreContainer = getLoreItems();
        add.onClick(_ -> {
            String keyText = addKeyInput.getCurrentText();
            String valueText = addValueInput.getCurrentText();

            if (keyText.isEmpty() || valueText.isEmpty()) {
                return;
            }

            userEditView.getLoreBook().put(keyText, valueText);

            VerticalLayout scrollLayout = (VerticalLayout) scrollLoreContainer.getLayout();
            CardContainer newLoreEntryCard = buildLoreEntry(keyText, scrollLayout);
            scrollLayout.addElement(newLoreEntryCard);

            addKeyInput.setCurrentText("");
            addValueInput.setCurrentText("");
        });

        displayBox.addElement(addContainer);
        displayBox.addElement(scrollLoreContainer);

        this.addElement(userEditView.buildSubmitBox());
    }

    private ScrollContainer getLoreItems() {
        VerticalLayout scrollLayout = new VerticalLayout(0, -1);

        ScrollContainer root = new ScrollContainer(scrollLayout, 0, 0, 450, appSettings.getHeight() - 300);
        root.setMaxSize(450, appSettings.getHeight() - 300);
        root.setVerticalScroll(true);
        root.setScrollWhenNeeded(false);
        root.setHorizontalScroll(false);
        root.setPannable(true);

        App.logger.info("Building lore cache...");
        for (Map.Entry<String, String> entry : userEditView.getLoreBook().entrySet()) {
            scrollLayout.addElement(buildLoreEntry(entry.getKey(), scrollLayout));
        }
        return root;
    }

    private CardContainer buildLoreEntry(String key, VerticalLayout scrollContainer) {
        CardContainer card = new CardContainer(0, 0, 400, 300);

        TextFieldOverlay entryKey = new TextFieldOverlay(key, 0, 0, 400, 50);
        entryKey.setEnabled(false); // Make key read-only
        card.setHeader(entryKey);

        TextAreaOverlay entryValue = new TextAreaOverlay(userEditView.getLoreBook().get(key), 0, 0, 400, 200);
        entryValue.onInputSetEvent(event -> userEditView.getLoreBook().put(key, event.getInput()));
        card.setBody(entryValue);

        ButtonOverlay remove = new ButtonBuilder("remove").setText("Remove").build();
        remove.setX(170);
        remove.addStyle(Styles.DANGER);
        remove.addStyle(Styles.BUTTON_OUTLINED);
        card.setFooter(remove);

        remove.onClick(_ -> {
            userEditView.getLoreBook().remove(key);
            scrollContainer.removeElement(card);
        });

        return card;
    }
}
