package me.piitex.app.utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import me.piitex.app.App;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class UserCardImporter {
    public static JSONObject getImageMetaData(File file) throws ImageProcessingException, IOException {
        App.logger.info("Gathering card data...");
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        JSONObject toReturn = null;


        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                // Slightly easier with Silly Tavern. They all start with Chara: for the metadata.
                if (tag.getDescription().startsWith("user")) {
                    // Remove the beginning "chara: " so it follows the json scheme.
                    String data = tag.getDescription().replace("user: ", "");
                    try {
                        data = new String(Base64.getDecoder().decode(data));
                    } catch (IllegalArgumentException ignored) {
                        // Thrown if the user entry cannot be decoded into base64.
                        // This happens if there is multiple user entries which is possible.
                        // Go to next entry to loop.
                        continue;
                    }
                    try {
                        toReturn = new JSONObject(data);
                        return toReturn;
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return null;
    }

    private static JSONObject getUserJson(JSONObject metaData) throws JSONException {
        if (metaData.has("user")) {
            return metaData.getJSONObject("user");
        }
        // Can process other structures

        return metaData;
    }

    public static String getUserDisplay(JSONObject metaData) {
        JSONObject userJson = getUserJson(metaData);
        if (userJson.has("userDisplay")) {
            return userJson.getString("userDisplay");
        }
        return null;
    }

    public static String getUserPersona(JSONObject metaData) {
        JSONObject userJson = getUserJson(metaData);
        if (userJson.has("userPersona")) {
            return userJson.getString("userPersona");
        }
        return null;
    }

    public static Map<String, String> getLoreItems(JSONObject metaData) throws JSONException {
        JSONObject userJson = getUserJson(metaData);
        Map<String, String> map = new HashMap<>();
        if (userJson.has("loreItems")) {

            JSONArray array = userJson.getJSONArray("loreItems");
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String key = object.getString("key");
                String value = object.getString("value");
                map.put(key, value);
            }

            return map;
        }
        return map;
    }
}
