package io.mrarm.mcversion;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.mrarm.mcversion.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }

    private VersionInstaller installer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        VersionDownloader downloader = new VersionDownloader(new File(getFilesDir(), "minecraft"));
        installer = new VersionInstaller();
        installer.updateInstalledVersion(this);
        installer.deleteHangingSession(this);
        installer.registerIntentHandler(this);
        VersionListAdapter adapter = new VersionListAdapter(downloader, installer);

        VersionList versionList = new VersionList();
        File versionListCacheFile = new File(getCacheDir(), "versions.json");
        versionList.loadCached(versionListCacheFile);
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            try {
                Log.d("MainActivity", "Loading version list from network");
                versionList.loadNetwork("https://raw.githubusercontent.com/minecraft-linux/mcpelauncher-versiondb/master/versions.json");
                versionList.saveToCache(versionListCacheFile);
                Log.d("MainActivity", "Loaded version list from network: " + versionList.versions.size() + " versions");
                runOnUiThread(() -> adapter.setVersions(versionList.versions));
            } catch (IOException e) {
                Log.e("MainActivity", "Loading version list from network failed");
                runOnUiThread(() -> Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        });

        adapter.setVersions(versionList.versions != null ? versionList.versions : new ArrayList<>());

        binding.content.setLayoutManager(new LinearLayoutManager(this));
        binding.content.setAdapter(adapter);
        setContentView(binding.content);

        if (!PlayHelper.getInstance(this).hasSavedAccount())
            openGoogleLogin();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        installer.unregisterIntentHandler(this);
    }

    private void openGoogleLogin() {
        Intent intent = new Intent(this, GoogleLoginActivity.class);
        startActivity(intent);
    }

}
