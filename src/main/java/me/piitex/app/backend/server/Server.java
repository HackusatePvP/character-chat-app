package me.piitex.app.backend.server;

import atlantafx.base.util.BBCodeParser;
import javafx.application.Platform;
import me.piitex.app.App;
import me.piitex.app.backend.ChatMessage;
import me.piitex.app.backend.Response;
import me.piitex.app.configuration.ModelSettings;
import me.piitex.app.utils.Placeholder;
import me.piitex.app.views.chats.components.ReasoningLayout;
import me.piitex.engine.Element;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.layouts.TitledLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.TextFlowOverlay;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.piitex.app.views.Positions.*;

public class Server {
    private static final String baseUrl = "http://localhost:8187";

    public static String getHealth() throws JSONException {
        HttpGet get = new HttpGet(baseUrl + "/health");

        JSONObject object;
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(get, new HttpClientContext())
        ) {
            String content = EntityUtils.toString(response.getEntity());
            object = new JSONObject(content);
        } catch (IOException | ParseException | JSONException e) {
            object = new JSONObject("{\"status\": \"error\"}");
        }

        if (object.has("error")) {
            return object.getJSONObject("error").getString("message");
        }

        return object.getString("status");
    }

    public static String generateResponseOAIStream(ChatMessage chatMessage, VerticalLayout chatMessageBox, CardContainer card, Response response) throws JSONException, IOException, InterruptedException {

        App.logger.info("Collecting response from server...");

        // Prepare request
        HttpPost post = prepareOAIStreamRequest(response);

        StringBuilder responseAppender = new StringBuilder();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            response.setGenerating(true);
            // Execute and process stream
            executeAndProcessStream(client, post, chatMessage, chatMessageBox, card, response, responseAppender);
        } finally {
            // Set the response status to generating
            response.setGenerating(false);
        }

        // Cleanup processes
        return postProcessResponse(responseAppender.toString(), response);
    }

    /**
     * Prepares the HttpPost object with all necessary OAI settings and messages.
     */
    private static HttpPost prepareOAIStreamRequest(Response response) throws JSONException {
        HttpPost post = new HttpPost(baseUrl + "/v1/chat/completions");
        ModelSettings settings = response.getCharacter().getModelSettings();
        JSONObject toPost = new JSONObject();

        // Method to add all settings to the JSON object
        addModelSettingsToJSON(toPost, settings);

        response.createOAIContext(true);
        toPost.put("messages", response.getMessages());

        post.setEntity(new StringEntity(toPost.toString(), ContentType.APPLICATION_JSON));
        return post;
    }

    /**
     * Helper to consolidate all model settings into the {@link JSONObject}.
     */
    private static void addModelSettingsToJSON(JSONObject toPost, ModelSettings settings) throws JSONException {
        toPost.put("stream", true);
        toPost.put("temperature", settings.getTemperature());
        toPost.put("dynatemp_range", settings.getDynamicTempRage());
        toPost.put("dynatemp_exponent", settings.getDynamicExponent());
        toPost.put("top_k", settings.getTopK());
        toPost.put("top_p", settings.getTopP());
        toPost.put("min_p", settings.getMinP());
        toPost.put("xtc_probability", settings.getXtcProbability());
        toPost.put("xtc_threshold", settings.getXtcThreshold());
        toPost.put("typical_p", settings.getTypicalP());
        toPost.put("repeat_last_n", settings.getRepeatTokens());
        toPost.put("repeat_penalty", settings.getRepeatPenalty());
        toPost.put("presence_penalty", settings.getPresencePenalty());
        toPost.put("frequency_penalty", settings.getFrequencyPenalty());
        toPost.put("dry_multiplier", settings.getDryMultiplier());
        toPost.put("dry_base", settings.getDryBase());
        toPost.put("dry_allowed_length", settings.getDryAllowedLength());
        toPost.put("dry_penalty_last_n", settings.getDryPenaltyTokens());
    }

    /**
     * Executes the HTTP request and processes the incoming streaming response line by line.
     */
    private static void executeAndProcessStream(CloseableHttpClient client, HttpPost post, ChatMessage chatMessage, VerticalLayout chatMessageBox, CardContainer card, Response response,
            StringBuilder appender) throws IOException, InterruptedException, JSONException {

        try (CloseableHttpResponse httpResponse = client.execute(post, new HttpClientContext());
             Scanner scanner = new Scanner(httpResponse.getEntity().getContent())) {

            boolean stopGenerating = false;
            while (scanner.hasNextLine()) {
                response.setResponse(appender.toString());

                // Check for interruption and halts early
                if (handleInterruption(response)) {
                    throw new InterruptedException("Response generation was interrupted by user.");
                }
                if (response.isHalt()) {
                    App.logger.info("Halting response generation...");
                    break;
                }

                String content = scanner.nextLine().replaceFirst("data: ", "");

                if (content.startsWith("error")) {
                    App.logger.error("Error occurred: {}", content);
                    break;
                }

                if (content.isEmpty()) continue;

                // Process the JSON chunk
                stopGenerating = processStreamChunk(content, appender, chatMessage, chatMessageBox, card, response);

                if (stopGenerating) {
                    break;
                }
            }
        }
    }

    /**
     * Handles checking for thread interruption and sets state.
     * @return true if the thread was interrupted.
     */
    private static boolean handleInterruption(Response response) {
        if (Thread.currentThread().isInterrupted()) {
            App.logger.info("Response interrupted. Stopping...");
            Thread.currentThread().interrupt();
            response.setGenerating(false);
            response.setHalt(true);
            return true;
        }
        return false;
    }

    /**
     * Parses a single stream chunk and updates the response and UI.
     * @return true if a 'stop' reason was received.
     */
    private static boolean processStreamChunk(String content, StringBuilder appender, ChatMessage chatMessage, VerticalLayout chatMessageBox, CardContainer card, Response response) throws JSONException {

        JSONObject receive = new JSONObject(content);
        if (!receive.has("choices")) {
            App.logger.error("Error has occurred: {}", receive.toString(1));
            return true; // Stop on unexpected format
        }

        JSONArray choices = receive.getJSONArray("choices");
        for (int i = 0; i < choices.length(); i++) {
            JSONObject arrayObject = choices.getJSONObject(i);
            String finish = arrayObject.optString("finish_reason", "");
            if (finish.equalsIgnoreCase("stop")) {
                response.setGenerating(false);
                return true; // Stop generation
            }

            JSONObject delta = arrayObject.getJSONObject("delta");
            String line = delta.optString("content", "");
            if (line.equalsIgnoreCase("null")) continue;

            appender.append(line);

            // Perform UI actions to notify the user of response progress
            updateUIOnStream(appender.toString(), chatMessage, chatMessageBox, card, response);
        }
        return false; // Continue generation
    }

    /**
     * Executes UI updates on the JavaFX Platform thread.
     */
    private static void updateUIOnStream(String currentResponse, ChatMessage chatMessage, VerticalLayout chatMessageBox, CardContainer card, Response response) {

        Platform.runLater(() -> {
            String updated = currentResponse;

            // Format placeholders and BBCode
            updated = formatResponseText(updated, response);

            // Handle 'Think' tags for the reasoning layout
            if (updated.toLowerCase().startsWith("<think>") && !updated.toLowerCase().contains("</think>")) {
                handleThinkTagStart(updated, chatMessage, chatMessageBox, response);
            } else {
                // Handle 'Think' tag cleanup and final display
                handleThinkTagCleanupAndDisplay(updated, chatMessageBox, card, response);
            }
        });
    }

    /**
     * Formats placeholders and applies dynamic BBCode coloring to the response text.
     */
    private static String formatResponseText(String updated, Response response) {
        // Process any placeholders into the actual value. Example {character} -> character.getDisplayName()
        updated = Placeholder.formatSymbols(updated);
        updated = Placeholder.formatPlaceholders(updated, response.getCharacter(), response.getCharacter().getUser());

        // Apply coloring for roleplay. Yellow around quotes, blue around astrix
        updated = Placeholder.applyDynamicBBCode(updated);
        return updated;
    }

    /**
     * Handles the logic for displaying the 'thinking' message and updating the reasoning card
     * when the response is inside the <think> block.
     */
    private static void handleThinkTagStart(String updated, ChatMessage chatMessage, VerticalLayout chatMessageBox, Response response) {

        // This is a think response
        updated = updated.replace("<think>", "").replace("</think", "").trim();

        // Check to see if the think view already exists.
        Element element = chatMessageBox.getElementAt(0);

        TitledLayout thinkCard;
        if (element instanceof CardContainer cardContainer) {
            // If the first element is the main chat card, insert the think card above it.
            thinkCard = new ReasoningLayout(chatMessage, CHAT_BOX_IMAGE_WIDTH, CHAT_BOX_HEIGHT);
            chatMessageBox.addElement(thinkCard, 0);

            // Set card body to 'thinking' (This targets the main response card, not the think card)
            TextFlowOverlay textFlowOverlay = (TextFlowOverlay) cardContainer.getBody();
            textFlowOverlay.setText("Thinking...");
        } else {
            // Assume the first element is the existing TitledLayout (think card)
            thinkCard = (TitledLayout) chatMessageBox.getElementAt(0);
            thinkCard.setMaxSize(0, -1);

            CardContainer cardContainer = (CardContainer) chatMessageBox.getElementAt(1); // Main response card is now at index 1
            TextFlowOverlay textFlowOverlay = (TextFlowOverlay) cardContainer.getBody();
            textFlowOverlay.setText("Thinking...");
        }

        // Update the content of the think card
        if (thinkCard.getElements().isEmpty()) {
            TextFlowOverlay textFlowOverlay = new TextFlowOverlay(updated, CHAT_BOX_IMAGE_WIDTH, -1);
            thinkCard.addElement(textFlowOverlay);
        }

        if (thinkCard.getElementAt(0) instanceof TextFlowOverlay textFlowOverlay) {
            if (textFlowOverlay.getText() == null || textFlowOverlay.getText().isEmpty()) {
                thinkCard.setExpanded(true); // Auto-expand if content starts
            }
            textFlowOverlay.setText(updated);
        }
    }

    /**
     * Handles the logic for exiting the <think> block, collapsing the reasoning card,
     * and updating the final response card body.
     */
    private static void handleThinkTagCleanupAndDisplay(
            String updated, VerticalLayout chatMessageBox, CardContainer card, Response response) {

        // Check if the </think> closing tag has appeared and there is content after it
        if (updated.contains("</think>") && updated.split("</think>").length > 1) {

            // Save the reasoning content before stripping the tags
            if (response.getReasoning() == null) {
                TitledLayout thinkCard = (TitledLayout) chatMessageBox.getElementAt(0);
                thinkCard.setExpanded(false); // Collapse the reasoning card

                // Save the content for post-processing/storage
                String reasoningContent = updated.replace("\n", "!@!").replace("<think>", "").replace("</think>", "");
                response.setReasoning(reasoningContent);
            }

            // Get the actual response part (after </think>)
            updated = updated.split("</think>")[1];

            // Ensure any stray or malformed tags are gone from the final response part
            updated = updated.replace("</think>", "").replace("<think>", "");
        }

        // Final display of the response in the main chat card
        boolean caught = false;
        try {
            // Check if the current BBCode is valid before attempting to render
            BBCodeParser.createFormattedText(updated);
        } catch (IllegalStateException ignored) {
            caught = true; // BBCode error (e.g., unclosed tag)
        } finally {
            if (!caught) {
                TextFlowOverlay textFlowOverlay = (TextFlowOverlay) card.getBody();
                textFlowOverlay.setText(updated); // Update the main response card
            }
        }
    }

    /**
     * Cleans up and finalizes the complete response string.
     */
    private static String postProcessResponse(String rawResponse, Response response) {
        String finalResponse = Placeholder.formatPlaceholders(rawResponse, response.getCharacter(), response.getCharacter().getUser());

        // Remove <think> tags if ThinkMode is off
        if (!App.getInstance().getSettings().isThinkMode()) {
            finalResponse = removeThinkTags(finalResponse);
        }

        // Clean up leading newlines and "null" prefixes
        finalResponse = cleanUpResponsePrefix(finalResponse);

        finalResponse = Placeholder.formatSymbols(finalResponse);
        response.setResponse(finalResponse);

        return finalResponse;
    }

    /**
     * Utility to remove <think> tags and their content.
     */
    private static String removeThinkTags(String s) {
        Pattern pattern = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            s = s.replace(matcher.group(1), "");
            s = s.replace("<think></think>", "");
        }
        return s;
    }

    /**
     * Utility to remove "null" prefix and leading newlines.
     */
    private static String cleanUpResponsePrefix(String s) {
        if (s.startsWith("null")) {
            s = s.replaceFirst("null", "");
        }
        while (s.startsWith("\n")) {
            s = s.replaceFirst("\n", "");
        }
        return s;
    }

    /**
     * Returns the token length of the given string. Useful for calculating context.
     * @param string Prompt to be calculated.
     * @return The array length of tokens.
     * @throws JSONException Improper json parsing.
     */
    public static int tokenize(String string) throws JSONException {
        // "content": "Content"
        HttpPost post = new HttpPost(baseUrl + "/tokenize");
        JSONObject toPost = new JSONObject();
        toPost.put("content", string);
        post.setEntity(new StringEntity(toPost.toString(), ContentType.APPLICATION_JSON));

        JSONObject object;
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post, new HttpClientContext())
        ) {
            String content = EntityUtils.toString(response.getEntity());
            object = new JSONObject(content);
        } catch (IOException | ParseException | JSONException e) {
            object = new JSONObject("{\"status\": \"error\"}");
        }

        if (object.has("error") || (object.has("status") && object.getString("status").equalsIgnoreCase("error"))) {
            return 0;
        }
        JSONArray array = object.getJSONArray("tokens");
        return array.length();
    }
}
