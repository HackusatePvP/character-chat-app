package me.piitex.app.utils;

import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.Chat;
import me.piitex.app.backend.ChatMessage;
import me.piitex.os.configurations.FileCrypter;

import javax.crypto.IllegalBlockSizeException;
import java.io.*;
import java.nio.file.Files;

public class ChatUtil {

    public static void importChat(Character character, File file) throws IOException, IllegalBlockSizeException {
        // Imports an unencrypted chat file.
        File temp = Files.createTempFile(null, null).toFile();
        FileCrypter.decryptFile(file, temp, "abcdefghijklmnop");

        Chat chat = new Chat(new File(character.getChatDirectory(), "import-" + file.getName()));

        BufferedReader reader = new BufferedReader(new FileReader(temp));
        while (reader.ready()) {
            String line = reader.readLine();
            ChatMessage message = chat.parseLineToChatMessage(line);
            chat.addLine(message);
        }
        chat.update();

        character.getChats().add(chat);
    }

    public static void exportChat(Chat chat, File output) throws IOException {
        App.logger.info("Exporting '{}' to '{}'", chat.getFile().getAbsolutePath(), output.getAbsolutePath());
        File temp = Files.createTempFile(null, null).toFile();

        FileWriter writer = new FileWriter(temp, false);
        for (ChatMessage msg : chat.getMessages()) {
            writer.write(chat.chatMessageToRawLine(msg) + "\n");
        }
        writer.close();

        // Encrypt the file with a basic string.
        // Privacy is very important
        FileCrypter.encryptFile(temp, output, "abcdefghijklmnop");
        App.logger.info("Created encrypted chat file: {}", output.getAbsolutePath());
    }


}
