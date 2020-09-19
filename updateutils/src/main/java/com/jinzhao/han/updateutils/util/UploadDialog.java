package com.jinzhao.han.updateutils.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.jinzhao.han.updateutils.R;
import com.jinzhao.han.updateutils.customview.ConfirmDialog;
import com.jinzhao.han.updateutils.model.UpdateBean;

/**
 * Created by mac on 2020/9/19.
 */

public class UploadDialog extends DialogFragment {

    private TextView content;
    private TextView sureBtn;
    private TextView cancleBtn;

    private UpdateBean updateBean;

    private static final int PERMISSION_CODE = 1001;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //设置无标题
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(true);
        View view = inflater.inflate(R.layout.view_version_tips_dialog, container);

        sureBtn = (TextView) view.findViewById(R.id.dialog_confirm_sure);
        cancleBtn = (TextView) view.findViewById(R.id.dialog_confirm_cancle);
        content = (TextView) view.findViewById(R.id.dialog_confirm_title);
        initData();
        return view;

    }

    private void initData() {
        if (updateBean == null || content == null) {
            return;
        }
        String contentStr = "发现新版本:" + updateBean.getServerVersionName() + "\n是否下载更新?";
        if (!TextUtils.isEmpty(updateBean.getUpdateInfo())) {
            contentStr = "发现新版本:" + updateBean.getServerVersionName() + "是否下载更新?\n\n" + updateBean.getUpdateInfo();
        }

        content.setText(contentStr);

        if (updateBean.getForce()) {
            cancleBtn.setText("退出");
        } else {
            cancleBtn.setText("取消");
        }

        cancleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (updateBean.getForce()) {
                    System.exit(0);
                } else {
                    dismiss();
                }
            }
        });

        sureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preDownLoad();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public void setBeen(UpdateBean updateBean) {
        this.updateBean = updateBean;
        initData();
    }


    /**
     * 预备下载 进行 6.0权限检查
     */
    private void preDownLoad() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            download();
        } else {
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                download();

            } else {//申请权限
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CODE);
            }
        }
    }


    private void download() {

        getActivity().startService(new Intent(getContext(), UpdateAppService.class));

        if (updateBean.getDownloadBy() == UpdateAppUtils.DOWNLOAD_BY_APP) {
            if (isWifiConnected(getContext())) {
                realDownload();
            } else {
                new ConfirmDialog(getContext(), new ConfirmDialog.Callback() {
                    @Override
                    public void callback(int position) {
                        if (position == 1) {
                            realDownload();
                        } else {
                            if (updateBean.getForce()) {
                                System.exit(0);
                            } else {
                                dismiss();
                            }
                        }
                    }
                }).setContent("目前手机不是WiFi状态\n确认是否继续下载更新？").show();
            }
        } else if (updateBean.getDownloadBy() == UpdateAppUtils.DOWNLOAD_BY_BROWSER) {
            DownloadAppUtils.downloadForWebView(getContext(), updateBean.getApkPath());
        }

        //finish();
    }

    private void realDownload() {
        DownloadAppUtils.download(getContext(), updateBean.getApkPath(), updateBean.getServerVersionName());
        if (!updateBean.getForce()) {
            Toast.makeText(getContext(), "更新下载中", Toast.LENGTH_SHORT).show();
            dismiss();
        } else {
            content.setText("更新下载中...");
        }
    }


    /**
     * 权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    download();
                } else {
                    new ConfirmDialog(getContext(), new ConfirmDialog.Callback() {
                        @Override
                        public void callback(int position) {
                            if (position == 1) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getActivity().getPackageName())); // 根据包名打开对应的设置界面
                                startActivity(intent);
                            }
                        }
                    }).setContent("暂无读写SD卡权限\n是否前往设置？").show();
                }
                break;
        }
    }

    /**
     * 检测wifi是否连接
     */
    private boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            }
        }
        return false;
    }


}
