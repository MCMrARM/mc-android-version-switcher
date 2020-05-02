package io.mrarm.mcversion;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class IOUtil {

    private IOUtil() {
    }

    public static byte[] readRawResource(Context ctx, int resId) {
        try (InputStream stream = ctx.getResources().openRawResource(resId)) {
            byte[] b = new byte[stream.available()];
            int o = 0;
            while (o < b.length) {
                int r = stream.read(b, o, b.length - o);
                if (r == -1)
                    throw new IOException();
                o += r;
            }
            return b;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyRawResource(Context ctx, int resId, File outPath) {
        try (InputStream inStream = ctx.getResources().openRawResource(resId);
                OutputStream outStream = new FileOutputStream(outPath)) {
            byte[] b = new byte[16 * 1024];
            int n;
            while ((n = inStream.read(b)) >= 0)
                outStream.write(b, 0, n);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteDirectory(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File child : files)
                deleteDirectory(child);
        }
        file.delete();
    }

}
