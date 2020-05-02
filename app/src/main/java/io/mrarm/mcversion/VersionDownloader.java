package io.mrarm.mcversion;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class VersionDownloader {

    private File storageDir;

    public VersionDownloader(File storageDir) {
        this.storageDir = storageDir;
    }

    public File getVersionDir(UiVersion version) {
        return new File(storageDir, String.valueOf(version.getVersionCode()));
    }

    public void updateDownloadedStatus(UiVersion version) {
        version.isDownloaded().set(getVersionDir(version).exists());
    }

    public void download(Context context, UiVersion version) {
        if (!PlayHelper.getInstance(context).isAuthedToApi()) {
            Toast.makeText(context, R.string.error_api_not_authenticated, Toast.LENGTH_LONG).show();
            return;
        }
        if (version.isDownloading().get())
            return;
        version.getDownloadComplete().set(0);
        version.getDownloadTotal().set(0);
        version.isDownloading().set(true);
        PlayHelper.getInstance(context).getApi().requestDelivery("com.mojang.minecraftpe", version.getVersionCode(), new PlayApi.DeliveryCallback() {
            @Override
            public void onSuccess(PlayApi.DownloadInfo[] download) {
                for (PlayApi.DownloadInfo i : download)
                    Log.d("VersionDownloader", "Download: " + i.getName() + " " + i.getGzippedUrl());

                UiThreadHelper.runOnUiThread(() -> {
                    DownloadTracker tracker = new DownloadTracker(context, version, download, getVersionDir(version));
                    tracker.downloadNextFile();
                });
            }

            @Override
            public void onError(String str) {
                UiThreadHelper.runOnUiThread(() -> {
                    Toast.makeText(context, str, Toast.LENGTH_LONG).show();
                    version.isDownloading().set(false);
                });
            }
        });
    }

    public void delete(UiVersion version) {
        IOUtil.deleteDirectory(getVersionDir(version));
        version.isDownloaded().set(false);
    }

    private static class DownloadTracker {

        private final Context context;
        private final UiVersion version;
        private final PlayApi.DownloadInfo[] downloads;
        private final File downloadDir;
        private long totalSize, totalDownloaded;
        private int nextFileIndex = 0;

        public DownloadTracker(Context context, UiVersion version, PlayApi.DownloadInfo[] downloads, File downloadDir) {
            this.context = context;
            this.version = version;
            this.downloads = downloads;
            this.downloadDir = downloadDir;

            for (PlayApi.DownloadInfo i : downloads)
                totalSize += i.getSize();
            version.getDownloadTotal().set(totalSize);
        }

        public void downloadNextFile() {
            if (nextFileIndex == downloads.length) {
                version.isDownloading().set(false);
                version.isDownloaded().set(true);
                return;
            }

            downloadDir.mkdirs();

            PlayApi.DownloadInfo dlInfo = downloads[nextFileIndex++];
            FileDownloader downloader = new FileDownloader();
            downloader.setProgressCallback((c, t) -> UiThreadHelper.runOnUiThread(() -> {
                version.getDownloadComplete().set(totalDownloaded + c);
            }));
            downloader.setCompletionCallback(() -> UiThreadHelper.runOnUiThread(() -> {
                totalDownloaded += dlInfo.getSize();
                version.getDownloadComplete().set(totalDownloaded);
                downloadNextFile();
            }));
            downloader.setErrorCallback(e -> UiThreadHelper.runOnUiThread(() -> {
                Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();

                version.isDownloading().set(false);
            }));
            AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> downloader.execute(dlInfo.getUrl(), new File(downloadDir, dlInfo.getName() + ".apk")));
        }

    }

}
