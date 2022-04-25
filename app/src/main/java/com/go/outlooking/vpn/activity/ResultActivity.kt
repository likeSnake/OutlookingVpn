package com.go.outlooking.vpn.activity


import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import com.go.outlooking.vpn.constant.ConstantSharedPreference
import com.go.outlooking.vpn.R
import com.go.outlooking.vpn.ads.TemplateView
import com.go.outlooking.vpn.constant.Constant
import com.go.outlooking.vpn.util.ConfigUtil
import com.google.android.gms.ads.*

import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.tencent.mmkv.MMKV
import timber.log.Timber


class ResultActivity : AppCompatActivity() {


    companion object {
        const val INTENT_STATUS: String = "status"

    }



    private lateinit var ivBack : ImageView
    private lateinit var tvStatusTitle: TextView
    private lateinit var ivStatus : ImageView
    private lateinit var tvStatus: TextView
    private lateinit var tvDesc: TextView
    private lateinit var ivCountryFlag: ImageView


    private lateinit var adsTopContainer: FrameLayout

    private lateinit var adsBottomContainer: FrameLayout


    private var currentNativeAdTop: NativeAd? = null

    private var currentNativeAdBottom: NativeAd? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result)

        initUI()

        if (ConfigUtil.getAdStatus(ConfigUtil.TYPE_AD_NATIVE)) {

            ConfigUtil.getOutlookingNativeAdId()?.let { loadNativeAdTop(it) }
            ConfigUtil.getOutlookingNativeAdId()?.let { loadNativeAdBottom(it) }

        }

    }



    private fun initUI() {

        ivBack = findViewById(R.id.iv_back)
        tvStatusTitle = findViewById(R.id.tv_status_title)
        ivStatus = findViewById(R.id.iv_status)
        tvStatus = findViewById(R.id.tv_status)
        ivCountryFlag = findViewById(R.id.country_flag)
        adsTopContainer = findViewById(R.id.ads_connection_result_container_top)
        adsBottomContainer = findViewById(R.id.ads_connection_result_container_bottom)

        ivBack.setOnClickListener {
            finish()
        }

        tvDesc = findViewById(R.id.tv_desc)

        if (ConfigUtil.getAdStatus(ConfigUtil.TYPE_AD_NATIVE)) {
            ConfigUtil.getOutlookingNativeAdId()?.let { loadNativeAdTop(it) }
            ConfigUtil.getOutlookingNativeAdId()?.let { loadNativeAdBottom(it) }
        } else {
            adsTopContainer.visibility = View.GONE
            adsBottomContainer.visibility = View.GONE
        }

        val desc = MMKV.defaultMMKV().decodeString(ConstantSharedPreference.SP_SELECTED_VPN_DESC, null)

        tvDesc.text = desc


        when (intent.getIntExtra(INTENT_STATUS, Constant.STATUS_VPN_CONNECTED)) {

            Constant.STATUS_VPN_CONNECTED -> {

                tvStatusTitle.setText(R.string.success)
                ivStatus.setImageResource(R.drawable.ic_status_connected)
                tvStatus.setText(R.string.success_connected)
            }

            Constant.STATUS_VPN_DISCONNECTED -> {

                tvStatusTitle.setText(R.string.disconnected)
                ivStatus.setImageResource(R.drawable.ic_status_disconnected)
                tvStatus.setText(R.string.disconnected)

            }
        }


        when (MMKV.defaultMMKV().decodeString(ConstantSharedPreference.SP_SELECTED_VPN_CNT, null)) {
            "US" -> {
                ivCountryFlag.setImageResource(R.drawable.flag_unitedstates)

            }

//            "GB" -> {
//                ivCountryFlag.setImageResource(R.drawable.flag_unitedkingdom)
//            }
//
//            "DE" -> {
//                ivCountryFlag.setImageResource(R.drawable.flag_germany)
//            }
//
//            "FR" -> {
//                ivCountryFlag.setImageResource(R.drawable.flag_france)
//            }
//
//            "JP" -> {
//                ivCountryFlag.setImageResource(R.drawable.flag_japan)
//            }
//
//            "AU" -> {
//                ivCountryFlag.setImageResource(R.drawable.flag_australia)
//            }
//
//            "SG" -> {
//                ivCountryFlag.setImageResource(R.drawable.flag_singapore)
//            }
        }
    }




    private fun loadNativeAdBottom(nativeAdId: String) {

        val builder = AdLoader.Builder(this, nativeAdId)

        builder.forNativeAd { nativeAd ->
            //  展示 Bottom Native Ad
            showNativeAdBottom(nativeAd)
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
//                val error =
//                    """
//           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
//          """"
//
//                Timber.tag("--->").e("DoraActivity NativeAd bottom onAdFailedToLoad error:  $error")

            }

            override fun onAdClicked() {
                super.onAdClicked()

//                Timber.tag("--->").e( "native ad bottom onAdClicked --------------------------------------")

                //  native ads 被点击 记录总广告次数

                val adsClickTimes = MMKV.defaultMMKV().decodeInt(ConstantSharedPreference.SP_ADS_CLICK_TIMES, 0)

                MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_ADS_CLICK_TIMES, adsClickTimes + 1)

                removeAds()

            }

        }).build()


        adLoader.loadAd(AdRequest.Builder().build())

    }


    private fun showNativeAdBottom(nativeAd: NativeAd) {


//        Timber.tag("--->").e("ConnectResultActivity nativeAd == $nativeAd")

        // OnUnifiedNativeAdLoadedListener implementation.
        // If this callback occurs after the activity is destroyed, you must call
        // destroy and return or you may get a memory leak.

        var activityDestroyed = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            activityDestroyed = isDestroyed == true
        }

        if (activityDestroyed || isFinishing || isChangingConfigurations) {
            nativeAd.destroy()
            return
        }

        // You must call destroy on old ads when you are done with them,
        // otherwise you will have a memory leak.
        currentNativeAdBottom?.destroy()
        currentNativeAdBottom = nativeAd


        var adTemplateView: TemplateView =
            LayoutInflater.from(this).inflate(R.layout.ad_nat_small_bottom, null) as TemplateView


        adTemplateView.setNativeAd(nativeAd)

        adsBottomContainer.removeAllViews()
        adsBottomContainer.addView(adTemplateView)

    }



    private fun loadNativeAdTop(nativeAdId: String) {

        val builder = AdLoader.Builder(this, nativeAdId)

        builder.forNativeAd { nativeAd ->
            //  展示 Top Native Ad
            showNativeAdTop(nativeAd)
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
                Timber.tag("--->").e( "native ad top onAdClicked --------------------------------------")


                //  native ads 被点击 记录总广告次数

                val adsClickTimes = MMKV.defaultMMKV().decodeInt(ConstantSharedPreference.SP_ADS_CLICK_TIMES, 0)

                MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_ADS_CLICK_TIMES, adsClickTimes + 1)

                removeAds()

            }

        }).build()


        adLoader.loadAd(AdRequest.Builder().build())

    }


    private fun showNativeAdTop(nativeAd: NativeAd) {


        Timber.tag("--->").e("ConnectResultActivity nativeAd == $nativeAd")

        // OnUnifiedNativeAdLoadedListener implementation.
        // If this callback occurs after the activity is destroyed, you must call
        // destroy and return or you may get a memory leak.

        var activityDestroyed = false
        activityDestroyed = isDestroyed == true

        if (activityDestroyed || isFinishing || isChangingConfigurations) {
            nativeAd.destroy()
            return
        }

        // You must call destroy on old ads when you are done with them,
        // otherwise you will have a memory leak.
        currentNativeAdTop?.destroy()
        currentNativeAdTop = nativeAd


        var adTemplateView: TemplateView =
            LayoutInflater.from(this).inflate(R.layout.ad_nat_small_top, null) as TemplateView


        adTemplateView.setNativeAd(nativeAd)

        adsTopContainer.removeAllViews()
        adsTopContainer.addView(adTemplateView)

    }


    private fun removeAds() {

        if (ConfigUtil.getAdStatus(ConfigUtil.TYPE_AD_NATIVE)) {
            adsTopContainer.removeAllViews()
            adsBottomContainer.removeAllViews()

            currentNativeAdTop?.destroy()
            currentNativeAdBottom?.destroy()
        }
    }





}