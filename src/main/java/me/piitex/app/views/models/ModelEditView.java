package me.piitex.app.views.models;

import atlantafx.base.theme.Styles;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.configuration.ModelSettings;
import me.piitex.app.views.SidebarView;
import me.piitex.engine.Container;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.TileContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ModelEditView {
    private final ModelSettings settings;
    private int layoutSpacing = 150;
    private final Container container;

    private final AppSettings appSettings = App.getInstance().getAppSettings();

    private String instructions = "Text transcript of a never-ending conversation between {user} and {character}. In the transcript, write everything {character}'s reply from a third person perspective with dialogue written in quotations. Assuming any action of {user} is strictly forbidden. You are {character}. Write {character}'s reply only.";
    private int contextSize = 4096;
    private String multimodal = "None / Disabled";
    private double temperature = 0.8;
    private double minP = 0.1;
    private double repeatPenalty = 1.1;
    private int repeatTokens = 64;
    private String chatTemplate = "default";
    private String reasoningTemplate = "disabled";
    private boolean jinja = false;

    public ModelEditView(ModelSettings settings) {
        this.settings = settings;
        initializeSettings();
        container = new EmptyContainer(appSettings.getWidth() - 300, appSettings.getHeight() - 100);

        HorizontalLayout main = new HorizontalLayout(0, 0);
        main.setSpacing(35);
        main.addElement(new SidebarView(main, true).getRoot());
        container.addElement(main);

        VerticalLayout layout = new VerticalLayout(0, 0);

        ScrollContainer scrollContainer = new ScrollContainer(layout, 0, 20, appSettings.getWidth() - 250, appSettings.getHeight() - 100);
        scrollContainer.setMaxSize(appSettings.getWidth() - 250, appSettings.getHeight() - 100);
        scrollContainer.setVerticalScroll(true);
        scrollContainer.setScrollWhenNeeded(true);
        scrollContainer.setHorizontalScroll(false);
        main.addElement(scrollContainer);

        layout.addElement(buildInstructions());
        layout.addElement(buildContextSize());
        layout.addElement(buildModalFile());
        layout.addElement(buildTemperature());
        layout.addElement(buildMinP());
        layout.addElement(buildRepeatPenalty());
        layout.addElement(buildRepeatTokens());
        layout.addElement(buildChatTemplates());
        layout.addElement(buildReasoningTemplate());
        layout.addElement(buildJinjaTemplate());
        layout.addElement(buildSubmitBox());
    }

    private void initializeSettings() {
        this.instructions = settings.getModelInstructions();
        this.contextSize = settings.getContextSize();
        this.multimodal = settings.getMmProj();
        this.temperature = settings.getTemperature();
        this.minP = settings.getMinP();
        this.repeatPenalty = settings.getRepeatPenalty();
        this.repeatTokens = settings.getRepeatTokens();
        this.chatTemplate = settings.getChatTemplate();
        this.reasoningTemplate = settings.getReasoningTemplate();
        this.jinja = settings.isJinja();
    }

    public TileContainer buildInstructions() {
        TileContainer tileContainer = new TileContainer(appSettings.getWidth() - 300, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.setTitle("Model Instructions");
        tileContainer.setDescription("Provide base instructions for the model.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Supports placeholders for both character and user. {chara} {character} {char} {user} {usr}");
        tileContainer.setGraphic(info);

        RichTextAreaOverlay input = new RichTextAreaOverlay(instructions, 600, 200);
        input.setBackgroundColor(appSettings.getThemeDefaultColor(appSettings.getTheme()));
        input.setBorderColor(appSettings.getThemeBorderColor(appSettings.getTheme()));
        input.setTextFill(appSettings.getThemeTextColor(appSettings.getTheme()));
        input.onInputSetEvent(event -> {
            this.instructions = event.getInput();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildContextSize() {
        TileContainer tileContainer = new TileContainer(appSettings.getWidth() - 300, 120);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 120);
        tileContainer.setTitle("Context Size");
        tileContainer.setDescription("The size of the prompts tokens.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Different models will support different sizes. Recommend setting this between 4096 - 16384");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Integer.MAX_VALUE, contextSize);
        input.onValueChange(event -> {
            this.contextSize = (int) event.getNewValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildModalFile() {
        TileContainer tileContainer = new TileContainer(appSettings.getWidth() - 300, 120);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 120);
        tileContainer.setTitle("Multimodal Support");
        tileContainer.setDescription("Set multimodal file.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Set the MM-Proj file for vision support. The file will have to contain 'mmproj' . Only works if the model has a supported MM-Proj. Without setting this, image processing won't work");
        tileContainer.setGraphic(info);

        List<String> items = new ArrayList<>();
        items.add("None / Disabled");
        items.addAll(App.getModelNames("mmproj"));

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 250, 50);
        selection.setDefaultItem(multimodal);
        selection.onItemSelect(event -> {
            this.multimodal = event.getItem();
        });

        tileContainer.setAction(selection);

        return tileContainer;
    }

    private TileContainer buildTemperature() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Temperature");
        tileContainer.setDescription("Set the temperature for the model.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Controls the creativity and randomness of the model's output. Higher values make the output more diverse and surprising, while lower values make it more focused, deterministic.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, temperature);
        input.onValueChange(event -> {
            this.temperature = event.getNewValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildMinP() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Min-P");
        tileContainer.setDescription("Filters out less likely tokens during generation.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Min-P keeps only tokens whose probability is at least 'p' times the probability of the most likely token. A value of 0 disables this filtering.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, minP);
        input.onValueChange(event -> {
            this.minP = event.getNewValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildRepeatPenalty() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Repeat Penalty");
        tileContainer.setDescription("Adjusts how strongly the model is penalized for repeating tokens that have appeared recently in the generated text.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Higher values (e.g, 1.1-1.5) aggressively discourage repetition, while a value of 1.0 applies no penalty. This helps in generating more varied and natural-sounding responses.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, repeatPenalty);
        input.onValueChange(event -> {
            this.repeatPenalty = event.getNewValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildRepeatTokens() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Repeat Tokens");
        tileContainer.setDescription("Specifies the number of recent tokens (from the model's output history) to consider when applying the repetition penalty.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("A larger value means the model will look further back in its generated text to avoid repeating phrases or patterns.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Integer.MAX_VALUE, repeatTokens);
        input.onValueChange(event -> {
            this.repeatTokens = (int) event.getNewValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildChatTemplates() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Chat Template");
        tileContainer.setDescription("Set the chat format template.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Changing this could drastically effect generation quality.");
        tileContainer.setGraphic(info);

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

        ComboBoxOverlay selection = new ComboBoxOverlay(templates, 250, 50);
        selection.setDefaultItem(chatTemplate);
        selection.onItemSelect(event -> {
            this.chatTemplate = event.getItem();
        });

        tileContainer.setAction(selection);

        return tileContainer;
    }

    private TileContainer buildReasoningTemplate() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Reasoning Template");
        tileContainer.setDescription("Set the reasoning format template. Only works for models that support reasoning.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Controls the integration and extraction of internal 'thought' processes from the model's output. When enabled, the model might include special tags (e.g, `<thought>...</thought>`) showing its reasoning steps.");
        tileContainer.setGraphic(info);

        LinkedList<String> items = new LinkedList<>();
        items.add("deepseek");
        items.add("none");
        items.add("disabled");

        ComboBoxOverlay selection = new ComboBoxOverlay(items, 400, 50);
        selection.setDefaultItem(reasoningTemplate);
        selection.onItemSelect(event -> {
            this.reasoningTemplate = event.getItem();
        });
        tileContainer.setAction(selection);

        return tileContainer;
    }

    private TileContainer buildJinjaTemplate() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Jinja Template");
        tileContainer.setDescription("Enables or disables the use of Jinja templating for chat formatting.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Only enable if your model explicitly supports Jinja templating, as incorrect usage can lead to malformed outputs.");
        tileContainer.setGraphic(info);

        ToggleSwitchOverlay switchOverlay = new ToggleSwitchOverlay(jinja);
        switchOverlay.onToggle(event -> {
            this.jinja = event.getNewValue(); // ???
        });
        tileContainer.setAction(switchOverlay);

        return tileContainer;
    }

    public HorizontalLayout buildSubmitBox() {
        HorizontalLayout layout = new HorizontalLayout(300, 150);
        layout.setY(20); // Offset the y axis
        layout.setMaxSize(300, 150);
        layout.setSpacing(50);
        layout.setX((appSettings.getWidth() - 300) / 2);

        ButtonOverlay discard = new ButtonOverlay("disc", "Discard");
        discard.addStyle(Styles.DANGER);
        discard.addStyle(Styles.BUTTON_OUTLINED);
        discard.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new ModelsView().getContainer());
            App.window.render();
        });

        ButtonOverlay submit = new ButtonOverlay("submit", "Submit");
        submit.addStyle(Styles.SUCCESS);
        submit.addStyle(Styles.BUTTON_OUTLINED);
        submit.onClick(event -> {
            settings.setModelInstructions(instructions);
            settings.setContextSize(contextSize);
            settings.setMmProj(multimodal);
            settings.setTemperature(temperature);
            settings.setMinP(minP);
            settings.setRepeatPenalty(repeatPenalty);
            settings.setRepeatTokens(repeatTokens);
            settings.setChatTemplate(chatTemplate);
            settings.setReasoningTemplate(reasoningTemplate);
            settings.setJinja(jinja);

            App.window.clearContainers();
            App.window.addContainer(new ModelsView().getContainer());
            App.window.render();
        });

        layout.addElements(discard, submit);

        return layout;
    }

    public Container getContainer() {
        return container;
    }
}
