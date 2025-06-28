package me.piitex.app.backend.server;

import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.configuration.InfoFile;

import java.io.File;
import java.util.LinkedList;

public class ServerSettings {
    private final InfoFile infoFile;
    private String backend = "Cuda";
    private String device = "Auto";
    private int gpuLayers = 10;
    private boolean memoryLock = false;
    private String chatTemplate = "default";
    private String reasoningTemplate = "none";
    private boolean jinja = false;
    private boolean thinkMode;
    private boolean installed = true;
    private String lastModel = "";
    private String modelPath = "%APPDATA%/chat-app/models/";
    private String mmProjModel = "";
    private LinkedList<String> devices = new LinkedList<>();
    private boolean astrixEnabled = true;
    private boolean flashAttention = false;

    public ServerSettings() {
        infoFile = new InfoFile(new File(App.getAppDirectory(), "settings.info"), false);

        if (infoFile.hasKey("backend")) {
            backend = infoFile.get("backend");
        } else {
            infoFile.set("backend", "cuda");
        }
        if (infoFile.hasKey("device")) {
            device = infoFile.get("device");
        } else {
            infoFile.set("device", device);
        }
        if (infoFile.hasKey("gpu-layers")) {
            gpuLayers = infoFile.getInteger("gpu-layers");
        } else {
            infoFile.set("gpu-layers", gpuLayers + "");
        }
        if (infoFile.hasKey("memory-lock")) {
            memoryLock = infoFile.getBoolean("memory-lock");
        } else {
            infoFile.set("memory-lock", memoryLock + "");
        }
        if (infoFile.hasKey("chat-template")) {
            chatTemplate = infoFile.get("chat-template");
        } else {
            infoFile.set("chat-template", chatTemplate);
        }
        if (infoFile.hasKey("reasoning-template")) {
            reasoningTemplate = infoFile.get("reasoning-template");
        } else {
            infoFile.set("reasoning-template", reasoningTemplate);
        }
        if (infoFile.hasKey("jinja")) {
            jinja = infoFile.getBoolean("jinja");
        } else {
            infoFile.set("jinja", false + "");
        }
        if (infoFile.hasKey("think-mode")) {
            thinkMode = infoFile.getBoolean("think-mode");
        } else {
            infoFile.set("think-mode", thinkMode + "");
        }
        if (infoFile.hasKey("model-path")) {
            this.modelPath = infoFile.get("model-path");
        } else {
            infoFile.set("model-path", modelPath);
        }
        if (infoFile.hasKey("mmproj")) {
            this.mmProjModel = infoFile.get("mmproj");
        } else {
            infoFile.set("mmproj", "");
        }
        if (infoFile.hasKey("last-model")) {
            lastModel = infoFile.get("last-model");
        }
        if (infoFile.hasKey("installed")) {
            installed = infoFile.getBoolean("installed");
        } else {
            infoFile.set("installed", false);
        }
        if (infoFile.hasKey("astrix-enabled")) {
            this.astrixEnabled = infoFile.getBoolean("astrix-enabled");
        } else {
            infoFile.set("astrix-enabled", true);
        }
        if (infoFile.hasKey("flash-attention")) {
            this.flashAttention = infoFile.getBoolean("flash-attention");
        } else {
            infoFile.set("flash-attention", false);
        }
    }

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
        infoFile.set("backend", backend);
    }

    public String getDevice() {
        return device;
    }

    public String getFormattedDevice() {
        if (device.contains(":")) {
            return device.split(":")[0].trim();
        }

        return device;
    }

    public void setDevice(String device) {
        this.device = device;
        infoFile.set("device", device);
    }

    public int getGpuLayers() {
        return gpuLayers;
    }

    public void setGpuLayers(int gpuLayers) {
        this.gpuLayers = gpuLayers;
        infoFile.set("gpu-layers", gpuLayers + "");
    }

    public boolean isMemoryLock() {
        return memoryLock;
    }

    public void setMemoryLock(boolean memoryLock) {
        this.memoryLock = memoryLock;
        infoFile.set("memory-lock", memoryLock + "");
    }

    public String getChatTemplate() {
        return chatTemplate;
    }

    public void setChatTemplate(String chatTemplate) {
        this.chatTemplate = chatTemplate;
        infoFile.set("chat-template", chatTemplate);
    }

    public String getReasoningTemplate() {
        return reasoningTemplate;
    }

    public void setReasoningTemplate(String reasoningTemplate) {
        this.reasoningTemplate = reasoningTemplate;
        infoFile.set("reasoning-template", reasoningTemplate);
    }

    public boolean isJinja() {
        return jinja;
    }

    public void setJinja(boolean jinja) {
        this.jinja = jinja;
        infoFile.set("jinja", jinja + "");
    }

    public boolean isThinkMode() {
        return thinkMode;
    }

    public void setThinkMode(boolean thinkMode) {
        this.thinkMode = thinkMode;
        infoFile.set("think-mode", thinkMode + "");
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
        infoFile.set("model-path", modelPath);
    }

    public Model getLastModel() {
        if (lastModel.isEmpty()) {
            return null;
        }
        return new Model(new File(lastModel));
    }

    public String getLastModelString() {
        if (lastModel.isEmpty() || getLastModel() == null) {
            return "";
        }

        return getLastModel().getFile().getName();
    }

    public void setLastModel(String lastModel) {
        this.lastModel = lastModel;
        mmProjModel = "";
        infoFile.set("last-model", lastModel);
    }

    public Model getMmProjModel() {
        if (mmProjModel.isEmpty()) {
            return null;
        }
        return new Model(new File(mmProjModel));
    }

    public String getMMprojString() {
        if (mmProjModel.isEmpty() || getMmProjModel() == null) {
            return "";
        }

        return getMmProjModel().getFile().getName();
    }

    public void setMmProjModel(String mmProjModel) {
        this.mmProjModel = mmProjModel;
        infoFile.set("mmproj", mmProjModel);
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public LinkedList<String> getDevices() {
        if (!devices.contains("Auto")) {
            devices.addFirst("Auto");
        }
        return devices;
    }

    public void setDevices(LinkedList<String> devices) {
        LinkedList<String> format = new LinkedList<>();
        for (String s : devices) {
            format.add(s.trim());
        }
        this.devices = format;
    }

    public boolean isAstrixEnabled() {
        return astrixEnabled;
    }

    public void setAstrixEnabled(boolean astrixEnabled) {
        this.astrixEnabled = astrixEnabled;
        infoFile.set("astrix-enabled", astrixEnabled);
    }

    public boolean isFlashAttention() {
        return flashAttention;
    }

    public void setFlashAttention(boolean flashAttention) {
        this.flashAttention = flashAttention;
        infoFile.set("flash-attention", flashAttention);
    }

    public InfoFile getInfoFile() {
        return infoFile;
    }
}
