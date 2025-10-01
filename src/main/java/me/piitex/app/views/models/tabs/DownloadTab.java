package me.piitex.app.views.models.tabs;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.configuration.AppSettings;
import me.piitex.engine.configurations.ConfigUtil;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.containers.tabs.TabsContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.TitledLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonBuilder;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.TextOverlay;
import me.piitex.os.DownloadInfo;
import me.piitex.os.DownloadListener;
import me.piitex.os.FileDownloader;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class DownloadTab extends Tab {
    private final TabsContainer tabsContainer;
    private final AppSettings appSettings;
    private final ScrollContainer scrollContainer;
    private final VerticalLayout downloadListLayout;

    // NEW: The centralized FileDownloader instance
    private final FileDownloader downloader;

    private static final int TILE_LAYOUT_WIDTH = 400;
    private static final int TILE_LAYOUT_HEIGHT = 75;
    private static final int ICON_X_OFFSET = 20;
    private static final int QUANT_WIDTH = 100;
    private static final int CONTAINER_SPACING = 15;
    private static final int SCROLL_MAX_WIDTH_OFFSET = 300;
    private static final int SCROLL_MAX_HEIGHT_OFFSET = 200;

    private ConfigUtil downloadCache;


    public DownloadTab(TabsContainer tabsContainer) {
        super("Download");
        this.tabsContainer = tabsContainer;
        this.appSettings = App.getInstance().getAppSettings();
        this.downloader = new FileDownloader();

        File file = new File(App.getModelsDirectory(), "download-cache.dat");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            downloadCache = new ConfigUtil(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.downloadListLayout = createMainLayout();

        this.scrollContainer = createScrollContainer(downloadListLayout);
        addElement(scrollContainer);
        loadAndBuildDownloadList();
    }

    private VerticalLayout createMainLayout() {
        VerticalLayout layout = new VerticalLayout(0, -1);
        layout.setX(20);
        layout.setSpacing(10);
        layout.setPrefSize(appSettings.getWidth() - 500, -1);
        return layout;
    }

    private ScrollContainer createScrollContainer(VerticalLayout contentLayout) {
        ScrollContainer container = new ScrollContainer(contentLayout, 0, 20, appSettings.getWidth() - SCROLL_MAX_WIDTH_OFFSET, appSettings.getHeight() - SCROLL_MAX_HEIGHT_OFFSET);
        container.setMaxSize(appSettings.getWidth() - SCROLL_MAX_WIDTH_OFFSET, appSettings.getHeight() - SCROLL_MAX_HEIGHT_OFFSET);
        container.setVerticalScroll(true);
        container.setScrollWhenNeeded(true);
        container.setHorizontalScroll(false);
        return container;
    }

    private void loadAndBuildDownloadList() {
        App.getThreadPoolManager().submitTask(() -> {
            App.logger.info("Initiated httpclient thread.");
            ConfigUtil configUtil;
            try {
                configUtil = new ConfigUtil(new File(App.getModelsDirectory(), "model-list.dat"));
            } catch (IOException e) {
                App.logger.error("Could not locate configuration file `model-list.dat`", e);
                return;
            }

            for (String key : configUtil.getRawConfigData().keySet()) {
                String name = configUtil.getString(key + ".name");
                Map<String, Object> rawLinks = configUtil.getSection(key + ".map");
                String description = configUtil.getString(key + ".description");

                DownloadModel downloadModel = new DownloadModel(key, name, rawLinks);
                TitledLayout downloadContainer = createDownloadContainer(name, description, downloadModel);
                Platform.runLater(() -> {
                    downloadListLayout.addElement(downloadContainer);
                });
            }

            App.logger.info("Finished thread...");
        });
    }

    private TitledLayout createDownloadContainer(String title, String description, DownloadModel downloadModel) {
        TitledLayout titledContainer = new TitledLayout(title, scrollContainer.getWidth() - 10, -1);
        titledContainer.addStyle(Styles.DENSE);
        titledContainer.addStyle(Tweaks.ALT_ICON);
        titledContainer.setSpacing(CONTAINER_SPACING);
        titledContainer.setCollapse(true);
        titledContainer.setExpanded(false);
        titledContainer.setMaxSize(scrollContainer.getWidth() - QUANT_WIDTH, scrollContainer.getHeight() - QUANT_WIDTH);

        TextOverlay descriptionOverlay = new TextOverlay(description);
        titledContainer.addElement(descriptionOverlay);

        downloadModel.getLinks().forEach((quantization, url) -> {
            Platform.runLater(() -> {
                HorizontalLayout tileLayout = createDownloadTile(titledContainer, quantization, url, downloadModel.getKey());
                titledContainer.addElement(tileLayout);
            });
        });

        return titledContainer;
    }

    private HorizontalLayout createDownloadTile(TitledLayout titledContainer, String quantization, String url, String modelKey) {
        HorizontalLayout tileLayout = new HorizontalLayout(TILE_LAYOUT_WIDTH, TILE_LAYOUT_HEIGHT);
        tileLayout.setAlignment(Pos.CENTER_LEFT);
        tileLayout.addStyle(Styles.BORDER_MUTED);
        tileLayout.addStyle(appSettings.getGlobalTextSize());
        tileLayout.setSpacing(50);

        ButtonOverlay downloadIcon = new ButtonBuilder("download").setIcon(new FontIcon(Material2MZ.SAVE_ALT)).build();
        downloadIcon.addStyle(Styles.ACCENT);
        downloadIcon.addStyle(Styles.BUTTON_CIRCLE);
        downloadIcon.addStyle(Styles.BUTTON_OUTLINED);
        downloadIcon.setX(ICON_X_OFFSET);
        tileLayout.addElement(downloadIcon);

        TextOverlay quantizationText = new TextOverlay(quantization);
        quantizationText.setUnderline(true);
        quantizationText.addStyle(Styles.TEXT_BOLD);
        quantizationText.setTooltip("The smaller the quantization the better performance. Comes at the cost of quality.");
        tileLayout.addElement(quantizationText);

        TextOverlay sizeText = new TextOverlay("Unknown"); // Will be updated asynchronously
        sizeText.addStyle(Styles.BUTTON_OUTLINED);
        sizeText.addStyle(Styles.TEXT_MUTED);
        tileLayout.addElement(sizeText);

        AtomicReference<FileInfo> fileInfoRef = new AtomicReference<>();

        File file = new File(App.getModelsDirectory(), "download-cache.dat");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        setupFileInfoFetch(tileLayout, modelKey, url, quantization, sizeText, downloadIcon, fileInfoRef);
        setupDownloadAction(tileLayout, downloadIcon, url, modelKey, fileInfoRef, titledContainer); // Pass titledContainer for relocation


        // Needs to be on a delay in order to properly put the card at the top.
        App.getThreadPoolManager().submitSchedule(() -> {
            Platform.runLater(() -> {
                // Check to see if the url is still being downloaded.
                DownloadInfo info = downloader.getDownloadInfo(url);
                if (info != null && !info.isComplete()) {
                    titledContainer.setExpanded(true);

                    // Re-apply the download indicators and controls based on the active DownloadInfo
                    FileInfo fileInfo = new FileInfo(info.getTotalFileSize(), info.getFileName(), modelKey);
                    fileInfo.setDownloaded(false);
                    fileInfoRef.set(fileInfo);
                    createDownloadInputs(tileLayout, downloadIcon, url, modelKey, fileInfoRef, downloader);

                    // Add downloads to the top of the view.
                    downloadListLayout.getPane().getChildren().remove(titledContainer.getTitledPane());
                    downloadListLayout.getPane().getChildren().addFirst(titledContainer.getTitledPane());
                }
            });
        }, 500, TimeUnit.MILLISECONDS);

        return tileLayout;
    }

    private void setupFileInfoFetch(HorizontalLayout tileLayout, String key, String url, String quant, TextOverlay sizeText, ButtonOverlay downloadIcon, AtomicReference<FileInfo> fileInfoRef) {
        long lastCheck;
        String dlKey = key + quant;
        if (downloadCache.has(dlKey)) {
            lastCheck = downloadCache.getLong(dlKey + ".fetch");
        } else {
            lastCheck = 0;
        }

        long instant = System.currentTimeMillis();
        long difference = instant - lastCheck;
        long millisInDay = 1000L * 60 * 60 * 24;

        if (difference < millisInDay && !downloadCache.getString(dlKey + ".name").equals("Unknown")) {
            if (downloadCache.has(dlKey)) {
                String name = downloadCache.getString(dlKey + ".name");
                long size = downloadCache.getLong(dlKey + ".size");
                FileInfo fileInfo = new FileInfo(size, name, key);
                fileInfoRef.set(fileInfo);

                tileLayout.addRenderEvent(event -> {
                    sizeText.setText(fileInfo.getDownloadSize());

                    // Check to see if the model is already downloaded
                    if (fileInfo.isDownloaded() && downloader.getDownloadInfo(url) == null) {
                        Platform.runLater(() -> {
                            addDownloadedTag(tileLayout);
                            downloadIcon.setEnabled(false);
                        });
                    }
                });
            }
        } else {
            App.getThreadPoolManager().submitTask(() -> {
                long fileSize = downloader.getRemoteFileSize(url);
                String fileName = url.substring(url.lastIndexOf('/') + 1);

                Platform.runLater(() -> {
                    FileInfo fileInfo = new FileInfo(fileSize, fileName, key);
                    fileInfoRef.set(fileInfo);

                    sizeText.setText(fileInfo.getDownloadSize());

                    if (fileInfo.isDownloaded() && downloader.getDownloadInfo(url) == null) {
                        addDownloadedTag(tileLayout);
                        downloadIcon.setEnabled(false);
                    }

                    // Write to cache
                    downloadCache.set(dlKey + ".name", fileName);
                    downloadCache.set(dlKey + ".size", fileSize);
                    downloadCache.set(dlKey + ".fetch", System.currentTimeMillis());

                    try {
                        downloadCache.save();
                    } catch (IOException e) {
                        App.logger.error("Error saving download cache", e);
                    }
                });
            });
        }
    }

    private void setupDownloadAction(HorizontalLayout tileLayout, ButtonOverlay downloadIcon, String url, String modelKey, AtomicReference<FileInfo> fileInfoRef, TitledLayout titledContainer) {
        downloadIcon.onClick(event -> {
            FileInfo fileInfo = fileInfoRef.get();
            // Check if download is already active
            if ((fileInfo == null || fileInfo.isDownloaded()) && downloader.getDownloadInfo(url) != null) {
                return;
            }

            // Move card to top on download start
            downloadListLayout.getPane().getChildren().remove(titledContainer.getTitledPane());
            downloadListLayout.getPane().getChildren().addFirst(titledContainer.getTitledPane());
            titledContainer.setExpanded(true);

            createDownloadInputs(tileLayout, downloadIcon, url, modelKey, fileInfoRef, downloader);
        });
    }

    private void createDownloadInputs(HorizontalLayout tileLayout, ButtonOverlay downloadIcon, String url, String modelKey, AtomicReference<FileInfo> fileInfoRef, FileDownloader downloader) {

        Platform.runLater(() -> {
            // Remove existing indicators if present (for re-entry)
            tileLayout.getPane().getChildren().removeIf(node -> node instanceof RingProgressIndicator || node instanceof TextField || (node instanceof Text && node.toString().contains("STOP_CIRCLE")));
            downloadIcon.setEnabled(false);
        });

        File modelDirectory = new File(App.getInstance().getSettings().getModelPath(), modelKey);

        // Ensure the model's directory exists
        if (!modelDirectory.exists()) {
            modelDirectory.mkdirs();
            App.logger.info("Created model directory: {}", modelDirectory.getAbsolutePath());
        }

        // The FileInfo object, which was populated during setupFileInfoFetch, holds the correct file name.
        FileInfo fileInfo = fileInfoRef.get();
        if (fileInfo == null || fileInfo.getFileName() == null || fileInfo.getFileName().equals("Unknown")) {
            return;
        }

        // The final destination file
        File destinationFile = new File(modelDirectory, fileInfo.getFileName().replace("?download=true", ""));

        // Check for existing download info (for re-entry)
        DownloadInfo existingInfo = downloader.getDownloadInfo(url);
        RingProgressIndicator progressIndicator = new RingProgressIndicator(0, false);

        TextOverlay stopButton = createStopButton(url, destinationFile, downloadIcon, tileLayout, fileInfoRef);
        stopButton.addStyle(Styles.LARGE);

        TextField downloadSpeed = new TextField("");
        downloadSpeed.setMaxWidth(100);
        downloadSpeed.setEditable(false);
        downloadSpeed.getStyleClass().add(Styles.ROUNDED);

        Platform.runLater(() -> {
            tileLayout.getPane().getChildren().add(progressIndicator);
            tileLayout.getPane().getChildren().add(downloadSpeed);
            tileLayout.getPane().getChildren().add(stopButton.assemble());
        });

        AtomicReference<Double> smoothedSpeedRef = new AtomicReference<>(0.0);

        AtomicLong lastDownload = new AtomicLong(existingInfo != null ? existingInfo.getDownloadedBytes() : 0L);
        AtomicLong lastChecked = new AtomicLong(System.currentTimeMillis());

        // Handle all events with the download.
        DownloadListener downloadListener = new DownloadListener() {

            private void cleanupAndRemove(DownloadInfo info, boolean success) {
                // Remove the listener first
                downloader.removeDownloadListener(this);

                App.logger.info("Download task finished/cancelled. Status: {}", success ? "SUCCESS" : "FAILED/CANCELED");

                // Perform all UI cleanup on the JavaFX thread
                Platform.runLater(() -> {
                    // Ensure the captured Nodes are removed using the exact references
                    tileLayout.getPane().getChildren().remove(progressIndicator);
                    tileLayout.getPane().getChildren().remove(stopButton.getNode());
                    tileLayout.getPane().getChildren().remove(downloadSpeed);

                    if (success) {
                        addDownloadedTag(tileLayout);
                        downloadIcon.setEnabled(false);
                        // Refresh model list on success
                        tabsContainer.replaceTab(tabsContainer.getTabs().get("List"), new ListTab(tabsContainer));
                    } else {
                        // Re-enable download button on failure/cancellation
                        downloadIcon.setEnabled(true);
                    }
                });
            }

            @Override
            public void onDownloadStart(DownloadInfo info) {
                // Initial progress update for the UI on start
                Platform.runLater(() -> {
                    progressIndicator.progressProperty().set(0.0);
                });
            }

            @Override
            public void onDownloadProgress(DownloadInfo info) {
                // if the passed info is null, either the download was cancelled or failed.
                if (info == null) {
                    downloader.removeDownloadListener(this);
                    return;
                }

                Platform.runLater(() -> {
                    double speed = calculateDownloadSpeed(info.getDownloadedBytes(), lastDownload, lastChecked, smoothedSpeedRef);
                    double mb = speed / (1024 * 1024);

                    // Update UI
                    downloadSpeed.setText(String.format("%.2f", mb) + " MB/s");
                    progressIndicator.progressProperty().set(info.getDownloadProgress());
                });
            }

            @Override
            public void onDownloadComplete(DownloadInfo info, File outputFile) {
                cleanupAndRemove(info, true);
            }

            @Override
            public void onDownloadError(DownloadInfo info, Exception e) {
                cleanupAndRemove(info, false);
            }

            @Override
            public void onDownloadCancel(DownloadInfo info) {
                cleanupAndRemove(info, false);
            }
        };

        downloader.addDownloadListener(downloadListener);
        if (existingInfo == null) {
            downloader.startDownload(url, destinationFile);
        }
    }

    private TextOverlay createStopButton(String url, File fileToDelete, ButtonOverlay downloadIcon, HorizontalLayout tileLayout, AtomicReference<FileInfo> fileInfoRef) {
        TextOverlay stop = new TextOverlay(new FontIcon(Material2MZ.STOP_CIRCLE));
        stop.addStyle(Styles.DANGER);
        stop.addStyle(Styles.TITLE_4);
        stop.onClick(event -> {
            App.logger.info("Attempting to stop current download: {}", url);

            // Calls the thread to be cancelled and clears mappings.
            downloader.cancelDownload(url);

            // Synchronous File Deletion (must be here as a final cleanup)
            if (fileToDelete.exists()) {
                fileToDelete.delete();
                App.logger.info("Deleted partial file: {}", fileToDelete.getName());
            }

            fileInfoRef.get().setDownloaded(false);
        });
        return stop;
    }

    private void addDownloadedTag(HorizontalLayout tileLayout) {
        ButtonOverlay tag = new ButtonBuilder("tag").setText("Downloaded").build();
        tag.addStyle(Styles.BUTTON_OUTLINED);
        Platform.runLater(() -> {
            if (tileLayout.getElements().size() == 3) {
                tileLayout.addElement(tag);
                tag.setEnabled(false);
            }
        });

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

    protected static class DownloadModel {
        private final String key;
        private final String name;
        private final Map<String, String> links = new LinkedHashMap<>();

        protected DownloadModel(String key, String name, Map<String, Object> rawLinks) {
            this.key = key;
            this.name = name;
            // Convert object into string for links
            rawLinks.forEach((quantization, urlObject) -> links.put(quantization, urlObject.toString()));
        }

        public Map<String, String> getLinks() {
            return links;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }
    }

    protected static class FileInfo {
        private final long sizeInBytes;
        private final String fileName;
        private final String key;
        private boolean downloaded = false;

        public FileInfo(long sizeInBytes, String fileName, String key) {
            this.sizeInBytes = sizeInBytes;
            this.fileName = fileName;
            this.key = key;
            checkIfDownloaded();
        }

        private void checkIfDownloaded() {
            for (Model model : App.getModels("all").values()) { // Assuming App.getModels exists
                if (model.getFile() != null && model.getFile().getName().equalsIgnoreCase(fileName) && model.getFile().getParent().contains(key)) {
                    this.downloaded = true;
                    break;
                }
            }
        }

        public long getSizeInBytes() {
            return sizeInBytes;
        }

        public String getFileName() {
            return fileName;
        }

        public boolean isDownloaded() {
            return downloaded;
        }

        public void setDownloaded(boolean downloaded) {
            this.downloaded = downloaded;
        }

        public String getDownloadSize() {
            if (sizeInBytes <= 0) {
                return "0.00 GB";
            }
            double gigabytes = (double) sizeInBytes / (1024L * 1024L * 1024L);
            return String.format("%.2f GB", gigabytes);
        }
    }
}