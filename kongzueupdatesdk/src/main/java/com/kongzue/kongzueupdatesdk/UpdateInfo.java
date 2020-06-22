package com.kongzue.kongzueupdatesdk;

@Deprecated
public class UpdateInfo {

    private String info;
    private String ver;
    private String downloadUrl;

    public UpdateInfo() {
        this.info = info;
        this.ver = ver;
        this.downloadUrl = downloadUrl;
    }

    public UpdateInfo(String info, String ver, String downloadUrl) {
        this.info = info;
        this.ver = ver;
        this.downloadUrl = downloadUrl;
    }

    public String getInfo() {
        return info;
    }

    public UpdateInfo setInfo(String info) {
        this.info = info;
        return this;
    }

    public String getVer() {
        return ver;
    }

    public UpdateInfo setVer(String ver) {
        this.ver = ver;
        return this;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public UpdateInfo setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        return this;
    }
}
