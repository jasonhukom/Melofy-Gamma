package com.melofy.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.melofy.app.data.YouTubeMusicClient;
import com.melofy.app.server.LocalMelofyServer;

import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String ANDROID_UA_SUFFIX = " MelofyAndroidApp/1.0";

    private FrameLayout rootContainer;
    private WebView webView;
    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private LocalMelofyServer localServer;
    private String appBaseUrl;
    private String pendingHandoffCode = null;
    private boolean pageLoaded = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.parseColor("#0A0905"));
        getWindow().setNavigationBarColor(Color.parseColor("#0A0905"));

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
            insetsController.setAppearanceLightNavigationBars(false);
        }

        setContentView(R.layout.activity_main);
        rootContainer = findViewById(R.id.root_container);
        webView = findViewById(R.id.webview);
        fullscreenContainer = findViewById(R.id.fullscreen_container);

        ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        configureWebView();
        startLocalServerAndLoad();
        handleDeepLink(getIntent());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (customView != null && customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    return;
                }
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    moveTaskToBack(true);
                }
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        String baseUa = settings.getUserAgentString();
        if (baseUa != null) {
            // Remove WebView "; wv" token so embedded YouTube IFrame Player API serves standard playback.
            baseUa = baseUa.replace("; wv)", ")").replace("; wv", "");
            settings.setUserAgentString(baseUa + ANDROID_UA_SUFFIX);
        }

        webView.setBackgroundColor(Color.parseColor("#0A0905"));
        webView.setWebViewClient(new MelofyWebViewClient());
        webView.setWebChromeClient(new MelofyWebChromeClient());
    }

    private void startLocalServerAndLoad() {
        localServer = new LocalMelofyServer(getAssets(), new YouTubeMusicClient(BuildConfig.YOUTUBE_API_KEY));
        try {
            appBaseUrl = localServer.start();
            webView.loadUrl(appBaseUrl + "/");
        } catch (IOException e) {
            Toast.makeText(this, "Could not start local music server", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        Uri uri = intent.getData();
        if ("melofy".equals(uri.getScheme()) && "auth".equals(uri.getHost())) {
            String code = uri.getQueryParameter("code");
            if (code != null && !code.isEmpty()) {
                if (pageLoaded) {
                    deliverHandoffCode(code);
                } else {
                    pendingHandoffCode = code;
                }
            }
        }
    }

    private void deliverHandoffCode(String code) {
        String js = "window.completeAndroidHandoff && window.completeAndroidHandoff("
                + JSONObject.quote(code) + ");";
        webView.evaluateJavascript(js, null);
    }

    private boolean isLocalAppUri(Uri uri) {
        if (uri == null) return false;
        String host = uri.getHost();
        return "127.0.0.1".equals(host) || "localhost".equals(host);
    }

    private class MelofyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (uri == null) return false;
            String scheme = uri.getScheme();
            if ("melofy".equals(scheme)) {
                Intent deep = new Intent(Intent.ACTION_VIEW, uri);
                handleDeepLink(deep);
                return true;
            }
            if (isLocalAppUri(uri)) {
                return false;
            }
            // Open Google sign-in / external YouTube links in Chrome Custom Tabs.
            if ("http".equals(scheme) || "https".equals(scheme)) {
                try {
                    new CustomTabsIntent.Builder().build().launchUrl(MainActivity.this, uri);
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
                return true;
            }
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            pageLoaded = true;
            if (pendingHandoffCode != null) {
                String code = pendingHandoffCode;
                pendingHandoffCode = null;
                deliverHandoffCode(code);
            }
        }
    }

    private class MelofyWebChromeClient extends WebChromeClient {
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }
            customView = view;
            customViewCallback = callback;
            fullscreenContainer.setVisibility(View.VISIBLE);
            fullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        @Override
        public void onHideCustomView() {
            if (customView == null) return;
            fullscreenContainer.removeView(customView);
            fullscreenContainer.setVisibility(View.GONE);
            customView = null;
            if (customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
                customViewCallback = null;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (localServer != null) {
            localServer.stop();
            localServer = null;
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
