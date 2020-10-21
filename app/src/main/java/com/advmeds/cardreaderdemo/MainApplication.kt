package com.advmeds.cardreaderdemo

import android.app.Application
import com.vise.baseble.ViseBle

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        ViseBle.config()
            .setScanTimeout(-1) // 扫描超时时间，这里设置为永久扫描
        //蓝牙信息初始化，全局唯一，必须在应用初始化时调用
        ViseBle.getInstance().init(this);
    }
}