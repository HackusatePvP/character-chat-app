package me.piitex.app.configuration;

import atlantafx.base.theme.*;
import javafx.scene.paint.Color;
import me.piitex.app.App;

import java.io.File;

public class AppSettings {
    private int width = 720, height = 1280;
    private String textSize = "default";
    private String imagesPath = "";
    private String theme = "Primer Dark";

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
        if (infoFile.hasKey("theme")) {
            this.theme = infoFile.get("theme");
        }
        if (infoFile.hasKey("images-path")) {
            this.imagesPath = infoFile.get("images-path");
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

    public String getImagesPath() {
        return imagesPath;
    }

    public void setImagesPath(String imagesPath) {
        this.imagesPath = imagesPath;
        infoFile.set("images-path", imagesPath);
    }

    public void setTheme(String theme) {
        this.theme = theme;
        infoFile.set("theme", theme);
    }

    public String getTheme() {
        return theme;
    }

    public Theme getStyleTheme(String name) {
        if (name.equalsIgnoreCase("primer light")) {
            return new PrimerLight();
        } else if (name.equalsIgnoreCase("nord light")) {
            return new NordLight();
        } else if (name.equalsIgnoreCase("nord dark")) {
            return new NordDark();
        } else if (name.equalsIgnoreCase("cupertino light")) {
            return new CupertinoLight();
        } else if (name.equalsIgnoreCase("cupertino dark")) {
            return new CupertinoDark();
        } else if (name.equalsIgnoreCase("dracula")) {
            return new Dracula();
        } else {
            return new PrimerDark();
        }
    }

    public String getThemeSubtleColor(String theme) {
        if (theme.equalsIgnoreCase("primer light")) {
            return "#6e7781";
        } else if (theme.equalsIgnoreCase("nord light")) {
            return "#eef1f5";
        } else if (theme.equalsIgnoreCase("nord dark")) {
            return "#3B4252";
        } else if (theme.equalsIgnoreCase("cupertino light")) {
            return "242, 242, 247";
        } else if (theme.equalsIgnoreCase("cupertino dark")) {
            return "44, 44, 46";
        } else if (theme.equalsIgnoreCase("dracula")) {
            return "#3d3f4a";
        } else {
            return "#161b22";
        }
    }

}
