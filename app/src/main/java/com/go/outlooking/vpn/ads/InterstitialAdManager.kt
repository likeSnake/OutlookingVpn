package com.go.outlooking.vpn.ads

import android.os.CountDownTimer
import com.go.outlooking.vpn.App
import com.go.outlooking.vpn.activity.MainActivity
import com.go.outlooking.vpn.constant.Constant
import com.go.outlooking.vpn.constant.ConstantSharedPreference
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.tencent.mmkv.MMKV
import java.lang.ref.WeakReference

class InterstitialAdManager(activity: MainActivity) {

    companion object {
        const val GAME_LENGTH_MILLISECONDS = 3000L
    }

    private var weakReference: WeakReference<MainActivity> = WeakReference(activity)



    private var countDownTimer: CountDownTimer? = null
    private var gameIsInProgress: Boolean = false

    var adIsLoading: Boolean = false

    private var mTimerMilliseconds = 0L




    fun onResume() {
        if (gameIsInProgress) {
            resumeGame(mTimerMilliseconds)
        }
    }

    fun onPause() {

        countDownTimer?.cancel()
    }


    fun onDestroy() {
        countDownTimer?.cancel()
    }



    // Request a new ad if one isn't already loaded, hide the button, and kick off the timer.
    fun startGame() {
        val activity = weakReference.get()
        if (!adIsLoading && activity?.interstitialAd == null) {
            adIsLoading = true
            loadInterstitialAd()
        }

        resumeGame(GAME_LENGTH_MILLISECONDS)
    }


    private fun resumeGame(milliseconds: Long) {
        // Create a new timer for the correct length and start it.
        gameIsInProgress = true
        mTimerMilliseconds = milliseconds
        createTimer(milliseconds)
        countDownTimer?.start()
    }


    // Create the game timer, which counts down to the end of the level
    // and shows the "retry" button.
    private fun createTimer(milliseconds: Long) {

        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(milliseconds, 50) {
            override fun onTick(millisUntilFinished: Long) {
                mTimerMilliseconds = millisUntilFinished

//                Log.e("--->", "mCountDownTimer mTimerMilliseconds == $mTimerMilliseconds")
            }

            override fun onFinish() {
                gameIsInProgress = false

//                Log.e("--->", "mCountDownTimer onFinish =================================")
            }
        }
    }


//  ========  Interstitial  Ad  =========================================================

    private fun loadInterstitialAd() {

        val adRequest = AdManagerAdRequest.Builder().build()

        val activity = weakReference.get()

        AdManagerInterstitialAd.load(
            App.instance,
            Constant.OUTLOOKING_MAIN_INTERSTITIAL,
            adRequest,
            object : AdManagerInterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {

                    // The mInterstitialAd reference will be null until
                    // an ad is loaded.

                    activity?.interstitialAd = interstitialAd
                    activity?.loadingDialog?.dismiss()

                    activity?.showInterstitial()

//                    Timber.tag("--->").e( "InterstitialAd  onAdLoaded  ------------------")

                    interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {

                        override fun onAdDismissedFullScreenContent() {

//                            Timber.tag("--->").e("InterstitialAd onAdDismissedFullScreenContent ----------------------")

                            activity?.interstitialAd = null

                            //  插屏广告消失 记录总广告次数

                            val adsClickTimes = MMKV.defaultMMKV().decodeInt(
                                ConstantSharedPreference.SP_ADS_CLICK_TIMES, 0)

                            MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_ADS_CLICK_TIMES, adsClickTimes + 1)

                            activity?.removeAllAdsIfCannotShow()


                            //  跳结果页
                            activity?.goToResultPage()

                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {

//                            Timber.tag("--->").e("InterstitialAd onAdFailedToShowFullScreenContent --- adError == ${adError.message}")

                            activity?.interstitialAd = null

                            //  跳结果页
                            activity?.goToResultPage()

                        }

                        override fun onAdShowedFullScreenContent() {

//                            Timber.tag("--->").e( "InterstitialAd onAdShowedFullScreenContent ------------------------------")

                            //   插屏广告展示出来，记录展示当前时间 和  插屏广告展示次数

                            MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_ADS_INTERSTITIAL_LAST_SHOW_TIME, System.currentTimeMillis())

                            val interstitialAdDisplayTimes = MMKV.defaultMMKV().decodeInt(
                                ConstantSharedPreference.SP_ADS_INTERSTITIAL_DISPLAY_TIMES)

                            MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_ADS_INTERSTITIAL_DISPLAY_TIMES, interstitialAdDisplayTimes + 1)

                        }
                    }

                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {

                    adIsLoading = false
                    activity?.interstitialAd = null
                    activity?.loadingDialog?.dismiss()



//                    val error = String.format(
//                        "domain: %s, code: %d, message: %s",
//                        loadAdError.domain, loadAdError.code, loadAdError.message
//                    )
//
//                    Timber.tag("--->").e( "InterstitialAd onAdFailedToLoad $error")

                    //  跳结果页
                    activity?.goToResultPage()

                }
            })


    }



}