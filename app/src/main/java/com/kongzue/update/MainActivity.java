package com.kongzue.update;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kongzue.kongzueupdatesdk.v3.UpdateUtil;

public class MainActivity extends AppCompatActivity {
    
    private UpdateUtil updateUtil;
    
    private EditText editDownloadUrl;
    private Button btnDownload;
    private ProgressBar progressBar;
    private TextView txtCustomStatus;
    private Button btnDownloadCustom;
    private Button btnStopDownloadCustom;
    private CheckBox chkAutoInstall;
    private Button btnInstallCustom;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        editDownloadUrl = findViewById(R.id.edit_download_url);
        btnDownload = findViewById(R.id.btn_download);
        progressBar = findViewById(R.id.progressBar);
        txtCustomStatus = findViewById(R.id.txt_custom_status);
        btnDownloadCustom = findViewById(R.id.btn_download_custom);
        btnStopDownloadCustom = findViewById(R.id.btn_stop_download_custom);
        chkAutoInstall = findViewById(R.id.chk_auto_install);
        btnInstallCustom = findViewById(R.id.btn_install_custom);
        
        updateUtil = new UpdateUtil(this);
        
        //默认下载方式
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUtil.setOnUpdateStatusChangeListener(null)
                        .showDownloadNotification("正在更新", "正在下载UpdateV3框架更新...")
                        .showDownloadProgressDialog("正在下载", "后台下载", "取消")
                        .start(editDownloadUrl.getText().toString(),
                                "发现更新",
                                "1.上线了极力要求以至于无法再拒绝的收入功能\n" +
                                        "2.出行的二级分类加入了地铁、地铁、地铁\n" +
                                        "3.「关于」新增应用商店评分入口，你们知道怎么做\n" +
                                        "4.「关于」还加入了GitHub地址，情怀+1s\n" +
                                        "5.全新的底层适配框架，优化更多机型",
                                "开始",
                                "取消",
                                "去商店",
                                false
                        );
                
            }
        });
        
        //自定义UI方式下载
        btnDownloadCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUtil.setOnUpdateStatusChangeListener(onUpdateStatusChangeListener)
                        .hideDownloadProgressDialog()
                        .start(editDownloadUrl.getText().toString());
            }
        });
        
        //停止下载
        btnStopDownloadCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUtil.cancel();
            }
        });
        
        //手动安装
        btnInstallCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!updateUtil.installApk()) {
                    Toast.makeText(MainActivity.this, "你需要先下载完成", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        //是否自动安装
        chkAutoInstall.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateUtil.setInstallWhenDownloadFinish(isChecked);
            }
        });
        updateUtil.setInstallWhenDownloadFinish(chkAutoInstall.isChecked());
    }
    
    UpdateUtil.OnUpdateStatusChangeListener onUpdateStatusChangeListener = new UpdateUtil.OnUpdateStatusChangeListener() {
        @Override
        public void onDownloadStart() {
            txtCustomStatus.setText("开始下载");
        }
        
        @Override
        public void onDownloading(int progress) {
            txtCustomStatus.setText("正在下载：" + progress + "%");
            progressBar.setProgress(progress);
        }
        
        @Override
        public void onDownloadCompleted() {
            txtCustomStatus.setText("下载完成");
        }
        
        @Override
        public void onInstallStart() {
            txtCustomStatus.setText("开始安装");
        }
        
        @Override
        public void onDownloadCancel() {
            txtCustomStatus.setText("下载取消");
        }
    };
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateUtil.recycle();
    }
}
