package me.piitex.app.utils;

import me.piitex.app.App;
import me.piitex.app.backend.Character;

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

public class CharacterCardExporter {
    private final Character character;

    public CharacterCardExporter(File output, Character character) {
        this.character = character;
        // Using swing for now
        try {
            Files.copy(new File(character.getIconPath()).toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BufferedImage image;
        try {
            image = ImageIO.read(output);
        } catch (IOException e) {
            App.logger.error("Could load character export output image.", e);
            return;
        }

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
        textEntry.setAttribute("value", Base64.getEncoder().encodeToString(CharacterUtil.toJson(character).toString().getBytes(StandardCharsets.UTF_8)));
        textNode.appendChild(textEntry);

        try {
            metadata.setFromTree(nativeFormat, root);

            try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
                writer.setOutput(stream);
                writer.write(new IIOImage(image, null, metadata));
            } finally {
                writer.dispose();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //        IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
//        textEntry.setAttribute("keyword", key); // The metadata key (e.g., Author, Comment)
//        textEntry.setAttribute("value", value); // The metadata value
//        textNode.appendChild(textEntry);
//
//        metadata.setFromTree(nativeFormat, root);
//
//        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
//            writer.setOutput(stream);
//            writer.write(new IIOImage(image, null, metadata));
//        } finally {
//            writer.dispose();
//        }

    }


}
