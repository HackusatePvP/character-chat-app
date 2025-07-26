package me.piitex.app.views.chats.components;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import me.piitex.app.App;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.chats.ChatView;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.RichTextAreaOverlay;
import org.fxmisc.richtext.StyledTextArea;

public class SendBox extends VerticalLayout {
    private final RichTextAreaOverlay send;
    private final ButtonOverlay submit;
    private final ChatView parentView;
    private final AppSettings appSettings = App.getInstance().getAppSettings();


    public SendBox(RichTextAreaOverlay send, ButtonOverlay submit, ChatView parentView, double width, double height) {
        super(width, height);
        this.send = send;
        this.submit = submit;
        this.parentView = parentView;
        buildSendBox();
    }

    public void buildSendBox() {
        setAlignment(Pos.CENTER);


        addElement(parentView.buildTopControls());

        HorizontalLayout bottom = new HorizontalLayout(getWidth(), getHeight());
        bottom.setSpacing(20);
        bottom.setMaxSize(getWidth(), getHeight());

        addElement(bottom);

        send.setBackgroundColor(appSettings.getThemeDefaultColor(appSettings.getTheme()));
        send.setBorderColor(appSettings.getThemeBorderColor(appSettings.getTheme()));
        send.setTextFill(appSettings.getThemeTextColor(appSettings.getTheme()));

        send.addStyle(Styles.BG_DEFAULT);
        send.addStyle(appSettings.getChatTextSize());
        send.addStyle(Styles.TEXT_ON_EMPHASIS);
        bottom.addElement(send);

        submit.addStyle(Styles.ACCENT);
        submit.setY(25);
        submit.setWidth(100);
        submit.setHeight(50);
        bottom.addElement(submit);

        send.onSubmit(event -> {
            // Handle submit action. Send the input to the model and generate a response
            StyledTextArea textArea = (StyledTextArea) send.getNode();
            parentView.handleSubmit(textArea.getText().stripTrailing());
            textArea.replaceText("");
        });

        submit.onClick(event -> {
            StyledTextArea textArea = (StyledTextArea) send.getNode();
            parentView.handleSubmit(textArea.getText());
            textArea.replaceText("");
        });

        // onRender will prevent race conditions regarding the ui.
        // It will ensure everything is converted to JavaFX when called.
        addRenderEvent(event -> {
            Platform.runLater(parentView::checkServer);
        });
    }
}
