package me.piitex.app.utils;

import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.User;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Iterator;

public class ImageCardExporter {

   public static void exportCharacter(Character character, File output) throws IOException {

       Files.copy(new File(character.getIconPath()).toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);

       BufferedImage image = ImageIO.read(output);

       Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
       if (!writers.hasNext()) {
           App.logger.error("Could not find png writer for exporter.", new RuntimeException());
           return;
       }
       ImageWriter writer = writers.next();

       // Prepare ImageWriteParam and IIOMetadata
       ImageWriteParam writeParam = writer.getDefaultWriteParam();
       ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromRenderedImage(image);
       IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
       String nativeFormat = metadata.getNativeMetadataFormatName();

       // Create the native metadata tree
       IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(nativeFormat);
       IIOMetadataNode textNode = null;


       // Check if a tEXt node already exists
       for (int i = 0; i < root.getChildNodes().getLength(); i++) {
           if (root.getChildNodes().item(i).getNodeName().equals("tEXt")) {
               textNode = (IIOMetadataNode) root.getChildNodes().item(i);
               break;
           }
       }

       // If not, create a new one and append it to the root
       if (textNode == null) {
           textNode = new IIOMetadataNode("tEXt");
           root.appendChild(textNode);
       }

       IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
       textEntry.setAttribute("keyword", "chara");
       textEntry.setAttribute("value", Base64.getEncoder().encodeToString(toJson(character).toString().getBytes(StandardCharsets.UTF_8)));
       textNode.appendChild(textEntry);


       metadata.setFromTree(nativeFormat, root);

       try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
           writer.setOutput(stream);
           writer.write(new IIOImage(image, null, metadata));
       } finally {
           writer.dispose();
       }
   }

    public static void exportUser(User user, File output) throws IOException {

        Files.copy(new File(user.getIconPath()).toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);

        BufferedImage image = ImageIO.read(output);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            App.logger.error("Could not find png writer for exporter.", new RuntimeException());
            return;
        }
        ImageWriter writer = writers.next();

        // Prepare ImageWriteParam and IIOMetadata
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromRenderedImage(image);
        IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
        String nativeFormat = metadata.getNativeMetadataFormatName();

        // Create the native metadata tree
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(nativeFormat);
        IIOMetadataNode textNode = null;


        // Check if a tEXt node already exists
        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            if (root.getChildNodes().item(i).getNodeName().equals("tEXt")) {
                textNode = (IIOMetadataNode) root.getChildNodes().item(i);
                break;
            }
        }

        // If not, create a new one and append it to the root
        if (textNode == null) {
            textNode = new IIOMetadataNode("tEXt");
            root.appendChild(textNode);
        }

        IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
        textEntry.setAttribute("keyword", "user");
        textEntry.setAttribute("value", Base64.getEncoder().encodeToString(toJson(user).toString().getBytes(StandardCharsets.UTF_8)));
        textNode.appendChild(textEntry);


        metadata.setFromTree(nativeFormat, root);

        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            writer.write(new IIOImage(image, null, metadata));
        } finally {
            writer.dispose();
        }
    }

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

    public static JSONObject toJson(User user) {
        JSONObject root = new JSONObject();

        JSONObject characterRoot = new JSONObject();
        characterRoot.put("userDisplay", user.getDisplayName());
        characterRoot.put("userPersona", user.getPersona());

        JSONArray loreRoot = new JSONArray();

        for (String loreId : user.getLorebook().keySet()) {
            String loreValue = user.getLorebook().get(loreId);
            JSONObject loreEntry = new JSONObject();
            loreEntry.put("id", loreId);
            loreEntry.put("key", loreId);
            loreEntry.put("value", loreValue);
            loreRoot.put(loreEntry);
        }
        characterRoot.put("loreItems", loreRoot);
        root.put("user", characterRoot);

        return root;
    }

}
