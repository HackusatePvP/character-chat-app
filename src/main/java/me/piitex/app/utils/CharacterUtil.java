package me.piitex.app.utils;

import me.piitex.app.App;
import me.piitex.app.backend.Character;
import org.json.JSONArray;
import org.json.JSONObject;

public class CharacterUtil {

    public static JSONObject toJson(Character character) {
        JSONObject root = new JSONObject();

        JSONObject characterRoot = new JSONObject();
        characterRoot.put("basePrompt", App.getInstance().getSettings().getGlobalModel().getSettings().getModelInstructions());
        characterRoot.put("customDialogue", "");
        characterRoot.put("firstMessage", character.getFirstMessage());
        characterRoot.put("scenario", character.getChatScenario());
        characterRoot.put("aiDisplayName", character.getDisplayName());
        characterRoot.put("aiName", character.getId());
        characterRoot.put("aiPersona", character.getPersona());
        characterRoot.put("userDisplay", character.getUser().getDisplayName());
        characterRoot.put("userPersona", character.getUser().getPersona());

        JSONArray loreRoot = new JSONArray();

        for (String loreId : character.getLorebook().keySet()) {
            String loreValue = character.getLorebook().get(loreId);
            JSONObject loreEntry = new JSONObject();
            loreEntry.put("id", loreId);
            loreEntry.put("key", loreId);
            loreEntry.put("value", loreValue);
            loreRoot.put(loreEntry);
        }
        characterRoot.put("loreItems", loreRoot);
        root.put("character", characterRoot);

        return root;
    }
}
