package me.piitex.app.views.setup;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.app.configuration.ServerSettings;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.views.Positions;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.RingProgressOverlay;
import me.piitex.engine.overlays.SeparatorOverlay;
import me.piitex.engine.overlays.TextOverlay;
import me.piitex.os.DownloadInfo;
import me.piitex.os.DownloadListener;
import me.piitex.os.FileDownloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public class SetupRunnerView extends VerticalLayout {
    private final SetupView parent;
    private final AppSettings appSettings = App.getInstance().getAppSettings();
    private final ServerSettings settings = App.getInstance().getSettings();

    private TextOverlay progressText;

    public SetupRunnerView(SetupView parent) {
        super(0, 0);
        this.parent = parent;
        setWidth(appSettings.getWidth() - Positions.SIDEBAR_WIDTH - 30);
        setHeight(appSettings.getHeight());
        addStyle(Styles.BG_DEFAULT);
        setAlignment(Pos.CENTER);


        init();

        App.getThreadPoolManager().submitTask(this::startRunner);
    }

    private void init() {
        // The runner will test to make sure the system works. It will go through numerous automated steps.
        // 1. Download a small model (500MiB).
        // 2. Start the model.
        // 3. Gather system information (VRAM Allocation)
        // 4. Check for start up erros. (Missing drivers, permissions errors, ect)
        // 5. Shutdown the model.

        TextOverlay header = new TextOverlay("Please wait while we automatically configure your system.");
        header.addStyle(Styles.TITLE_1);
        addElement(header);

        SeparatorOverlay separatorOverlay = new SeparatorOverlay(Orientation.HORIZONTAL);
        separatorOverlay.setMaxWidth(400);
        addElement(separatorOverlay);

        progressText = new TextOverlay("Gathering information...");
        progressText.addStyle(Styles.TITLE_4);
        addElement(progressText);

        // Have a spinner for animation with changing text.
        RingProgressOverlay ringProgressOverlay = new RingProgressOverlay();
        ringProgressOverlay.getProgressBar().setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        ringProgressOverlay.setWidth(150);
        ringProgressOverlay.setHeight(150);
        addElement(ringProgressOverlay);

    }

    private void startRunner() {
        // Skip hardware testing if connecting to a remote node
        if (settings.isRemoteMode()) {
            Platform.runLater(() -> {
                parent.getRoot().removeAllElements();
                parent.getRoot().addElement(new ConfigureBackendView(parent));
            });
            return;
        }

        // First download the model. Once the download finish it will call the next section.
        downloadSmallModel();
    }

    private void downloadSmallModel() {
        File model = new File(App.getModelsDirectory(), "gemma-3-270m-it-UD-IQ2_M.gguf");
        if (model.exists()) {
            startLLama(model);
            return;
        }

        FileDownloader fileDownloader = new FileDownloader();
        fileDownloader.addDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(DownloadInfo info) {
                Platform.runLater(() -> {
                    progressText.setText("Downloading testing model...");
                });
            }

            @Override
            public void onDownloadProgress(DownloadInfo info) {

            }

            @Override
            public void onDownloadComplete(DownloadInfo info, File outputFile) {
                Platform.runLater(() -> {
                    progressText.setText("Finished download!");
                });
                startLLama(outputFile);
            }

            @Override
            public void onDownloadError(DownloadInfo info, Exception e) {

            }

            @Override
            public void onDownloadCancel(DownloadInfo info) {

            }
        });

        fileDownloader.startDownload("https://huggingface.co/unsloth/gemma-3-270m-it-GGUF/resolve/main/gemma-3-270m-it-UD-IQ2_M.gguf?download=true", new File(settings.getModelPath(), "gemma-3-270m-it-UD-IQ2_M.gguf"));
    }

    private void startLLama(File model) {
        if (ServerProcess.getCurrentServer() != null) {
            ServerProcess.getCurrentServer().stop();
        }

        if (!model.setExecutable(true)) {
            App.logger.info("Updated file permissions for: '{}'", model.getAbsolutePath());
        }

        Platform.runLater(() -> {
            progressText.setText("Testing hardware configuration...");
        });

        ServerProcess serverProcess = new ServerProcess(new Model(model));

        boolean loading = serverProcess.isLoading();
        boolean error = false;
        while (loading) {
            loading = serverProcess.isLoading();

            if (serverProcess.isError()) {
                error = true;
                break;
            }
        }

        if (error) {
            Platform.runLater(() -> {
                progressText.setText("An error has occurred during the setup.");
                progressText.setTextFill(Color.RED);
            });
            serverProcess.stop();
            return;
        }
        serverProcess.stop();

        progressText.setText("Testing has completed successfully.");
        
        // This will need a slight buffer. llama-server doesn't close instantly
        App.getThreadPoolManager().submitSchedule(() -> {
            try {
                Files.delete(model.toPath());
            } catch (IOException e) {
                App.logger.error("Could not delete testing model!", e);
            }
        }, 1L, TimeUnit.SECONDS);

        App.getThreadPoolManager().submitSchedule(() -> {
            // Configure backend.
            Platform.runLater(() -> {
                parent.getRoot().removeAllElements();
                parent.getRoot().addElement(new ConfigureBackendView(parent));
            });
        }, 1L, TimeUnit.SECONDS);
    }
}
