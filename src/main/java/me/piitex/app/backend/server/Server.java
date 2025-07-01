package me.piitex.app.backend.server;

import atlantafx.base.controls.Card;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.TextFlow;
import me.piitex.app.App;
import me.piitex.app.backend.ChatMessage;
import me.piitex.app.backend.Response;
import me.piitex.app.backend.Role;
import me.piitex.app.configuration.ModelSettings;
import me.piitex.app.utils.Placeholder;
import me.piitex.app.views.chats.ChatView;
import me.piitex.engine.hanlders.events.OverlayClickEvent;
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

    public static String generateResponseOAIStream(ScrollPane scrollPane, Card card, Response response) throws JSONException, IOException, InterruptedException {
        App.logger.info("Generating response from server...");
        HttpPost post = new HttpPost(baseUrl + "/v1/chat/completions");
        ModelSettings settings = response.getCharacter().getModelSettings();
        JSONObject toPost = new JSONObject();
        toPost.put("stream", true);
        toPost.put("temperature", settings.getTemperature());
        toPost.put("min_p", settings.getMinP());
        toPost.put("repeat_last_n", settings.getRepeatTokens());
        toPost.put("repeat_penalty", settings.getRepeatPenalty());
        //TODO: Add more sampling parameters!

        response.createContext();
        toPost.put("messages", response.getMessages());

        post.setEntity(new StringEntity(toPost.toString(), ContentType.APPLICATION_JSON));

        StringBuilder appender = new StringBuilder();

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse httpResponse = client.execute(post, new HttpClientContext())
        ) {
            Scanner scanner = new Scanner(httpResponse.getEntity().getContent());

            boolean stop = false;
            response.setGenerating(true);
            while (scanner.hasNextLine()) {
                response.setResponse(appender.toString());
                // Check for interruption.
                if (Thread.currentThread().isInterrupted()) {
                    App.logger.info("Response interrupted. Stopping...");
                    // Re-set the interrupted status and throw InterruptedException
                    Thread.currentThread().interrupt(); // Restore interrupted status
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
                        updated = Placeholder.formatPlaceholders(updated, response.getCharacter(), response.getCharacter().getUser());

                        // Apply coloring for roleplay. Yellow around quotes, blue around astrix
                        updated = Placeholder.applyDynamicBBCode(updated);

                        TextFlowOverlay textFlow = ChatView.buildTextFlow(new ChatMessage(Role.ASSISTANT, updated, null), response.getChat(), response.getIndex());

                        TextFlow fxFlow = (TextFlow) textFlow.render();
                        textFlow.setNode(fxFlow);
                        fxFlow.setOnMouseClicked(event -> {
                            if (textFlow.getOnClick() != null) {
                                textFlow.getOnClick().onClick(new OverlayClickEvent(textFlow, event));
                            }
                        });

                        card.setBody(fxFlow);
                        fxFlow.widthProperty().addListener(observable -> {
                            if (scrollPane.getVvalue() >= 0.93) {
                                scrollPane.setVvalue(1);
                            }
                        });

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
            Pattern pattern = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL); // Ty Gemma
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
