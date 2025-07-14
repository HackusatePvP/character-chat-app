package me.piitex.app.views.characters.tabs;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
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
    private final CharacterEditView parentView;

    // UI elements
    private RichTextAreaOverlay firstMessageInput;
    private RichTextAreaOverlay chatScenarioInput;
    private SpinnerNumberOverlay chatContextSpinner;

    private final double TEXT_AREA_WIDTH = 500;
    private final double TEXT_AREA_HEIGHT = 200;

    public ChatTab(AppSettings appSettings, InfoFile infoFile, CharacterEditView parentView) {
        super("Chat");
        this.appSettings = appSettings;
        this.infoFile = infoFile;
        this.parentView = parentView;

        buildChatTabContent(parentView.getChatFirstMessage(), parentView.getChatScenario(), parentView.getChatContextSize());
    }

    private void buildChatTabContent(String chatFirstMessage, String chatScenario, int chatContextSize) {
        this.setWidth(appSettings.getWidth() - 300);
        this.setHeight(appSettings.getHeight());
        int layoutSpacing = 200;

        VerticalLayout layout = new VerticalLayout(appSettings.getWidth() - 300, appSettings.getHeight() - 100);
        layout.setAlignment(Pos.TOP_CENTER);
        this.addElement(layout);

        TextOverlay info = new TextOverlay(new FontIcon(Material2AL.INFO));
        info.setTooltip("Use the following placeholders; {char}, {{char}}, {chara}, {{chara}}, {character}, {{character}}, {user}, {{user}}, {usr}, {{usr}}");
        layout.addElement(info);

        CardContainer firstCard = new CardContainer(0, 0, 0, 0);
        firstCard.setMaxSize(appSettings.getWidth() - 300, TEXT_AREA_HEIGHT);
        layout.addElement(firstCard);

        HorizontalLayout firstBox = new HorizontalLayout(0, TEXT_AREA_HEIGHT);
        firstBox.setMaxSize(appSettings.getWidth() - 300, TEXT_AREA_HEIGHT);
        firstBox.setSpacing(layoutSpacing);
        firstCard.setBody(firstBox);

        TextFlowOverlay firstDesc = new TextFlowOverlay("Set the first message from the assistant.", (appSettings.getWidth() - 300) / 2, 200);
        firstDesc.setTextFillColor(Color.WHITE);
        firstBox.addElement(firstDesc);

        firstMessageInput = new RichTextAreaOverlay(chatFirstMessage, TEXT_AREA_WIDTH, TEXT_AREA_HEIGHT);
        firstMessageInput.setBackgroundColor(appSettings.getThemeDefaultColor(appSettings.getTheme()));
        firstMessageInput.setBorderColor(appSettings.getThemeBorderColor(appSettings.getTheme()));
        firstMessageInput.setTextFill(appSettings.getThemeTextColor(appSettings.getTheme()));
        firstMessageInput.onInputSetEvent(event -> {
            parentView.setChatFirstMessage(event.getInput());
            parentView.warnTokens();
        });
        firstMessageInput.addStyle(Styles.BG_DEFAULT);
        firstMessageInput.addStyle(appSettings.getChatTextSize());
        firstMessageInput.addStyle(Styles.TEXT_ON_EMPHASIS);
        firstBox.addElement(firstMessageInput);

        CardContainer scenarioCard = new CardContainer(0, 0, 0, 0);
        scenarioCard.setMaxSize(appSettings.getWidth() - 300, TEXT_AREA_HEIGHT);
        layout.addElement(scenarioCard);

        HorizontalLayout scenarioBox = new HorizontalLayout(0, TEXT_AREA_HEIGHT);
        scenarioBox.setMaxSize(appSettings.getWidth() - 300, TEXT_AREA_HEIGHT);
        scenarioBox.setSpacing(layoutSpacing);
        scenarioCard.setBody(scenarioBox);

        TextFlowOverlay scenarioDesc = new TextFlowOverlay("Set the chat scenario. Can be used to define the tone or story of the chat.", (appSettings.getWidth() - 300) / 2, 50);
        scenarioDesc.setTextFillColor(Color.WHITE);
        scenarioBox.addElement(scenarioDesc);

        chatScenarioInput = new RichTextAreaOverlay(chatScenario, TEXT_AREA_WIDTH, TEXT_AREA_HEIGHT);
        chatScenarioInput.setBackgroundColor(appSettings.getThemeDefaultColor(appSettings.getTheme()));
        chatScenarioInput.setBorderColor(appSettings.getThemeBorderColor(appSettings.getTheme()));
        chatScenarioInput.setTextFill(appSettings.getThemeTextColor(appSettings.getTheme()));
        scenarioBox.addElement(chatScenarioInput);
        chatScenarioInput.onInputSetEvent(event -> {
            parentView.setChatScenario(event.getInput());
            infoFile.set("scenario", event.getInput());
            parentView.warnTokens();
        });
        chatScenarioInput.addStyle(Styles.BG_DEFAULT);
        chatScenarioInput.addStyle(appSettings.getChatTextSize());
        chatScenarioInput.addStyle(Styles.TEXT_ON_EMPHASIS);

        CardContainer contextCard = new CardContainer(0, 0, 0, 0);
        contextCard.setMaxSize(appSettings.getWidth() - 300, TEXT_AREA_HEIGHT);
        layout.addElement(contextCard);

        HorizontalLayout contextBox = new HorizontalLayout(0, 0);
        contextBox.setAlignment(Pos.BASELINE_LEFT);
        contextBox.setMaxSize(appSettings.getWidth() - 300, TEXT_AREA_HEIGHT);
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

    public RichTextAreaOverlay getFirstMessageInput() {
        return firstMessageInput;
    }

    public RichTextAreaOverlay getChatScenarioInput() {
        return chatScenarioInput;
    }

    public SpinnerNumberOverlay getChatContextSpinner() {
        return chatContextSpinner;
    }
}
