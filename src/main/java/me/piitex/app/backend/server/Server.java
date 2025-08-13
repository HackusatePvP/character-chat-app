package me.piitex.app.backend.server;

import atlantafx.base.util.BBCodeHandler;
import atlantafx.base.util.BBCodeParser;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
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
        HttpPost post = new HttpPost(baseUrl + "/v1/chat/completions");
        ModelSettings settings = response.getCharacter().getModelSettings();
        JSONObject toPost = new JSONObject();
        toPost.put("stream", true);
        toPost.put("temperature", settings.getTemperature());
        toPost.put("dynatemp_range", settings.getDynamicTempRage());
        toPost.put("dynatemp_exponent", settings.getDynamicExponent());
        toPost.put("top_k", settings.getTopK());
        toPost.put("top_p", settings.getTopP());
        toPost.put("min_p", settings.getMinP());
        toPost.put("xtc_probability", settings.getXtcProbability());
        toPost.put("xtc_threshold", settings.getXtxThreshold());
        toPost.put("typical_p", settings.getTypicalP());
        toPost.put("repeat_last_n", settings.getRepeatTokens());
        toPost.put("repeat_penalty", settings.getRepeatPenalty());
        toPost.put("presence_penalty", settings.getPresencePenalty());
        toPost.put("frequency_penalty", settings.getFrequencyPenalty());
        toPost.put("dry_multiplier", settings.getDryMultiplier());
        toPost.put("dry_base", settings.getDryBase());
        toPost.put("dry_allowed_length", settings.getDryAllowedLength());
        toPost.put("dry_penalty_last_n", settings.getDryPenaltyTokens());

        response.createOAIContext(true);
        toPost.put("messages", response.getMessages());

        post.setEntity(new StringEntity(toPost.toString(), ContentType.APPLICATION_JSON));

        StringBuilder appender = new StringBuilder();

        try (CloseableHttpClient client = HttpClients.createDefault(); CloseableHttpResponse httpResponse = client.execute(post, new HttpClientContext())) {
            Scanner scanner = new Scanner(httpResponse.getEntity().getContent());
            boolean stop = false;
            response.setGenerating(true);
            while (scanner.hasNextLine()) {
                response.setResponse(appender.toString());
                // Check for interruption.
                if (Thread.currentThread().isInterrupted()) {
                    App.logger.info("Response interrupted. Stopping...");
                    Thread.currentThread().interrupt();
                    response.setGenerating(false);
                    response.setHalt(true);
                    client.close();
                    throw new InterruptedException("Response generation was interrupted by user.");
                }

                String content = scanner.nextLine();
                content = content.replaceFirst("data: ", "");

                if (content.startsWith("error")) {
                    App.logger.error("Error occurred: {}", content);
                    break;
                }

                if (response.isHalt()) {
                    App.logger.info("Halting response generation...");
                    break;
                }

                if (content.isEmpty()) continue;
                JSONObject receive = new JSONObject(content);
                if (!receive.has("choices")) {
                    App.logger.error("Error has occurred: {}", receive.toString(1));
                    break;
                }
                JSONArray choices = receive.getJSONArray("choices");
                for (int i = 0; i < choices.length(); i++) {
                    JSONObject arrayObject = choices.getJSONObject(i);
                    if (!arrayObject.has("finish_reason")) continue;
                    String finish = arrayObject.optString("finish_reason", "");
                    if (finish.equalsIgnoreCase("stop")) {
                        response.setGenerating(false);
                        stop = true;
                        break;
                    }
                    JSONObject delta = arrayObject.getJSONObject("delta");
                    String line = delta.optString("content", "");
                    if (line.equalsIgnoreCase("null")) continue;
                    appender.append(line);

                    Platform.runLater(() -> {
                        String updated = appender.toString();

                        // Process any placeholders into the actual value. Example {character} -> character.getDisplayName()
                        updated = Placeholder.formatSymbols(updated);
                        updated = Placeholder.formatPlaceholders(updated, response.getCharacter(), response.getCharacter().getUser());

                        // Apply coloring for roleplay. Yellow around quotes, blue around astrix
                        updated = Placeholder.applyDynamicBBCode(updated);

                        // 1. Check to see if response starts with think tags. <think>
                        // 2. Make sure the think tags haven't ended. Does not contain </think>
                        if (updated.toLowerCase().startsWith("<think>") && !updated.toLowerCase().contains("</think>")) {
                            // This is a think response
                            updated = updated.replace("<think>", "").replace("</think", "").trim();

                            // Check to see if the think view already exists.
                            Element element = chatMessageBox.getElementAt(0);

                            TitledLayout thinkCard;
                            if (element instanceof CardContainer cardContainer) {
                                thinkCard = new ReasoningLayout(chatMessage, CHAT_BOX_IMAGE_WIDTH, CHAT_BOX_HEIGHT);
                                chatMessageBox.addElement(thinkCard, 0);

                                // Set card body to 'thinking'
                                TextFlowOverlay textFlowOverlay = (TextFlowOverlay) cardContainer.getBody();
                                textFlowOverlay.setText("Thinking...");
                            } else {
                                thinkCard = (TitledLayout) chatMessageBox.getElementAt(0);
                                thinkCard.setMaxSize(0, -1);

                                CardContainer cardContainer = (CardContainer) chatMessageBox.getElementAt(1);
                                TextFlowOverlay textFlowOverlay = (TextFlowOverlay) cardContainer.getBody();
                                textFlowOverlay.setText("Thinking...");
                            }

                            if (thinkCard.getElements().isEmpty()) {
                                TextFlowOverlay textFlowOverlay = new TextFlowOverlay(updated, CHAT_BOX_IMAGE_WIDTH, -1);
                                thinkCard.addElement(textFlowOverlay);
                            }

                            if (thinkCard.getElementAt(0) instanceof TextFlowOverlay textFlowOverlay) {
                                if (textFlowOverlay.getText() == null || textFlowOverlay.getText().isEmpty()) {
                                    thinkCard.setExpanded(true);
                                }
                                textFlowOverlay.setText(updated);
                            }

                        } else {
                            // Filter out think tags if possible
                            if (updated.contains("</think>") && updated.split("</think>").length > 1) {

                                if (response.getReasoning() == null) {
                                    TitledLayout thinkCard = (TitledLayout) chatMessageBox.getElementAt(0);
                                    thinkCard.setExpanded(false);
                                    response.setReasoning(updated.replace("\n", "!@!").replace("<think>", "").replace("</think>", ""));
                                }

                                updated = updated.split("</think>")[1];

                                updated = updated.replace("</think>", "").replace("<think>", "");
                            }

                            boolean caught = false;
                            try {
                                BBCodeParser.createFormattedText(updated);
                            } catch (IllegalStateException ignored) {
                                caught = true;
                            } finally {
                                if (!caught) {
                                    TextFlowOverlay textFlowOverlay = (TextFlowOverlay) card.getBody();
                                    textFlowOverlay.setText(updated);
                                }
                            }
                        }

                    });
                }

                if (stop) {
                    break;
                }
            }
        } finally {
            response.setGenerating(false);
        }

        String s = appender.toString();
        s = Placeholder.formatPlaceholders(s, response.getCharacter(), response.getCharacter().getUser());
        if (!App.getInstance().getSettings().isThinkMode()) {
            Pattern pattern = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(appender.toString());
            if (matcher.find()) {
                // Group 1 (.*?) captures the content between the tags.
                s = s.replace(matcher.group(1), "");
                s = s.replace("<think></think>", "");
            }
            if (s.startsWith("null")) {
                s = s.replaceFirst("null", "");
            }
            while (s.startsWith("\n")) {
                s = s.replaceFirst("\n", "");
            }
        }

        s = Placeholder.formatSymbols(s);
        response.setResponse(s);

        return s;
    }

    public static String generateResponseStream(VerticalLayout chatMessageBox, CardContainer card, Response response) throws InterruptedException, IOException {
        App.logger.info("Collecting response from server...");
        HttpPost post = new HttpPost(baseUrl + "/v1/chat/completions");
        ModelSettings settings = response.getCharacter().getModelSettings();
        JSONObject toPost = new JSONObject();
        toPost.put("stream", true);
        toPost.put("temperature", settings.getTemperature());
        toPost.put("min_p", settings.getMinP());
        toPost.put("top_p", settings.getTopP());
        toPost.put("top_k", settings.getTopK());
        toPost.put("repeat_last_n", settings.getRepeatTokens());
        toPost.put("repeat_penalty", settings.getRepeatPenalty());

        response.createOAIContext(false);
        toPost.put("messages", response.getMessages());

        StringBuilder appender = new StringBuilder();

        try (CloseableHttpClient client = HttpClients.createDefault(); CloseableHttpResponse httpResponse = client.execute(post, new HttpClientContext())) {
            Scanner scanner = new Scanner(httpResponse.getEntity().getContent());
            boolean stop = false;
            response.setGenerating(true);

            while (scanner.hasNext()) {
                response.setResponse(appender.toString());
                if (Thread.currentThread().isInterrupted()) {
                    App.logger.info("Response interrupted. Stopping...");
                    Thread.currentThread().interrupt();
                    response.setGenerating(false);
                    response.setHalt(true);
                    client.close();
                    throw new InterruptedException("Response generation was interrupted by user.");
                }

                String content = scanner.nextLine();
                content = content.replaceFirst("data: ", "");

                if (content.startsWith("error")) {
                    App.logger.error("Error occurred: {}", content);
                    break;
                }

                if (response.isHalt()) {
                    App.logger.info("Halting response generation...");
                    break;
                }

                if (content.isEmpty()) continue;
                JSONObject receive = new JSONObject(content);
                if (!receive.has("choices")) {
                    App.logger.error("Error has occurred: {}", receive.toString(1));
                    break;
                }


                JSONArray choices = receive.getJSONArray("choices");
                for (int i = 0; i < choices.length(); i++) {
                    JSONObject arrayObject = choices.getJSONObject(i);
                    if (!arrayObject.has("finish_reason")) continue;
                    String finish = arrayObject.optString("finish_reason", "");
                    if (finish.equalsIgnoreCase("stop")) {
                        response.setGenerating(false);
                        stop = true;
                        break;
                    }
                    JSONObject delta = arrayObject.getJSONObject("delta");
                    String line = delta.optString("content", "");
                    if (line.equalsIgnoreCase("null")) continue;
                    appender.append(line);

                    Platform.runLater(() -> {
                        String updated = appender.toString();

                        // Process any placeholders into the actual value. Example {character} -> character.getDisplayName()
                        updated = Placeholder.formatSymbols(updated);
                        updated = Placeholder.formatPlaceholders(updated, response.getCharacter(), response.getCharacter().getUser());

                        // Apply coloring for roleplay. Yellow around quotes, blue around astrix
                        updated = Placeholder.applyDynamicBBCode(updated);

                        // 1. Check to see if response starts with think tags. <think>
                        // 2. Make sure the think tags haven't ended. Does not contain </think>
                        if (updated.toLowerCase().startsWith("<think>") && !updated.toLowerCase().contains("</think>")) {
                            // This is a think response
                            updated = updated.replace("<think>", "").replace("</think", "").trim();

                            // Check to see if the think view already exists.
                            Element element = chatMessageBox.getElementAt(0);

                            TitledLayout thinkCard;
                            if (element instanceof CardContainer cardContainer) {
                                // Think card does not exist
                                thinkCard = new TitledLayout("Reasoning", chatMessageBox.getWidth() - 50, 0);
                                thinkCard.setMaxSize(chatMessageBox.getMaxWidth() - 50, 0);
                                chatMessageBox.addElement(thinkCard, 0);

                                // Set card body to 'thinking'
                                TextFlowOverlay textFlowOverlay = (TextFlowOverlay) cardContainer.getBody();
                                textFlowOverlay.setText("Thinking...");
                            } else {
                                thinkCard = (TitledLayout) chatMessageBox.getElementAt(0);

                                CardContainer cardContainer = (CardContainer) chatMessageBox.getElementAt(1);

                                TextFlowOverlay textFlowOverlay = (TextFlowOverlay) cardContainer.getBody();
                                textFlowOverlay.setText("Thinking...");
                            }

                            if (thinkCard.getElements().isEmpty()) {
                                int width = CHAT_BOX_WIDTH;
                                TextFlowOverlay textFlowOverlay = new TextFlowOverlay(updated, width - 30, 0);
                                thinkCard.addElement(textFlowOverlay);
                            }

                            TextFlowOverlay textFlowOverlay = (TextFlowOverlay) thinkCard.getElementAt(0);
                            if (textFlowOverlay.getText().isEmpty()) {
                                thinkCard.setExpanded(true);
                            }
                            textFlowOverlay.setText(updated);
                        } else {
                            // Filter out think tags if possible
                            if (updated.contains("</think>") && updated.split("</think>").length > 1) {

                                if (response.getReasoning() == null) {
                                    TitledLayout thinkCard = (TitledLayout) chatMessageBox.getElementAt(0);
                                    thinkCard.setExpanded(false);
                                    response.setReasoning(updated.replace("\n", "!@!").replace("<think>", "").replace("</think>", ""));
                                }

                                updated = updated.split("</think>")[1];

                                updated = updated.replace("</think>", "").replace("<think>", "");
                            }

                            TextFlowOverlay textFlowOverlay = (TextFlowOverlay) card.getBody();
                            textFlowOverlay.setText(updated);
                        }

                    });
                }

                if (stop) {
                    break;
                }
            }
        } finally {
            response.setGenerating(false);
        }

        String s = appender.toString();
        s = Placeholder.formatPlaceholders(s, response.getCharacter(), response.getCharacter().getUser());
        if (!App.getInstance().getSettings().isThinkMode()) {
            Pattern pattern = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(appender.toString());
            if (matcher.find()) {
                // Group 1 (.*?) captures the content between the tags.
                s = s.replace(matcher.group(1), "");
                s = s.replace("<think></think>", "");
            }
            if (s.startsWith("null")) {
                s = s.replaceFirst("null", "");
            }
            while (s.startsWith("\n")) {
                s = s.replaceFirst("\n", "");
            }
        }

        s = Placeholder.formatSymbols(s);
        response.setResponse(s);

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
