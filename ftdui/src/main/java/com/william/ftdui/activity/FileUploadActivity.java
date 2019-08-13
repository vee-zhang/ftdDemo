package com.william.ftdui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.william.ftd_core.FtdClient;
import com.william.ftd_core.callback.FtdMicroTipCallback;
import com.william.ftd_core.callback.FtdPicUploadCallback;
import com.william.ftd_core.entity.AnalyzeResultBean;
import com.william.ftd_core.entity.Conclusion;
import com.william.ftd_core.entity.FtdResponse;
import com.william.ftd_core.entity.MicroTipBean;
import com.william.ftd_core.entity.UploadResult;
import com.william.ftd_core.exception.FtdException;
import com.william.ftdui.constant.Constant;
import com.william.ftdui.R;

import java.io.File;

import io.reactivex.disposables.Disposable;

public class FileUploadActivity extends BaseActivity {

    private Button btnSubmit;
    private TextView tvTitle;
    private TextView tvContent;
    private ImageView ivRefresh;

    private TextView tvFaceUploadResult;
    private TextView tvTongueTopUploadResult;
    private TextView tvTongueBottomUploadResult;

    private NestedScrollView nsv;

    private ProgressBar pbSub;

    @Override
    public void onCreated(@NonNull View view,@Nullable Bundle savedInstanceState) {
        ImageView ivFace = findViewById(R.id.iv_face);
        ImageView ivTongueTop = findViewById(R.id.iv_tongue_top);
        ImageView ivTongueBottom = findViewById(R.id.iv_tongue_bottom);

        btnSubmit = findViewById(R.id.submit);

        ivRefresh = findViewById(R.id.iv_refresh);
        ivRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMicroTip();
            }
        });
        tvTitle = findViewById(R.id.tv_title);
        tvContent = findViewById(R.id.tv_content);

        tvFaceUploadResult = findViewById(R.id.tv_result_face);
        tvTongueTopUploadResult = findViewById(R.id.tv_result_tongue_top);
        tvTongueBottomUploadResult = findViewById(R.id.tv_result_tongue_bottom);

        nsv = findViewById(R.id.nsv);

        File faceImg = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), Constant.steps.get(Constant.STEP_FACE).getFileName() + ".jpeg");
        File tongueTopImg = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), Constant.steps.get(Constant.STEP_TONGUE_TOP).getFileName() + ".jpeg");
        File tongueBottomImg = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), Constant.steps.get(Constant.STEP_TONGUE_BOTTOM).getFileName() + ".jpeg");

        loadImg(ivFace, faceImg);
        loadImg(ivTongueTop, tongueTopImg);
        loadImg(ivTongueBottom, tongueBottomImg);

        pbSub = findViewById(R.id.pb_sub);

        upload(faceImg, tongueTopImg, tongueBottomImg);

        getMicroTip();
    }

    @Override
    protected String setTitle() {
        return "上传分析";
    }

    @Override
    public int setContentViewResId() {
        return R.layout.activity_file_upload1;
    }

    private void loadImg(@NonNull ImageView iv, @NonNull File file) {
        Glide.with(iv).load(file).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(iv);
    }

    /**
     * 上传图片去分析
     *
     * @param FaceImg
     * @param TongueTopImg
     * @param TongueBottomImg
     */
    private void upload(File FaceImg, File TongueTopImg, File TongueBottomImg) {
        Disposable fileUploadDisposable = FtdClient.getInstance().picUpload(FaceImg, TongueTopImg, TongueBottomImg, new FtdPicUploadCallback() {
            @Override
            public void onSuccess(Conclusion result) {
                dismissProgress();
                FtdResponse<UploadResult> faceUploadResult = result.getFaceResult();
                FtdResponse<UploadResult> tongueUploadResult = result.getTongueTopResult();
                FtdResponse<UploadResult> tongueBottomResult = result.getTongueBottomResult();

                boolean faceSuccess = changeTvDescState(tvFaceUploadResult, faceUploadResult);
                boolean tongueTopSuccess = changeTvDescState(tvTongueTopUploadResult, tongueUploadResult);
                boolean tongueBottomSuccess = changeTvDescState(tvTongueBottomUploadResult, tongueBottomResult);

                changeBtnSubmitState(faceSuccess && tongueTopSuccess && tongueBottomSuccess);
            }

            @Override
            public void onError(FtdException e) {
                dismissProgress();
                showToast(e.getMsg());
            }
        });
        addDisposable(fileUploadDisposable);
    }

    private Boolean changeTvDescState(TextView tv, FtdResponse<UploadResult> response) {
        int drawableRes = R.drawable.close3;
        String content = "分析失败";
        boolean b = false;
        UploadResult result = response.getData();
        if (result != null) {
            b = result.getErrCode() == 0;
            if (b) {
                drawableRes = R.drawable.correct1;
                content = "分析成功";
            }
        }
        tv.setCompoundDrawablesWithIntrinsicBounds(drawableRes, 0, 0, 0);
        tv.setText(content);
        return b;
    }

    private void changeBtnSubmitState(final boolean b) {
        String content;
        int bjRes;
        if (b) {
            content = "去问诊";
            bjRes = R.drawable.selector_btn_submit;
        } else {
            content = "返回重拍";
            bjRes = R.drawable.selector_btn_submit1;
        }
        btnSubmit.setText(content);
        btnSubmit.setBackgroundResource(bjRes);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Class clazz;
                if (b) {
                    clazz = QuestionListActivity.class;
                } else {
                    clazz = FtdActivity.class;
                }
                Intent intent = new Intent();
                intent.setClass(FileUploadActivity.this, clazz);
                startActivity(intent);
                finish();
            }
        });
        btnSubmit.setEnabled(true);
    }

    /**
     * 获取健康微语
     */
    private void getMicroTip() {
        pbSub.setVisibility(View.VISIBLE);
        nsv.setScrollY(0);
        Disposable tipDisposable = FtdClient.getInstance().getMicroTip(new FtdMicroTipCallback() {
            @Override
            public void onSuccess(MicroTipBean bean) {
                tvTitle.setText(bean.getName());
                tvContent.setText(bean.getAnalysis());
                pbSub.setVisibility(View.GONE);
            }

            @Override
            public void onError(FtdException e) {
                pbSub.setVisibility(View.GONE);
                tvContent.setText(e.getMsg());
            }
        });
        addDisposable(tipDisposable);
    }
}
