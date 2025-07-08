package me.piitex.app.backend;

import me.piitex.app.App;
import me.piitex.app.backend.server.Server;
import me.piitex.app.configuration.InfoFile;
import me.piitex.app.configuration.ModelSettings;
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
            responses.put(Integer.valueOf(s), s2);
        });
    }

    public void createContext() throws JSONException {
        App.logger.info("Creating current context tokens...");

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

        LinkedList<ChatMessage> chatMessages = chat.getMessages(); // Now returns ChatMessage objects
        Collections.reverse(chatMessages);

        LinkedList<ChatMessage> chatContext = new LinkedList<>();

        for (ChatMessage s : chatMessages) {
            tokens += Server.tokenize(s.getContent());
            if (tokens < maxTokens) {
                chatContext.add(s);
            } else {
                break;
            }
        }

        character.getLorebook().forEach((s, s2) -> {
            if (prompt.toLowerCase().contains(s.toLowerCase())) {
                if (!messages.toString().contains(s2)) {
                    JSONObject lore = new JSONObject();
                    try {
                        lore.put("role", "system");
                        lore.put("content", format(s2, character, user));
                        messages.put(lore);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        Collections.reverse(chatContext);

        //TODO: Add images to the chat data. That way each message can have an image as context.
        // This would take up significant token size. I believe in the near future, per message image is possible.
        // For now it will only use the last image uploaded.
        int index = 0;
        for (ChatMessage currentChatMessage : chatContext) {
            JSONObject chatMessageContext = new JSONObject();
            chatMessageContext.put("role", currentChatMessage.getSender().name().toLowerCase());
            JSONArray contentArray = new JSONArray();

            JSONObject textPart = new JSONObject();
            textPart.put("type", "text");
            textPart.put("text", format(currentChatMessage.getContent(), character, user));
            contentArray.put(textPart);

            if (image != null && index == chat.getMessages().size() - 1) {
                if (image.exists() && image.isFile()) {
                    App.logger.debug("Processing bas64 data...");
                    try {
                        byte[] fileContent = FileUtils.readFileToByteArray(image);
                        String iData = Base64.getEncoder().encodeToString(fileContent);
                        JSONObject imgPart = new JSONObject();

                        imgPart.put("type", "image_url");
                        imgPart.put("image_url", new JSONObject().put("url", "data:image/" + getImageType() + ";base64," + iData));
                        contentArray.put(imgPart);
                    } catch (IOException e) {
                        App.logger.error("Error reading image file for base64 encoding: {}", image, e);
                    }
                }
            }
            chatMessageContext.put("content", contentArray);
            messages.put(chatMessageContext);
            index++;
        }
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

    public boolean isGenerating() {
        return generating;
    }

    public void setGenerating(boolean generating) {
        this.generating = generating;
    }

    private String format(String line, Character character, User user) {
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
