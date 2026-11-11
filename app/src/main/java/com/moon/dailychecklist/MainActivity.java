package com.moon.dailychecklist;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF0F1115);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new Bridge(), "Android");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.evaluateJavascript("document.dispatchEvent(new Event('visibilitychange'))", null);
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        android.webkit.CookieManager.getInstance().flush();
    }

    class Bridge {
        @JavascriptInterface
        public void copy(String text) {
            runOnUiThread(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("backup", text));
            });
        }

        @JavascriptInterface
        public void vibrate() {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(15);
            }
        }
    }
}
