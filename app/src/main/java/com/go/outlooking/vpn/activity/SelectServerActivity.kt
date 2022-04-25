package com.go.outlooking.vpn.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.go.outlooking.vpn.bean.ServerBean
import com.go.outlooking.vpn.constant.ConstantSharedPreference
import com.go.outlooking.vpn.R
import com.go.outlooking.vpn.adapter.OnItemClickListener

import com.go.outlooking.vpn.adapter.ServerAdapter
import com.go.outlooking.vpn.ads.TemplateView
import com.go.outlooking.vpn.util.ConfigUtil
import com.go.outlooking.vpn.view.GridSpacingItemDecoration
import com.google.android.gms.ads.*

import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.tencent.mmkv.MMKV
import timber.log.Timber


class SelectServerActivity : AppCompatActivity() {


    companion object {
        const val VPN_SERVERS = "vpn_servers"
        const val VPN_SERVER_COUNTRY = "vpn_country"
    }

    private var totalList: MutableList<ServerBean>? = null


    private lateinit var ivBack: ImageView

    private lateinit var recycler: RecyclerView

    private lateinit var adapter: ServerAdapter

    private lateinit var country: String


    var currentNativeAdTop: NativeAd? = null

    private lateinit var adsTopContainer: FrameLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_select_server)

        initView()
        initVpnServers()
    }



    private fun initView() {

        ivBack = findViewById(R.id.iv_back)
        recycler = findViewById(R.id.recycler_view)

        adsTopContainer = findViewById(R.id.ads_servers_container_top)


        ivBack.setOnClickListener {finish()}

        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        val spacing = GridSpacingItemDecoration.dip2px(5f, this)

        recycler.addItemDecoration(GridSpacingItemDecoration(this, 1, spacing, true))

        adapter = ServerAdapter()

        recycler.adapter = adapter



        //   选择 服务器
        adapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(serverBean: ServerBean, position: Int) {

                serverBean.id?.let {
                    MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_SELECTED_VPN_ID, it)
                }

                MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_SELECTED_VPN_SHARE_CODE, serverBean.shareCode)
                MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_SELECTED_VPN_CNT, serverBean.countryCode)
                MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_SELECTED_VPN_DESC, serverBean.name)


                totalList?.map {
                    it.selected = it.id == serverBean.id
                }

                val intent = Intent()

                intent.putExtra(VPN_SERVERS, totalList?.toTypedArray())

                setResult(RESULT_OK, intent)

                finish()
            }
        }

        if (ConfigUtil.getAdStatus(ConfigUtil.TYPE_AD_NATIVE)) {
            adsTopContainer.visibility = View.VISIBLE
            ConfigUtil.getOutlookingNativeAdId()?.let { loadNativeAdTop(it) }
        } else{
            adsTopContainer.visibility = View.GONE
        }

    }


    private fun initVpnServers() {
        try {
//            var serializableExtra = intent.getParcelableArrayExtra(VPN_LIST) as ArrayList<VpnInfoEntity>

            var serializableExtra = intent.getSerializableExtra(VPN_SERVERS)

            country = intent.getStringExtra(VPN_SERVER_COUNTRY).toString()

            if (serializableExtra != null) {

                totalList = (serializableExtra as Array<ServerBean>).toMutableList()

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        adapter.refreshData(totalList!!)


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
//                var adsClickTimes = sharedPreference.getInt(Constant.ADS_CLICK_TIMES, 0)

                val adsClickTimes = MMKV.defaultMMKV().decodeInt(ConstantSharedPreference.SP_ADS_CLICK_TIMES, 0)

                MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_ADS_CLICK_TIMES, adsClickTimes + 1)

                removeAllAdsIfCannotShow()
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


    private fun removeAllAdsIfCannotShow() {

        if (ConfigUtil.getAdStatus(ConfigUtil.TYPE_AD_NATIVE)) {
            adsTopContainer.removeAllViews()

            currentNativeAdTop?.destroy()
        }
    }




}