package io.mrarm.mcversion;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;

public class PlayHelper {

    private static final String PREF_ACCOUNT_ID = "play_account_id";
    private static final String PREF_ACCOUNT_TOKEN = "play_account_token";

    private static PlayHelper instance;

    public static PlayHelper getInstance(Context context) {
        if (instance == null)
            instance = new PlayHelper(context.getApplicationContext());
        return instance;
    }

    private final Context context;
    private File dataDir;
    private PlayApi api;
    private boolean authToApiPending = false;
    private boolean authedToApi = false;

    public PlayHelper(Context context) {
        this.context = context;
        dataDir = new File(context.getFilesDir(), "playapi");
        dataDir.mkdirs();
        String dataDirS = dataDir.getAbsolutePath();
        if (!dataDirS.endsWith("/"))
            dataDirS += "/";
        initCurlSsl(context);
        api = new PlayApi(dataDirS);
        loadDevice(R.raw.device_arm64, "arm64");
        if (hasSavedAccount()) {
            loadAccountFromPrefs();
            requestAuthToApi();
        }
    }

    private static void initCurlSsl(Context context) {
        File outCaFile = new File(context.getFilesDir(), "google_ca.pem");
        IOUtil.copyRawResource(context, R.raw.google_ca, outCaFile);
        PlayApi.initCurlSsl(outCaFile.getAbsolutePath());
    }

    private void loadDevice(int resId, String stateName) {
        try {
            String deviceConfig = new String(IOUtil.readRawResource(context, resId), "UTF-8");
            api.setDevice(deviceConfig, new File(dataDir, "device-" + stateName + "-state.conf").getAbsolutePath());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadAccountFromPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String accountId = prefs.getString(PREF_ACCOUNT_ID, null);
        String accountToken = prefs.getString(PREF_ACCOUNT_TOKEN, null);
        if (accountId != null && accountToken != null)
            api.setLoginToken(accountId, accountToken);
    }

    public boolean hasSavedAccount() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.contains(PREF_ACCOUNT_ID) && prefs.contains(PREF_ACCOUNT_TOKEN);
    }

    public void saveAccount(String id, String token) {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefs.putString(PREF_ACCOUNT_ID, id);
        prefs.putString(PREF_ACCOUNT_TOKEN, token);
        prefs.apply();
    }

    public PlayApi getApi() {
        return api;
    }

    public void requestAuthToApi() {
        synchronized (this) {
            if (authToApiPending)
                return;
            authToApiPending = true;
        }
        api.authToApi(new PlayApi.Callback() {
            @Override
            public void onSuccess() {
                Log.i("PlayHelper", "Authenticated to api");
                synchronized (PlayHelper.this) {
                    authToApiPending = false;
                    authedToApi = true;
                }
            }

            @Override
            public void onError(String str) {
                synchronized (PlayHelper.this) {
                    authToApiPending = false;
                }
                UiThreadHelper.runOnUiThread(() -> Toast.makeText(context, str, Toast.LENGTH_LONG).show());
            }
        });
    }

    public synchronized boolean isAuthedToApi() {
        return authedToApi;
    }

}
