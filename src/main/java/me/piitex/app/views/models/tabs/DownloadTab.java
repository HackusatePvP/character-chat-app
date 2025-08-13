package me.piitex.app.views.models.tabs;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.text.Text;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.FileDownloadProcess;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.utils.ConfigUtil;
import me.piitex.app.views.models.ModelsView;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.containers.tabs.TabsContainer;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.TitledLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonBuilder;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.TextOverlay;
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
        ScrollContainer container = new ScrollContainer(
                contentLayout,
                0,
                20,
                appSettings.getWidth() - SCROLL_MAX_WIDTH_OFFSET,
                appSettings.getHeight() - SCROLL_MAX_HEIGHT_OFFSET
        );
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
            HorizontalLayout tileLayout = createDownloadTile(titledContainer, quantization, url, downloadModel.getKey());

            Platform.runLater(() -> {
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
        setupDownloadAction(tileLayout, downloadIcon, url, modelKey, fileInfoRef);


        // Needs to be on a delay in order to properly put the card at the top.
        App.getThreadPoolManager().submitSchedule(() -> {
            Platform.runLater(() -> {
                // This has to be done AFTER the layout has rendered.
                // If the user left the download page the download will continue.
                // When they re-enter the page check to see if the url is still being downloaded.
                // If so re-apply the download indicators and controls.
                if (FileDownloadProcess.getCurrentDownloads().containsKey(url)) {
                    titledContainer.setExpanded(true);
                    FileInfo fileInfo = new FileInfo(0, "Unknown", modelKey);
                    fileInfo.setDownloaded(false);
                    fileInfoRef.set(fileInfo);
                    createDownloadInputs(tileLayout, downloadIcon, url, modelKey, fileInfoRef);

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
            lastCheck = 0; // Or System.currentTimeMillis() if you want the first check to be instant
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

                    if (fileInfo.isDownloaded() && !FileDownloadProcess.getCurrentDownloads().containsKey(url)) {
                        Platform.runLater(() -> {
                            addDownloadedTag(tileLayout);
                            downloadIcon.setEnabled(false);
                        });
                    }
                });
            }
        } else {
            FileDownloadProcess fileInfoFetcher = new FileDownloadProcess();
            fileInfoFetcher.getFileInfoByUrlAsync(url);
            fileInfoFetcher.setFileInfoCompleteListener(result -> Platform.runLater(() -> {
                String fileName = result.getFileName().orElse("Unknown");
                long fileSize = result.getFileSizeInBytes().orElse(0L);
                FileInfo fileInfo = new FileInfo(fileSize, fileName, key);
                fileInfoRef.set(fileInfo);

                sizeText.setText(fileInfo.getDownloadSize());

                if (fileInfo.isDownloaded() && !FileDownloadProcess.getCurrentDownloads().containsKey(url)) {
                    addDownloadedTag(tileLayout);
                    downloadIcon.setEnabled(false);
                }

                // Write long
                downloadCache.set(dlKey + ".name", fileName);
                downloadCache.set(dlKey + ".size", fileSize);
                downloadCache.set(dlKey + ".fetch", System.currentTimeMillis());

                try {
                    downloadCache.save();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }
    }

    private void setupDownloadAction(HorizontalLayout tileLayout, ButtonOverlay downloadIcon, String url, String modelKey, AtomicReference<FileInfo> fileInfoRef) {
        downloadIcon.onClick(event -> {
            FileInfo fileInfo = fileInfoRef.get();
            if ((fileInfo == null || fileInfo.isDownloaded()) && !FileDownloadProcess.getCurrentDownloads().containsKey(url)) {
                return;
            }

            createDownloadInputs(tileLayout, downloadIcon, url, modelKey, fileInfoRef);
        });
    }

    private void createDownloadInputs(HorizontalLayout tileLayout, ButtonOverlay downloadIcon, String url, String modelKey, AtomicReference<FileInfo> fileInfoRef) {

        Platform.runLater(() -> {
            downloadIcon.setEnabled(false);
        });


        FileDownloadProcess downloadProcess;
        FileDownloadProcess.DownloadTask currentTask;
        File destinationFile = new File(App.getInstance().getSettings().getModelPath(), modelKey);
        if (FileDownloadProcess.getCurrentDownloads().containsKey(url)) {
            downloadProcess = FileDownloadProcess.getCurrentDownloads().get(url);
            currentTask = FileDownloadProcess.getCurrentDownloadTasks().get(url);
        } else {
            downloadProcess = new FileDownloadProcess();
            currentTask = downloadProcess.downloadFileAsync(url, destinationFile.toPath());
        }

        RingProgressIndicator progressIndicator = new RingProgressIndicator(0, false);

        TextOverlay stopButton = createStopButton(currentTask, destinationFile, downloadIcon, tileLayout, downloadProcess, fileInfoRef);
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

        AtomicLong lastDownload = new AtomicLong(0L);
        AtomicLong lastChecked = new AtomicLong(0L);

        downloadProcess.setDownloadProgressListener((totalBytesRead, totalFileSize, percent) -> {
            Platform.runLater(() -> {
                progressIndicator.progressProperty().set((double) totalBytesRead / totalFileSize);
                double speed = calculateDownloadSpeed(totalBytesRead, lastDownload, lastChecked);
                double mb = speed / (1024 * 1024);
                downloadSpeed.setText(String.format("%.2f", mb) + " MB/s");
            });
        });

        downloadProcess.setDownloadCompleteListener(result -> Platform.runLater(() -> {
            // Remove the download control buttons
            tileLayout.getPane().getChildren().remove(progressIndicator);
            tileLayout.getPane().getChildren().remove(stopButton.assemble());
            tileLayout.getPane().getChildren().remove(downloadSpeed);

            if (result.isSuccess()) {
                addDownloadedTag(tileLayout);
            } else {
                downloadIcon.setEnabled(true);
            }

            tabsContainer.replaceTab(tabsContainer.getTabs().get("List"), new ListTab(tabsContainer));

        }));

    }

    private TextOverlay createStopButton(FileDownloadProcess.DownloadTask downloadTask, File fileToDelete, ButtonOverlay downloadIcon, HorizontalLayout tileLayout, FileDownloadProcess downloadProcess, AtomicReference<FileInfo> fileInfoRef) {
        TextOverlay stop = new TextOverlay(new FontIcon(Material2MZ.STOP_CIRCLE));
        stop.addStyle(Styles.DANGER);
        stop.addStyle(Styles.TITLE_4);
        stop.onClick(event -> {
            App.logger.info("Attempting to stop current download...");

            downloadTask.cancel();
            if (fileToDelete.exists()) {
                fileToDelete.delete();
            }
            // Remove UI elements and re-enable download button
            Platform.runLater(() -> {
                tileLayout.getPane().getChildren().removeIf(node -> node instanceof RingProgressIndicator || (node instanceof Text && ((Text)node).getText().equals(new FontIcon(Material2MZ.STOP_CIRCLE).toString())));
                downloadIcon.setEnabled(true);
            });

            fileInfoRef.get().setDownloaded(false);
        });
        return stop;
    }

    private void addDownloadedTag(HorizontalLayout tileLayout) {
        ButtonOverlay tag = new ButtonBuilder("tag").setText("Downloaded").build();
        tag.setEnabled(false);
        tag.addStyle(Styles.BUTTON_OUTLINED);
        Platform.runLater(() -> {
            if (tileLayout.getElements().size() == 3) {
                tileLayout.addElement(tag);
            }
        });

    }


    private double calculateDownloadSpeed(long totalBytesRead, AtomicLong lastDownloadBytes, AtomicLong lastDownloadTime) {
        long currentTime = System.currentTimeMillis();

        // Calculate bytes downloaded since the last update
        long bytesDownloadedSinceLastUpdate = totalBytesRead - lastDownloadBytes.get();

        // Calculate time elapsed since the last update
        long timeElapsedSinceLastUpdate = currentTime - lastDownloadTime.get();

        double bytesPerSecond = 0.0;

        // Ensure both time has passed and bytes have been downloaded to avoid division by zero
        // and to get a meaningful speed.
        if (timeElapsedSinceLastUpdate > 0 && bytesDownloadedSinceLastUpdate > 0) {
            // Convert milliseconds to seconds (timeElapsedSinceLastUpdate / 1000.0)
            bytesPerSecond = (double) bytesDownloadedSinceLastUpdate / (timeElapsedSinceLastUpdate / 1000.0);
        }

        // IMPORTANT: Update the 'last' values for the next calculation
        lastDownloadBytes.set(totalBytesRead);
        lastDownloadTime.set(currentTime);

        return bytesPerSecond;
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
            for (Model model : App.getModels("all")) { // Assuming App.getModels exists
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