package me.piitex.app.views.setup;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.text.TextAlignment;
import me.piitex.app.App;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.backend.server.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.Positions;
import me.piitex.app.views.creator.CreatorView;
import me.piitex.app.views.creator.characters.CharacterCreator;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.*;
import me.piitex.os.DownloadInfo;
import me.piitex.os.DownloadListener;
import me.piitex.os.FileDownloader;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class SetupModelView extends VerticalLayout {
    private final SetupView parent;
    private final AppSettings appSettings = App.getInstance().getAppSettings();
    private final ServerSettings settings = App.getInstance().getSettings();

    private TextOverlay progressText;

    public SetupModelView(SetupView parent) {
        super(0, 0);
        this.parent = parent;
        setWidth(appSettings.getWidth() - Positions.SIDEBAR_WIDTH - 30);
        setHeight(appSettings.getHeight());
        addStyle(Styles.BG_DEFAULT);
        setAlignment(Pos.CENTER);


        init();
    }

    private void init() {
        TextOverlay header = new TextOverlay("Download Model");
        header.addStyle(Styles.TITLE_1);
        addElement(header);

        TextOverlay desc = new TextOverlay("Please download or install a model.");
        desc.setTextAlignment(TextAlignment.CENTER);
        addElement(desc);

        SeparatorOverlay separatorOverlay = new SeparatorOverlay(Orientation.HORIZONTAL);
        separatorOverlay.setMaxWidth(400);
        addElement(separatorOverlay);

        ButtonOverlay downloadRecommendedModel = new ButtonBuilder("download").setText("Download Recommended Model").addStyle(Styles.ACCENT).addStyle(Styles.BUTTON_OUTLINED).build();
        addElement(downloadRecommendedModel);

        ButtonOverlay openModelsFolder = new ButtonBuilder("open").setText("Open Models Folder").addStyle(Styles.ACCENT).addStyle(Styles.BUTTON_OUTLINED).build();
        addElement(openModelsFolder);
        openModelsFolder.onClick(event -> {
            File directoryToOpen = App.getModelsDirectory();
            if (Desktop.isDesktopSupported() && directoryToOpen.exists() && directoryToOpen.isDirectory()) {
                Platform.runLater(() -> {
                    try {
                        Desktop.getDesktop().open(directoryToOpen);
                    } catch (IOException e) {
                        System.err.println("Failed to open directory: " + e.getMessage());
                    }
                });
            } else {
                System.err.println("Desktop API is not supported or directory is invalid.");
            }
        });

        ButtonOverlay next = new ButtonBuilder("next").setText("Next").addStyle(Styles.SUCCESS).addStyle(Styles.BUTTON_OUTLINED).build();
        addElement(next);
        next.onClick(event -> {
            App.window.clearContainers();
            App.window.addContainer(new CharacterCreator(null));
        });


        downloadRecommendedModel.onClick(event -> {
            // This model will fit any system that meets the minimal specs.
            // https://huggingface.co/mradermacher/L3-8B-Stheno-v3.2-GGUF/resolve/main/L3-8B-Stheno-v3.2.Q5_K_M.gguf?download=true
            ProgressBarOverlay progressBarOverlay = new ProgressBarOverlay();

            App.getThreadPoolManager().submitTask(() -> {
                FileDownloader fileDownloader = new FileDownloader();
                fileDownloader.addDownloadListener(new DownloadListener() {
                    @Override
                    public void onDownloadStart(DownloadInfo info) {
                        App.logger.info("Started model...");
                        Platform.runLater(() -> {
                            removeElement(next);
                            progressBarOverlay.setMaxSize(100, 15);
                            addElement(progressBarOverlay);
                        });
                    }

                    @Override
                    public void onDownloadProgress(DownloadInfo info) {
                        Platform.runLater(() -> {
                            progressBarOverlay.getProgressBar().setProgress(info.getDownloadProgress());
                        });
                    }

                    @Override
                    public void onDownloadComplete(DownloadInfo info, File outputFile) {
                        Platform.runLater(() -> {
                            removeElement(progressBarOverlay);
                            addElement(next);
                        });

                        // Start the server
                        App.reloadModelList();
                        new ServerProcess(App.getModelsByName(outputFile.getName()).getFirst());
                    }

                    @Override
                    public void onDownloadError(DownloadInfo info, Exception e) {
                        // TODO: Prompt an error occurred. Typically caused by network issues.
                    }

                    @Override
                    public void onDownloadCancel(DownloadInfo info) {
                        // Can't be cancelled.
                    }
                });

                fileDownloader.startDownload("https://huggingface.co/mradermacher/L3-8B-Stheno-v3.2-GGUF/resolve/main/L3-8B-Stheno-v3.2.Q5_K_M.gguf?download=true", new File(App.getModelsDirectory(), "L3-8B-Stheno-v3.2.Q5_K_M.gguf"));
            });
        });

    }
}
