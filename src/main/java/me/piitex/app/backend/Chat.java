package me.piitex.app.backend;

import me.piitex.app.App;
import me.piitex.os.configurations.FileCrypter;

import javax.crypto.IllegalBlockSizeException;
import java.io.*;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class Chat {
    private final File file;
    private Response response;
    private final LinkedList<ChatMessage> messages = new LinkedList<>();

    public Chat(File file) {
        this.file = file;
        loadChat();
    }

    private void loadChat() {
        messages.clear();
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    App.logger.warn("Could not create chat file. It may already exist.");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create chat file: " + file.getAbsolutePath(), e);
            }
            return;
        }

        if (file.length() == 0) return;

        File targetFile = file;
        File out = new File(file.getParent(), file.getName() + "out.dat"); // Temporary decrypted file

        try {
            FileCrypter.decryptFile(file, out);
            targetFile = out;
        } catch (IllegalBlockSizeException | IOException e) {
            App.logger.error("Error decrypting chat file: {}", file.getAbsolutePath(), e);
            return; // Stop loading if decryption fails
        }


        // Read the binary data
        try (DataInputStream dis = new DataInputStream(new FileInputStream(targetFile))) {
            int messageCount = dis.readInt(); // Read how many messages are in the file
            AtomicInteger count = new AtomicInteger();

            for (int i = 0; i < messageCount; i++) {
                String roleName = dis.readUTF();
                Role sender = Role.valueOf(roleName.toUpperCase());
                String content = dis.readUTF();

                // Check for image
                String imageUrl = null;
                if (dis.readBoolean()) {
                    imageUrl = dis.readUTF();
                }

                // Check for reasoning
                String reasoning = null;
                if (dis.readBoolean()) {
                    reasoning = dis.readUTF();
                }

                messages.add(new ChatMessage(sender, content, imageUrl, reasoning));
                count.getAndIncrement();
            }
            App.logger.debug("Loaded {} messages", count);

        } catch (java.io.EOFException e) {
            App.logger.warn("Reached unexpected end of file while reading chat data.");
        } catch (IOException | IllegalArgumentException e) {
            App.logger.error("Error reading binary chat file: {}", targetFile.getAbsolutePath(), e);
        } finally {
            try {
                Files.delete(out.toPath());
            } catch (IOException e) {
                App.logger.error("Failed to delete decrypted chat file!", e);
            }
        }
    }

    public File getFile() {
        return file;
    }

    public ChatMessage parseLineToChatMessage(String rawLine) {
        Role sender;
        String contentPart;

        if (rawLine.startsWith("assistant:")) {
            sender = Role.ASSISTANT;
            contentPart = rawLine.substring("assistant:".length());
        } else if (rawLine.startsWith("user:")) {
            sender = Role.USER;
            contentPart = rawLine.substring("user:".length());
        } else {
            App.logger.warn("Unrecognized chat line format: {}", rawLine);
            return null;
        }

        String content;
        String imageUrl = null;
        String reasoning = null;

        int reasoningDelimiterIndex = contentPart.indexOf("!!REASONING!!");
        int imgDelimiterIndex = contentPart.indexOf("!!IMG!!");

        // Handle reasoning if present
        if (reasoningDelimiterIndex != -1) {
            reasoning = contentPart.substring(reasoningDelimiterIndex + "!!REASONING!!".length()).trim();
            // Update contentPart to exclude the reasoning section
            contentPart = contentPart.substring(0, reasoningDelimiterIndex);
            if (reasoning.isBlank()) {
                reasoning = null;
            }
        }

        // Handle image if present in the remaining contentPart
        if (imgDelimiterIndex != -1 && imgDelimiterIndex < contentPart.length()) {
            content = contentPart.substring(0, imgDelimiterIndex).trim();
            imageUrl = contentPart.substring(imgDelimiterIndex + "!!IMG!!".length()).trim();
            if (imageUrl.isBlank()) {
                imageUrl = null;
            }
        } else {
            // If no image, the entire contentPart is the content
            content = contentPart.trim();
        }

        content = content.replace("!@!", "\n");


        return new ChatMessage(sender, content, imageUrl, reasoning);
    }

    public String chatMessageToRawLine(ChatMessage message) {
        String formattedContent = message.getContent().replace("\n", "!@!");
        StringBuilder rawLineBuilder = new StringBuilder();
        rawLineBuilder.append(message.getSender().name().toLowerCase()).append(":");
        rawLineBuilder.append(formattedContent);

        if (message.hasImage()) {
            rawLineBuilder.append("!!IMG!!").append(message.getImageUrl());
        }
        if (message.getReasoning() != null && !message.getReasoning().isEmpty()) {
            rawLineBuilder.append("!!REASONING!!").append(message.getReasoning().replace("\n", "!@!"));
        }
        return rawLineBuilder.toString();
    }


    public ChatMessage addLine(Role role, String content, String imageUrl, String reasoning) {
        ChatMessage newMessage = new ChatMessage(role, content, imageUrl, reasoning);
        messages.add(newMessage);
        update();

        return newMessage;
    }

    public void addLine(ChatMessage chatMessage) {
        messages.add(chatMessage);
        update();
    }

    public void addLine(Role role, String content) {
        addLine(role, content, null, null);
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
                    existingMessage.getImageUrl(),
                    existingMessage.getReasoning()
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
        File targetFile = new File(file.getParent(), "temp_in.dat");

        // Write the binary data
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(targetFile))) {
            dos.writeInt(messages.size()); // Write the total number of messages first

            for (ChatMessage msg : messages) {
                dos.writeUTF(msg.getSender().name()); // Write role (e.g., "USER" or "ASSISTANT")
                dos.writeUTF(msg.getContent());       // Write content (newlines natively supported!)

                // Write image data
                boolean hasImage = msg.hasImage();
                dos.writeBoolean(hasImage);
                if (hasImage) {
                    dos.writeUTF(msg.getImageUrl());
                }

                // Write reasoning data
                boolean hasReasoning = msg.getReasoning() != null && !msg.getReasoning().isBlank();
                dos.writeBoolean(hasReasoning);
                if (hasReasoning) {
                    dos.writeUTF(msg.getReasoning());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing binary chat data to temporary file: " + targetFile.getAbsolutePath(), e);
        }

        FileCrypter.encryptFile(targetFile, file);
        if (targetFile.exists()) {
            try {
                Files.delete(targetFile.toPath());
            } catch (IOException e) {
                App.logger.error("Could not delete temporary chat file during encryption!", e);
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
