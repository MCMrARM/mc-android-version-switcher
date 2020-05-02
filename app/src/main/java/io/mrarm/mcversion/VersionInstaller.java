package io.mrarm.mcversion;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.databinding.ObservableInt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VersionInstaller {

    private static final String ACTION_INSTALL_STATUS = "io.mrarm.mcversion.installstatus";
    private static final String ACTION_UNINSTALL_STATUS = "io.mrarm.mcversion.uninstallstatus";
    private static final String ARG_SESSION_ID = "session_id";
    private static final String ARG_CALLBACK_ID = "callback_id";

    private final ObservableInt installedVersion = new ObservableInt();

    private final Map<String, InstallCallback> callbackMap = new HashMap<>();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            final int statusCode = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            if (statusCode == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent launchIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                context.startActivity(launchIntent);
                return;
            }
            if (intent.getAction().equals(ACTION_INSTALL_STATUS)) {
                Log.d("VersionInstaller", "Install status: " + statusCode);

                PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
                try {
                    packageInstaller.abandonSession(intent.getIntExtra(ARG_SESSION_ID, -1));
                } catch (SecurityException ignored) {
                }
            } else if (intent.getAction().equals(ACTION_UNINSTALL_STATUS)) {
                Log.d("VersionInstaller", "Uninstall status: " + statusCode);
            }

            updateInstalledVersion(context);

            if (intent.hasExtra(ARG_CALLBACK_ID)) {
                InstallCallback r = callbackMap.remove(intent.getStringExtra(ARG_CALLBACK_ID));
                if (r == null)
                    return;
                r.onComplete(statusCode);
            }
        }
    };

    public void deleteHangingSession(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
            for (PackageInstaller.SessionInfo s : packageInstaller.getMySessions()) {
                packageInstaller.abandonSession(s.getSessionId());
            }
        }
    }

    public void registerIntentHandler(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_INSTALL_STATUS);
        intentFilter.addAction(ACTION_UNINSTALL_STATUS);
        context.registerReceiver(receiver, intentFilter, null, null);
    }

    public void unregisterIntentHandler(Context context) {
        context.unregisterReceiver(receiver);
    }

    private String registerCallback(InstallCallback callback) {
        String uuid = UUID.randomUUID().toString();
        callbackMap.put(uuid, callback);
        return uuid;
    }

    public ObservableInt getInstalledVersion() {
        return installedVersion;
    }

    public void updateInstalledVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);
            installedVersion.set(info.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            installedVersion.set(0);
        }
    }

    public boolean needsUninstall(Context context, int versionCode) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);
            return info.versionCode > versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void uninstall(Context context, InstallCallback callback) {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        Intent intent = new Intent(ACTION_UNINSTALL_STATUS);
        intent.putExtra(ARG_CALLBACK_ID, registerCallback(callback));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 10000, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        packageInstaller.uninstall("com.mojang.minecraftpe", pendingIntent.getIntentSender());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void install(Context context, File versionDir, InstallCallback callback) throws IOException {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName("com.mojang.minecraftpe");
        int sessionId = packageInstaller.createSession(params);
        PackageInstaller.Session session = packageInstaller.openSession(sessionId);
        for (File f : versionDir.listFiles()) {
            try (InputStream inStream = new FileInputStream(f);
                 OutputStream outStream = session.openWrite(f.getName(), 0, -1)) {
                byte[] b = new byte[512 * 1024];
                int n;
                while ((n = inStream.read(b)) >= 0)
                    outStream.write(b, 0, n);
                session.fsync(outStream);
            }
        }

        Intent intent = new Intent(ACTION_INSTALL_STATUS);
        intent.putExtra(ARG_SESSION_ID, sessionId);
        intent.putExtra(ARG_CALLBACK_ID, registerCallback(callback));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 10001, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        session.commit(pendingIntent.getIntentSender());
        session.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void install(Context context, VersionDownloader downloader, UiVersion version, InstallCallback callback)
            throws IOException {
        install(context, downloader.getVersionDir(version), callback);
    }

    public interface InstallCallback {
        void onComplete(int status);
    }

}
