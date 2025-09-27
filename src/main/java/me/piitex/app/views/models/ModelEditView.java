package me.piitex.app.views.models;

import atlantafx.base.theme.Styles;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.configuration.ModelSettings;
import me.piitex.app.views.SidebarView;
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

public class ModelEditView extends EmptyContainer {
    private final ModelSettings settings;
    private int layoutSpacing = 150;
    private final AppSettings appSettings = App.getInstance().getAppSettings();

    private String instructions = "Text transcript of a never-ending conversation between {user} and {character}. In the transcript, write everything {character}'s reply from a third person perspective with dialogue written in quotations. Assuming any action of {user} is strictly forbidden. You are {character}. Write {character}'s reply only.";
    private int contextSize = 4096;
    private double temperature = 0.8; // min 0
    private double topP = 1; // min 0
    private double minP = 0.1; // min 0.05
    private int topK = 40; // Min 0
    private int repeatTokens = 64; // min -1
    private double repeatPenalty = 1.1; // min 1.0
    private double dynamicTempRage = 0.0;
    private double dynamicExponent = 1;
    private double xtcProbability = 0;
    private double xtcThreshold = 0.1;
    private double typicalP = 1.0;
    private double presencePenalty = 0;
    private double frequencyPenalty = 0;
    private double dryMultiplier = 0;
    private double dryBase = 1.75;
    private int dryAllowedLength = 2;
    private int dryPenaltyTokens = -1;
    private String mmProj = "None / Disabled";
    private String chatTemplate = "default";
    private String reasoningTemplate = "disabled";
    private boolean jinja = false;

    public ModelEditView(ModelSettings settings) {
        super(App.getInstance().getAppSettings().getWidth(), App.getInstance().getAppSettings().getHeight() - 100);
        this.settings = settings;
        initializeSettings();

        HorizontalLayout main = new HorizontalLayout(0, 0);
        main.addStyle(Styles.BG_INSET);
        main.setSpacing(35);
        main.addElement(new SidebarView(main, false));
        addElement(main);

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
        layout.addElement(buildDynamicTempRange());
        layout.addElement(buildDynamicTempExponent());
        layout.addElement(buildTopP());
        layout.addElement(buildMinP());
        layout.addElement(buildTopK());
        layout.addElement(buildRepeatPenalty());
        layout.addElement(buildRepeatTokens());
        layout.addElement(buildPresencePenalty());
        layout.addElement(buildFrequencyPenalty());
        layout.addElement(buildXtcProbability());
        layout.addElement(buildXtcThreshold());
        layout.addElement(buildTypicalP());
        layout.addElement(buildDryMultiplier());
        layout.addElement(buildDryBase());
        layout.addElement(buildDryAllowedLength());
        layout.addElement(buildDryPenaltyToken());
        layout.addElement(buildChatTemplates());
        layout.addElement(buildReasoningTemplate());
        layout.addElement(buildJinjaTemplate());
        layout.addElement(buildSubmitBox());
    }

    private void initializeSettings() {
        this.instructions = settings.getModelInstructions();
        this.contextSize = settings.getContextSize();
        this.mmProj = settings.getMmProj();
        this.temperature = settings.getTemperature();
        this.dynamicTempRage = settings.getDynamicTempRage();
        this.dynamicExponent = settings.getDynamicExponent();
        this.topP = settings.getTopP();
        this.minP = settings.getMinP();
        this.topK = settings.getTopK();
        this.repeatPenalty = settings.getRepeatPenalty();
        this.repeatTokens = settings.getRepeatTokens();
        this.xtcProbability = settings.getXtcProbability();
        this.xtcThreshold = settings.getXtcThreshold();
        this.presencePenalty = settings.getPresencePenalty();
        this.frequencyPenalty = settings.getFrequencyPenalty();
        this.dryMultiplier = settings.getDryMultiplier();
        this.dryBase = settings.getDryBase();
        this.dryAllowedLength = settings.getDryAllowedLength();
        this.dryPenaltyTokens = settings.getDryPenaltyTokens();
        this.chatTemplate = settings.getChatTemplate();
        this.reasoningTemplate = settings.getReasoningTemplate();
        this.jinja = settings.isJinja();
    }

    public TileContainer buildInstructions() {
        TileContainer tileContainer = new TileContainer(appSettings.getWidth() - 300, 0);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
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
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 120);
        tileContainer.setTitle("Context Size");
        tileContainer.setDescription("Maximum size for context tokens. Recommended setting this between 4096 - 16834.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("The max amount of tokens to store for context. -1 will auto assign this value to the max. Higher values will consume large amounts of V-Ram.");
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
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
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
        selection.setDefaultItem(mmProj);
        selection.onItemSelect(event -> {
            this.mmProj = event.getNewValue();
        });

        tileContainer.setAction(selection);

        return tileContainer;
    }

    private TileContainer buildTemperature() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Temperature");
        tileContainer.setDescription("Set the temperature for the model.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Controls the randomness of the generated text by affecting the probability distribution of the output tokens. Higher = more random, lower = more focused.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, temperature);
        input.onValueChange(event -> {
            this.temperature = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildDynamicTempRange() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Dynamic Temperature Range");
        tileContainer.setDescription("Addon for the temperature sampler.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("The added value to the range of dynamic temperature, which adjusts probabilities by entropy of tokens.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, dynamicTempRage);
        input.onValueChange(event -> {
            this.dynamicTempRage = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildDynamicTempExponent() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Dynamic Temperature Exponent");
        tileContainer.setDescription("Addon for the temperature sampler.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Smoothes out the probability redistribution based on the most probable token.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, dynamicTempRage);
        input.onValueChange(event -> {
            this.dynamicExponent = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildTopP() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Top P");
        tileContainer.setDescription("Set the top-p value for the model.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Limits tokens to those that together have a cumulative probability of at least p");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, topP);
        input.onValueChange(event -> {
            this.topP = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildMinP() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Min-P");
        tileContainer.setDescription("Filters out less likely tokens during generation.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Limits tokens based on the minimum probability for a token to be considered, relative to the probability of the most likely token.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, minP);
        input.onValueChange(event -> {
            this.minP = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildTopK() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Top K");
        tileContainer.setDescription("Limit the next token selection to the K most probable tokens..");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Default is 40.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Integer.MAX_VALUE, topK);
        input.onValueChange(event -> {
            this.topK = event.getNewValue().intValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildRepeatPenalty() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Repeat Penalty");
        tileContainer.setDescription("Adjusts how strongly the model is penalized for repeating tokens that have appeared recently in the generated text.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Higher values (e.g, 1.1-1.5) aggressively discourage repetition, while a value of 1.0 applies no penalty. This helps in generating more varied and natural-sounding responses.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, repeatPenalty);
        input.onValueChange(event -> {
            this.repeatPenalty = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildRepeatTokens() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Repeat Tokens");
        tileContainer.setDescription("Specifies the number of recent tokens (from the model's output history) to consider when applying the repetition penalty.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("A larger value means the model will look further back in its generated text to avoid repeating phrases or patterns. -1 will use context size.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(-1, Integer.MAX_VALUE, repeatTokens);
        input.onValueChange(event -> {
            this.repeatTokens = event.getNewValue().intValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildPresencePenalty() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Presence Penalty");
        tileContainer.setDescription("Limits tokens based on whether they appear in the output or not.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Repeat alpha presence penalty. Default: 0.0, which is disabled.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, presencePenalty);
        input.onValueChange(event -> {
            this.presencePenalty = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildFrequencyPenalty() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Frequency Penalty");
        tileContainer.setDescription("Limits tokens based on how often they appear in the output.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Repeat alpha frequency penalty. Default: 0.0, which is disabled.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, frequencyPenalty);
        input.onValueChange(event -> {
            this.frequencyPenalty = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildXtcProbability() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("XTC Probability");
        tileContainer.setDescription("Set the chance for token removal via XTC sampler.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Controls the chance of cutting tokens at all. 0 disables XTC.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, xtcProbability);
        input.onValueChange(event -> {
            this.xtcProbability = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildXtcThreshold() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("XTC Threshold");
        tileContainer.setDescription("Set a minimum probability threshold for tokens to be removed");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Controls the token probability that is required to cut that token.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, xtcThreshold);
        input.onValueChange(event -> {
            this.xtcThreshold = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildTypicalP() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Typical P");
        tileContainer.setDescription("Sorts and limits tokens based on the difference between log-probability and entropy.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Enable locally typical sampling with parameter p. Default: 1.0, which is disabled.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, typicalP);
        input.onValueChange(event -> {
            this.typicalP = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildDryMultiplier() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Dry Multiplier");
        tileContainer.setDescription("Set the DRY (Don't Repeat Yourself) repetition penalty multiplier.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Default: 0.0, which is disabled.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, dryMultiplier);
        input.onValueChange(event -> {
            this.dryMultiplier = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildDryBase() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Dry Base");
        tileContainer.setDescription("Set the DRY repetition penalty base value.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Default: 1.75");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, dryBase);
        input.onValueChange(event -> {
            this.dryBase = event.getNewValue().doubleValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildDryAllowedLength() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Dry Length");
        tileContainer.setDescription("Tokens that extend repetition beyond this receive exponentially increasing penalty.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("multiplier * base ^ (length of repeating sequence before token - allowed length). Default: 2");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, dryAllowedLength);
        input.onValueChange(event -> {
            this.dryAllowedLength = event.getNewValue().intValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildDryPenaltyToken() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
        tileContainer.setMaxSize(appSettings.getWidth() - 300, 150);
        tileContainer.setTitle("Dry Penalty Last Token");
        tileContainer.setDescription("How many tokens to scan for repetitions.");

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Default: -1, where 0 is disabled and -1 is context size.");
        tileContainer.setGraphic(info);

        SpinnerNumberOverlay input = new SpinnerNumberOverlay(0, Double.MAX_VALUE, dryPenaltyTokens);
        input.onValueChange(event -> {
            this.dryPenaltyTokens = event.getNewValue().intValue();
        });
        tileContainer.setAction(input);

        return tileContainer;
    }

    private TileContainer buildChatTemplates() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
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
            this.chatTemplate = event.getNewValue();
        });

        tileContainer.setAction(selection);

        return tileContainer;
    }

    private TileContainer buildReasoningTemplate() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
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
            this.reasoningTemplate = event.getNewValue();
        });
        tileContainer.setAction(selection);

        return tileContainer;
    }

    private TileContainer buildJinjaTemplate() {
        TileContainer tileContainer = new TileContainer(0, 0);
        tileContainer.addStyle(Styles.BORDER_DEFAULT);
        tileContainer.addStyle(Styles.BG_DEFAULT);
        tileContainer.addStyle(appSettings.getGlobalTextSize());
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

        ButtonOverlay discard = new ButtonBuilder("discard").setText("Discard").build();
        discard.addStyle(Styles.DANGER);
        discard.addStyle(Styles.BUTTON_OUTLINED);
        discard.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new ModelsView("List"));
        });

        ButtonOverlay submit = new ButtonBuilder("submit").setText("Submit").build();
        submit.addStyle(Styles.SUCCESS);
        submit.addStyle(Styles.BUTTON_OUTLINED);
        submit.onClick(event -> {
            settings.setModelInstructions(instructions);
            settings.setContextSize(contextSize);
            settings.setTemperature(temperature);
            settings.setTopP(topP);
            settings.setMinP(minP);
            settings.setTopK(topK);
            settings.setRepeatPenalty(repeatPenalty);
            settings.setRepeatTokens(repeatTokens);
            settings.setDynamicTempRage(dynamicTempRage);
            settings.setDynamicExponent(dynamicExponent);
            settings.setXtcProbability(settings.getXtcProbability());
            settings.setXtcThreshold(settings.getXtcThreshold());
            settings.setTypicalP(settings.getTypicalP());
            settings.setPresencePenalty(settings.getPresencePenalty());
            settings.setFrequencyPenalty(settings.getFrequencyPenalty());
            settings.setDryMultiplier(settings.getDryMultiplier());
            settings.setDryBase(settings.getDryBase());
            settings.setDryAllowedLength(settings.getDryAllowedLength());
            settings.setDryPenaltyTokens(settings.getDryPenaltyTokens());
            settings.setMmProj(mmProj);
            settings.setChatTemplate(chatTemplate);
            settings.setReasoningTemplate(reasoningTemplate);
            settings.setJinja(jinja);

            App.window.clearContainers();
            App.window.addContainer(new ModelsView("List"));
        });

        layout.addElements(discard, submit);

        return layout;
    }
}
