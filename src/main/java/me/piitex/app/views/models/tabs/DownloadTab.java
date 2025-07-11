package me.piitex.app.views.models.tabs;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.text.Text;
import me.piitex.app.App;
import me.piitex.app.backend.Model;
import me.piitex.app.backend.FileDownloadProcess;
import me.piitex.app.configuration.AppSettings;
import me.piitex.app.utils.ConfigUtil;
import me.piitex.engine.containers.ScrollContainer;
import me.piitex.engine.containers.TitledContainer;
import me.piitex.engine.containers.tabs.Tab;
import me.piitex.engine.layouts.HorizontalLayout;
import me.piitex.engine.layouts.VerticalLayout;
import me.piitex.engine.overlays.ButtonOverlay;
import me.piitex.engine.overlays.TextOverlay;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class DownloadTab extends Tab {

    private final AppSettings appSettings;
    private final ScrollContainer scrollContainer;
    private final VerticalLayout downloadListLayout;

    private static final int LAYOUT_SPACING = 0;
    private static final int SCROLL_CONTAINER_PADDING = 20;
    private static final int TILE_LAYOUT_WIDTH = 400;
    private static final int TILE_LAYOUT_HEIGHT = 75;
    private static final int ICON_X_OFFSET = 20;
    private static final int QUANT_WIDTH = 100;
    private static final int CONTAINER_SPACING = 15;
    private static final int PREF_WIDTH_OFFSET = 300;
    private static final int SCROLL_MAX_WIDTH_OFFSET = 250;
    private static final int SCROLL_MAX_HEIGHT_OFFSET = 200;


    public DownloadTab() {
        super("Download");
        this.appSettings = App.getInstance().getAppSettings();

        this.downloadListLayout = createMainLayout();

        this.scrollContainer = createScrollContainer(downloadListLayout);
        addElement(scrollContainer);

        loadAndBuildDownloadList();
    }

    private VerticalLayout createMainLayout() {
        VerticalLayout layout = new VerticalLayout(LAYOUT_SPACING, LAYOUT_SPACING);
        layout.setSpacing(LAYOUT_SPACING);
        layout.setPrefSize(appSettings.getWidth() - PREF_WIDTH_OFFSET, LAYOUT_SPACING);
        return layout;
    }

    private ScrollContainer createScrollContainer(VerticalLayout contentLayout) {
        ScrollContainer container = new ScrollContainer(
                contentLayout,
                SCROLL_CONTAINER_PADDING,
                SCROLL_CONTAINER_PADDING,
                appSettings.getWidth() - PREF_WIDTH_OFFSET,
                appSettings.getHeight() - SCROLL_MAX_HEIGHT_OFFSET
        );
        container.setMaxSize(appSettings.getWidth() - SCROLL_MAX_WIDTH_OFFSET, appSettings.getHeight() - SCROLL_MAX_HEIGHT_OFFSET);
        container.setVerticalScroll(true);
        container.setScrollWhenNeeded(true);
        container.setHorizontalScroll(false);
        return container;
    }

    private void loadAndBuildDownloadList() {
        new Thread(() -> {
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

                TitledContainer downloadContainer = createDownloadContainer(name, description, downloadModel);

                Platform.runLater(() -> downloadListLayout.getPane().getChildren().add(downloadContainer.build().getKey()));
            }
        }).start();
    }

    private TitledContainer createDownloadContainer(String title, String description, DownloadModel downloadModel) {
        TitledContainer titledContainer = new TitledContainer(title, LAYOUT_SPACING, LAYOUT_SPACING);
        titledContainer.addStyle(Styles.DENSE);
        titledContainer.addStyle(Tweaks.ALT_ICON);
        titledContainer.setSpacing(CONTAINER_SPACING);
        titledContainer.setCollapse(true);
        titledContainer.setExpanded(false);
        titledContainer.setMaxSize(scrollContainer.getWidth() - QUANT_WIDTH, scrollContainer.getHeight() - QUANT_WIDTH);

        TextOverlay descriptionOverlay = new TextOverlay(description);
        titledContainer.addElement(descriptionOverlay);

        downloadModel.getLinks().forEach((quantization, url) -> {
            HorizontalLayout tileLayout = createDownloadTile(quantization, url, downloadModel.getKey());
            titledContainer.addElement(tileLayout);
        });

        return titledContainer;
    }

    private HorizontalLayout createDownloadTile(String quantization, String url, String modelKey) {
        HorizontalLayout tileLayout = new HorizontalLayout(TILE_LAYOUT_WIDTH, TILE_LAYOUT_HEIGHT);
        tileLayout.setAlignment(Pos.CENTER_LEFT);
        tileLayout.addStyle(Styles.BORDER_MUTED);
        tileLayout.setSpacing(50);

        ButtonOverlay downloadIcon = new ButtonOverlay("download", new FontIcon(Material2MZ.SAVE_ALT));
        downloadIcon.addStyle(Styles.ACCENT);
        downloadIcon.addStyle(Styles.BUTTON_CIRCLE);
        downloadIcon.addStyle(Styles.BUTTON_OUTLINED);
        downloadIcon.setX(ICON_X_OFFSET);
        tileLayout.addElement(downloadIcon);

        TextOverlay quantizationText = new TextOverlay(quantization);
        quantizationText.setWidth(QUANT_WIDTH);
        quantizationText.setUnderline(true);
        quantizationText.addStyle(Styles.TEXT_BOLD);
        quantizationText.setTooltip("The smaller the quantization the better performance. Comes at the cost of quality.");
        tileLayout.addElement(quantizationText);

        TextOverlay sizeText = new TextOverlay(""); // Will be updated asynchronously
        sizeText.addStyle(Styles.BUTTON_OUTLINED);
        sizeText.addStyle(Styles.TEXT_MUTED);
        tileLayout.addElement(sizeText);

        AtomicReference<FileInfo> fileInfoRef = new AtomicReference<>();
        setupFileInfoFetch(modelKey, url, sizeText, downloadIcon, fileInfoRef, tileLayout);
        setupDownloadAction(downloadIcon, url, modelKey, fileInfoRef, tileLayout);

        return tileLayout;
    }

    private void setupFileInfoFetch(String key, String url, TextOverlay sizeText, ButtonOverlay downloadIcon,
                                    AtomicReference<FileInfo> fileInfoRef, HorizontalLayout tileLayout) {
        FileDownloadProcess fileInfoFetcher = new FileDownloadProcess();
        fileInfoFetcher.getFileInfoByUrlAsync(url);
        fileInfoFetcher.setFileInfoCompleteListener(result -> Platform.runLater(() -> {
            String fileName = result.getFileName().orElse("Unknown");
            long fileSize = result.getFileSizeInBytes().orElse(0L);
            FileInfo fileInfo = new FileInfo(fileSize, fileName, key);
            fileInfoRef.set(fileInfo);

            ((Text) sizeText.getNode()).setText(fileInfo.getDownloadSize());

            if (fileInfo.isDownloaded()) {
                addDownloadedTag(tileLayout);
                downloadIcon.getNode().setDisable(true);
            }
            fileInfoFetcher.shutdown();
        }));
    }

    private void setupDownloadAction(ButtonOverlay downloadIcon, String url, String modelKey,
                                     AtomicReference<FileInfo> fileInfoRef, HorizontalLayout tileLayout) {
        downloadIcon.onClick(event -> {
            FileInfo fileInfo = fileInfoRef.get();
            if (fileInfo == null || fileInfo.isDownloaded()) {
                return;
            }

            downloadIcon.getNode().setDisable(true);

            FileDownloadProcess downloadProcess = new FileDownloadProcess();
            File destinationFile = new File(App.getInstance().getSettings().getModelPath(), modelKey);

            RingProgressIndicator progressIndicator = new RingProgressIndicator(0, false);

            FileDownloadProcess.DownloadTask currentTask = downloadProcess.downloadFileAsync(url, destinationFile.toPath());

            TextOverlay stopButton = createStopButton(currentTask, destinationFile, downloadIcon, tileLayout, downloadProcess);

            Platform.runLater(() -> {
                tileLayout.getPane().getChildren().add(progressIndicator);
                tileLayout.getPane().getChildren().add(stopButton.render());
            });

            downloadProcess.setDownloadProgressListener((totalBytesRead, totalFileSize, percent) ->
                    Platform.runLater(() -> progressIndicator.progressProperty().set((double) totalBytesRead / totalFileSize))
            );

            downloadProcess.setDownloadCompleteListener(result -> Platform.runLater(() -> {
                // Remove the download control buttons
                tileLayout.getPane().getChildren().remove(progressIndicator);
                tileLayout.getPane().getChildren().remove(stopButton.render());
                downloadProcess.shutdown();

                if (result.isSuccess()) {
                    fileInfo.setDownloaded(true);
                    addDownloadedTag(tileLayout);
                } else {
                    downloadIcon.getNode().setDisable(false);
                }
            }));
        });
    }

    private TextOverlay createStopButton(FileDownloadProcess.DownloadTask downloadTask, File fileToDelete,
                                         ButtonOverlay downloadIcon, HorizontalLayout tileLayout, FileDownloadProcess downloadProcess) {
        TextOverlay stop = new TextOverlay(new FontIcon(Material2MZ.STOP_CIRCLE));
        stop.addStyle(Styles.DANGER);
        stop.addStyle(Styles.TITLE_4);
        stop.onClick(event -> {
            downloadTask.cancel();
            if (fileToDelete.exists()) {
                fileToDelete.delete();
            }
            // Remove UI elements and re-enable download button
            Platform.runLater(() -> {
                tileLayout.getPane().getChildren().removeIf(node -> node instanceof RingProgressIndicator || (node instanceof Text && ((Text)node).getText().equals(new FontIcon(Material2MZ.STOP_CIRCLE).toString())));
                downloadIcon.getNode().setDisable(false);
            });
            downloadProcess.shutdown();
        });
        return stop;
    }

    private void addDownloadedTag(HorizontalLayout tileLayout) {
        ButtonOverlay tag = new ButtonOverlay("tag", "Downloaded");
        tag.setEnabled(false);
        tag.addStyle(Styles.BUTTON_OUTLINED);
        Platform.runLater(() -> tileLayout.getPane().getChildren().add(tag.render()));
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