package io.mrarm.mcversion;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class VersionList {

    private static Gson gson = new Gson();

    public List<Version> versions;

    public void loadCached(File file) {
        try (FileReader reader = new FileReader(file)) {
            versions = gson.fromJson(reader, new TypeToken<List<Version>>(){}.getType());
        } catch (IOException ignored) {
        }
    }

    public void saveToCache(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(versions, writer);
        } catch (IOException ignored) {
        }
    }

    public void loadNetwork(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        if (conn == null)
            throw new IOException();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            List<Version> versions = gson.fromJson(reader, new TypeToken<List<Version>>(){}.getType());
            if (versions == null)
                throw new IOException("Version list empty");
            this.versions = versions;
        }
    }


    public static class Version {
        @SerializedName("version_name")
        public String name;

        @SerializedName("codes")
        public Map<String, Integer> codes;
    }


}
