# KongzueUpdateSDK
Kongzue APP更新工具

<a href="https://github.com/kongzue/KongzueUpdateSDK">
<img src="https://img.shields.io/badge/KongzueUpdateSDK-1.4.5-green.svg" alt="KongzueUpdateSDK">
</a> 
<a href="https://bintray.com/myzchh/maven/KongzueUpdateSDK">
<img src="https://img.shields.io/badge/Maven-1.4.5-blue.svg" alt="Maven">
</a> 
<a href="http://www.apache.org/licenses/LICENSE-2.0">
<img src="https://img.shields.io/badge/License-Apache%202.0-red.svg" alt="Maven">
</a> 
<a href="http://www.kongzue.com">
<img src="https://img.shields.io/badge/Homepage-Kongzue.com-brightgreen.svg" alt="Maven">
</a> 


## 引入KongzueUpdateSDK到您的项目

引入方法：
```
implementation 'com.kongzue.kongzueupdatesdk:kongzueupdatesdk:1.4.5'
```

## 重要说明
1) 本工具无需权限，但在 targetSdkVersion >= 26 的情况时可能出现安装程序闪退但不报错的问题，系 Android 8.0 的新规定，请注意在您的应用中额外添加权限：
```
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
```

2) 本工具提供下载、安装步骤，因网络请求框架不确定，本工具不包含到您服务器的检查更新的网络请求，请在获取到相应的更新信息请您自行完成。
3) 本工具需要您提供的参数对照表如下：

字段 | 含义 | 是否必须
---|---|---
info | 更新日志 | 可选
ver | 版本号 | 可选
downloadUrl | 下载地址 | 必须
me(Context) | 上下文索引 | 必须
packageName | 包名 | 必须
onDownloadListener | 下载监听器 | 可选

需要的主要权限：
```
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET"/>
```

## 准备
1) 修改 AndroidManifest.xml
因 Android 7.0规范限定，我们需要创建一个共享目录来存储下载的文件
请在 AndroidManifest.xml 中加入如下代码：
```
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="您的包名">

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        ...>
        
        ...
        
        <provider
            android:name="android.support.v4.content.FileProvider"
            android:authorities="您的包名.fileProvider"
            android:grantUriPermissions="true"
            android:exported="false">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_path"/>
        </provider>
    </application>
</manifest>
```
接下来在 res 目录下创建文件夹 xml，并添加文件 file_path.xml：
```
<resources xmlns:android="http://schemas.android.com/apk/res/android">
    <paths>
        <external-path path="" name="download"/>
    </paths>
</resources>
```

2) 使用前请先创建UpdateInfo，举例方法如下

```
updateInfo = new UpdateInfo()
.setInfo("1.上线了极力要求以至于无法再拒绝的收入功能\n" +
        "2.出行的二级分类加入了地铁、地铁、地铁\n" +
        "3.「关于」新增应用商店评分入口，你们知道怎么做\n" +
        "4.「关于」还加入了GitHub地址，情怀+1s\n" +
        "5.全新的底层适配框架，优化更多机型")
.setVer("v2.5")
.setDownloadUrl("http://paywhere.kongzue.com/downloads/paywhere.apk");
```

3) 开发模式

建议在开发过程中打开日志输出方便排查故障
```
UpdateUtil.DEBUGMODE = true;
```
此设置也可跟您的项目的 BuildConfig.DEBUG 进行关联。

## 下载并安装
1) 使用如下语句创建下载工具：
me(Context) 传入上下文索引，一般使用本 Activity 即可
packageName 可直接使用 BuildConfig.APPLICATION_ID 获取：

```
UpdateUtil updateUtil = new UpdateUtil(MainActivity.this);
```
2) 开始下载（结束后自动会弹出安装界面）：

```
updateUtil.doUpdate(updateInfo);
```

3) 取消下载
```
updateUtil.cancel();
```

4) 手动执行安装
```
updateUtil.installApk(MainActivity.this);
```

## 关于下载的监听
您可以通过以下代码监听下载过程：

```
UpdateUtil updateUtil = new UpdateUtil(MainActivity.this, BuildConfig.APPLICATION_ID)
        .setOnDownloadListener(new UpdateUtil.OnDownloadListener() {
            @Override
            public void onStart(long downloadId) {
                Log.i("MainActivity", "onStart: 下载开始");
            }
            @Override
            public void onDownloading(long downloadId, int progress) {
                Log.i("MainActivity", "onStart: 下载中："+progress);
            }
            @Override
            public void onSuccess(long downloadId) {
                Log.i("MainActivity", "onStart: 下载完成");
            }
            @Override
            public void onCancel(long downloadId) {
                Log.i("MainActivity", "onStart: 下载取消");
            }
        })
```
## 其他
可选的更新提示对话框
KongzueUpdateSDK 提供了一个可选使用的简易更新提示对话框，调用方法如下：

```
UpdateUtil updateUtil = new UpdateUtil(MainActivity.this, BuildConfig.APPLICATION_ID)
        .showNormalUpdateDialog(updateInfo,
                "检查到更新（" + updateInfo.getVer() + "）",
                "从商店下载",
                "直接下载",
                "取消");
```

其中"从商店下载"、"直接下载"可传 null，若使用 null 则会隐藏该按钮。

额外的小工具：
```
UpdateUtil.isWifi()                         //判断Wifi状态
UpdateUtil.isShowProgressDialog = true;     //是否开启进度对话框（默认关闭）
```

额外设置：
```
UpdateUtil.updateTitle = (String)           //设置更新提示标题
UpdateUtil.progressDialogTitle = (String)   //下载进度提示框和通知标题
UpdateUtil.progressDescription = (String)   //下载进度通知内容
```

## 强制更新
从 1.4.3 版本起，在使用 showNormalUpdateDialog 方法时新增了一个可选参数 isForced，当它传入 true 值时会开启强制更新。

强制更新开启后，弹出检查到更新对话框时，无法取消，在更新过程中点击对话框外无法取消。

但用户在更新过程中依然可以点击“取消下载”按钮退出下载过程，此时 OnDownloadListener 监听器的 onCancel 事件会被调用，开发者可以自行选择退出程序等操作。

下载完成后 UpdateUtil 会自动弹出安装，但因为安装过程不可控，用户可能手动返回，因此强烈建议开发者在 OnDownloadListener 的 onSuccess 事件中对软件进行退出，或者弹出一个自己的对话框阻止用户操作。

额外的，UpdateUtil 公开了 installApk(Context) 方法，使用该方法可以手动重新启动安装，但此方法必须通过 UpdateUtil 下载完成后才可以使用。

## 常见问题
1. 在 Pixel 或原生系统中，未报错情况下出现下载进度一直处于 0% 的情况
请检查网络是否可以正常连接 Google 服务器，因 Google 会进行 APK 安全性校验，若无法连接至 Google 会导致卡 0% 却不报错的问题。


## 开源协议
```
Copyright KongzueUpdateSDK

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 更新日志：
1.4.5：
- 新增日志开关 DEBUGMODE；
- 修复一些问题；

1.4.4：
- 新增下载时通知，详细设置请参考本文档。
- 新增公开方法 cancel() 取消下载；
- 默认情况下不显示下载进度对话框，但若使用 showNormalUpdateDialog(...) 默认更新提示对话框启动，则会显示更新进度对话框；
- 新增方法 showNormalUpdateDialog(UpdateInfo) 用于快速显示更新提示

1.4.3：
- 更新提示对话框的"从商店下载"、"直接下载"按钮新增 null 文本判断，若使用 null 则会隐藏该按钮。
- 新增强制更新属性，使用默认更新提示对话框开启该属性时无法取消，且对话框无法关闭；

1.4.2：
- 修复当 updateInfo.getVer() 为空时导致安装失败的问题；

1.4.1：
- 修复 onDownloadListener.onSuccess 不执行的问题；

1.4.0：
- 修复进度显示可能存在问题的bug；

1.3.0：
- 修复安卓8.0设备上安装崩溃的问题；