package com.prpr.androidpprog2.entregable.controller.callbacks;

import com.downloader.Progress;

import java.io.IOException;

public interface DownloadCallback {
    void doNext() throws IOException;
}
