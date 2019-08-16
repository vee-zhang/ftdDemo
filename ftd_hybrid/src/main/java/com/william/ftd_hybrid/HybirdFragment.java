package com.william.ftd_hybrid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;


public class HybirdFragment extends Fragment {

    private static final String urlReg = "%smsz/index?companyCode=%s&AppId=%s&sourceType=shixiu&userTel=%s";
    private String url;
    private WebView wv;
    private ProgressBar pb;
    private ValueCallback<Uri[]> uploadMessageAboveL;

    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 0;
    private static final int CAMERA_REQUEST_CODE = 1;

    public static HybirdFragment newInstance(String appId, String companyCode, String mobile) {
        HybirdFragment fragment = new HybirdFragment();
        Bundle b = new Bundle();
        b.putString("appId", appId);
        b.putString("companyCode", companyCode);
        b.putString("mobile", mobile);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();
        String mobile = b.getString("mobile");
        String appId = b.getString("appId");
        String companyCode = b.getString("companyCode");
        url = String.format(urlReg, BuildConfig.HOST, companyCode, appId, mobile);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hybird, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        wv = getView().findViewById(R.id.wv);
        pb = getView().findViewById(R.id.pb);
        setupWebView(wv);
        wv.loadUrl(url);
    }

    /**
     * 对WebView的必须设置
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView wv) {
        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
//        settings.setTextZoom(100);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
//        this.wv.setWebChromeClient(new MyWebChromeClient(this));
        this.wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                showProgress();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hideProgress();
            }
        });
        this.wv.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams
                    fileChooserParams) {
                uploadMessageAboveL = filePathCallback;

                for (String requiredPermission : REQUIRED_PERMISSIONS) {
                    if (ContextCompat.checkSelfPermission(getContext(), requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(), REQUIRED_PERMISSIONS, CAMERA_PERMISSION_REQUEST_CODE);
                        filePathCallback.onReceiveValue(null);
                        return true;
                    }
                }
                //打开相机
                FtdActivity.getPicFromCamera(HybirdFragment.this, CAMERA_REQUEST_CODE);
                return true;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            FtdActivity.getPicFromCamera(this, CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri[] uris = null;
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            String[] paths = data.getStringArrayExtra("paths");
            uris = new Uri[3];
            for (int i = 0; i < paths.length; i++) {
                File file = new File(paths[i]);
                uris[i] = Uri.fromFile(file);
            }
        }
        uploadMessageAboveL.onReceiveValue(uris);
        uploadMessageAboveL = null;
    }

    /**
     * 展示进度条
     */
    protected void showProgress() {
        if (pb != null && !pb.isEnabled()) {
            pb.setEnabled(true);
            pb.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 隐藏进度条（不占位）
     */
    protected void hideProgress() {
        if (pb != null && pb.isEnabled()) {
            pb.setVisibility(View.GONE);
            pb.setEnabled(false);
        }
    }

    public boolean goback(){
        if (wv.canGoBack()){
            wv.goBack();
            return true;
        } else {
            return false;
        }
    }
}
