package me.piitex.app.configuration;


import com.drew.lang.annotations.Nullable;

public class ModelSettings {
    private String modelInstructions = "Text transcript of a never-ending conversation between {user} and {character}. In the transcript, write everything {character}'s reply from a third person perspective with dialogue written in quotations. Assuming any action of {user} is strictly forbidden. You are {character}. Write {character}'s reply only."; //TODO: Default
    private int contextSize = 4096; // 4096 is a good baseline. Most modern models can go way higher (32k)
    private double temperature = 0.8; // min 0
    private double minP = 0.1; // min 0.05
    private int repeatTokens = 64; // min -1
    private double repeatPenalty = 1.1; // min 1.0
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
        if (infoFile.hasKey("min-p")) {
            this.minP = infoFile.getDouble("min-p");
        } else {
            infoFile.set("min-p", minP);
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

    public double getMinP() {
        return minP;
    }

    public void setMinP(double minP) {
        this.minP = minP;
        if (infoFile != null) {
            infoFile.set("min-p", minP);
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
