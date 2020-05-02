package io.mrarm.mcversion;

import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableLong;

public final class UiVersion {

    private final String name;
    private final int versionCode;
    private final ObservableBoolean downloaded = new ObservableBoolean();
    private final ObservableBoolean downloading = new ObservableBoolean();
    private final ObservableLong downloadTotal = new ObservableLong();
    private final ObservableLong downloadComplete = new ObservableLong();

    public UiVersion(String name, int versionCode) {
        this.name = name;
        this.versionCode = versionCode;
    }

    public String getName() {
        return name;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public ObservableBoolean isDownloaded() {
        return downloaded;
    }

    public ObservableBoolean isDownloading() {
        return downloading;
    }

    public ObservableLong getDownloadTotal() {
        return downloadTotal;
    }

    public ObservableLong getDownloadComplete() {
        return downloadComplete;
    }

}
