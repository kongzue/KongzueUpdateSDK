package com.kongzue.kongzueupdatesdk;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
import static android.content.Context.DOWNLOAD_SERVICE;
import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_NEUTRAL;
import static android.content.DialogInterface.BUTTON_POSITIVE;

public class UpdateUtil {
    
    public static boolean DEBUGMODE = false;
    public static String updateTitle = "发现新的版本";
    public static boolean isShowProgressDialog = false;          //是否显示默认更新进度对话框
    public static String hideProgressDialogButtonCaption = "后台下载";
    public static String cancelProgressDialogButtonCaption = "取消下载";
    public static String progressDialogTitle = "正在下载更新";
    public static String progressDescription = "";
    
    private Context me;
    private String packageName;
    private OnDownloadListener onDownloadListener;
    
    private DownloadManager downloadManager;
    private long downloadId;
    private DownloadFinishReceiver mReceiver;
    private File file;
    
    private UpdateUtil() {
        log("使用本组件请严格按照 https://github.com/kongzue/KongzueUpdateSDK 使用说明进行配置");
    }
    
    public UpdateUtil(Context me) {
        this.me = me;
        this.packageName = me.getPackageName();
        downloadManager = (DownloadManager) me.getSystemService(DOWNLOAD_SERVICE);
    }
    
    public UpdateUtil(Context me, String packageName) {
        this.me = me;
        this.packageName = packageName;
        downloadManager = (DownloadManager) me.getSystemService(DOWNLOAD_SERVICE);
    }
    
    public boolean doUpdate(UpdateInfo updateInfo) {
        if (updateInfo == null) {
            return false;
        }
        mReceiver = new DownloadFinishReceiver();
        me.registerReceiver(mReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        log("开始下载：" + updateInfo.getDownloadUrl());
        String ver = updateInfo.getVer() == null ? "" : "_" + updateInfo.getVer();
        file = new File(me.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), packageName + ver + ".apk");
        if (file.exists()) file.delete();       //文件存在则删除
        Uri path = Uri.fromFile(file);
        log("下载到:" + path.toString());
        
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(updateInfo.getDownloadUrl()));
        request.setDestinationUri(path);
        request.setMimeType("application/vnd.android.package-archive");
        request.setTitle(progressDialogTitle);
        request.setDescription(progressDescription);
        request.setVisibleInDownloadsUi(true);
        request.setNotificationVisibility(VISIBILITY_VISIBLE | VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        downloadId = downloadManager.enqueue(request);
        if (onDownloadListener != null) onDownloadListener.onStart(downloadId);
        doGetProgress();
        showProgressDialog();
        
        return true;
    }
    
    private void OpenWebBrowserAndOpenLink(String downloadUrl) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(downloadUrl);
        intent.setData(content_url);
        me.startActivity(intent);
    }
    
    private Timer downloadProgressTimer;
    
    private void doGetProgress() {
        if (downloadProgressTimer != null) downloadProgressTimer.cancel();
        downloadProgressTimer = new Timer();
        downloadProgressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                int progress = getProgress(downloadId);
                log(progress);
                if (onDownloadListener != null)
                    onDownloadListener.onDownloading(downloadId, progress);
                if (progress != 100) {
                    if (progressDialog != null) progressDialog.setProgress(progress);
                } else {
                    downloadProgressTimer.cancel();
                    if (progressDialog != null) progressDialog.dismiss();
                    isDownloadCompleted = true;
                    installApk(me);
                    
                    if (onDownloadListener != null) onDownloadListener.onSuccess(downloadId);
                    return;
                }
            }
        }, 100, 10);
    }
    
    private boolean isDownloadCompleted = false;
    
    public void installApk(Context context) {
        if (!isDownloadCompleted) {
            log("请先确保下载完成后才可执行安装");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            log("若未启动，请注意：\n" +
                    "· 请确保已在 AndroidManifest.xml 配置“" + (packageName + ".fileProvider") + "”" + "\n" +
                    "· 请确保已声明 android.permission.REQUEST_INSTALL_PACKAGES 权限");
            Uri contentUri = FileProvider.getUriForFile(context, packageName + ".fileProvider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            intent.setDataAndType(Uri.fromFile(getRealFileInAndroudM(context, downloadId)), "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(
                    Uri.fromFile(file),
                    "application/vnd.android.package-archive"
            );
        }
        context.startActivity(intent);
    }
    
    private File getRealFileInAndroudM(Context context, long downloadId) {
        File file = null;
        DownloadManager downloader = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadId != -1) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
            Cursor cur = downloader.query(query);
            if (cur != null) {
                if (cur.moveToFirst()) {
                    String uriString = cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    if (!uriString.isEmpty()) {
                        file = new File(Uri.parse(uriString).getPath());
                    }
                }
                cur.close();
            }
        }
        return file;
    }
    
    private int getProgress(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query()
                .setFilterById(downloadId);
        Cursor cursor = null;
        int progress = 0;
        try {
            cursor = downloadManager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                int downloadSoFar = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));       //当前的下载量
                int totalBytes = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));          //文件总大小
                progress = (int) (downloadSoFar * 1.0f / totalBytes * 100);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return progress;
    }
    
    public UpdateUtil showNormalUpdateDialog(UpdateInfo updateInfo) {
        return showNormalUpdateDialog(updateInfo, updateTitle, "从应用商店下载", "立即下载", "取消", false);
    }
    
    public UpdateUtil showNormalUpdateDialog(UpdateInfo updateInfo, String titleStr, String downloadByShopStr, String downloadNowStr, String cancelStr) {
        return showNormalUpdateDialog(updateInfo, titleStr, downloadByShopStr, downloadNowStr, cancelStr, false);
    }
    
    private boolean isForced = false;
    
    public UpdateUtil showNormalUpdateDialog(final UpdateInfo updateInfo, String titleStr, final String downloadByShopStr, String downloadNowStr, String cancelStr, boolean isForced) {
        isShowProgressDialog = true;
        this.isForced = isForced;
        android.support.v7.app.AlertDialog.Builder builder;
        builder = new android.support.v7.app.AlertDialog.Builder(me);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setTitle(titleStr);
        alertDialog.setCancelable(!isForced);
        alertDialog.setMessage(updateInfo.getInfo());
        if (downloadNowStr != null) {
            alertDialog.setButton(BUTTON_POSITIVE, downloadNowStr, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    doUpdate(updateInfo);
                }
            });
        }
        if (downloadByShopStr != null) {
            alertDialog.setButton(BUTTON_NEUTRAL, downloadByShopStr, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                
                }
            });
        }
        if (cancelStr == null) cancelStr = "CANCEL";
        if (!isForced) {
            alertDialog.setButton(BUTTON_NEGATIVE, cancelStr, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                
                }
            });
        }
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openMarket();
                    }
                });
            }
        });
        alertDialog.show();
        return this;
    }
    
    private void openMarket() {
        try {
            String str = "market://details?id=" + packageName;
            Intent localIntent = new Intent(Intent.ACTION_VIEW);
            localIntent.setData(Uri.parse(str));
            me.startActivity(localIntent);
        } catch (Exception e) {
            // 打开应用商店失败 可能是没有手机没有安装应用市场
            e.printStackTrace();
            // 调用系统浏览器进入商城
            openLinkBySystem("https://www.coolapk.com/apk/" + packageName);
        }
    }
    
    private void openLinkBySystem(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        me.startActivity(intent);
    }
    
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }
    
    public OnDownloadListener getOnDownloadListener() {
        return onDownloadListener;
    }
    
    public UpdateUtil setOnDownloadListener(OnDownloadListener onDownloadListener) {
        this.onDownloadListener = onDownloadListener;
        return this;
    }
    
    public interface OnDownloadListener {
        
        void onStart(long downloadId);
        
        void onDownloading(long downloadId, int progress);
        
        void onSuccess(long downloadId);
        
        void onCancel(long downloadId);
        
    }
    
    public interface OnDownloadingListener {
    }
    
    private void log(Object o) {
        if (DEBUGMODE)
            Log.d(">>>", o.toString());
    }
    
    public boolean isWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) me.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null
                && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }
    
    private ProgressDialog progressDialog;
    
    public void showProgressDialog() {
        if (!isShowProgressDialog) return;
        progressDialog = new ProgressDialog(me);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);// 设置水平进度条
        progressDialog.setCancelable(!isForced);// 设置是否可以通过点击Back键取消
        progressDialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
        progressDialog.setTitle(progressDialogTitle);
        progressDialog.setMax(100);
        if (!isForced) {
            progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, hideProgressDialogButtonCaption,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            progressDialog.dismiss();
                        }
                    }
            );
        }
        progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, cancelProgressDialogButtonCaption,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progressDialog.dismiss();
                        cancel();
                    }
                }
        );
        progressDialog.show();
    }
    
    public void cancel() {
        if (mReceiver != null) me.unregisterReceiver(mReceiver);
        if (downloadManager != null && downloadId != 0) downloadManager.remove(downloadId);
        if (onDownloadListener != null) onDownloadListener.onCancel(downloadId);
        if (downloadProgressTimer != null) downloadProgressTimer.cancel();
    }
    
    private boolean isNull(String s) {
        if (s == null || s.trim().isEmpty() || s.equals("null")) {
            return true;
        }
        return false;
    }
    
    public UpdateUtil setForced(boolean forced) {
        isForced = forced;
        return this;
    }
}
