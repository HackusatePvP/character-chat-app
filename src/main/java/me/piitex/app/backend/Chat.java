package me.piitex.app.backend;

import me.piitex.app.App;
import me.piitex.app.utils.FileCrypter;

import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class Chat {
    private final File file;
    private Response response;
    private final LinkedList<ChatMessage> messages = new LinkedList<>();
    private final boolean dev = false;
    private boolean initialized = false;


    public Chat(File file) {
        this.file = file;
    }

    public void loadChat() {
        messages.clear();
        if (initialized) return;
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create chat file: " + file.getAbsolutePath(), e);
            }
        } else if (file.length() > 0 && !dev) {
            File out = new File(file.getParent(), "out.dat"); // Temporary decrypted file
            try {
                FileCrypter.decryptFile(file, out);
                AtomicInteger count = new AtomicInteger();
                Files.readAllLines(out.toPath()).forEach(rawLine -> {
                    ChatMessage msg = parseLineToChatMessage(rawLine);
                    if (msg != null) {
                        count.getAndIncrement();
                        messages.add(msg);
                    }
                });
                App.logger.debug("Loaded {} messages", count);
            } catch (IOException | IllegalBlockSizeException e) {
                System.err.println("Error decrypting or reading chat file: " + file.getAbsolutePath());
                e.printStackTrace();
            } finally {
                if (out.exists()) {
                    out.delete();
                }
            }
        } else {
            try {
                if (file.length() > 0) {
                    Files.readAllLines(file.toPath()).forEach(rawLine -> {
                        ChatMessage msg = parseLineToChatMessage(rawLine);
                        if (msg != null) {
                            messages.add(msg);
                        }
                    });
                }
            } catch (IOException e) {
                System.err.println("Error reading chat file in dev mode: " + file.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }

    public File getFile() {
        return file;
    }

    private ChatMessage parseLineToChatMessage(String rawLine) {
        Role sender;
        String contentPart;

        if (rawLine.startsWith("assistant:")) {
            sender = Role.ASSISTANT;
            contentPart = rawLine.substring("assistant:".length());
        } else if (rawLine.startsWith("user:")) {
            sender = Role.USER;
            contentPart = rawLine.substring("user:".length());
        } else {
            // Unrecognized format, skip or log an error
            System.err.println("Warning: Unrecognized chat line format: " + rawLine);
            return null; // Or throw an exception
        }
        String content;
        String imageUrl = null;
        int imgDelimiterIndex = contentPart.indexOf("!!IMG!!");

        if (imgDelimiterIndex != -1) {
            content = contentPart.substring(0, imgDelimiterIndex);
            if (imgDelimiterIndex + "!!IMG!!".length() < contentPart.length()) {
                imageUrl = contentPart.substring(imgDelimiterIndex + "!!IMG!!".length());
            }
            if (imageUrl != null && imageUrl.isBlank()) {
                imageUrl = null;
            }
        } else {
            content = contentPart;
        }

        content = content.replace("!@!", "\n");

        return new ChatMessage(sender, content, imageUrl);
    }

    private String chatMessageToRawLine(ChatMessage message) {
        String formattedContent = message.getContent().replace("\n", "!@!");
        StringBuilder rawLineBuilder = new StringBuilder();
        rawLineBuilder.append(message.getSender().name().toLowerCase()).append(":");
        rawLineBuilder.append(formattedContent);

        if (message.hasImage()) {
            rawLineBuilder.append("!!IMG!!").append(message.getImageUrl());
        }
        return rawLineBuilder.toString();
    }


    public ChatMessage addLine(Role role, String content, String imageUrl) {
        ChatMessage newMessage = new ChatMessage(role, content, imageUrl);
        messages.add(newMessage);
        update();

        return newMessage;
    }

    public void addLine(Role role, String content) {
        addLine(role, content, null);
    }

    public LinkedList<ChatMessage> getMessages() {
        return new LinkedList<>(messages);
    }

    public ChatMessage getMessage(int index) {
        if (index >= 0 && index < messages.size()) {
            return messages.get(index);
        }
        return null;
    }

    public void removeMessage(int index) {
        if (index >= 0 && index < messages.size()) {
            messages.remove(index);
            update();
        }
    }

    public ChatMessage replaceMessage(int index, ChatMessage newMessage) {
        if (index >= 0 && index < messages.size()) {
            ChatMessage oldMessage = messages.set(index, newMessage);
            update();
            return oldMessage;
        } else {
            return null;
        }
    }

    public ChatMessage replaceMessageContent(int index, String newContent) {
        ChatMessage existingMessage = getMessage(index);
        if (existingMessage != null) {
            ChatMessage updatedMessage = new ChatMessage(
                    existingMessage.getSender(),
                    newContent, // New content
                    existingMessage.getImageUrl()
            );
            return replaceMessage(index, updatedMessage);
        }
        return null;
    }

    public ChatMessage getLastLine(int currentIndex) {
        int previousIndex = currentIndex - 1;
        return getMessage(previousIndex);
    }

    public void update() {
        File tempIn = new File(file.getParent(), "temp_in.dat");
        if (dev) {
            tempIn = file;
        }
        try (FileWriter writer = new FileWriter(tempIn)) {
            for (ChatMessage msg : messages) {
                writer.write(chatMessageToRawLine(msg) + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing chat data to temporary file: " + tempIn.getAbsolutePath(), e);
        } finally {
            if (!dev) {
                FileCrypter.encryptFile(tempIn, file);
            }
            if (tempIn.exists() && !dev) {
                tempIn.delete();
            }
        }
    }
    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }
}
