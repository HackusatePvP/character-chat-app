package me.piitex.app.views.characters.tabs;

import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.User;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.configuration.InfoFile;
import me.piitex.app.views.characters.CharacterEditView;
import me.piitex.engine.containers.CardContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;


public class ChatTab extends Tab {

    private final AppSettings appSettings;
    private final InfoFile infoFile;
    private final ServerSettings serverSettings;
    private final CharacterEditView parentView;
    private final Character character;

    // UI elements
    private TextAreaOverlay firstMessageInput;
    private TextAreaOverlay chatScenarioInput;
    private SpinnerNumberOverlay chatContextSpinner;

    public ChatTab(AppSettings appSettings, InfoFile infoFile, ServerSettings serverSettings, Character character, User user, String chatFirstMessage, String chatScenario, int chatContextSize, CharacterEditView parentView) {
        super("Chat");
        this.appSettings = appSettings;
        this.infoFile = infoFile;
        this.serverSettings = serverSettings;
        this.character = character;
        this.parentView = parentView;

        buildChatTabContent(chatFirstMessage, chatScenario, chatContextSize);
    }

    private void buildChatTabContent(String chatFirstMessage, String chatScenario, int chatContextSize) {
        int layoutSpacing = 200;

        VerticalLayout layout = new VerticalLayout(appSettings.getWidth() - 300, appSettings.getHeight() - 100);
        layout.setAlignment(Pos.TOP_CENTER);
        this.addElement(layout);

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Use the following placeholders; {char}, {{char}}, {chara}, {{chara}}, {character}, {{character}}, {user}, {{user}}, {usr}, {{usr}}");
        layout.addElement(info);

        CardContainer firstCard = new CardContainer(0, 0, 0, 0);
        firstCard.setMaxSize(appSettings.getWidth() - 300, 200);
        layout.addElement(firstCard);

        HorizontalLayout firstBox = new HorizontalLayout(0, 0);
        firstBox.setAlignment(Pos.BASELINE_LEFT);
        firstBox.setMaxSize(appSettings.getWidth() - 300, 120);
        firstBox.setSpacing(layoutSpacing);
        firstCard.setBody(firstBox);

        TextFlowOverlay firstDesc = new TextFlowOverlay("Set the first message from the assistant.", (appSettings.getWidth() - 300) / 2, 200);
        firstDesc.setTextFillColor(Color.WHITE);
        firstBox.addElement(firstDesc);

        firstMessageInput = new TextAreaOverlay(chatFirstMessage, 0, 0, 800, 400);
        firstBox.addElement(firstMessageInput);
        firstMessageInput.onInputSetEvent(event -> {
            parentView.setChatFirstMessage(event.getInput());
            infoFile.set("first-message", event.getInput());
            parentView.warnTokens();
        });

        CardContainer scenarioCard = new CardContainer(0, 0, 0, 0);
        scenarioCard.setMaxSize(appSettings.getWidth() - 300, 200);
        layout.addElement(scenarioCard);

        HorizontalLayout scenarioBox = new HorizontalLayout(0, 0);
        scenarioBox.setAlignment(Pos.BASELINE_LEFT);
        scenarioBox.setMaxSize(appSettings.getWidth() - 300, 120);
        scenarioBox.setSpacing(layoutSpacing);
        scenarioCard.setBody(scenarioBox);

        TextFlowOverlay scenarioDesc = new TextFlowOverlay("Set the chat scenario. Can be used to define the tone or story of the chat.", (appSettings.getWidth() - 300) / 2, 50);
        scenarioDesc.setTextFillColor(Color.WHITE);
        scenarioBox.addElement(scenarioDesc);

        chatScenarioInput = new TextAreaOverlay(chatScenario, 0, 0, 800, 400);
        scenarioBox.addElement(chatScenarioInput);
        chatScenarioInput.onInputSetEvent(event -> {
            parentView.setChatScenario(event.getInput());
            infoFile.set("scenario", event.getInput());
            parentView.warnTokens();
        });

        CardContainer contextCard = new CardContainer(0, 0, 0, 0);
        contextCard.setMaxSize(appSettings.getWidth() - 300, 50);
        layout.addElement(contextCard);

        HorizontalLayout contextBox = new HorizontalLayout(0, 0);
        contextBox.setAlignment(Pos.BASELINE_LEFT);
        contextBox.setMaxSize(appSettings.getWidth() - 300, 50);
        contextBox.setSpacing(layoutSpacing);
        contextCard.setBody(contextBox);

        TextFlowOverlay contextDesc = new TextFlowOverlay("Set the chat context size.", (appSettings.getWidth() - 300) / 2, 50);
        contextDesc.setTextFillColor(Color.WHITE);
        contextBox.addElement(contextDesc);

        chatContextSpinner = new SpinnerNumberOverlay(1024, Integer.MAX_VALUE, chatContextSize);
        contextBox.addElement(chatContextSpinner);
        chatContextSpinner.onValueChange(event -> {
            int newContextSize = (int) event.getNewValue();
            parentView.setChatContextSize(newContextSize);
            infoFile.set("chat-context", newContextSize);
            parentView.warnTokens();
        });

        this.addElement(parentView.buildSubmitBox());
    }

    public TextAreaOverlay getFirstMessageInput() {
        return firstMessageInput;
    }

    public TextAreaOverlay getChatScenarioInput() {
        return chatScenarioInput;
    }

    public SpinnerNumberOverlay getChatContextSpinner() {
        return chatContextSpinner;
    }
}
