package me.piitex.app.backend.server;

public interface ServerLoadingListener {
    /**
     * Called when the server has finished loading (either successfully or with an error).
     * @param success true if the server loaded successfully, false if an error occurred.
     */
    void onServerLoadingComplete(boolean success);
}