package me.piitex.app.updater;

import javafx.application.Platform;
import me.piitex.app.App;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.engine.Window;
import me.piitex.engine.WindowBuilder;
import me.piitex.engine.containers.Container;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.loaders.ImageLoader;
import me.piitex.engine.overlays.ButtonBuilder;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.ProgressBarOverlay;
import me.piitex.engine.overlays.TextOverlay;
import me.piitex.os.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ApplicationUpdater {
    private String currentVersion;
    private Window window;
    private Container container;

    public ApplicationUpdater(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public void checkForUpdates() {
        App.logger.info("Checking version: {}", currentVersion);
        GitHubUtil gitHubUtil = new GitHubUtil("https://api.github.com/repos/HackusatePvP/character-chat-app/");
        try {
            String latestVersionString = gitHubUtil.getLatestReleaseJson().getString("tag_name");
            Version latestVersion = VersionUtil.parseVersion(latestVersionString);
            App.logger.info("Latest tag is {}", latestVersionString);

            if (!currentVersion.startsWith("v")) {
                currentVersion = "v" + currentVersion;
            }
            Version version = VersionUtil.parseVersion(currentVersion);

            if (version.compareTo(latestVersion) < 0) {
                App.logger.info("New application version available.");
                Platform.runLater(() -> {
                    buildAndDisplayUpdateWindow(gitHubUtil);
                });
            } else {
                App.logger.info("No update available.");
            }

        } catch (IOException | URISyntaxException e) {
            App.logger.error("Could not fetch latest app release!", e);
        }
    }

    public void buildAndDisplayUpdateWindow(GitHubUtil gitHubUtil) {
        window = new WindowBuilder("Update").setDimensions(400, 150).setIcon(new ImageLoader(new File(App.getAppDirectory(), "logo.png"))).build();
        container = new EmptyContainer(400, 150);
        window.addContainer(container);

        TextOverlay textOverlay = new TextOverlay("App updates available. Click 'Update' to start.");
        textOverlay.setY(20);
        textOverlay.setX(10);
        container.addElement(textOverlay);

        ButtonOverlay buttonOverlay = new ButtonBuilder("update").setText("Update").build();
        container.addElement(buttonOverlay);
        buttonOverlay.setX(150);
        buttonOverlay.setY(70);

        buttonOverlay.onClick(_ -> {
            App.window.close(false);
            App.getInstance().getCharacters().clear();
            App.getInstance().getUserTemplates().clear();
            App.reloadModelList();
            if (ServerProcess.getCurrentServer() != null) {
                ServerProcess serverProcess = ServerProcess.getCurrentServer();
                serverProcess.stop();
            }

            container.removeAllElements();
            TextOverlay updateInfo = new TextOverlay("Preparing for installation...");
            updateInfo.setY(20);
            updateInfo.setX(10);
            container.addElement(updateInfo);

            ProgressBarOverlay progressBarOverlay = new ProgressBarOverlay();
            progressBarOverlay.setX(150);
            progressBarOverlay.setY(70);
            container.addElement(progressBarOverlay);

            // Start download


        });
        App.window.getStage().getScene().getRoot().setDisable(true);
        window.getStage().setAlwaysOnTop(true);
        window.render();
    }

    private void downloadUpdate(GitHubUtil gitHubUtil, TextOverlay textOverlay, ProgressBarOverlay progressBarOverlay) {
        textOverlay.setText("Download updates...");
        progressBarOverlay.getProgressBar().setProgress(0);

        App.getThreadPoolManager().submitTask(() -> {
            try {
                gitHubUtil.downloadAsset(gitHubUtil.getLatestReleaseID(), new File("download.jar"), new DownloadListener() {
                    @Override
                    public void onDownloadStart(DownloadInfo info) {
                        App.logger.info("Starting download...");
                    }

                    @Override
                    public void onDownloadProgress(DownloadInfo info) {
                        progressBarOverlay.getProgressBar().progressProperty().set(info.getDownloadProgress());
                    }

                    @Override
                    public void onDownloadComplete(DownloadInfo info, File outputFile) {
                        App.logger.info("Update completed! Shutting down...");
                        Platform.exit();
                        try {
                            Files.copy(outputFile.toPath(), new File(App.getExecutedDirectory(), "character-chat-app.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        System.exit(1);
                    }

                    @Override
                    public void onDownloadError(DownloadInfo info, Exception e) {

                    }

                    @Override
                    public void onDownloadCancel(DownloadInfo info) {

                    }

                });
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
