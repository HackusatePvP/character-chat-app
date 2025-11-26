package me.piitex.app.backend;

import me.piitex.app.App;
import me.piitex.os.configurations.InfoFile;
import me.piitex.app.configuration.ModelSettings;

import java.io.File;

public class Model {
    private final File file;
    private final ModelSettings settings;

    public Model(File file) {
        this.file = file;
        this.settings = new ModelSettings(new InfoFile(new File(App.getAppDirectory(), "models/" + file.getName().split("\\.")[0] + ".info"), false));
    }

    public ModelSettings getSettings() {
        return settings;
    }

    public File getFile() {
        return file;
    }
}
