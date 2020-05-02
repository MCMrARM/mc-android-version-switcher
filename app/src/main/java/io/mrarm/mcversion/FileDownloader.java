package io.mrarm.mcversion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownloader {

    private HttpURLConnection conn;
    private CompletionCallback completionCallback;
    private ErrorCallback errorCallback;
    private ProgressCallback progressCallback;

    private synchronized void assertNotExecuted() {
        if (conn != null)
            throw new RuntimeException("Can only set conn before execute()");
    }

    public void setCompletionCallback(CompletionCallback completionCallback) {
        assertNotExecuted();
        this.completionCallback = completionCallback;
    }

    public void setErrorCallback(ErrorCallback errorCallback) {
        assertNotExecuted();
        this.errorCallback = errorCallback;
    }

    public void setProgressCallback(ProgressCallback progressCallback) {
        assertNotExecuted();
        this.progressCallback = progressCallback;
    }

    public void execute(String url, File outPath) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            synchronized (this) {
                this.conn = conn;
            }
            if (conn == null)
                throw new IOException();
            long contentLength = conn.getContentLength();
            try (InputStream inStream = conn.getInputStream();
                 OutputStream outStream = new FileOutputStream(outPath)) {
                byte[] b = new byte[128 * 1024];
                int n;
                long downloaded = 0;
                while ((n = inStream.read(b)) > 0) {
                    outStream.write(b, 0, n);
                    downloaded += n;
                    if (progressCallback != null)
                        progressCallback.onProgress(downloaded, contentLength);
                }
            }
            completionCallback.onComplete();
        } catch (IOException e) {
            errorCallback.onError(e);
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    public synchronized void cancel() {
        if (conn != null)
            conn.disconnect();
    }

    public interface CompletionCallback {
        void onComplete();
    }

    public interface ErrorCallback {
        void onError(IOException exception);
    }

    public interface ProgressCallback {
        void onProgress(long downloaded, long total);
    }

}
