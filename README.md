# KongzueUpdateSDK
Kongzue APP更新工具

### 说明
0) 本工具无需本地存储读写权限，但使用前请务必保证 targetSdkVersion <= 25 ，经测试，部分机器在大于25的情况下无法正常弹出安装。
1) 本工具提供下载、安装步骤，因网络请求框架不确定，本工具不包含到您服务器的检查更新的网络请求，请在获取到相应的更新信息请您自行完成。
2) 本工具需要您提供的参数对照表如下：

字段 | 含义 | 是否必须
---|---|---
info | 更新日志 | 可选
ver | 版本号 | 可选
downloadUrl | 下载地址 | 必须
me(Context) | 上下文索引 | 必须
packageName | 包名 | 必须
onDownloadListener | 下载监听器 | 可选

3) 准备
使用前请先创建UpdateInfo，举例方法如下

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

4) 下载并安装
使用如下语句创建下载工具：
me(Context) 传入上下文索引，一般使用本 Activity 即可
packageName 可直接使用 BuildConfig.APPLICATION_ID 获取：

```
UpdateUtil updateUtil = new UpdateUtil(MainActivity.this, BuildConfig.APPLICATION_ID);
```
开始下载：

```
updateUtil.doUpdate(updateInfo);
```
下载完成后会自动调用安装。

5) 关于下载的监听
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
        })
```

6) 可选的更新提示对话框
KongzueUpdateSDK 提供了一个可选使用的简易更新提示对话框，调用方法如下：

```
UpdateUtil updateUtil = new UpdateUtil(MainActivity.this, BuildConfig.APPLICATION_ID)
        .showNormalUpdateDialog(updateInfo,
                "检查到更新（" + updateInfo.getVer() + "）",
                "从商店下载",
                "直接下载",
                "取消");
```

### 引入KongzueUpdateSDK到您的项目
当前版本号：1.0.0

引入方法：
```
compile 'com.kongzue.kongzueupdatesdk:kongzueupdatesdk:1.0.0'
```