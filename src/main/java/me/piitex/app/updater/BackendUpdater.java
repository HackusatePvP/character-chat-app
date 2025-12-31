package me.piitex.app.updater;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
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
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class BackendUpdater {
    private final String currentVersion;
    private Window window;
    private Container container;

    private final GitHubUtil gitHubUtil;
    private Version current, latest;

    public BackendUpdater(String currentVersion) {
        this.currentVersion = currentVersion;

        gitHubUtil = new GitHubUtil("https://api.github.com/repos/ggml-org/llama.cpp/");
        fetchLatestRelease();
    }

    private void fetchLatestRelease() {
        String release = null;
        try {
            release = gitHubUtil.getLatestReleaseJson().getString("tag_name");
            App.logger.info("Latest release: {}", release);
        } catch (IOException | URISyntaxException e) {
            App.logger.error("Could not fetch latest tag!", e);
        }
        if (release != null) {
            current = VersionUtil.parseVersion(currentVersion);
            latest = VersionUtil.parseVersion(release);
        }
    }

    public boolean isUpdateAvailable() {
        if (current != null && latest != null) {
            return current.compareTo(latest) < 0;
        }
        return false;
    }

    public boolean startUpdate() {
        if (isUpdateAvailable()) {
            Platform.runLater(() -> {
                buildAndDisplayUpdateWindow(gitHubUtil);
            });
            return true;
        }
        return false;
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
            if (OSUtil.getOS().contains("Windows")) {
                App.getThreadPoolManager().submitTask(() -> {
                    downloadCudaBackend(gitHubUtil, updateInfo, progressBarOverlay);
                });
            } else {
                App.getThreadPoolManager().submitTask(() -> {
                    downloadVulkan(gitHubUtil, null, updateInfo, progressBarOverlay);
                });
            }
        });
        App.window.getStage().getScene().getRoot().setDisable(true);
        window.getStage().setAlwaysOnTop(true);
        window.render();

        window.getStage().setOnCloseRequest(windowEvent -> {
            App.window.getScene().getRoot().setDisable(false);
        });
    }

    public void downloadCudaBackend(GitHubUtil gitHubUtil, TextOverlay textOverlay, ProgressBarOverlay progressBarOverlay) {

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

                            Platform.runLater(() -> {
                                container.removeElement(progressBarOverlay);

                                textOverlay.setText("Download failed!");
                                textOverlay.setTextFill(Color.RED);

                                ButtonOverlay exit = new ButtonBuilder("ex").setText("Exit").addStyle(Styles.DANGER).build();
                                exit.setX(50);
                                exit.setY(50);
                                exit.onClick(_ -> {
                                    Platform.exit();
                                    System.exit(0);
                                });
                                container.addElement(exit);
                            });

                        }

                        @Override
                        public void onDownloadCancel(DownloadInfo info) {
                            App.logger.error("Download cancelled!");
                        }
                    });


        } catch (IOException | URISyntaxException e) {
            App.logger.error("Unable to download cuda release!", e);
        }
    }

    public void downloadVulkan(GitHubUtil gitHubUtil, File cudaZip, TextOverlay textOverlay, ProgressBarOverlay progressBarOverlay) {
        try {
            //llama-b7087-bin-win-vulkan-x64.zip
            String os = OSUtil.getOS().contains("Windows") ? "win" : "ubuntu";
            String extension = OSUtil.getOS().contains("Windows") ? "zip" : "tar.gz";
            JSONObject releaseAsset = gitHubUtil.getReleaseAsset(gitHubUtil.getLatestReleaseID(),
                    "llama-[a-zA-Z0-9]+-bin-" + os + "-vulkan-x64\\." + extension);
            gitHubUtil.downloadAsset(releaseAsset.getInt("id"),
                    new File(App.getBackendDirectory(), releaseAsset.getString("name")),
                    new DownloadListener() {
                        @Override
                        public void onDownloadStart(DownloadInfo info) {
                            Platform.runLater(() -> {
                                String fileName = info.getFileName();
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
                            if (OSUtil.getOS().contains("Windows")) {
                                downloadHip(gitHubUtil, cudaZip, outputFile, textOverlay, progressBarOverlay);
                            } else {
                                prepareInstallation(gitHubUtil, null, outputFile, null, textOverlay, progressBarOverlay);
                            }
                        }

                        @Override
                        public void onDownloadError(DownloadInfo info, Exception e) {
                            Platform.runLater(() -> {
                                container.removeElement(progressBarOverlay);

                                textOverlay.setText("Download failed!");
                                textOverlay.setTextFill(Color.RED);

                                ButtonOverlay exit = new ButtonBuilder("ex").setText("Exit").addStyle(Styles.DANGER).build();
                                exit.setX(50);
                                exit.setY(50);
                                exit.onClick(_ -> {
                                    Platform.exit();
                                    System.exit(0);
                                });
                                container.addElement(exit);
                            });
                        }

                        @Override
                        public void onDownloadCancel(DownloadInfo info) {
                            App.logger.error("Download cancelled!");
                        }
                    });
        } catch (IOException | URISyntaxException e) {
            App.logger.error("Unable to download vulkan release!", e);
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
                            Platform.runLater(() -> {
                                container.removeElement(progressBarOverlay);

                                textOverlay.setText("Download failed!");
                                textOverlay.setTextFill(Color.RED);

                                ButtonOverlay exit = new ButtonBuilder("ex").setText("Exit").addStyle(Styles.DANGER).build();
                                exit.setX(50);
                                exit.setY(50);
                                exit.onClick(_ -> {
                                    Platform.exit();
                                    System.exit(0);
                                });
                                container.addElement(exit);
                            });
                        }

                        @Override
                        public void onDownloadCancel(DownloadInfo info) {
                            App.logger.error("Download cancelled!");
                        }
                    });
        } catch (IOException | URISyntaxException e) {
            App.logger.error("Unable to download hip release!", e);
        }
    }

    public void prepareInstallation(GitHubUtil gitHubUtil, File cudaZip, File vulkanZip, File hipZip, TextOverlay textOverlay, ProgressBarOverlay progressBarOverlay) {
        Platform.runLater(() -> {
            textOverlay.setText("Deleting old files...");
            progressBarOverlay.getProgressBar().setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        });

        if (ServerProcess.getCurrentServer() != null) {
            if (ServerProcess.getCurrentServer().isAlive() && !ServerProcess.getCurrentServer().stop()) {
                App.logger.warn("Could not stop llama-server. Installation may fail.");
            }
        }
        File cudaDir = new File(App.getBackendDirectory(), "cuda/");
        File hipDir = new File(App.getBackendDirectory(), "hip/");
        File vulkanDir = new File(App.getBackendDirectory(), "vulkan/");

        if (cudaZip != null) {
            try {
                FileUtils.deleteDirectory(cudaDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (hipZip != null) {
            try {
                FileUtils.deleteDirectory(hipDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (vulkanZip != null) {
            try {
                FileUtils.deleteDirectory(vulkanDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Platform.runLater(() -> {
            textOverlay.setText("Extracting files...");
            progressBarOverlay.getProgressBar().setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        });
        try {
            App.logger.info("Unzipping backend files...");
            if (cudaZip != null) {
                ZipUtil.unzipFile(cudaZip, new File(App.getBackendDirectory(), "cuda/"));
            } else {
                App.logger.warn("Could not find cuda installation!");
            }
            if (hipZip != null) {
                ZipUtil.unzipFile(hipZip, new File(App.getBackendDirectory(), "hip/"));
            } else {
                App.logger.warn("Could not find HIP installation!");
            }
            if (vulkanZip != null) {
                App.logger.info("Unzipping {} ...", vulkanZip.getName());
                if (vulkanZip.getName().endsWith(".tar.gz")) {
                    ZipUtil.unzipTarGzFile(vulkanZip, new File(App.getBackendDirectory(), "vulkan/"));
                } else {
                    ZipUtil.unzipFile(vulkanZip, new File(App.getBackendDirectory(), "vulkan/"));
                }
            } else {
                App.logger.error("Could not find vulkan installation!"); // Every os should have Vulkan
            }

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
            if (llamaVersion.createNewFile()) {
                App.logger.info("Created llama.cpp version file.");
            }

            // Updates may alter model efficiency. Flag the models to be rescanned after update.
            App.logger.info("Flagging all models to be rescanned...");
            for (Model model : App.getModels("excluded")) {
                model.getSettings().setChange(true);
            }

            // Verify that all files are in the base directory. Not sub-directories.
            File[] vulkanFiles = vulkanDir.listFiles();
            if (vulkanFiles.length == 1 && vulkanFiles[0].isDirectory()) {
                File parentDir = vulkanFiles[0];
                for (File file : parentDir.listFiles()) {
                    Path sourcePath = file.toPath();
                    Path destinationPath = vulkanDir.toPath().resolve(file.getName());

                    Files.move(
                            sourcePath,
                            destinationPath,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }

            // Update permissions
            for (File file : vulkanDir.listFiles()) {
                file.setExecutable(true);
            }

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
            App.logger.error("Error occurred during post installation process!", e);
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

    public Version getCurrent() {
        return current;
    }

    public Version getLatest() {
        return latest;
    }
}
