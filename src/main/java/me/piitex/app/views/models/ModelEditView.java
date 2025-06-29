package me.piitex.app.views.models;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.configuration.ModelSettings;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.Container;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ModelEditView {
    private final ModelSettings settings;
    private int layoutSpacing = 150;
    private final Container container;

    public ModelEditView(Model model) {
        this.settings = model.getSettings();
        container = new EmptyContainer(1670, 1500);

        HorizontalLayout main = new HorizontalLayout(0, 0);
        main.setSpacing(35);
        main.addElement(new SidebarView().getRoot());
        container.addElement(main);

        VerticalLayout layout = new VerticalLayout(0, 0);

        ScrollContainer scrollContainer = new ScrollContainer(layout, 0, 20, 1670, 800);
        scrollContainer.setMaxSize(1670, 1000);
        scrollContainer.setVerticalScroll(true);
        scrollContainer.setScrollWhenNeeded(true);
        scrollContainer.setHorizontalScroll(false);
        main.addElement(scrollContainer);

        layout.addElement(buildInstructions());
        layout.addElement(buildModalFile());
        layout.addElement(buildTemperature());
        layout.addElement(buildMinP());
        layout.addElement(buildRepeatPenalty());
        layout.addElement(buildRepeatTokens());
        layout.addElement(buildChatTemplates());
        layout.addElement(buildReasoningTemplate());
        layout.addElement(buildJinjaTemplate());
    }

    private CardContainer buildInstructions() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 0);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);

        TextOverlay key = new TextOverlay("Model Instructions: ");
        key.addStyle(Styles.TEXT_BOLD);
        description.add(key);

        TextOverlay value = new TextOverlay("Provide custom instructions or a system prompt for the model. This defines the model's persona, behavior, or specific task goals for all interactions. Only modify if you are familiar with prompt engineering.");
        description.add(value);

        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        TextAreaOverlay input = new TextAreaOverlay(settings.getModelInstructions(), 0, 0, 800, 200);
        root.addElement(input);
        input.onInputSetEvent(event -> {
            settings.setModelInstructions(event.getInput());
        });

        card.setBody(root);

        return card;
    }

    private CardContainer buildModalFile() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        TextOverlay key = new TextOverlay("Multimodal Support: ");
        key.addStyle(Styles.TEXT_BOLD);
        description.add(key);

        TextOverlay value = new TextOverlay("Set the MM-Proj file for vision support. Only works if the model has a supported MM-Proj. Without setting this, image processing won't work.");
        description.add(value);

        List<String> items = new ArrayList<>();
        items.add("None / Disabled");
        items.addAll(App.getModelNames());

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setDefaultItem(settings.getMmProj());
        root.addElement(selection);
        selection.onItemSelect(event -> {
            settings.setMmProj(event.getItem());
        });

        card.setBody(root);

        return card;
    }

    private CardContainer buildTemperature() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        TextOverlay key = new TextOverlay("Temperature: ");
        key.addStyle(Styles.TEXT_BOLD);
        description.add(key);

        // Updated description for Temperature
        TextOverlay value = new TextOverlay("Controls the creativity and randomness of the model's output. Higher values (e.g, 0.8-1.0) make the output more diverse and surprising, while lower values (e.g, 0.2-0.5) make it more focused, deterministic, and less prone to unexpected phrasing. A value of 0 makes the output completely deterministic.");
        description.add(value);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, settings.getTemperature());
        input.onValueChange(event -> {
            settings.setTemperature(event.getNewValue());
        });
        root.addElement(input);

        card.setBody(root);

        return card;
    }

    private CardContainer buildMinP() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        TextOverlay key = new TextOverlay("Min-P: ");
        key.addStyle(Styles.TEXT_BOLD);
        description.add(key);

        TextOverlay value = new TextOverlay("Filters out less likely tokens during generation. `Min-P` (minimum probability) keeps only tokens whose probability is at least `p` times the probability of the most likely token. A value of 0 disables this filtering. Higher values can lead to more coherent but less diverse text.");
        description.add(value);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, settings.getMinP());
        input.onValueChange(event -> {
            settings.setMinP(event.getNewValue());
        });
        root.addElement(input);

        card.setBody(root);

        return card;
    }

    private CardContainer buildRepeatTokens() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        TextOverlay key = new TextOverlay("Repeat Tokens: ");
        key.addStyle(Styles.TEXT_BOLD);
        description.add(key);

        TextOverlay value = new TextOverlay("Specifies the number of recent tokens (from the model's output history) to consider when applying the repetition penalty. A larger value means the model will look further back in its generated text to avoid repeating phrases or patterns.");
        description.add(value);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Integer.MAX_VALUE, settings.getRepeatTokens());
        input.onValueChange(event -> {
            settings.setRepeatTokens((int) event.getNewValue());
        });
        root.addElement(input);

        card.setBody(root);

        return card;
    }

    private CardContainer buildRepeatPenalty() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        TextOverlay key = new TextOverlay("Repeat Penalty: ");
        key.addStyle(Styles.TEXT_BOLD);
        description.add(key);

        TextOverlay value = new TextOverlay("Adjusts how strongly the model is penalized for repeating tokens that have appeared recently in the generated text. Higher values (e.g, 1.1-1.5) aggressively discourage repetition, while a value of 1.0 applies no penalty. This helps in generating more varied and natural-sounding responses.");
        description.add(value);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, settings.getRepeatPenalty());
        input.onValueChange(event -> {
            settings.setRepeatPenalty(event.getNewValue());
        });
        root.addElement(input);

        card.setBody(root);

        return card;
    }

    private CardContainer buildChatTemplates() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        TextOverlay key = new TextOverlay("Chat Template: ");
        key.addStyle(Styles.TEXT_BOLD);
        description.add(key);

        TextOverlay value = new TextOverlay("Defines the specific format and structure for how prompts and responses are presented to and generated by the model. This is crucial for the model to correctly interpret user input and generate appropriate replies. Only change if you know the correct template for your specific model. `Default` attempts to auto-detect.");
        description.add(value);

        String[] templates = {
                "default",
                "chatglm3",
                "chatglm4",
                "chatml",
                "command-r",
                "deepseek",
                "deepseek2",
                "deepseek3",
                "exaone3",
                "falcon3",
                "gemma",
                "gigachat",
                "glmedge",
                "granite",
                "llama2",
                "llama2-sys",
                "llama2-sys-bos",
                "llama2-sys-strip",
                "llama3",
                "llama4",
                "megrez",
                "minicpm",
                "mistral-v1",
                "mistral-v3",
                "mistral-v3-tekken",
                "mistral-v7",
                "mistral-v7-tekken",
                "monarch",
                "openchat",
                "orion",
                "phi3",
                "phi4",
                "rwkv-world",
                "smolvlm",
                "vicuna",
                "vicuna-orca",
                "yandex",
                "zephyr"
        };

        ComboBoxOverlay selection = new ComboBoxOverlay(templates, 400, 50);
        selection.setDefaultItem(settings.getChatTemplate());
        root.addElement(selection);
        selection.onItemSelect(event -> {
            settings.setChatTemplate(event.getItem());
        });

        card.setBody(root);

        return card;
    }

    private CardContainer buildReasoningTemplate() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        TextOverlay key = new TextOverlay("Reasoning Template: ");
        key.addStyle(Styles.TEXT_BOLD);
        description.add(key);

        TextOverlay value = new TextOverlay("Controls the integration and extraction of internal 'thought' processes from the model's output. When enabled, the model might include special tags (e.g, `<thought>...</thought>`) showing its reasoning steps. `Disabled` will prevent the model from generating these internal thoughts.");
        description.add(value);

        LinkedList<String> items = new LinkedList<>();
        items.add("deepseek");
        items.add("none");
        items.add("disabled");

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setDefaultItem(settings.getReasoningTemplate());
        root.addElement(selection);
        selection.onItemSelect(event -> {
            settings.setReasoningTemplate(event.getItem());
        });

        card.setBody(root);

        return card;
    }

    private CardContainer buildJinjaTemplate() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("", 600, 200);
        description.setMaxWidth(600);
        description.setMaxHeight(200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        TextOverlay key = new TextOverlay("Jinja: ");
        key.addStyle(Styles.TEXT_BOLD);
        description.add(key);

        TextOverlay value = new TextOverlay("Enables or disables the use of Jinja templating for chat formatting. If enabled, the model will expect and generate chat messages structured according to Jinja syntax, overriding the standard chat template. Only enable if your model explicitly supports Jinja templating, as incorrect usage can lead to malformed outputs.");
        description.add(value);

        ToggleSwitchOverlay switchOverlay = new ToggleSwitchOverlay(settings.isJinja());
        switchOverlay.onToggle(event -> {
            settings.setJinja(!settings.isJinja());
        });
        root.addElement(switchOverlay);

        card.setBody(root);

        return card;
    }

    /*private CardContainer buildAstrixRP() {
        CardContainer card = new CardContainer(0, 0, 1600, 120);
        card.setMaxSize(1600, 120);

        HorizontalLayout root = new HorizontalLayout(0, 0);
        root.setMaxSize(1600, 120);
        root.setAlignment(Pos.BASELINE_LEFT);
        root.setSpacing(layoutSpacing);

        TextFlowOverlay description = new TextFlowOverlay("Do you want to include \"*\" in chats. Example: *waves* Hey! User come here. If disabled the app will automatically remove the astrix.", 600, 200);
        description.setTextFillColor(Color.WHITE);
        root.addElement(description);

        ToggleSwitchOverlay switchOverlay = new ToggleSwitchOverlay(settings.isAstrixEnabled());
        switchOverlay.onToggle(event -> {
            settings.setAstrixEnabled(!settings.isAstrixEnabled());
        });
        root.addElement(switchOverlay);

        card.setBody(root);

        return card;
    }*/

    public Container getContainer() {
        return container;
    }
}
