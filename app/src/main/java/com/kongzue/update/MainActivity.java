package com.kongzue.update;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kongzue.kongzueupdatesdk.UpdateInfo;
import com.kongzue.kongzueupdatesdk.UpdateUtil;

public class MainActivity extends AppCompatActivity {

    private Button btnDownload;

    private UpdateInfo updateInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDownload = findViewById(R.id.btn_download);

        updateInfo = new UpdateInfo()
                .setInfo("1.上线了极力要求以至于无法再拒绝的收入功能\n" +
                        "2.出行的二级分类加入了地铁、地铁、地铁\n" +
                        "3.「关于」新增应用商店评分入口，你们知道怎么做\n" +
                        "4.「关于」还加入了GitHub地址，情怀+1s\n" +
                        "5.全新的底层适配框架，优化更多机型")
                .setVer("v2.5")
                .setDownloadUrl("http://paywhere.kongzue.com/downloads/paywhere.apk");

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateUtil updateUtil = new UpdateUtil(MainActivity.this, BuildConfig.APPLICATION_ID)
                        .setOnDownloadListener(new UpdateUtil.OnDownloadListener() {
                            @Override
                            public void onStart(long downloadId) {
                                Log.i("MainActivity", "onStart: 下载开始");
                            }

                            @Override
                            public void onDownloading(long downloadId, int progress) {
                                Log.i("MainActivity", "onStart: 下载中：" + progress);
                            }

                            @Override
                            public void onSuccess(long downloadId) {
                                Log.i("MainActivity", "onStart: 下载完成");
                            }
                        })
                        .showNormalUpdateDialog(updateInfo,
                                "检查到更新（" + updateInfo.getVer() + "）",
                                "从商店下载",
                                "直接下载",
                                "取消");
            }
        });
    }
}
