package me.piitex.app.backend;

import me.piitex.app.App;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileDownloadProcess {

    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename\\*?=['\"]?(?:UTF-8''|)([^;\"\\s]+)");

    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    private DownloadCompleteListener downloadCompleteListener;
    private DownloadProgressListener downloadProgressListener;
    private FileInfoCompleteListener fileInfoCompleteListener;

    public void setDownloadCompleteListener(DownloadCompleteListener listener) {
        this.downloadCompleteListener = listener;
    }

    public void setDownloadProgressListener(DownloadProgressListener listener) {
        this.downloadProgressListener = listener;
    }

    public void setFileInfoCompleteListener(FileInfoCompleteListener listener) {
        this.fileInfoCompleteListener = listener;
    }

    public static class DownloadResult {
        private final boolean success;
        private final Optional<String> fileName;
        private final Optional<Long> fileSizeInBytes;
        private final Optional<String> errorMessage;
        private final boolean cancelled;

        public DownloadResult(boolean success, Optional<String> fileName, Optional<Long> fileSizeInBytes) {
            this(success, fileName, fileSizeInBytes, Optional.empty(), false);
        }

        public DownloadResult(boolean cancelled, Optional<String> fileName, Optional<Long> fileSizeInBytes, boolean isCancelled) {
            this(false, fileName, fileSizeInBytes, Optional.of("Download cancelled."), isCancelled);
        }

        public DownloadResult(boolean success, Optional<String> fileName, Optional<Long> fileSizeInBytes, Optional<String> errorMessage, boolean cancelled) {
            this.success = success;
            this.fileName = fileName;
            this.fileSizeInBytes = fileSizeInBytes;
            this.errorMessage = errorMessage;
            this.cancelled = cancelled;
        }

        public boolean isSuccess() {
            return success;
        }

        public Optional<String> getFileName() {
            return fileName;
        }

        public Optional<Long> getFileSizeInBytes() {
            return fileSizeInBytes;
        }

        public Optional<String> getErrorMessage() {
            return errorMessage;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public Optional<String> getFileSizeInGB() {
            return fileSizeInBytes.map(bytes -> {
                if (bytes <= 0) {
                    return "0.00 GB";
                }
                double gigabytes = (double) bytes / (1024.0 * 1024.0 * 1024.0);
                return String.format("%.2f GB", gigabytes);
            });
        }

        @Override
        public String toString() {
            return "DownloadResult{" +
                    "success=" + success +
                    ", fileName=" + fileName.orElse("N/A") +
                    ", fileSizeInBytes=" + fileSizeInBytes.map(Object::toString).orElse("N/A") +
                    ", errorMessage=" + errorMessage.orElse("N/A") +
                    ", cancelled=" + cancelled +
                    '}';
        }
    }

    public static class DownloadTask {
        private final Future<?> future;
        private volatile HttpGet activeRequest;
        private volatile CloseableHttpClient activeClient;

        public DownloadTask(Future<?> future) {
            this.future = future;
        }

        public void setActiveRequest(HttpGet activeRequest) {
            this.activeRequest = activeRequest;
        }

        public void setActiveClient(CloseableHttpClient activeClient) {
            this.activeClient = activeClient;
        }

        public boolean cancel() {
            if (activeRequest != null) {
                activeRequest.abort();
            }
            if (activeClient != null) {
                try {
                    activeClient.close();
                } catch (IOException e) {
                    App.logger.error("Error occurred while cancelling download!", e);
                }
            }
            return future.cancel(true);
        }

        public boolean isDone() {
            return future.isDone();
        }

        public boolean isCancelled() {
            return future.isCancelled();
        }
    }

    public DownloadTask downloadFileAsync(String fileUrl, Path destinationDirectory) {
        DownloadTask task = new DownloadTask(null);

        Future<?> future = executorService.submit(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(fileUrl);

                task.setActiveClient(httpClient);
                task.setActiveRequest(httpGet);

                DownloadResult result = downloadFile(fileUrl, destinationDirectory, httpGet, httpClient);
                if (downloadCompleteListener != null) {
                    downloadCompleteListener.onDownloadComplete(result);
                }
            } catch (IOException e) {
                App.logger.error("Error occurred while downloading model!", e);
                DownloadResult result = new DownloadResult(false, Optional.empty(), Optional.empty(),
                        Optional.of("Async task setup error: " + e.getMessage()), false);
                if (downloadCompleteListener != null) {
                    downloadCompleteListener.onDownloadComplete(result);
                }
            }
        });
        try {
            java.lang.reflect.Field futureField = DownloadTask.class.getDeclaredField("future");
            futureField.setAccessible(true);
            futureField.set(task, future);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            App.logger.error("Error occurred while downloading model!", e);
        }
        return task;
    }

    public DownloadResult downloadFile(String fileUrl, Path destinationDirectory, HttpGet httpRequest, CloseableHttpClient httpClient) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return new DownloadResult(false, Optional.empty(), Optional.empty(),
                    Optional.of("File URL cannot be null or empty for download."), false);
        }

        if (!destinationDirectory.toFile().exists() && !destinationDirectory.toFile().mkdirs()) {
            return new DownloadResult(false, Optional.empty(), Optional.empty(),
                    Optional.of("Failed to create destination directory: " + destinationDirectory), false);
        }

        try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
            int statusCode = response.getCode();

            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return new DownloadResult(false, Optional.empty(), Optional.empty(),
                            Optional.of("No content found in the response for URL: " + fileUrl), false);
                }

                Optional<Long> fileSize = Optional.ofNullable(entity.getContentLength())
                        .filter(len -> len >= 0);

                Optional<String> fileName = getFileNameFromHeaders(response)
                        .or(() -> getFileNameFromUrl(fileUrl));

                String finalFileName = fileName.orElse("downloaded_file");
                Path outputFile = destinationDirectory.resolve(finalFileName);

                long totalBytesRead = 0;
                long totalFileSize = fileSize.orElse(-1L);
                int lastReportedPercent = -1;

                try (InputStream inputStream = entity.getContent();
                     FileOutputStream outputStream = new FileOutputStream(outputFile.toFile())) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        if (Thread.currentThread().isInterrupted() || httpRequest.isAborted()) {
                            try { outputFile.toFile().delete(); } catch (Exception e) { /* ignore cleanup error */ }
                            return new DownloadResult(true, fileName, fileSize, true);
                        }

                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        if (downloadProgressListener != null) {
                            int currentPercent = -1;
                            if (totalFileSize > 0) {
                                currentPercent = (int) ((totalBytesRead * 100) / totalFileSize);
                            }

                            if (currentPercent != lastReportedPercent ||
                                    (totalFileSize <= 0 && totalBytesRead % (4096L * 10) == 0) ||
                                    totalBytesRead == bytesRead) {
                                downloadProgressListener.onProgress(totalBytesRead, totalFileSize, currentPercent);
                                lastReportedPercent = currentPercent;
                            }
                        }
                    }
                    outputStream.flush();
                    EntityUtils.consume(entity);

                    if (downloadProgressListener != null) {
                        downloadProgressListener.onProgress(totalBytesRead, totalFileSize, (totalFileSize > 0) ? 100 : -1);
                    }

                    final long actualTotalBytesRead = totalBytesRead;

                    return new DownloadResult(true, Optional.of(finalFileName), fileSize.or(() -> Optional.of(actualTotalBytesRead)), Optional.empty(), false);

                } catch (IOException e) {
                    if (httpRequest.isAborted() || Thread.currentThread().isInterrupted()) {
                        try { outputFile.toFile().delete(); } catch (Exception ex) { /* ignore cleanup error */ }
                        return new DownloadResult(true, fileName, fileSize, true);
                    }
                    App.logger.error("Error occurred while downloading model!", e);
                    return new DownloadResult(false, fileName, fileSize,
                            Optional.of("Error writing file to disk: " + e.getMessage()), false);
                }
            } else {
                String errorMsg = "HTTP Error: " + statusCode + " " + response.getReasonPhrase() + " for URL: " + fileUrl;
                EntityUtils.consume(response.getEntity());
                return new DownloadResult(false, Optional.empty(), Optional.empty(), Optional.of(errorMsg), false);
            }
        } catch (IOException e) {
            if (httpRequest.isAborted() || Thread.currentThread().isInterrupted()) {
                return new DownloadResult(true, Optional.empty(), Optional.empty(), true);
            }
            App.logger.error("Error occurred while downloading model!", e);
            return new DownloadResult(false, Optional.empty(), Optional.empty(),
                    Optional.of("Network or client error: " + e.getMessage()), false);
        }
    }

    public void getFileInfoByUrlAsync(String fileUrl) {
        executorService.submit(() -> {
            DownloadResult result = getFileInfoByUrl(fileUrl);
            if (fileInfoCompleteListener != null) {
                fileInfoCompleteListener.onFileInfoComplete(result);
            }
        });
    }

    public DownloadResult getFileInfoByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return new DownloadResult(false, Optional.empty(), Optional.empty(),
                    Optional.of("File URL cannot be null or empty for info retrieval."), false);
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpHead httpHead = new HttpHead(fileUrl);

            try (CloseableHttpResponse response = httpClient.execute(httpHead)) {
                int statusCode = response.getCode();

                if (statusCode == HttpStatus.SC_OK) {
                    Optional<Long> fileSize = Optional.ofNullable(response.getFirstHeader("Content-Length"))
                            .map(Header::getValue)
                            .flatMap(s -> {
                                try {
                                    long len = Long.parseLong(s);
                                    return (len >= 0) ? Optional.of(len) : Optional.empty();
                                } catch (NumberFormatException e) {
                                    return Optional.empty();
                                }
                            });

                    Optional<String> fileName = getFileNameFromHeaders(response)
                            .or(() -> getFileNameFromUrl(fileUrl));

                    if (response.getEntity() != null) {
                        EntityUtils.consume(response.getEntity());
                    }

                    return new DownloadResult(true, fileName, fileSize, false);
                } else {
                    String errorMsg = "HTTP Error (HEAD): " + statusCode + " " + response.getReasonPhrase() + " for URL: " + fileUrl;
                    if (response.getEntity() != null) {
                        EntityUtils.consume(response.getEntity());
                    }
                    return new DownloadResult(false, Optional.empty(), Optional.empty(), Optional.of(errorMsg), false);
                }
            }
        } catch (IOException e) {
            App.logger.error("Error occurred while downloading model!", e);
            return new DownloadResult(false, Optional.empty(), Optional.empty(),
                    Optional.of("Network or client error (HEAD): " + e.getMessage()), false);
        }
    }

    private Optional<String> getFileNameFromHeaders(CloseableHttpResponse response) {
        Header contentDispositionHeader = response.getFirstHeader("Content-Disposition");
        if (contentDispositionHeader != null) {
            String headerValue = contentDispositionHeader.getValue();
            Matcher matcher = FILENAME_PATTERN.matcher(headerValue);
            if (matcher.find()) {
                String encodedFileName = matcher.group(1);
                try {
                    return Optional.of(URLDecoder.decode(encodedFileName, StandardCharsets.UTF_8));
                } catch (IllegalArgumentException e) {
                    return Optional.of(encodedFileName);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> getFileNameFromUrl(String fileUrl) {
        try {
            URI uri = new URI(fileUrl);
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                int lastSlashIndex = path.lastIndexOf('/');
                if (lastSlashIndex != -1 && lastSlashIndex < path.length() - 1) {
                    String fileName = path.substring(lastSlashIndex + 1);
                    int questionMarkIndex = fileName.indexOf('?');
                    if (questionMarkIndex != -1) {
                        fileName = fileName.substring(0, questionMarkIndex);
                    }
                    return Optional.of(URLDecoder.decode(fileName, StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            App.logger.error("Error occurred while downloading model!", e);
        }
        return Optional.empty();
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}