package me.piitex.app.backend;

public interface DownloadProgressListener {
    void onProgress(long totalBytesRead, long totalFileSize, int percent);
}