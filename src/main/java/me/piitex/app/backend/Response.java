package me.piitex.app.backend;

import me.piitex.app.App;
import me.piitex.app.backend.server.Server;
import me.piitex.app.configuration.InfoFile;
import me.piitex.app.configuration.ModelSettings;
import me.piitex.app.utils.Placeholder;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Response {
    private final int index;
    private String prompt;
    private final Character character;
    private final User user;
    private final Chat chat;
    private final InfoFile responseFile;

    private final Map<Integer, String> responses = new HashMap<>();

    private JSONArray messages = new JSONArray();
    private File image;

    private String response;
    private String reasoning;

    private boolean generating = false;
    private boolean halt = false;

    public Response(int index, String prompt, Character character, User user, Chat chat) {
        this.index = index;
        this.prompt = prompt;
        this.character = character;
        this.user = user;
        this.chat = chat;
        File dir = new File(getResponseDirectory(), chat.getFile().getName());
        dir.mkdirs();
        File file = new File(dir, index + ".info");

        responseFile = new InfoFile(file, true); // False for testing switch later.

        responseFile.getEntryMap().forEach((s, s2) -> {
            //System.out.println("S: " + s);
            //responses.put(Integer.valueOf(s), s2);
        });
    }

    public void createOAIContext(boolean oai) throws JSONException {
        messages = new JSONArray();

        int tokens = 0;
        int maxTokens = character.getChatContext();

        ModelSettings settings = character.getModelSettings();
        JSONObject modelInstructions = new JSONObject();
        modelInstructions.put("role", "system");
        modelInstructions.put("content", format(settings.getModelInstructions(), character, user));
        messages.put(modelInstructions);
        tokens += Server.tokenize(settings.getModelInstructions());

        if (!character.getPersona().isEmpty()) {
            JSONObject characterPersona = new JSONObject();
            characterPersona.put("role", "system");
            characterPersona.put("content", format(character.getPersona(), character, user));
            messages.put(characterPersona);
            tokens += Server.tokenize(character.getPersona());
        }

        if (!user.getPersona().isEmpty()) {
            JSONObject userPersona = new JSONObject();
            userPersona.put("role", "system");
            userPersona.put("content", format(user.getPersona(), character, user));
            messages.put(userPersona);
            tokens += Server.tokenize(user.getPersona());
        }

        if (!character.getChatScenario().isEmpty()) {
            JSONObject chatScenario = new JSONObject();
            chatScenario.put("role", "system");
            chatScenario.put("content", format(character.getChatScenario(), character, user));
            messages.put(chatScenario);
            tokens += Server.tokenize(character.getChatScenario());
        }

        List<String> processedLores = new ArrayList<>();
        List<String> loreItems = new ArrayList<>(character.getLorebook().keySet());
        loreItems.addAll(user.getLorebook().keySet());


        LinkedList<ChatMessage> chatMessages = chat.getMessages(); // Now returns ChatMessage objects
        Collections.reverse(chatMessages);

        LinkedList<ChatMessage> chatContext = new LinkedList<>();

        for (ChatMessage s : chatMessages) {
            tokens += Server.tokenize(s.getContent());
            if (s.getReasoning() != null) {
                tokens += Server.tokenize(s.getReasoning());
            }
            if (tokens < maxTokens) {
                // Process lore with each chat message
                // Lore works with "keys" which are words.
                // If the "word" is typed add the lore value if it's not already processed.
                // This is poorly optimized as it has multiple nested loops.
                // It does run async but not really a good fix.
                for (String input : s.getContent().split(" ")) {
                    input = input.trim();
                    if (input.isEmpty()) continue;

                    for (String loreEntry : loreItems) {
                        String lore = character.getLorebook().get(loreEntry);
                        if (lore == null || lore.isEmpty()) {
                            lore = user.getLorebook().get(loreEntry);
                        }
                        lore = lore.trim();

                        String[] loreKeys = loreEntry.split(",");

                        boolean process = false;
                        for (String key : loreKeys) {
                            key = key.trim();
                            if (input.toLowerCase().contains(key.toLowerCase())) {
                                process = true;
                                break;
                            }
                        }
                        if (process) {
                            if (!processedLores.contains(loreEntry)) {
                                processedLores.add(loreEntry);
                                JSONObject loreItem = new JSONObject();
                                try {
                                    loreItem.put("role", "system");
                                    loreItem.put("content", format(lore, character, user));
                                    messages.put(loreItem);
                                    tokens += Server.tokenize(lore);
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
                chatContext.add(s);
            } else {
                break;
            }
        }

        StringBuilder appender = new StringBuilder();
        character.getExampleDialogue().forEach((s, s2) -> {
            if (s2.startsWith("{character}:")) {
                s2 = s2.replace("{character}:", "Assistant:");
                s2 = format(s2, character, user);
                appender.append(s2).append("\n");
            }
            if (s2.startsWith("{user}:")) {
                s2 = s2.replace("{user}:", "User:");
                s2 = format(s2, character, user);
                appender.append(s2).append("\n");
            }
        });

        JSONObject exampleDialogue = new JSONObject();
        exampleDialogue.put("role", "system");
        exampleDialogue.put("content", "Use the following format when responding.\n\n" + appender.toString().trim());

        Collections.reverse(chatContext);

        int index = 0;
        for (ChatMessage currentChatMessage : chatContext) {
            JSONObject chatMessageContext = new JSONObject();
            chatMessageContext.put("role", currentChatMessage.getSender().name().toLowerCase());
            JSONArray contentArray = new JSONArray();

            JSONObject textPart = new JSONObject();
            textPart.put("type", "text");
            if (currentChatMessage.getReasoning() != null && !currentChatMessage.getReasoning() .isEmpty()) {
                String reason = "<think> " + currentChatMessage.getReasoning()  + " </think> ";
                textPart.put("text", format(reason + currentChatMessage.getContent(), character, user));
            } else {
                textPart.put("text", format(currentChatMessage.getContent(), character, user));
            }
            contentArray.put(textPart);

            //TODO: Add images to the chat data. That way each message can have an image as context.
            // This would take up significant token size. I believe in the near future, per message image is possible.
            // For now it will only use the last image uploaded.
            // Only add images for open ai context.

            if (image != null && index == chat.getMessages().size() - 1) {
                if (image.exists() && image.isFile()) {
                    App.logger.debug("Processing bas64 data...");
                    try {
                        byte[] fileContent = FileUtils.readFileToByteArray(image);
                        String iData = Base64.getEncoder().encodeToString(fileContent);
                        JSONObject imgPart = new JSONObject();

                        if (oai) {
                            imgPart.put("type", "image_url");
                            imgPart.put("image_url", new JSONObject().put("url", "data:image/" + getImageType() + ";base64," + iData));
                            contentArray.put(imgPart);
                        }
                    } catch (IOException e) {
                        App.logger.error("Error reading image file for base64 encoding: {}", image, e);
                    }
                }
            }
            chatMessageContext.put("content", contentArray);
            messages.put(chatMessageContext);
            index++;
        }

        App.logger.info("Gathered {} tokens of context", tokens);
    }

    public int getIndex() {
        return index;
    }

    private String getImageType() {
        if (image == null) {
            return "";
        }
        String fileName = image.getName();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "jpeg";
        }
        if (fileName.endsWith(".png")) {
            return "png";
        }

        return "";
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Character getCharacter() {
        return character;
    }

    public User getUser() {
        return user;
    }

    public Chat getChat() {
        return chat;
    }

    public JSONArray getMessages() {
        return messages;
    }

    public File getImage() {
        return image;
    }

    public void setImage(File image) {
        this.image = image;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public boolean isGenerating() {
        return generating;
    }

    public void setGenerating(boolean generating) {
        this.generating = generating;
    }

    private String format(String line, Character character, User user) {
        line = Placeholder.retrieveOriginalText(line);
        return line.replace("{char}", character.getDisplayName()).replace("{character}", character.getDisplayName())
                .replace("{{char}}", character.getDisplayName()).replace("{{character}}", character.getDisplayName())
                .replace("{user}", user.getDisplayName()).replace("{{user}}", user.getDisplayName());
    }

    public File getResponseDirectory() {
        return new File(character.getCharacterDirectory(), "chats/responses/");
    }

    public Map<Integer, String> getResponses() {
        return responses;
    }

    public boolean isHalt() {
        return halt;
    }

    public void setHalt(boolean halt) {
        this.halt = halt;
    }

    public int getResponseIndex(String response) {
        Optional<Integer> keyOptional = getResponses().entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(response))
                .map(Map.Entry::getKey)
                .findFirst();

        return keyOptional.orElse(-1); // Return -1 if not found
    }

    public void update() {
        responses.forEach((integer, s) -> {
            responseFile.set(integer + "", s);
        });
        responseFile.update();
    }

    public InfoFile getResponseFile() {
        return responseFile;
    }
}
