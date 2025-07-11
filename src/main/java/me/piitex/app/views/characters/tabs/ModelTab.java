package me.piitex.app.views.characters.tabs;

import me.piitex.app.backend.Character;
import me.piitex.app.backend.User;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.configuration.InfoFile;
import me.piitex.app.views.characters.CharacterEditView;
import me.piitex.engine.containers.tabs.Tab;

public class ModelTab extends Tab {

    private final AppSettings appSettings;
    private final InfoFile infoFile;
    private final ServerSettings serverSettings;
    private final CharacterEditView parentView;

    public ModelTab(AppSettings appSettings, InfoFile infoFile, ServerSettings serverSettings, Character character, User user, CharacterEditView parentView) {
        super("Model");
        this.appSettings = appSettings;
        this.infoFile = infoFile;
        this.serverSettings = serverSettings;
        this.parentView = parentView;
        buildModelTabContent(character);
    }

    private void buildModelTabContent(Character character) {

    }
}