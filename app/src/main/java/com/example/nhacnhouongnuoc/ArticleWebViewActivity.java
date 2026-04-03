package com.example.nhacnhouongnuoc;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class ArticleWebViewActivity extends AppCompatActivity {
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_URL = "extra_url";

    private WebView webView;
    private View loadingView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_webview);

        webView = findViewById(R.id.article_webview);
        loadingView = findViewById(R.id.loading_container);
        TextView titleView = findViewById(R.id.tv_web_title);
        View backButton = findViewById(R.id.btn_back);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String url = getIntent().getStringExtra(EXTRA_URL);

        if (!TextUtils.isEmpty(title)) {
            titleView.setText(title);
        }

        backButton.setOnClickListener(v -> finish());

        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, R.string.invalid_article_url, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String loadedUrl) {
                super.onPageFinished(view, loadedUrl);
                loadingView.setVisibility(View.GONE);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                    return;
                }
                finish();
            }
        });

        loadingView.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }
}
