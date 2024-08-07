package org.phindassist.unofficial;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {

    private WebView chatWebView;
    private CookieManager chatCookieManager;
    private final Context context = this;

    private ValueCallback<Uri[]> mUploadMessage;
    private final static int FILE_CHOOSER_REQUEST_CODE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatWebView = findViewById(R.id.chatWebView);
        registerForContextMenu(chatWebView);

        chatCookieManager = CookieManager.getInstance();
        chatCookieManager.setAcceptCookie(true);
        chatCookieManager.setAcceptThirdPartyCookies(chatWebView, false);

        chatWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (consoleMessage.message().contains("NotAllowedError: Write permission denied.")) {
                    Toast.makeText(context, R.string.error_copy, Toast.LENGTH_LONG).show();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, FILE_CHOOSER_REQUEST_CODE);
                    return false;
                }
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                }

                mUploadMessage = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                return true;
            }
        });

        chatWebView.setWebViewClient(new WebViewClient());

        WebSettings chatWebSettings = chatWebView.getSettings();
        chatWebSettings.setJavaScriptEnabled(true);
        chatWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        chatWebSettings.setDomStorageEnabled(true);
        chatWebSettings.setAllowContentAccess(false);
        chatWebSettings.setAllowFileAccess(false);
        chatWebSettings.setBuiltInZoomControls(false);
        chatWebSettings.setDatabaseEnabled(false);
        chatWebSettings.setDisplayZoomControls(false);
        chatWebSettings.setSaveFormData(false);
        chatWebSettings.setGeolocationEnabled(false);

        chatWebSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 12; Unspecified Device) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.79 Mobile Safari/537.36");

        chatWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                Toast.makeText(context, getString(R.string.downloading), Toast.LENGTH_LONG).show();
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                request.addRequestHeader("Cookie", chatCookieManager.getCookie(url));
                request.addRequestHeader("Accept", "text/html, application/xhtml+xml, */*");
                request.addRequestHeader("Referer", url);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
            }
        });

        String urlToLoad = "https://phind.com/";
        chatWebView.loadUrl(urlToLoad);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (chatWebView.canGoBack()) {
                        chatWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && mUploadMessage != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, intent);
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, proceed with file chooser or other actions requiring permission
        } else {
            // Permission denied, inform user or disable functionality
        }
    }
}
