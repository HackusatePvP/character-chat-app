package me.piitex.app.utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import me.piitex.app.App;
import me.piitex.app.configuration.ModelSettings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

// Shout out to the 30 kilos of cocaine it took to figure this out.
// Works with SillyTavern cards and Backyard AI cards. Prroobablyy...
// This is lazy importing and only gathers the data that is needed.
// This app does not create character cards.
public class CharacterCardImporter {

    public static JSONObject getImageMetaData(File file) throws ImageProcessingException, IOException {
        App.logger.info("Gathering card data...");
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        JSONObject toReturn = null;

        for (Directory directory : metadata.getDirectoriesOfType(ExifSubIFDDirectory.class)) {
            if (directory.containsTag(37510)) {
                // 37510 is User Comment for the metadata for BackyardAI
                String data = new String(Base64.getDecoder().decode(directory.getDescription(37510)));
                try {
                    toReturn = new JSONObject(data);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (toReturn == null) {
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    // Slightly easier with Silly Tavern. They all start with Chara: for the metadata.
                    if (tag.getDescription().startsWith("chara")) {
                        // Remove the beginning "chara: " so it follows the json scheme.
                        String data = tag.getDescription().replace("chara: ", "");
                        data = new String(Base64.getDecoder().decode(data));
                        try {
                            toReturn = new JSONObject(data);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return toReturn;
    }

    private static JSONObject getCharacterJson(JSONObject metaData) throws JSONException {
        if (metaData.has("character")) {
            return metaData.getJSONObject("character");
        }
        if (metaData.has("chara")) {
            return metaData.getJSONObject("chara");
        }

        return metaData;
    }

    public static String getModelInstructions(JSONObject metaData) throws JSONException {
        JSONObject characterJson = getCharacterJson(metaData);
        if (characterJson.has("basePrompt")) {
            return characterJson.getString("basePrompt");
        }
        if (characterJson.has("system_prompt")) {
            return characterJson.getString("system_prompt");
        }

        if (characterJson.has("data")) {
            JSONObject data = characterJson.getJSONObject("data");
            if (data.has("basePrompt")) {
                return data.getString("basePrompt");
            }
            if (data.has("system_prompt")) {
                return data.getString("system_prompt");
            }
        }

        return "";
    }

    public static String getCharacterId(JSONObject metaData) throws JSONException {
        JSONObject characterJson = getCharacterJson(metaData);
        if (characterJson.has("aiDisplayName")) {
            return characterJson.getString("aiDisplayName");
        }
        if (characterJson.has("name")) {
            return characterJson.getString("name");
        }

        if (characterJson.has("data")) {
            JSONObject data = characterJson.getJSONObject("data");
            if (data.has("aiDisplayName")) {
                return data.getString("aiDisplayName");
            }
            if (data.has("name")) {
                return data.getString("name");
            }
        }

        return "";
    }

    public static String getCharacterDisplayName(JSONObject metaData) throws JSONException {
        JSONObject characterJson = getCharacterJson(metaData);
        if (characterJson.has("aiName")) {
            return characterJson.getString("aiName");
        }
        if (characterJson.has("name")) {
            return characterJson.getString("name");
        }

        if (characterJson.has("data")) {
            JSONObject data = characterJson.getJSONObject("data");
            if (data.has("aiName")) {
                return data.getString("aiName");
            }
            if (data.has("name")) {
                return data.getString("name");
            }
        }

        return "";
    }

    public static String getCharacterPersona(JSONObject metaData) throws JSONException {
        String persona = "";
        JSONObject characterJson = getCharacterJson(metaData);
        if (characterJson.has("aiPersona")) {
            persona += characterJson.getString("aiPersona");
        }
        if (characterJson.has("personality")) {
            persona += characterJson.getString("personality");
        }
        if (characterJson.has("description")) {
            persona += characterJson.getString("description");
        }
        if (characterJson.has("data") && persona.isEmpty()) {
            JSONObject data = characterJson.getJSONObject("data");
            if (data.has("aiPersona")) {
                persona += data.getString("aiPersona");
            }
            if (data.has("description")) {
                persona += data.getString("description");
            }
            if (data.has("personality")) {
                persona += data.getString("personality");
            }
        }
        return persona;
    }

    public static String getFirstMessage(JSONObject metaData) throws JSONException {
        JSONObject characterJson = getCharacterJson(metaData);
        if (characterJson.has("firstMessage")) {
            return characterJson.getString("firstMessage");
        }
        if (characterJson.has("first_mes")) {
            return characterJson.getString("first_mes");
        }

        if (characterJson.has("data")) {
            JSONObject data = characterJson.getJSONObject("data");
            if (data.has("firstMessage")) {
                return data.getString("firstMessage");
            }
            if (data.has("first_mes")) {
                return data.getString("first_mes");
            }
        }

        return "";
    }

    public static String getChatScenario(JSONObject metaData) throws JSONException {
        JSONObject characterJson = getCharacterJson(metaData);
        if (characterJson.has("scenario")) {
            return characterJson.getString("scenario");
        }
        JSONObject data = characterJson.getJSONObject("data");
        if (data.has("scenario")) {
            return data.getString("scenario");
        }
        return "";
    }

    public static Map<String, String> getLoreItems(JSONObject metaData) throws JSONException {
        JSONObject characterJson = getCharacterJson(metaData);
        Map<String, String> map = new HashMap<>();
        if (characterJson.has("loreItems")) {

            JSONArray array = characterJson.getJSONArray("loreItems");
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String key = object.getString("key");
                String value = object.getString("value");
                map.put(key, value);
            }

            return map;
        }

        if (characterJson.has("data")) {
            JSONObject data = characterJson.getJSONObject("data");
            if (data.has("character_book")) {
                JSONObject characterBook = data.getJSONObject("character_book");
                if (characterBook.has("entries")) {
                    JSONArray entries = characterBook.getJSONArray("entries");
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject object = entries.getJSONObject(i);
                        JSONArray keys = object.getJSONArray("keys");
                        String value = object.getString("content");

                        StringBuilder key = new StringBuilder();
                        for (int ii = 0; ii < keys.length(); ii++) {
                            key.append(keys.getString(ii)).append(",");
                        }
                        map.put(key.toString(), value);
                    }
                }
            }
        }

        return map;
    }

    public static ModelSettings getModelSettings(JSONObject metaData) throws JSONException {
        JSONObject characterJson = getCharacterJson(metaData);

        ModelSettings settings = new ModelSettings();
        if (characterJson.has("temperature")) {
            settings.setTemperature(characterJson.getDouble("temperature"));
        }
        if (characterJson.has("minP")) {
            settings.setMinP(characterJson.getDouble("minP"));
        }
        if (characterJson.has("repeatLastN")) {
            settings.setRepeatTokens(characterJson.getInt("repeatLastN"));
        }
        if (characterJson.has("repeatPenalty")) {
            settings.setRepeatPenalty(characterJson.getDouble("repeatPenalty"));
        }
        return settings;
    }
}
