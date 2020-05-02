package io.mrarm.mcversion;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GoogleLoginActivity extends AppCompatActivity {

    private String accountIdentifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CookieManager.getInstance().removeAllCookie();

        WebView view = new WebView(this);
        view.addJavascriptInterface(new LoginInterface(), "mm");
        view.setWebViewClient(new WebViewClient() {

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                if (view.getUrl().endsWith("#close")) {
                    completeLogin();
                }
                super.doUpdateVisitedHistory(view, url, isReload);
            }
        });
        view.getSettings().setJavaScriptEnabled(true);
        setContentView(view);
        view.loadUrl("https://accounts.google.com/embedded/setup/v2/android?source=com.android.settings&xoauth_display_name=Android%20Phone&canFrp=1&canSk=1&lang=en&langCountry=en_us&hl=en-US&cc=us");
    }

    private void completeLogin() {
        String accountToken = null;

        String cookies = CookieManager.getInstance().getCookie("https://accounts.google.com/");
        Log.d("GoogleLoginActivity", "Cookies: " + cookies);
        for (String c : cookies.split(";")) {
            String v = c.trim();
            int iof = v.indexOf("=");
            if (iof == -1)
                continue;
            String k = v.substring(0, iof);
            v = v.substring(iof + 1);
            if (k.equals("oauth_token"))
                accountToken = v;
        }
        CookieManager.getInstance().removeAllCookie();

        if (accountToken == null)
            return;

        Log.d("GoogleLoginActivity", "Oauth token: " + accountToken);

        PlayHelper.getInstance(this).getApi().loginWithAccessToken(accountIdentifier, accountToken, new PlayApi.AccessTokenCallback() {
            @Override
            public void onSuccess(String token) {
                Log.d("GoogleLoginActivity", "Login success! " + token);
                PlayHelper.getInstance(GoogleLoginActivity.this).saveAccount(accountIdentifier, token);
                PlayHelper.getInstance(GoogleLoginActivity.this).requestAuthToApi();

                finish();
            }

            @Override
            public void onError(String str) {
                Log.e("GoogleLoginActivity", "Login failed! " + str);
                runOnUiThread(() -> Toast.makeText(GoogleLoginActivity.this, str, Toast.LENGTH_LONG).show());
                finish();
            }
        });
    }

    public class LoginInterface {

        @JavascriptInterface
        public void showView() {
        }

        @JavascriptInterface
        public void setAccountIdentifier(String identifier) {
            Log.d("GoogleLoginActivity", "Account id: " + identifier);
            accountIdentifier = identifier;
        }

        @JavascriptInterface
        public void log(String text) {
            Log.d("GoogleLoginActivity", "WWW: " + text);
        }

    }

}
