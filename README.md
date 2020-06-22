# KongzueUpdateSDK V3
Kongzue APP更新工具

此框架用于快速实现 APP 的更新流程，包含了下载过程和自动调取安装过程。 

<a href="https://github.com/kongzue/KongzueUpdateSDK">
<img src="https://img.shields.io/badge/KongzueUpdateSDK-3.0.0-green.svg" alt="KongzueUpdateSDK">
</a> 
<a href="https://bintray.com/myzchh/maven/KongzueUpdateSDK">
<img src="https://img.shields.io/badge/Maven-3.0.0-blue.svg" alt="Maven">
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
implementation 'com.kongzue.kongzueupdatesdk:kongzueupdatesdk:3.0.0'
```

老版本使用方法详见： <a href="README_V2.md">KongzueUpdateSDK 旧版接入指南</a>

#### 配置
1) 本工具提供下载、安装步骤，因网络请求框架不确定，本工具不包含从您服务器的检查 APP 版本过程的网络请求。

2) 添加权限：
```
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
```

3) 对于非 https 的地址，建议在 AndroidManifest.xml 中的 <application> 标签添加以下属性：
```
...
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...
    >
    ...
    
</application>
```
然后在您的项目 res 下新建 xml 目录，添加文件 network_security_config.xml，内容如下：
```
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>
```

4) 在 AndroidManifest.xml 中 <application> 标签作用域内添加以下属性，注意将文中“{你的 APP 包名}”替换为你的 APP 真实包名。
```
<provider
    android:name="android.support.v4.content.FileProvider"
    android:authorities="{你的 APP 包名}.fileProvider"
    android:grantUriPermissions="true"
    android:exported="false">
    <!--元数据-->
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_path"/>
</provider>
```

然后在 res 的 xml 目录中新建 file_path.xml，并添加如下代码：
```
<resources xmlns:android="http://schemas.android.com/apk/res/android">
    <paths>
        <external-path path="" name="download"/>
    </paths>
</resources>
```

## 使用

### 使用默认提示和 UI
如果您可以接受默认的 Material 风格系统对话框样式，可以使用如下代码快速创建更新过程：
```
//初始化
UpdateUtil updateUtil = new UpdateUtil(this);

updateUtil.setOnUpdateStatusChangeListener(onUpdateStatusChangeListener)    //状态回调接口
        .showDownloadNotification("正在更新", "正在下载UpdateV3框架更新...")    //显示更新通知提示
        .showDownloadProgressDialog("正在下载", "后台下载", "取消")            //显示下载进度对话框
        .start(editDownloadUrl.getText().toString(),                       //开始下载
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
```
默认过程中，会先显示一个系统对话框提示用户有新的版本，用户可以选择开始下载、取消下载和去商店更新，其中去商店更新文本传值为 null 时不显示此按钮。

您可以设置是否强制更新，若强制更新，用户默认无法通过后退按钮关闭更新对话框，且您可以在状态回调接口 OnUpdateStatusChangeListener 的 onDownloadCancel 事件中做用户点击 “取消” 按钮时的相应处理。

此版本代码中的注释非常详尽，建议阅读 UpdateUtil 类中各方法的注释信息，了解更多细节。

### 使用自定义更新过程 UI

使用自定义更新过程 UI 可以允许您在不使用 UpdateUtil 提供的各种对话框的情况下自定义需要的更新界面逻辑。

首先您可能需要自行弹出一个更新提示，当用户点击立即更新时，执行以下逻辑：
```
updateUtil.setOnUpdateStatusChangeListener(onUpdateStatusChangeListener)
        .hideDownloadProgressDialog()                           //不要显示下载过程进度对话框
        .setInstallWhenDownloadFinish(false)                    //不要下载完成后立即安装
        .start(editDownloadUrl.getText().toString());           //开始下载
```
您需要设置一个回调方法以获得下载进度等信息，例如 Demo 代码中展示了一个让进度条组件 progressBar 更新进度的过程：
```
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
```
在下载完成事件 onDownloadCompleted() 触发后，您可以提示用户安装，此时执行以下逻辑即可进入安装流程：
```
updateUtil.installApk();        //此方法会返回一个 boolean 的标记，若此标记为 false 代表执行不成功，一般是未授权允许 APP 安装其他 APK，开启系统授权即可（此授权是自动显示的）
```

## 常见问题
1. 在 Pixel 或原生系统中，未报错情况下出现下载进度一直处于 0% 的情况
请检查网络是否可以正常连接 Google 服务器，因 Google 会进行 APK 安全性校验，若无法连接至 Google 会导致卡 0% 却不报错的问题。

2. 在退出 Activity 时停止流程和销毁内存：
```
updateUtil.recycle();
```

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
- 3.0.0：
重大更新；