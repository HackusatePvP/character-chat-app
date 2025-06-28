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

    public void createContext(boolean oai) throws JSONException, IOException {
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

        LinkedList<String> chatMessages = chat.getLines();
        Collections.reverse(chatMessages);

        LinkedList<String> chatContext = new LinkedList<>();

        for (String s : chatMessages) {
            tokens += Server.tokenize(s);
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
        for (String s : chatContext) {
            JSONObject chatMessageContext = new JSONObject();
            chatMessageContext.put("role", chat.getSender(s).name());
            chatMessageContext.put("content", format(chat.getContent(s), character, user));
            messages.put(chatMessageContext);
        }

        // Open API has the image embedded into the messages (context).
        // /completion has the image into the root object "image_data".
        if (oai) {
            if (image != null) {
                App.logger.info("Fetching character base64...");
                byte[] fileContent = FileUtils.readFileToByteArray(image);

                String iData = Base64.getEncoder().encodeToString(fileContent);

                JSONObject message = new JSONObject();
                message.put("role", "user");
                JSONArray content = new JSONArray();
                JSONObject img = new JSONObject();
                img.put("type", "image_url");
                img.put("image_url", new JSONObject().put("url", "data:image/" + getImageType() + ";base64," + iData));
                JSONObject object = new JSONObject();
                object.put("type", "text");
                object.put("text", format(prompt, character, user));
                content.put(img);
                content.put(object);
                message.put("content", content);
                messages.put(message);
                return; // Don;t process lastMessage as it's already included.
            }
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
