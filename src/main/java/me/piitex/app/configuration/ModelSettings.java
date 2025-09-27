package me.piitex.app.configuration;


import com.drew.lang.annotations.Nullable;
import me.piitex.engine.configurations.InfoFile;

public class ModelSettings {
    private String modelInstructions = "Text transcript of a never-ending conversation between {user} and {character}. In the transcript, write everything {character}'s reply from a third person perspective with dialogue written in quotations. Assuming any action of {user} is strictly forbidden. You are {character}. Write {character}'s reply only.";
    private int contextSize = 4096; // 4096 is a good baseline. Most modern models can go way higher (32k)
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
    private boolean useDefault;
    private boolean jinja = false;

    @Nullable
    private InfoFile infoFile;

    public ModelSettings() {
        // Empty file
    }

    public ModelSettings(InfoFile infoFile) {
        this.infoFile = infoFile;
        if (infoFile.hasKey("instructions")) {
            this.modelInstructions = infoFile.get("instructions");
        } else {
            infoFile.set("instructions", modelInstructions);
        }
        if (infoFile.hasKey("tokens")) {
            contextSize = infoFile.getInteger("tokens");
        } else {
            infoFile.set("tokens", contextSize);
        }
        if (infoFile.hasKey("temperature")) {
            this.temperature = infoFile.getDouble("temperature");
        } else {
            infoFile.set("temperature", temperature);
        }
        if (infoFile.hasKey("top-p")) {
            this.topP = infoFile.getDouble("top-p");
        } else {
            infoFile.set("top-p", topP);
        }
        if (infoFile.hasKey("min-p")) {
            this.minP = infoFile.getDouble("min-p");
        } else {
            infoFile.set("min-p", minP);
        }
        if (infoFile.hasKey("top-k")) {
            this.topK = infoFile.getInteger("top-k");
        } else {
            infoFile.set("top-k", topK);
        }
        if (infoFile.hasKey("repeat-tokens")) {
            this.repeatTokens = infoFile.getInteger("repeat-tokens");
        } else {
            infoFile.set("repeat-tokens", repeatTokens);
        }
        if (infoFile.hasKey("repeat-penalty")) {
            this.repeatPenalty = infoFile.getDouble("repeat-penalty");
        } else {
            infoFile.set("repeat-penalty", repeatPenalty);
        }
        if (infoFile.hasKey("dynamic-temp-range")) {
            this.dynamicTempRage = infoFile.getDouble("dynamic-temp-range");
        } else {
            infoFile.set("dynamic-temp-range", dynamicTempRage);
        }
        if (infoFile.hasKey("dynamic-exponent")) {
            this.dynamicExponent = infoFile.getDouble("dynamic-exponent");
        } else {
            infoFile.set("dynamic-exponent", dynamicExponent);
        }
        if (infoFile.hasKey("xtc-probability")) {
            this.xtcProbability = infoFile.getDouble("xtc-probability");
        } else {
            infoFile.set("xtc-probability", xtcProbability);
        }
        if (infoFile.hasKey("xtc-threshold")) {
            this.xtcThreshold = infoFile.getDouble("xtc-threshold");
        } else {
            infoFile.set("xtc-threshold", xtcThreshold);
        }
        if (infoFile.hasKey("typical-p")) {
            this.typicalP = infoFile.getDouble("typical-p");
        } else {
            infoFile.set("typical-p", typicalP);
        }
        if (infoFile.hasKey("presence-penalty")) {
            this.presencePenalty = infoFile.getDouble("presence-penalty");
        } else {
            infoFile.set("presence-penalty", presencePenalty);
        }
        if (infoFile.hasKey("frequency-penalty")) {
            this.frequencyPenalty = infoFile.getDouble("frequency-penalty");
        } else {
            infoFile.set("frequency-penalty", frequencyPenalty);
        }
        if (infoFile.hasKey("dry-multiplier")) {
            this.dryMultiplier = infoFile.getDouble("dry-multiplier");
        } else {
            infoFile.set("dry-multiplier", dryMultiplier);
        }
        if (infoFile.hasKey("dry-base")) {
            this.dryBase = infoFile.getDouble("dry-base");
        } else {
            infoFile.set("dry-base", dryBase);
        }
        if (infoFile.hasKey("dry-length")) {
            this.dryAllowedLength = infoFile.getInteger("dry-length");
        } else {
            infoFile.set("dry-length", dryAllowedLength);
        }
        if (infoFile.hasKey("dry-penalty-tokens")) {
            this.dryPenaltyTokens = infoFile.getInteger("dry-penalty-tokens");
        } else {
            infoFile.set("dry-penalty-tokens", dryPenaltyTokens);
        }
        if (infoFile.hasKey("chat-template")) {
            this.chatTemplate = infoFile.get("chat-template");
        } else {
            infoFile.set("chat-template", chatTemplate);
        }
        if (infoFile.hasKey("reasoning-template")) {
            this.reasoningTemplate = infoFile.get("reasoning-template");
        } else {
            infoFile.set("reasoning-template", reasoningTemplate);
        }
        if (infoFile.hasKey("default")) {
            this.useDefault = infoFile.getBoolean("default");
        } else {
            infoFile.set("default", useDefault);
        }
        if (infoFile.hasKey("jinja")) {
            this.jinja = infoFile.getBoolean("jinja");
        } else {
            infoFile.set("jinja", jinja);
        }
        if (infoFile.hasKey("mm-proj")) {
            mmProj = infoFile.get("mm-proj");
        } else {
            infoFile.set("mm-proj", mmProj);
        }
    }

    public String getModelInstructions() {
        return modelInstructions;
    }

    public void setModelInstructions(String modelInstructions) {
        this.modelInstructions = modelInstructions;
        if (infoFile != null) {
            infoFile.set("instructions", modelInstructions);
        }
    }

    public int getContextSize() {
        return contextSize;
    }

    public void setContextSize(int contextSize) {
        this.contextSize = contextSize;
        infoFile.set("tokens", contextSize);
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
        if (infoFile != null) {
            infoFile.set("temperature", temperature);
        }
    }

    public double getTopP() {
        return topP;
    }

    public void setTopP(double topP) {
        this.topP = topP;
        if (infoFile != null) {
            infoFile.set("top-p", topP);
        }
    }

    public double getMinP() {
        return minP;
    }

    public void setMinP(double minP) {
        this.minP = minP;
        if (infoFile != null) {
            infoFile.set("min-p", minP);
        }
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
        if (infoFile != null) {
            infoFile.set("top-k", topK);
        }
    }

    public int getRepeatTokens() {
        return repeatTokens;
    }

    public void setRepeatTokens(int repeatTokens) {
        this.repeatTokens = repeatTokens;
        if (infoFile != null) {
            infoFile.set("repeat-tokens", repeatTokens);
        }
    }

    public double getRepeatPenalty() {
        return repeatPenalty;
    }

    public void setRepeatPenalty(double repeatPenalty) {
        this.repeatPenalty = repeatPenalty;
        if (infoFile != null) {
            infoFile.set("repeat-penalty", repeatPenalty);
        }
    }

    public double getDynamicTempRage() {
        return dynamicTempRage;
    }

    public void setDynamicTempRage(double dynamicTempRage) {
        this.dynamicTempRage = dynamicTempRage;
        infoFile.set("dynamic-temp-range", dynamicTempRage);
    }

    public double getDynamicExponent() {
        return dynamicExponent;
    }

    public void setDynamicExponent(double dynamicExponent) {
        this.dynamicExponent = dynamicExponent;
        infoFile.set("dynamic-exponent", dynamicExponent);
    }

    public double getXtcProbability() {
        return xtcProbability;
    }

    public void setXtcProbability(double xtcProbability) {
        this.xtcProbability = xtcProbability;
        infoFile.set("xtc-probability", xtcProbability);
    }

    public double getXtcThreshold() {
        return xtcThreshold;
    }

    public void setXtcThreshold(double xtxThreshold) {
        this.xtcThreshold = xtxThreshold;
        infoFile.set("xtc-threshold", xtxThreshold);
    }

    public double getTypicalP() {
        return typicalP;
    }

    public void setTypicalP(double typicalP) {
        this.typicalP = typicalP;
        infoFile.set("typical-p", typicalP);
    }

    public double getPresencePenalty() {
        return presencePenalty;
    }

    public void setPresencePenalty(double presencePenalty) {
        this.presencePenalty = presencePenalty;
        infoFile.set("presence-penalty", presencePenalty);
    }

    public double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setFrequencyPenalty(double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
        infoFile.set("frequency-penalty", frequencyPenalty);
    }

    public double getDryMultiplier() {
        return dryMultiplier;
    }

    public void setDryMultiplier(double dryMultiplier) {
        this.dryMultiplier = dryMultiplier;
        infoFile.set("dry-multiplier", dryMultiplier);
    }

    public double getDryBase() {
        return dryBase;
    }

    public void setDryBase(double dryBase) {
        this.dryBase = dryBase;
        infoFile.set("dry-base", dryBase);
    }

    public int getDryAllowedLength() {
        return dryAllowedLength;
    }

    public void setDryAllowedLength(int dryAllowedLength) {
        this.dryAllowedLength = dryAllowedLength;
        infoFile.set("dry-length", dryAllowedLength);
    }

    public int getDryPenaltyTokens() {
        return dryPenaltyTokens;
    }

    public void setDryPenaltyTokens(int dryPenaltyTokens) {
        this.dryPenaltyTokens = dryPenaltyTokens;
        infoFile.set("dry-penalty-tokens", dryPenaltyTokens);
    }

    public String getChatTemplate() {
        return chatTemplate;
    }

    public void setChatTemplate(String chatTemplate) {
        this.chatTemplate = chatTemplate;
        if (infoFile != null) {
            infoFile.set("chat-template", chatTemplate);
        }
    }

    public String getReasoningTemplate() {
        return reasoningTemplate;
    }

    public void setReasoningTemplate(String reasoningTemplate) {
        this.reasoningTemplate = reasoningTemplate;
        if (infoFile != null) {
            infoFile.set("reasoning-template", reasoningTemplate);
        }
    }

    public boolean isDefault() {
        return useDefault;
    }

    public void setDefault(boolean useDefault) {
        this.useDefault = useDefault;
        infoFile.set("default", useDefault);
    }

    public boolean isJinja() {
        return jinja;
    }

    public void setJinja(boolean jinja) {
        this.jinja = jinja;
        infoFile.set("jinja", jinja);
    }

    public String getMmProj() {
        return mmProj;
    }

    public void setMmProj(String mmProj) {
        this.mmProj = mmProj;
        infoFile.set("mm-proj", mmProj);
    }

    @Nullable
    public InfoFile getInfoFile() {
        return infoFile;
    }
}
