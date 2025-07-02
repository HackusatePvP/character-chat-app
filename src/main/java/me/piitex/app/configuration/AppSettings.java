package me.piitex.app.configuration;

import me.piitex.app.App;

import java.io.File;

public class AppSettings {
    private int width = 720, height = 1280;
    private String textSize = "default";

    private final InfoFile infoFile;

    public AppSettings() {
        this.infoFile = new InfoFile(new File(App.getAppDirectory(), "app.info"), false);

        if (infoFile.hasKey("width")) {
            this.width = infoFile.getInteger("width");
        }
        if (infoFile.hasKey("height")) {
            this.height = infoFile.getInteger("height");
        }
        if (infoFile.hasKey("text-size")) {
            this.textSize = infoFile.get("text-size");
        }
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
        infoFile.set("width", width);
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
        infoFile.set("height", height);
    }

    public String getTextSize() {
        return textSize;
    }

    public void setTextSize(String textSize) {
        this.textSize = textSize;
        infoFile.set("text-size", textSize);
    }
}
