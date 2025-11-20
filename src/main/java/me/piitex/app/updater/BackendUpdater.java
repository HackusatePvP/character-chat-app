package me.piitex.app.updater;

import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
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
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class BackendUpdater {
    private final String currentVersion;
    private Window window;
    private Container container;

    public BackendUpdater(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public void checkForUpdates() {
        GitHubUtil gitHubUtil = new GitHubUtil("https://api.github.com/repos/ggerganov/llama.cpp/");
        try {
            String release = gitHubUtil.getLatestReleaseJson().getString("tag_name");
            App.logger.info("Using '{}' backend version.", currentVersion);
            App.logger.info("Checking '{}' for possible update...", release);


            Version current = VersionUtil.parseVersion(currentVersion);
            Version next = VersionUtil.parseVersion(release);
            App.logger.info("Comparing '{}' to '{}'", current.getCalculatedVersion(), next.getCalculatedVersion());
            if (next.compareTo(current) > 0) {
                App.logger.info("LLamaCPP update is available.");
                Platform.runLater(() -> {
                    buildAndDisplayUpdateWindow(gitHubUtil);
                });
            }
        } catch (IOException | URISyntaxException e) {
            App.logger.error("Error occurred while checking for backend updates!", e);
        }
    }

    public void buildAndDisplayUpdateWindow(GitHubUtil gitHubUtil) {
        window = new WindowBuilder("Update").setDimensions(400, 150).setIcon(new ImageLoader(new File(App.getAppDirectory(), "logo.png"))).build();
        container = new EmptyContainer(400, 150);
        window.addContainer(container);

        TextOverlay textOverlay = new TextOverlay("LLamaCPP updates available. Click 'Update' to start.");
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
            downloadCudaBackend(gitHubUtil, updateInfo, progressBarOverlay);
        });
        App.window.getStage().getScene().getRoot().setDisable(true);
        window.getStage().setAlwaysOnTop(true);
        window.render();
    }

    public void downloadCudaBackend(GitHubUtil gitHubUtil, TextOverlay textOverlay, ProgressBarOverlay progressBarOverlay) {
        App.getThreadPoolManager().submitTask(() -> {
            try {
                gitHubUtil.downloadAsset(gitHubUtil.getReleaseAsset(gitHubUtil.getLatestReleaseID(),
                                "llama-[a-zA-Z0-9]+-bin-win-cuda-12\\.4-x64\\.zip").getInt("id"),
                        new File(App.getBackendDirectory(), "cuda.zip"),
                        new DownloadListener() {
                            @Override
                            public void onDownloadStart(DownloadInfo info) {
                                Platform.runLater(() -> {
                                    progressBarOverlay.getProgressBar().progressProperty().set(0);
                                    textOverlay.setText("Downloading Cuda backend...");
                                });
                            }

                            @Override
                            public void onDownloadProgress(DownloadInfo info) {
                                Platform.runLater(() -> {
                                    // Update UI
                                    progressBarOverlay.getProgressBar().progressProperty().set(info.getDownloadProgress());
                                });
                            }

                            @Override
                            public void onDownloadComplete(DownloadInfo info, File outputFile) {
                                App.logger.info("Download for Cuda completed!");
                                downloadVulkan(gitHubUtil, outputFile, textOverlay, progressBarOverlay);
                            }

                            @Override
                            public void onDownloadError(DownloadInfo info, Exception e) {

                            }

                            @Override
                            public void onDownloadCancel(DownloadInfo info) {

                            }
                        });


            } catch (IOException | URISyntaxException e) {
                App.logger.info("Unable to download latest release!");
            }
        });
    }

    public void downloadVulkan(GitHubUtil gitHubUtil, File cudaZip, TextOverlay textOverlay, ProgressBarOverlay progressBarOverlay) {
        try {
            //llama-b7087-bin-win-vulkan-x64.zip
            gitHubUtil.downloadAsset(gitHubUtil.getReleaseAsset(gitHubUtil.getLatestReleaseID(),
                            "llama-[a-zA-Z0-9]+-bin-win-vulkan-x64\\.zip").getInt("id"),
                    new File(App.getBackendDirectory(), "vulkan.zip"),
                    new DownloadListener() {
                        @Override
                        public void onDownloadStart(DownloadInfo info) {
                            Platform.runLater(() -> {
                                progressBarOverlay.getProgressBar().progressProperty().set(0);
                                textOverlay.setText("Downloading Vulkan backend...");
                            });
                        }

                        @Override
                        public void onDownloadProgress(DownloadInfo info) {
                            Platform.runLater(() -> {
                                // Update UI
                                progressBarOverlay.getProgressBar().progressProperty().set(info.getDownloadProgress());
                            });
                        }

                        @Override
                        public void onDownloadComplete(DownloadInfo info, File outputFile) {
                            downloadHip(gitHubUtil, cudaZip, outputFile, textOverlay, progressBarOverlay);
                        }

                        @Override
                        public void onDownloadError(DownloadInfo info, Exception e) {

                        }

                        @Override
                        public void onDownloadCancel(DownloadInfo info) {

                        }
                    });
        } catch (IOException | URISyntaxException e) {
            App.logger.info("Unable to download latest release!");
        }
    }

    public void downloadHip(GitHubUtil gitHubUtil, File cudaZip, File vulkanZip, TextOverlay textOverlay, ProgressBarOverlay progressBarOverlay) {
        try {
            gitHubUtil.downloadAsset(gitHubUtil.getReleaseAsset(gitHubUtil.getLatestReleaseID(),
                            "llama-[a-zA-Z0-9]+-bin-win-hip-radeon-x64\\.zip").getInt("id"),
                    new File(App.getBackendDirectory(), "hip.zip"),
                    new DownloadListener() {
                        @Override
                        public void onDownloadStart(DownloadInfo info) {
                            Platform.runLater(() -> {
                                progressBarOverlay.getProgressBar().progressProperty().set(0);
                                textOverlay.setText("Downloading HIP backend...");
                            });
                        }

                        @Override
                        public void onDownloadProgress(DownloadInfo info) {
                            Platform.runLater(() -> {
                                // Update UI
                                progressBarOverlay.getProgressBar().progressProperty().set(info.getDownloadProgress());
                            });
                        }

                        @Override
                        public void onDownloadComplete(DownloadInfo info, File outputFile) {
                            // Delay for io operations.
                            App.getThreadPoolManager().submitSchedule(() -> {
                                prepareInstallation(gitHubUtil, cudaZip, vulkanZip, outputFile, textOverlay, progressBarOverlay);
                            }, 1L, TimeUnit.SECONDS);
                        }

                        @Override
                        public void onDownloadError(DownloadInfo info, Exception e) {

                        }

                        @Override
                        public void onDownloadCancel(DownloadInfo info) {

                        }
                    });
        } catch (IOException | URISyntaxException e) {
            App.logger.info("Unable to download latest release!");
        }
    }

    public void prepareInstallation(GitHubUtil gitHubUtil, File cudaZip, File vulkanZip, File hipZip, TextOverlay textOverlay, ProgressBarOverlay progressBarOverlay) {
        if (!cudaZip.exists() || !vulkanZip.exists() || !hipZip.exists()) {
            throw new RuntimeException("Critical installation file missing!");
        }

        Platform.runLater(() -> {
            textOverlay.setText("Deleting old files...");
            progressBarOverlay.getProgressBar().setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        });
        File cudaDir = new File(App.getBackendDirectory(), "cuda/");
        try {
            FileUtils.deleteDirectory(cudaDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File hipDir = new File(App.getBackendDirectory(), "hip/");
        try {
            FileUtils.deleteDirectory(hipDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File vulkanDir = new File(App.getBackendDirectory(), "vulkan/");
        try {
            FileUtils.deleteDirectory(vulkanDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Platform.runLater(() -> {
            textOverlay.setText("Extracting files...");
            progressBarOverlay.getProgressBar().setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        });
        try {
            App.logger.info("Unzipping backend files...");
            ZipUtil.unzipFile(cudaZip, new File(App.getBackendDirectory(), "cuda/"));
            ZipUtil.unzipFile(hipZip, new File(App.getBackendDirectory(), "hip/"));
            ZipUtil.unzipFile(vulkanZip, new File(App.getBackendDirectory(), "vulkan/"));

            App.logger.info("Finished unzipping!");
            Platform.runLater(() -> {
                textOverlay.setText("Cleaning up files...");
            });

            for (File file : App.getBackendDirectory().listFiles()) {
                if (file.isFile()) {
                    Files.delete(file.toPath());
                }
            }

            File llamaVersion = new File(App.getBackendDirectory(), gitHubUtil.getLatestReleaseJson().getString("tag_name") + ".txt");
            llamaVersion.createNewFile();

            Platform.runLater(() -> {
                textOverlay.setText("Update completed. Please re-launch the application.");
                container.removeElement(progressBarOverlay);
                ButtonOverlay buttonOverlay = new ButtonBuilder("close").setText("Close").build();
                buttonOverlay.setX(150);
                buttonOverlay.setY(70);
                buttonOverlay.onClick(_ -> {
                    window.close(false);
                    Platform.exit();
                    System.exit(0);
                });
                container.addElement(buttonOverlay);
            });


        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private double calculateDownloadSpeed(long totalBytesRead, AtomicLong lastDownloadBytes, AtomicLong lastDownloadTime, AtomicReference<Double> smoothedSpeedRef) {
        long currentTime = System.currentTimeMillis();

        long bytesDownloadedSinceLastUpdate = totalBytesRead - lastDownloadBytes.get();
        long timeElapsedSinceLastUpdate = currentTime - lastDownloadTime.get();

        double instantaneousBytesPerSecond = 0.0;
        if (timeElapsedSinceLastUpdate > 0 && bytesDownloadedSinceLastUpdate > 0) {
            instantaneousBytesPerSecond = (double) bytesDownloadedSinceLastUpdate / (timeElapsedSinceLastUpdate / 1000.0);
        }

        final double SMOOTHING_FACTOR = 0.15; // 0.15 is responsive but stable

        double previousSmoothedSpeed = smoothedSpeedRef.get();

        // Apply the EMA formula
        double newSmoothedSpeed = (instantaneousBytesPerSecond * SMOOTHING_FACTOR) +
                (previousSmoothedSpeed * (1.0 - SMOOTHING_FACTOR));

        // Update the reference for the next calculation
        smoothedSpeedRef.set(newSmoothedSpeed);

        // Update the 'last' values for the next calculation
        lastDownloadBytes.set(totalBytesRead);
        lastDownloadTime.set(currentTime);

        return newSmoothedSpeed; // Return the stable, smoothed value
    }
}
