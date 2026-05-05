package me.piitex.app.backend;

import me.piitex.app.App;
import me.piitex.app.backend.server.Server;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.os.configurations.InfoFile;
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
        if (dir.mkdirs()) {
            App.logger.info("Created response directory '{}'", getResponseDirectory().getAbsolutePath());
        }
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
        int maxTokens;
        if (ServerProcess.getCurrentServer() != null && ServerProcess.getCurrentServer().getModel() != null) {
            maxTokens = ServerProcess.getCurrentServer().getModel().getSettings().getContextSize();
        } else if (App.getInstance().getSettings().getGlobalModel() != null) {
            maxTokens = App.getInstance().getSettings().getGlobalModel().getSettings().getContextSize();
        } else {
            maxTokens = 4096; // Default to standard 4k
        }

        StringBuilder combinedSystemPrompt = new StringBuilder();
        ModelSettings settings = character.getModelSettings();

        // 2. Append Model Instructions
        combinedSystemPrompt.append(format(settings.getModelInstructions(), character, user)).append("\n\n");
        tokens += Server.tokenize(settings.getModelInstructions());

        if (!character.getPersona().isEmpty()) {
            combinedSystemPrompt.append(format(character.getPersona(), character, user)).append("\n\n");
            tokens += Server.tokenize(character.getPersona());
        }

        if (!user.getPersona().isEmpty()) {
            combinedSystemPrompt.append(format(user.getPersona(), character, user)).append("\n\n");
            tokens += Server.tokenize(user.getPersona());
        }

        if (!character.getChatScenario().isEmpty()) {
            combinedSystemPrompt.append(format(character.getChatScenario(), character, user)).append("\n\n");
            tokens += Server.tokenize(character.getChatScenario());
        }

        List<String> processedLores = new ArrayList<>();
        List<String> loreItems = new ArrayList<>(character.getLorebook().keySet());
        loreItems.addAll(user.getLorebook().keySet());

        LinkedList<ChatMessage> chatMessages = chat.getMessages();
        Collections.reverse(chatMessages);

        LinkedList<ChatMessage> chatContext = new LinkedList<>();

        for (ChatMessage s : chatMessages) {
            tokens += Server.tokenize(s.getContent());
            if (s.getReasoning() != null) {
                tokens += Server.tokenize(s.getReasoning());
            }
            if (tokens < maxTokens) {
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

                        if (process && !processedLores.contains(loreEntry)) {
                            processedLores.add(loreEntry);
                            // Append Lore to the combined system prompt
                            combinedSystemPrompt.append(format(lore, character, user)).append("\n\n");
                            tokens += Server.tokenize(lore);
                        }
                    }
                }
                chatContext.add(s);
            } else {
                break;
            }
        }

        StringBuilder exampleAppender = new StringBuilder();
        character.getExampleDialogue().forEach((s, s2) -> {
            if (s2.startsWith("{character}:")) {
                s2 = s2.replace("{character}:", "Assistant:");
                s2 = format(s2, character, user);
                exampleAppender.append(s2).append("\n");
            }
            if (s2.startsWith("{user}:")) {
                s2 = s2.replace("{user}:", "User:");
                s2 = format(s2, character, user);
                exampleAppender.append(s2).append("\n");
            }
        });

        if (!exampleAppender.isEmpty()) {
            combinedSystemPrompt.append("Use the following format when responding.\n\n")
                    .append(exampleAppender.toString().trim());
        }

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", combinedSystemPrompt.toString().trim());
        messages.put(systemMessage);

        Collections.reverse(chatContext);
        int index = 0;

        for (ChatMessage currentChatMessage : chatContext) {
            JSONObject chatMessageContext = new JSONObject();
            chatMessageContext.put("role", currentChatMessage.getSender().name().toLowerCase());
            JSONArray contentArray = new JSONArray();

            JSONObject textPart = new JSONObject();
            textPart.put("type", "text");
            if (currentChatMessage.getReasoning() != null && !currentChatMessage.getReasoning().isEmpty()) {
                String reason = "<think> " + currentChatMessage.getReasoning()  + " </think> ";
                textPart.put("text", format(reason + currentChatMessage.getContent(), character, user));
            } else {
                textPart.put("text", format(currentChatMessage.getContent(), character, user));
            }
            contentArray.put(textPart);

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
