package me.piitex.app.views.creator.users;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.engine.Element;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import me.piitex.os.configurations.InfoFile;
import org.kordamp.ikonli.material2.Material2AL;

import java.util.LinkedHashMap;
import java.util.TreeMap;

public class UserLoreCustomizationView extends EmptyContainer {
    private final VerticalLayout root;
    private final InfoFile infoFile;
    private final UserCreator parent;

    // Cached nodes for persistent states
    private TextFieldOverlay userIdInput;
    private TextFieldOverlay userDisplayInput;
    private RichTextAreaOverlay userPersonaInput;
    private ImageOverlay userImage;
    private VerticalLayout loreLayout;

    private final ScrollContainer scrollContainer;

    // Stores lore index with the string key+delimiter+value
    private final LinkedHashMap<Integer, String> tempLore = new LinkedHashMap<>();


    public UserLoreCustomizationView(UserCreator parent, InfoFile infoFile, double width, double height) {
        super(width, height);
        addStyle(Styles.BG_DEFAULT);
        this.parent = parent;
        this.infoFile = infoFile;
        root = new VerticalLayout(width, height);
        root.setMaxSize(width, -1);
        root.addStyle(Styles.BORDER_DEFAULT);
        root.setAlignment(Pos.CENTER);
        addProperties("progress", "User");

        scrollContainer = new ScrollContainer(root, width - 5, height - 40);
        scrollContainer.setMaxSize(scrollContainer.getWidth(), scrollContainer.getHeight());
        scrollContainer.setHorizontalScroll(false);
        scrollContainer.setScrollWhenNeeded(false);
        addElement(scrollContainer);

        init();
    }

    private void init() {

        if (infoFile == null) {
            App.logger.error("Could not initialize user data!", new RuntimeException());
        }

        root.addElement(buildLorebookLayout());
    }

    private VerticalLayout buildLorebookLayout() {
        VerticalLayout layout = new VerticalLayout(720, 0);
        layout.setMaxSize(layout.getWidth(), -1);
        layout.setSpacing(5);
        layout.setAlignment(Pos.TOP_CENTER);

        VerticalLayout wrapper = new VerticalLayout(0, 0);
        wrapper.setSpacing(5);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        layout.addElement(wrapper);

        TextOverlay header = new TextOverlay("Lorebook");
        header.addStyle(Styles.TEXT_BOLDER);
        wrapper.addElement(header);

        TextOverlay description = new TextOverlay("Context that is dynamically added to the AI. The context is triggered by the appearance of the key.");
        description.addStyle(Styles.TEXT_LIGHTER);
        wrapper.addElement(description);

        ButtonOverlay addEntry = new ButtonBuilder("add").setText("Add Entry").addStyle(Styles.BUTTON_OUTLINED).addStyle(Styles.ACCENT).setWidth(layout.getWidth()).build();
        layout.addElement(addEntry);

        loreLayout = new VerticalLayout(root.getWidth(), -1);
        loreLayout.setMaxSize(loreLayout.getWidth(), -1);
        layout.addElement(loreLayout);

        addEntry.onClick(_ -> {
            loreLayout.addElement(buildLoreEntry("", ""), 0);
        });

        if (infoFile.hasKey("lore")) {
            infoFile.getSortedStringMap("lore").forEach((key, value) -> {
                loreLayout.addElement(buildLoreEntry(key, value));
            });
        }


        return layout;
    }

    public VerticalLayout buildLoreEntry(String key, String value) {
        VerticalLayout layout = new VerticalLayout(root.getWidth(), 300);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setMaxSize(layout.getWidth(), layout.getHeight());

        // Index this entry
        int index = loreLayout.getElements().size();
        tempLore.put(index, key + "-" + value);

        HorizontalLayout wrapper = new HorizontalLayout(root.getWidth(), 35);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxSize(wrapper.getWidth(), wrapper.getHeight());
        layout.addElement(wrapper);

        TextFieldOverlay textFieldOverlay = new TextFieldOverlay(key, "Keys (Use comma to separate).", 700, 35);
        textFieldOverlay.setMaxSize(textFieldOverlay.getWidth(), textFieldOverlay.getHeight());
        wrapper.addElement(textFieldOverlay);

        IconOverlay delete = new IconOverlay(Material2AL.DELETE_FOREVER);
        delete.setIconSize(24);
        delete.setColor(Color.RED);
        wrapper.addElement(delete);

        delete.onClick(event -> {
            loreLayout.removeElement(layout);
            tempLore.remove(index);
        });


        RichTextAreaOverlay richTextAreaOverlay = new RichTextAreaOverlay(value, 720, 200);
        richTextAreaOverlay.setMaxSize(richTextAreaOverlay.getWidth(), richTextAreaOverlay.getHeight());
        richTextAreaOverlay.setBackgroundColor(App.getInstance().getAppSettings().getThemeDefaultColor(App.getInstance().getAppSettings().getTheme()));
        richTextAreaOverlay.setBorderColor(App.getInstance().getAppSettings().getThemeBorderColor(App.getInstance().getAppSettings().getTheme()));
        richTextAreaOverlay.setTextFill(App.getInstance().getAppSettings().getThemeTextColor(App.getInstance().getAppSettings().getTheme()));
        layout.addElement(richTextAreaOverlay);

        textFieldOverlay.onInputSetEvent(event -> {
            if (!richTextAreaOverlay.getCurrentText().isBlank()) {
                tempLore.put(index, textFieldOverlay.getCurrentText() + "-" + event.getInput());
            }
        });

        richTextAreaOverlay.onInputSetEvent(event -> {
            if (!textFieldOverlay.getCurrentText().isBlank()) {
                tempLore.put(index, event.getInput() + "-" + richTextAreaOverlay.getCurrentText());
            }
        });


        return layout;
    }

    public TreeMap<String, String> compileUserLore() {
        TreeMap<String, String> toReturn = new TreeMap<>();

        for (Element element : loreLayout.getElements().values()) {
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

    public VerticalLayout getLoreLayout() {
        return loreLayout;
    }

    public LinkedHashMap<Integer, String> getTempLore() {
        return tempLore;
    }
}
