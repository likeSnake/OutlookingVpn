package com.go.outlooking.vpn

import android.app.Application
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.github.shadowsocks.Core
import com.go.outlooking.vpn.activity.MainActivity
import com.go.outlooking.vpn.constant.ConstantSharedPreference
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVLogLevel
import timber.log.Timber


class App : Application(), androidx.work.Configuration.Provider by Core {

    companion object {
        lateinit var instance: App
    }


    override fun onCreate() {
        super.onCreate()

        instance = this

        val rootDir = MMKV.initialize(this, MMKVLogLevel.LevelNone)


        Core.init(this, MainActivity::class)

//        Timber.tag("--->").e("mmkv root: $rootDir")


        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_FB_INIT_FLAG, false)


    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Core.updateNotificationChannels()
    }


}