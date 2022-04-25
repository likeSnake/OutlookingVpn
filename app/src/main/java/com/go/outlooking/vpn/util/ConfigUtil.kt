package com.go.outlooking.vpn.util


import android.text.TextUtils


import com.go.outlooking.vpn.constant.ConstantSharedPreference
import com.go.outlooking.vpn.constant.ConstantSharedPreference.SP_ADS_CLICK_TIMES
import com.go.outlooking.vpn.constant.ConstantSharedPreference.SP_ADS_INTERSTITIAL_DISPLAY_TIMES
import com.go.outlooking.vpn.constant.ConstantSharedPreference.SP_ADS_INTERSTITIAL_LAST_SHOW_TIME
import com.go.outlooking.vpn.constant.FirebaseConfig
import com.go.outlooking.vpn.App
import com.go.outlooking.vpn.constant.Constant
import com.tencent.mmkv.MMKV

object ConfigUtil {


    const val TYPE_AD_NATIVE = 1
    const val TYPE_AD_INTERSTITIAL = 2


    //  1 原生广告    2  插屏广告
    fun getAdStatus(adsType: Int) : Boolean {



        val firebaseConfigAds = MMKV.defaultMMKV().decodeString(ConstantSharedPreference.SP_FIREBASE_CONFIG_ADS, FirebaseConfig.adConfig)


            if (TextUtils.isEmpty(firebaseConfigAds)) {
            return false
        }

        firebaseConfigAds?.let {

            try {

                val adConfig = DataUtil.getConfigData(firebaseConfigAds!!)!!

                //    广告全局开关，如果为true就不展示广告
                if (adConfig.closeAd == true) {
                    return false
                }

                //    广告特定版本开关，如果包含当前版本就不展示广告
                //    检测不显示的版本号
                if (adConfig.closeVersions != null && adConfig.closeVersions.isNotEmpty()) {

                    var packageManager = App.instance?.packageManager

                    val code = App.instance?.packageName?.let { packageManager?.getPackageInfo(it, 0) ?:  -1}

                    //  当前版本号 存在于 列表，返回 false
                    val closeAd = adConfig.closeVersions.contains(code.toString())

                    if (closeAd) {
                        return false
                    }
                }


                //   点击次数限制，针对所有广位，当点击次数大于等于该数字后不在展示新广告

                val clickTimes = MMKV.defaultMMKV().decodeInt(SP_ADS_CLICK_TIMES, 0)


//                    Timber.tag("--->").e( "clickTimes == $clickTimes")

                if (clickTimes != null && adConfig.clickLimit != null) {
                    if (clickTimes >= adConfig.clickLimit!!) {
                        return false
                    }
                }




                //  插屏广告
                if (adsType == TYPE_AD_INTERSTITIAL) {

                    //  如果配置是关闭插屏，怎不显示插屏
                    if (adConfig.closeInter == true) {
                        return false
                    }

                    //展示次数限制，针对插屏广告，当展示次数大于等于该数字后不在展示新广告

                    val interstitialDisplayTimes = MMKV.defaultMMKV().decodeInt(SP_ADS_INTERSTITIAL_DISPLAY_TIMES, 0)

                        if (adConfig.showLimit != null) {
                        if (interstitialDisplayTimes != null) {
                            if (interstitialDisplayTimes >= adConfig.showLimit!!) {
                                return false
                            }
                        }
                    }

                    // 时间间隔（单位秒），针对插屏广告，如果上次插屏展示时间到现在没有超过该值就不展示新的插屏

                    val interstitialLastShowTime = MMKV.defaultMMKV().decodeLong(SP_ADS_INTERSTITIAL_LAST_SHOW_TIME, 0)

                    val nowTime = System.currentTimeMillis()
                    val intervalMillis = adConfig.interval?.times(1000)

                    val timeSpan = interstitialLastShowTime?.let { nowTime.minus(it) }

//                    Timber.tag("--->").e("interstitial timeSpan == $timeSpan")

                    if (intervalMillis?.let { timeSpan?.compareTo(it) }!! < 0) {
                        return false
                    }
                }

            } catch (e: Exception) {
                return false
            }
        }


        return true
    }

    fun getOutlookingNativeAdId(): String? {


        var nativeAdIdList: MutableList<String> = arrayListOf()

        nativeAdIdList.add(Constant.OUTLOOKING_NATIVE_ONE)
        nativeAdIdList.add(Constant.OUTLOOKING_NATIVE_TWO)
        nativeAdIdList.add(Constant.OUTLOOKING_NATIVE_THREE)
        nativeAdIdList.add(Constant.OUTLOOKING_NATIVE_FOUR)
        nativeAdIdList.add(Constant.OUTLOOKING_NATIVE_FIVE)


        val lastShowedNativeAdId = MMKV.defaultMMKV().decodeString(ConstantSharedPreference.SP_LAST_SHOWED_NATIVE_ID, null)

        var list: MutableList<String>? = null

        list = if (lastShowedNativeAdId != null) {

            nativeAdIdList.filter {
                it != lastShowedNativeAdId
            } as MutableList<String>
        } else {
            nativeAdIdList
        }

        val randomNativeAdId = list?.random()

//        Timber.tag("--->").e("randomNativeAdId == $randomNativeAdId")


        MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_LAST_SHOWED_NATIVE_ID, randomNativeAdId)

//        return randomNativeAdId

        return Constant.OUTLOOKING_NATIVE_ONE


    }





}