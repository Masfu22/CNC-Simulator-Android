package com.cncsmartgen.simulator;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.webkit.WebViewAssetLoader;

import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Android host for the upstream CNC Simulator browser renderer. */
public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 4207;
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private WebViewAssetLoader assetLoader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF101820);
        getWindow().setNavigationBarColor(0xFF101820);

        webView = new WebView(this);
        webView.setBackgroundColor(0xFF101820);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setDefaultTextEncodingName("UTF-8");

        webView.addJavascriptInterface(new AndroidDownloads(), "AndroidCnc");
        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                return host != null && !host.equals("appassets.androidplatform.net");
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException ex) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "File picker tidak tersedia", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        setContentView(webView);
        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html");
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
            filePathCallback.onReceiveValue(result);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) filePathCallback.onReceiveValue(null);
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidCnc");
            webView.destroy();
        }
        super.onDestroy();
    }

    /** Saves the renderer's generated Blob text to Android's public Downloads collection. */
    private final class AndroidDownloads {
        @JavascriptInterface
        public void saveTextFile(String name, String text, String mimeType) {
            runOnUiThread(() -> {
                try {
                    byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Downloads.DISPLAY_NAME, safeName(name));
                        values.put(MediaStore.Downloads.MIME_TYPE, mimeType == null ? "text/plain" : mimeType);
                        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CNC Simulator");
                        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (uri == null) throw new IllegalStateException("Tidak dapat membuat file unduhan");
                        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                            if (out == null) throw new IllegalStateException("Tidak dapat membuka file unduhan");
                            out.write(bytes);
                        }
                        Toast.makeText(MainActivity.this, "Tersimpan di Downloads/CNC Simulator", Toast.LENGTH_LONG).show();
                    } else {
                        File base = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                        if (base == null) throw new IllegalStateException("Folder unduhan aplikasi tidak tersedia");
                        File folder = new File(base, "CNC Simulator");
                        if (!folder.exists() && !folder.mkdirs()) throw new IllegalStateException("Tidak dapat membuat folder unduhan");
                        File target = new File(folder, safeName(name));
                        try (FileOutputStream out = new FileOutputStream(target)) { out.write(bytes); }
                        Toast.makeText(MainActivity.this, "Tersimpan: " + target.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        private String safeName(String name) {
            if (name == null || name.trim().isEmpty()) return "cnc-program.nc";
            return name.replaceAll("[\\\\/:*?\"<>|]", "_");
        }
    }
}
