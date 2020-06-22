package com.kongzue.kongzueupdatesdk.v3;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.DownloadManager.Request.VISIBILITY_HIDDEN;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
import static android.content.Context.DOWNLOAD_SERVICE;
import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_NEUTRAL;
import static android.content.DialogInterface.BUTTON_POSITIVE;

/**
 * @author: Kongzue
 * @github: https://github.com/kongzue/
 * @homepage: http://kongzue.com/
 * @email: myzcxhh@live.cn
 * @createTime: 2020/6/22 15:24
 */
public class UpdateUtil {
    
    /**
     * 日志打印开关
     */
    public static boolean DEBUGMODE = false;
    
    /**
     * 主要
     */
    private WeakReference<Context> contextWeakReference;                    //上下文句柄
    private OnUpdateStatusChangeListener onUpdateStatusChangeListener;      //状态回调
    private DownloadManager downloadManager;                                //下载管理器
    private long downloadId;                                                //下载进程标记
    private String downloadUrl;                                             //下载地址 {get}
    private boolean installWhenDownloadFinish = true;                       //是否在下载结束时自动启动安装 {get/set}
    private File readyFile;                                                 //下载目标文件位置
    
    /**
     * 更新相关
     * 要显示更新提示对话框，需要使用 {@link #start(String, String, String, String, String, String, boolean)} 方法启动更新流程
     */
    private AlertDialog updateDialog;                                       //更新提示对话框
    private boolean isForced = false;                                       //是否强制更新
    
    /**
     * 状态栏提示通知相关
     * 使用 {@link #showDownloadNotification(String, String)} 开启状态栏提示
     */
    private boolean showNotification = true;                                //是否允许显示通知
    private String notificationTitle;                                       //通知标题
    private String notificationDescription;                                 //通知正文
    
    /**
     * 下载流程监控相关
     */
    private Timer downloadProgressTimer;                                    //下载流程进度刷新控制器
    private int oldProgress = -1;                                           //防止重复刷新标记
    private boolean isAlreadyDownloadApk;                                   //完成下载标记
    
    /**
     * 下载进度相关
     * 使用 {@link #showDownloadProgressDialog(String, String, String)} 开启下载进度对话框
     */
    private ProgressDialog progressDialog;                                  //下载进度对话框
    private boolean isShowProgressDialog = true;                            //是否显示下载进度对话框
    private String progressDialogTitle;                                     //下载进度提示框标题
    private String progressDialogBackgroundButton;                          //下载进度提示框 “后台下载” 按钮文本
    private String progressDialogCancelButton;                              //下载进度提示框 “取消” 按钮文本
    
    /**
     * 构造方法
     * 若使用对话框相关功能，context 建议使用 Activity，因为目前 Android 规定只能基于 Activity 构建对话框
     *
     * @param context 上下文所引
     */
    public UpdateUtil(@NonNull Context context) {
        contextWeakReference = new WeakReference<>(context);
        downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
    }
    
    /**
     * 启动下载和安装方法
     * 注意：若使用此方法启动则不会显示任何提示信息，直接进行下载和安装操作。
     * 下载流程和安装回调方法请参阅 {@link #setOnUpdateStatusChangeListener(OnUpdateStatusChangeListener)}
     * 要先提示用户更新信息，让用户选择是否更新，请使用 {@link #start(String, String, String, String, String, String, boolean)}
     *
     * @param apkUrl APK 文件下载链接
     */
    public void start(@NonNull String apkUrl) {
        downloadUrl = apkUrl;
        readyFile = new File(
                contextWeakReference.get().getExternalFilesDir("Update"),
                contextWeakReference.get().getPackageName() + ".apk"
        );
        if (readyFile.exists()) readyFile.delete();       //文件存在则删除
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setDestinationUri(Uri.fromFile(readyFile));
        request.setMimeType("application/vnd.android.package-archive");
        
        if (notificationTitle == null)
            notificationTitle = contextWeakReference.get().getApplicationInfo().name;
        if (notificationDescription == null)
            notificationDescription = contextWeakReference.get().getApplicationInfo().name + " UPDATE";
        request.setTitle(notificationTitle);
        request.setDescription(notificationDescription);
        if (showNotification) {
            request.setVisibleInDownloadsUi(false);
            request.setNotificationVisibility(VISIBILITY_VISIBLE | VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        } else {
            request.setVisibleInDownloadsUi(false);
            request.setNotificationVisibility(VISIBILITY_HIDDEN);
        }
        
        isAlreadyDownloadApk = false;
        getOnUpdateStatusChangeListener().onDownloadStart();
        downloadId = downloadManager.enqueue(request);
        log("开始下载：" + apkUrl + " 至：" + readyFile.getAbsolutePath());
        
        if (isShowProgressDialog) {
            progressDialog = new ProgressDialog(contextWeakReference.get());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(!isForced);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setTitle(progressDialogTitle);
            progressDialog.setMax(100);
            if (!isForced) {
                progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, progressDialogBackgroundButton,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                progressDialog.dismiss();
                            }
                        }
                );
            }
            progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, progressDialogCancelButton,
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
        watchDownloadProgress();
    }
    
    /**
     * 启动下载和安装方法 2
     * 通过此方法先启动一个更新提示对话框，然后由用户选择是否更新，或转向应用商店下载
     * 请注意，使用 isForced 参数标记是否强制更新，如果强制更新，那么对话框将不可使用 “返回” 按键关闭，
     * 另外你可以在 {@link OnUpdateStatusChangeListener#onDownloadCancel()} 中捕获更新取消事件进行处理
     * isForced 参数还会影响下载进度对话框，若为真，则下载进度对话框不会显示 “后台下载” 按钮
     *
     * @param apkUrl                  APK 文件下载链接
     * @param title                   对话框标题
     * @param message                 对话框内容
     * @param startDownloadButtonText 按钮 “开始更新” 文字
     * @param cancelButtonText        按钮 “取消” 文字
     * @param goToMarketText          按钮 “前往商店” 文字
     * @param isForced                是否强制更新
     */
    public void start(@NonNull final String apkUrl, @NonNull String title, @Nullable String message, @NonNull String startDownloadButtonText, @Nullable String cancelButtonText, @Nullable String goToMarketText, boolean isForced) {
        this.isForced = isForced;
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(contextWeakReference.get());
        updateDialog = builder.create();
        updateDialog.setTitle(title);
        updateDialog.setCancelable(!isForced);
        updateDialog.setMessage(message);
        if (startDownloadButtonText == null) {
            startDownloadButtonText = "START";
        }
        updateDialog.setButton(BUTTON_POSITIVE, startDownloadButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                start(apkUrl);
            }
        });
        if (goToMarketText != null) {
            updateDialog.setButton(BUTTON_NEUTRAL, goToMarketText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openMarket();
                    getOnUpdateStatusChangeListener().onDownloadCancel();
                }
            });
        }
        if (cancelButtonText == null) cancelButtonText = "CANCEL";
        updateDialog.setButton(BUTTON_NEGATIVE, cancelButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getOnUpdateStatusChangeListener().onDownloadCancel();
            }
        });
        updateDialog.show();
    }
    
    private void watchDownloadProgress() {
        if (downloadProgressTimer != null) downloadProgressTimer.cancel();
        downloadProgressTimer = new Timer();
        downloadProgressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                int progress = getProgress(downloadId);
                if (oldProgress != progress) {
                    oldProgress = progress;
                    log("已下载：" + progress);
                    getOnUpdateStatusChangeListener().onDownloading(progress);
                    if (progressDialog != null) progressDialog.setProgress(progress);
                    if (progress == 100) {
                        log("下载完毕：" + readyFile.getAbsolutePath());
                        isAlreadyDownloadApk = true;
                        getOnUpdateStatusChangeListener().onDownloadCompleted();
                        progressDialogDismiss();
                        if (installWhenDownloadFinish) {
                            installApk();
                        }
                        downloadProgressTimer.cancel();
                    }
                }
            }
        }, 100, 10);
    }
    
    /**
     * 手动调用此方法来安装已下载的 APK
     * 一般不建议使用，除非你自己实现了更新/下载/安装提示等 UI 逻辑。
     * 在下载完成后，调用此方法可以直接启动安装过程，
     * 如果出错，请检查是否声明权限 {@link android.Manifest.permission#INSTALL_PACKAGES} 以及是否正确配置 fileProvider
     *
     * @return 是否正确执行
     */
    public boolean installApk() {
        if (!isAlreadyDownloadApk) {
            log("请先确保下载完成后才可执行安装");
            return false;
        }
        log("准备启动安装步骤");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            log("若未启动，请注意：\n" +
                    "· 请确保已在 AndroidManifest.xml 配置“" + (contextWeakReference.get().getPackageName() + ".fileProvider") + "”" + "\n" +
                    "· 请确保已声明 android.permission.REQUEST_INSTALL_PACKAGES 权限");
            Uri contentUri = FileProvider.getUriForFile(contextWeakReference.get(), contextWeakReference.get().getPackageName() + ".fileProvider", readyFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            intent.setDataAndType(Uri.fromFile(getRealFileInAndroidM(contextWeakReference.get(), downloadId)), "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(readyFile), "application/vnd.android.package-archive");
        }
        contextWeakReference.get().startActivity(intent);
        getOnUpdateStatusChangeListener().onInstallStart();
        return true;
    }
    
    private File getRealFileInAndroidM(Context context, long downloadId) {
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
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
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
    
    /**
     * 显示下载进度通知
     * 建议使用，因为用户可以在软件后台时通过通知来启动更新的安装过程
     *
     * @param notificationTitle       通知标题
     * @param notificationDescription 通知内容
     * @return this
     */
    public UpdateUtil showDownloadNotification(@NonNull String notificationTitle, @NonNull String notificationDescription) {
        showNotification = true;
        this.notificationTitle = notificationTitle;
        this.notificationDescription = notificationDescription;
        return this;
    }
    
    /**
     * 隐藏下载进度通知
     * 注意，此操作需要声明权限 {"android.permission.DOWNLOAD_WITHOUT_NOTIFICATION"}
     *
     * @return this
     */
    public UpdateUtil hideDownloadNotification() {
        showNotification = false;
        return this;
    }
    
    /**
     * 显示下载进度对话框
     * 建议使用，因为可让用户在下载过程中明显感知下载进度
     *
     * @param progressTitle        下载进度对话框标题
     * @param backgroundButtonText “后台下载” 按钮文本
     * @param cancelButtonText     “取消” 按钮文本
     * @return this
     */
    public UpdateUtil showDownloadProgressDialog(@NonNull String progressTitle, @Nullable String backgroundButtonText, @Nullable String cancelButtonText) {
        isShowProgressDialog = true;
        this.progressDialogTitle = progressTitle;
        this.progressDialogBackgroundButton = backgroundButtonText;
        this.progressDialogCancelButton = cancelButtonText;
        return this;
    }
    
    /**
     * 隐藏下载进度对话框
     *
     * @return this
     */
    public UpdateUtil hideDownloadProgressDialog() {
        isShowProgressDialog = false;
        return this;
    }
    
    /**
     * 进入软件商店页
     */
    public void openMarket() {
        try {
            String str = "market://details?id=" + contextWeakReference.get().getPackageName();
            Intent localIntent = new Intent(Intent.ACTION_VIEW);
            localIntent.setData(Uri.parse(str));
            contextWeakReference.get().startActivity(localIntent);
        } catch (Exception e) {
            // 打开应用商店失败 可能是没有手机没有安装应用市场
            e.printStackTrace();
            // 调用系统浏览器进入商城
            openLinkBySystem("https://www.coolapk.com/apk/" + contextWeakReference.get().getPackageName());
        }
    }
    
    private void openLinkBySystem(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        contextWeakReference.get().startActivity(intent);
    }
    
    private OnUpdateStatusChangeListener getOnUpdateStatusChangeListener() {
        if (onUpdateStatusChangeListener == null)
            onUpdateStatusChangeListener = new DefaultUpdateStatusChangeCallBackImpl();
        return onUpdateStatusChangeListener;
    }
    
    /**
     * 设置回调监听器
     *
     * @param onUpdateStatusChangeListener 回调接口实现
     * @return this
     */
    public UpdateUtil setOnUpdateStatusChangeListener(@Nullable OnUpdateStatusChangeListener onUpdateStatusChangeListener) {
        this.onUpdateStatusChangeListener = onUpdateStatusChangeListener;
        return this;
    }
    
    /**
     * 取消一切正在进行的行为
     */
    public void cancel() {
        if (downloadManager != null && downloadId != 0) downloadManager.remove(downloadId);
        getOnUpdateStatusChangeListener().onDownloadCancel();
        if (downloadProgressTimer != null) downloadProgressTimer.cancel();
        progressDialogDismiss();
        if (updateDialog != null) updateDialog.cancel();
    }
    
    /**
     * 回收方法
     */
    public void recycle() {
        cancel();
        onUpdateStatusChangeListener = null;
    }
    
    private void progressDialogDismiss() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
    
    public interface OnUpdateStatusChangeListener {
        
        /**
         * 下载开始
         */
        void onDownloadStart();
        
        /**
         * 下载进度
         *
         * @param progress /100
         */
        void onDownloading(int progress);
        
        /**
         * 下载完成时执行
         */
        void onDownloadCompleted();
        
        /**
         * 开始安装
         */
        void onInstallStart();
        
        /**
         * 取消更新
         */
        void onDownloadCancel();
    }
    
    private void log(Object o) {
        if (DEBUGMODE)
            Log.d(">>>", o.toString());
    }
    
    public boolean isInstallWhenDownloadFinish() {
        return installWhenDownloadFinish;
    }
    
    /**
     * 下载完成后是否自动执行安装过程
     *
     * @param installWhenDownloadFinish 是否自动执行安装过程
     * @return this
     */
    public UpdateUtil setInstallWhenDownloadFinish(boolean installWhenDownloadFinish) {
        this.installWhenDownloadFinish = installWhenDownloadFinish;
        return this;
    }
    
    /**
     * 获取已设置的下载地址
     *
     * @return APK 下载链接
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }
}
