package io.mrarm.mcversion;

public final class PlayApi {

    private long handle;

    public PlayApi(String dataPath) {
        handle = init(dataPath);
    }

    public void setDevice(String deviceConfig, String statePath) {
        setDevice(handle, deviceConfig, statePath);
    }

    public void setLoginToken(String id, String token) {
        setLoginToken(handle, id, token);
    }

    public void loginWithAccessToken(String id, String token, AccessTokenCallback cb) {
        loginWithAccessToken(handle, id, token, cb);
    }

    public void authToApi(Callback callback) {
        authToApi(handle, callback);
    }

    public void requestDelivery(String pkgName, int pkgVer, DeliveryCallback cb) {
        requestDelivery(handle, pkgName, pkgVer, new NativeDeliveryCallback() {
            @Override
            public void onSuccess(String[] strings, long[] ints) {
                DownloadInfo[] info = new DownloadInfo[strings.length / 3];
                for (int i = strings.length / 3 - 1; i >= 0; --i)
                    info[i] = new DownloadInfo(strings[3 * i], strings[3 * i + 1], strings[3 * i + 2], ints[i]);
                cb.onSuccess(info);
            }

            @Override
            public void onError(String str) {
                cb.onError(str);
            }
        });
    }

    public static native void initCurlSsl(String caPath);

    private static native long init(String dataPath);

    private static native long setDevice(long self, String deviceConfig, String statePath);

    private static native long setLoginToken(long self, String id, String token);

    private static native long loginWithAccessToken(long self, String id, String token, AccessTokenCallback callback);

    private static native long authToApi(long self, Callback callback);

    private static native long requestDelivery(long self, String pkgName, int pkgVer, NativeDeliveryCallback callback);

    public interface Callback {

        void onSuccess();

        void onError(String str);

    }

    public interface AccessTokenCallback {

        void onSuccess(String token);

        void onError(String str);

    }

    private interface NativeDeliveryCallback {

        void onSuccess(String[] strings, long[] ints);

        void onError(String str);

    }

    public interface DeliveryCallback {

        void onSuccess(DownloadInfo[] download);

        void onError(String str);

    }

    public static class DownloadInfo {

        private final String name, url, gzippedUrl;
        private final long dlSize;

        private DownloadInfo(String name, String url, String gzippedUrl, long dlSize) {
            this.name = name;
            this.url = url;
            this.gzippedUrl = gzippedUrl;
            this.dlSize = dlSize;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public String getGzippedUrl() {
            return gzippedUrl;
        }

        public long getSize() {
            return dlSize;
        }
    }

}
