package me.piitex.app.views;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;

import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ProgressBarOverlay;
import me.piitex.engine.overlays.TextOverlay;

public class LoadingView extends VerticalLayout {

    public LoadingView(String message, double width, double height) {
        super(width, height);
        setMaxSize(width, height);
        setAlignment(Pos.CENTER);

        TextOverlay textOverlay = new TextOverlay(message);
        addElement(textOverlay);

        ProgressBarOverlay progressBarOverlay = new ProgressBarOverlay();
        addElement(progressBarOverlay);
    }

}
