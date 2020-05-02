package io.mrarm.mcversion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.widget.Toast;

import androidx.databinding.ObservableInt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mrarm.dataadapter.DataAdapter;
import io.mrarm.dataadapter.ListData;
import io.mrarm.dataadapter.ViewHolderType;

public class VersionListAdapter extends DataAdapter {

    private static final String[] ABIS = {"arm64-v8a", "armeabi-v7a"};

    private final VersionDownloader downloader;
    private final VersionInstaller installer;
    private List<UiVersion> currentVersions;

    public VersionListAdapter(VersionDownloader downloader, VersionInstaller installer) {
        this.downloader = downloader;
        this.installer = installer;
    }

    public ObservableInt getInstalledVersion() {
        return installer.getInstalledVersion();
    }

    public void setVersions(List<VersionList.Version> versions) {
        Map<Integer, UiVersion> oldDownloads = new HashMap<>();
        if (currentVersions != null) {
            for (UiVersion v : currentVersions)
                oldDownloads.put(v.getVersionCode(), v);
        }

        List<UiVersion> uiVersions = new ArrayList<>();
        for (VersionList.Version v : versions) {
            for (String abi : ABIS) {
                Integer versionCode = v.codes.get(abi);
                if (versionCode == null)
                    continue;
                if (oldDownloads.containsKey(versionCode))
                    uiVersions.add(oldDownloads.get(versionCode));
                else
                    uiVersions.add(new UiVersion(v.name + " (" + abi + ")", versionCode));
            }
        }
        for (UiVersion v : uiVersions)
            downloader.updateDownloadedStatus(v);
        Collections.reverse(uiVersions);
        ListData<UiVersion> data = new ListData<>(uiVersions, ITEM);
        data.setContext(this);
        setSource(data);
    }

    private void doInstall(Context context, UiVersion version, Runnable cb) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                installer.install(context, downloader, version, (s) -> cb.run());
            }
        } catch (IOException e) {
            Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public void downloadOrInstall(Context context, UiVersion version) {
        if (version.isDownloaded().get()) {
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setMessage(R.string.text_reinstall)
                    .setCancelable(false)
                    .show();

            if (installer.needsUninstall(context, version.getVersionCode())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    installer.uninstall(context, (s) -> {
                        if (s == PackageInstaller.STATUS_SUCCESS) {
                            doInstall(context, version, dialog::dismiss);
                        } else {
                            dialog.dismiss();
                        }
                    });
                }
            } else {
                doInstall(context, version, dialog::dismiss);
            }
        } else {
            downloader.download(context, version);
        }
    }

    public boolean showLongPressMenu(Context context, UiVersion version) {
        if (!version.isDownloaded().get())
            return false;
        new AlertDialog.Builder(context)
                .setTitle(version.getName())
                .setItems(new CharSequence[] { context.getString(R.string.action_delete) }, (d, which) -> {
                    if (which == 0) {
                        downloader.delete(version);
                    }
                })
                .show();
        return true;
    }


    private static final ViewHolderType<UiVersion> ITEM =
            ViewHolderType.<UiVersion>fromDataBinding(R.layout.version_entry)
                    .setValueVarId(BR.version)
                    .setContextVarId(BR.context)
                    .build();

}
