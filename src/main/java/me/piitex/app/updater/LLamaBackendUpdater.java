package me.piitex.app.updater;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.server.ServerProcess;
import me.piitex.engine.Window;
import me.piitex.engine.WindowBuilder;
import me.piitex.engine.containers.Container;
import me.piitex.engine.containers.DownloadContainer;
import me.piitex.engine.containers.EmptyContainer;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.loaders.image.BaseImageLoader;
import me.piitex.engine.overlays.*;
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

public class LLamaBackendUpdater {
    private final String currentVersion;
    private Window window;
    private Container container;

    private final GitHubUtil gitHubUtil;
    private Version current, latest;

    public LLamaBackendUpdater(String currentVersion) {
        this.currentVersion = currentVersion;
        App.logger.info("Current Version: {}", currentVersion);

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

    public synchronized boolean isUpdateAvailable() {
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
        window = new WindowBuilder("Update").setDimensions(450, 200).setIcon(new BaseImageLoader(new File(App.getExecutedDirectory(), "logo.png"))).build();
        container = new EmptyContainer(window.getWidth(), window.getHeight());
        window.addContainer(container);
        window.getStage().setOnHidden(windowEvent -> {
            App.shutdown();
        });

        VerticalLayout main = new VerticalLayout(container.getWidth(), container.getHeight());
        main.setMaxSize(container.getWidth(), container.getHeight());
        main.setAlignment(Pos.TOP_CENTER);
        main.setY(20);
        container.addElement(main);

        TextOverlay textOverlay = new TextOverlay("LLamaCPP updates available. Click 'Update' to start.");
        main.addElement(textOverlay);

        ButtonOverlay buttonOverlay = new ButtonBuilder("update").setText("Update").build();
        main.addElement(buttonOverlay);

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

            if (OSUtil.getOS().contains("Windows")) {
                try {
                    downloadCudaBackendNew(gitHubUtil);
                } catch (Exception ignored) {
                    VerticalLayout errorBox = new VerticalLayout(container.getWidth() - 20, container.getHeight() - 20);
                    errorBox.setMaxSize(errorBox.getWidth(), errorBox.getHeight());
                    container.addElement(errorBox);

                    TextFlowOverlay error = new TextFlowOverlay("Error occurred while fetching update. Please try updating at a later point.", errorBox.getWidth(), errorBox.getHeight());
                    error.setTextAlignment(TextAlignment.CENTER);
                    error.setMaxSize(error.getWidth(), error.getHeight());
                    error.setTextFillColor(Color.RED);
                    errorBox.addElement(error);
                }
            } else {
                downloadVulkanBackendNew(gitHubUtil, null);
            }
        });

        App.window.getStage().getScene().getRoot().setDisable(true);
        window.getStage().setAlwaysOnTop(true);
        window.render();

        window.getStage().setOnCloseRequest(windowEvent -> {
            App.window.getScene().getRoot().setDisable(false);
        });
    }

    public void downloadCudaBackendNew(GitHubUtil gitHubUtil) {
        container.removeAllElements();

        try {

            FileDownloader downloader = new FileDownloader(true);
            File output = new File(App.getBackendDirectory(), "cuda.zip");
            DownloadInfo downloadInfo = gitHubUtil.downloadAsset(gitHubUtil.getReleaseAsset(gitHubUtil.getLatestReleaseID(),
                    "llama-[a-zA-Z0-9]+-bin-win-cuda-12\\.4-x64\\.zip").getInt("id"), output);

            DownloadContainer downloadContainer = new DownloadContainer(container.getWidth(), container.getHeight(), "Downloading Cuda backend...", downloadInfo, downloader); // Callback will be handled by the container.

            container.addElement(downloadContainer);

            downloadContainer.onDownloadComplete(downloadInfo1 -> {
                Platform.runLater(() -> {
                    App.logger.info("Finished downloading '{}'", downloadInfo1.getFileName());
                    downloadContainer.getMessage().setText("Download completed!");
                });
                downloader.shutdown();
                downloadVulkanBackendNew(gitHubUtil, output);
            });

            downloadContainer.onDownloadError(_ -> {
                Platform.runLater(() -> {
                    downloadContainer.getMessage().setText("Error: Download failed! Please restart the application and check your internet connection.");
                });
            });

            downloadContainer.onDownloadCancelled(_ -> {
                // Not cancellable.
            });

            // Call download asynchronously.
            App.getThreadPoolManager().submitTask(() -> {
                App.logger.info("Starting download for cuda backend...");
                downloadContainer.startDownload();
            });

        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadVulkanBackendNew(GitHubUtil gitHubUtil, File cudaZip) {
        try {

            FileDownloader downloader = new FileDownloader(true);

            String os = OSUtil.getOS().contains("Windows") ? "win" : "ubuntu";
            String extension = OSUtil.getOS().contains("Windows") ? "zip" : "tar.gz";
            JSONObject releaseAsset = gitHubUtil.getReleaseAsset(gitHubUtil.getLatestReleaseID(),
                    "llama-[a-zA-Z0-9]+-bin-" + os + "-vulkan-x64\\." + extension);

            File output = new File(App.getBackendDirectory(), "vulkan.zip");
            DownloadInfo downloadInfo = gitHubUtil.downloadAsset(releaseAsset.getInt("id"), output);

            Platform.runLater(() -> {
                container.removeAllElements();

                DownloadContainer downloadContainer = new DownloadContainer(container.getWidth(), container.getHeight(), "Downloading Vulkan backend...", downloadInfo, downloader); // Callback will be handled by the container.
                container.addElement(downloadContainer);

                downloadContainer.onDownloadComplete(downloadInfo1 -> {
                    Platform.runLater(() -> {
                        App.logger.info("Finished downloading '{}'", downloadInfo1.getFileName());
                        downloadContainer.getMessage().setText("Download completed!");

                        downloader.shutdown();
                    });

                    downloadHipNew(gitHubUtil, cudaZip, output);
                });

                downloadContainer.onDownloadError(_ -> {
                    Platform.runLater(() -> {
                        downloadContainer.getMessage().setText("Error: Download failed! Please restart the application and check your internet connection.");
                    });
                });

                downloadContainer.onDownloadCancelled(_ -> {
                    // Not cancellable.
                });

                // Call download asynchronously.
                App.getThreadPoolManager().submitTask(() -> {
                    App.logger.info("Starting download for cuda backend...");
                    downloadContainer.startDownload();
                });
            });


        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadHipNew(GitHubUtil gitHubUtil, File cudaZip, File vulkanZip) {
        try {
            App.logger.info("Starting HIP download...");
            FileDownloader downloader = new FileDownloader(true);

            File output = new File(App.getBackendDirectory(), "hip.zip");

            DownloadInfo downloadInfo = gitHubUtil.downloadAsset(gitHubUtil.getReleaseAsset(gitHubUtil.getLatestReleaseID(),
                            "llama-[a-zA-Z0-9]+-bin-win-hip-radeon-x64\\.zip").getInt("id"), output);

            Platform.runLater(() -> {
                container.removeAllElements();

                DownloadContainer downloadContainer = new DownloadContainer(container.getWidth(), container.getHeight(), "Downloading HIP backend...", downloadInfo, downloader); // Callback will be handled by the container.
                container.addElement(downloadContainer);

                downloadContainer.onDownloadComplete(downloadInfo1 -> {
                    Platform.runLater(() -> {
                        App.logger.info("Finished downloading '{}'", downloadInfo1.getFileName());
                        downloadContainer.getMessage().setText("Download completed!");
                    });

                    downloader.shutdown();

                    App.getThreadPoolManager().submitSchedule(() -> {
                        prepareInstallation(gitHubUtil, cudaZip, vulkanZip, output);
                    }, 1L, TimeUnit.SECONDS);
                });

                downloadContainer.onDownloadError(_ -> {
                    Platform.runLater(() -> {
                        downloadContainer.getMessage().setText("Error: Download failed! Please restart the application and check your internet connection.");
                    });
                });

                downloadContainer.onDownloadCancelled(_ -> {
                    // Not cancellable.
                });

                // Call download asynchronously.
                App.getThreadPoolManager().submitTask(() -> {
                    App.logger.info("Starting download for cuda backend...");
                    downloadContainer.startDownload();
                });
            });


        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void prepareInstallation(GitHubUtil gitHubUtil, File cudaZip, File vulkanZip, File hipZip) {
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                container.removeAllElements();
                VerticalLayout main = new VerticalLayout(container.getWidth(), container.getHeight());
                main.setAlignment(Pos.CENTER);
                main.setY(20);
                container.addElement(main);

                TextOverlay textOverlay = new TextOverlay("Deleting old files...");
                main.addElement(textOverlay);

                ProgressBarOverlay progressBarOverlay = new ProgressBarOverlay();
                progressBarOverlay.getProgressBar().setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                main.addElement(progressBarOverlay);


            });
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
            container.removeAllElements();
            VerticalLayout main = new VerticalLayout(container.getWidth(), container.getHeight());
            main.setAlignment(Pos.CENTER);
            main.setY(20);
            container.addElement(main);

            TextOverlay textOverlay = new TextOverlay("Extracting files...");
            main.addElement(textOverlay);

            ProgressBarOverlay progressBarOverlay = new ProgressBarOverlay();
            progressBarOverlay.getProgressBar().setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            main.addElement(progressBarOverlay);
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
            if (vulkanFiles != null && vulkanFiles.length == 1 && vulkanFiles[0].isDirectory()) {
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
                container.removeAllElements();
                VerticalLayout main = new VerticalLayout(container.getWidth(), container.getHeight());
                main.setAlignment(Pos.CENTER);
                main.setY(20);
                container.addElement(main);

                TextOverlay textOverlay = new TextOverlay("Download completed. Please restart the application.");
                main.addElement(textOverlay);

                ButtonOverlay restart = new ButtonBuilder("Restart").setText("Exit").addStyle(Styles.BUTTON_OUTLINED).addStyle(Styles.DANGER).build();
                main.addElement(restart);
                restart.onClick(_ -> {
                    window.close(true);
                    Platform.exit();
                    System.exit(0);
                });
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
