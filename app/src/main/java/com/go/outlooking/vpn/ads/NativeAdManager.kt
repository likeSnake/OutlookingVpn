package com.go.outlooking.vpn.ads

import android.os.CountDownTimer
import android.util.Log
import com.go.outlooking.vpn.App
import com.go.outlooking.vpn.activity.MainActivity
import com.go.outlooking.vpn.constant.ConstantSharedPreference
import com.go.outlooking.vpn.util.ConfigUtil
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.tencent.mmkv.MMKV
import timber.log.Timber
import java.lang.ref.WeakReference

class NativeAdManager(activity: MainActivity) {


    private var nativeAdDownTimer: CountDownTimer? = null
    private var timerAdNativeMilliseconds = 30000L

    private var weakReference: WeakReference<MainActivity> = WeakReference(activity)


    fun onResume() {

        Log.e("--->", "NativeAdManager onResume ------------------------------------------")

        resumeNativeGame(timerAdNativeMilliseconds)
    }


    fun onPause() {
        Log.e("--->", "NativeAdManager onPause ------------------------------------------")
        nativeAdDownTimer?.cancel()
    }

    fun onDestroy() {
        Log.e("--->", "NativeAdManager onDestroy ------------------------------------------")
        nativeAdDownTimer?.cancel()
    }


    // Create the game timer, which counts down to the end of the level
    // and shows the "retry" button.
    private fun createNativeTimer(milliseconds: Long) {

        nativeAdDownTimer?.cancel()

        nativeAdDownTimer = object : CountDownTimer(milliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                timerAdNativeMilliseconds = millisUntilFinished

                Timber.tag("--->").e("nativeAdDownTimer mTimerMilliseconds == $millisUntilFinished")
            }

            override fun onFinish() {

                Timber.tag("--->").e("nativeAdDownTimer onFinish =================================")
                startNativeGame()

            }
        }
    }


    fun startNativeGame() {


        if (ConfigUtil.getAdStatus(ConfigUtil.TYPE_AD_NATIVE)) {

            val refreshTime = MMKV.defaultMMKV().decodeLong(ConstantSharedPreference.AD_CONFIG_REFRESH_TIME, 30)

            Timber.tag("--->").e("refreshTime == $refreshTime")

            resumeNativeGame(refreshTime * 1000)

            ConfigUtil.getOutlookingNativeAdId()?.let { loadNativeAdTop(it) }
            ConfigUtil.getOutlookingNativeAdId()?.let { loadNativeAdBottom(it) }
        } else {

            val activity = weakReference.get()

            activity?.removeAllAdsIfCannotShow()
        }


    }

    private fun resumeNativeGame(milliseconds: Long) {
        // Create a new timer for the correct length and start it.
//        mGameIsInProgress = true
//        mTimerMilliseconds = milliseconds

        createNativeTimer(milliseconds)

        nativeAdDownTimer?.start()
    }



    fun loadNativeAdTop(nativeAdId: String) {

        val activity = weakReference.get()

        val builder = AdLoader.Builder(App.instance, nativeAdId)

        builder.forNativeAd { nativeAd ->
            //  展示 Top Native Ad

//            val activity = weakReference?.get()
            activity?.showNativeAdTop(nativeAd)
        }

        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .build()

        builder.withNativeAdOptions(adOptions)

        val adLoader = builder.withAdListener(object : AdListener() {

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"

                Timber.tag("--->").e("DoraActivity NativeAd top onAdFailedToLoad error:  $error")

            }

            override fun onAdClicked() {
                super.onAdClicked()

//                Timber.tag("--->").e( "native ad top onAdClicked --------------------------------------")

                //  native ads 被点击 记录总广告次数

                val adsClickTimes = MMKV.defaultMMKV().decodeInt(ConstantSharedPreference.SP_ADS_CLICK_TIMES, 0)

                MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_ADS_CLICK_TIMES, adsClickTimes + 1)

                activity?.removeAllAdsIfCannotShow()

            }

        }).build()


        adLoader.loadAd(AdRequest.Builder().build())

    }


    fun loadNativeAdBottom(nativeAdId: String) {

        val activity = weakReference.get()

        val builder = AdLoader.Builder(App.instance, nativeAdId)

        builder.forNativeAd { nativeAd ->
            //  展示 Bottom Native Ad

            activity?.showNativeAdBottom(nativeAd)
        }

        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .build()

        builder.withNativeAdOptions(adOptions)

        val adLoader = builder.withAdListener(object : AdListener() {

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"

                Timber.tag("--->").e("DoraActivity NativeAd bottom onAdFailedToLoad error:  $error")

            }

            override fun onAdClicked() {
                super.onAdClicked()

//                Timber.tag("--->").e( "native ad bottom onAdClicked --------------------------------------")


                //  native ads 被点击 记录总广告次数

                val adsClickTimes = MMKV.defaultMMKV().decodeInt(ConstantSharedPreference.SP_ADS_CLICK_TIMES, 0)

                MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_ADS_CLICK_TIMES, adsClickTimes + 1)

                activity?.removeAllAdsIfCannotShow()

            }

        }).build()


        adLoader.loadAd(AdRequest.Builder().build())

    }



}