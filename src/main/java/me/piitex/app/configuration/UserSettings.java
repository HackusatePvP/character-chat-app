package me.piitex.app.configuration;

import me.piitex.app.App;

import java.io.File;

public class UserSettings {
    private boolean discardConfirm = true;
    private final InfoFile infoFile;

    public UserSettings() {
        this.infoFile = new InfoFile(new File(App.getAppDirectory(), "user-settings.info"), false);
        if (infoFile.hasKey("discard-confirm")) {
            discardConfirm = infoFile.getBoolean("discard-confirm");
        }
    }

    public boolean isDiscardConfirm() {
        return discardConfirm;
    }

    public void setDiscardConfirm(boolean discardConfirm) {
        this.discardConfirm = discardConfirm;
        infoFile.set("discard-confirm", discardConfirm);
    }
}
