package com.go.outlooking.vpn.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.facebook.FacebookSdk
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.aidl.TrafficStats
import com.go.outlooking.vpn.bean.ServerBean
import com.github.shadowsocks.bg.BaseService
import com.go.outlooking.vpn.constant.ConstantSharedPreference
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.net.HttpsTest
import com.github.shadowsocks.utils.StartService
import com.go.outlooking.vpn.R
import com.go.outlooking.vpn.ads.InterstitialAdManager
import com.go.outlooking.vpn.ads.NativeAdManager
import com.go.outlooking.vpn.ads.TemplateView
import com.go.outlooking.vpn.constant.Constant
import com.go.outlooking.vpn.data.FirebaseData
import com.go.outlooking.vpn.util.ConfigUtil
import com.go.outlooking.vpn.util.DataUtil
import com.go.outlooking.vpn.view.LoadingDialog
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.tencent.mmkv.MMKV
import timber.log.Timber
import java.util.concurrent.ThreadLocalRandom

class MainActivity : AppCompatActivity(), ShadowsocksConnection.Callback {


    private lateinit var serversList: MutableList<ServerBean>

    private val tester by viewModels<HttpsTest>()


    private val firebaseViewModel by viewModels<FirebaseData>()



    //  更换IP重新连接
    var changeSererConnectFlag = false


    // UI
    private lateinit var tvServerCountry: TextView
    private lateinit var tvVpnConnectStatus: TextView
    private lateinit var fltSelectLocation : FrameLayout
    private lateinit var ivToggle: ImageView
    private lateinit var adsTopContainer: FrameLayout
    private lateinit var adsBottomContainer: FrameLayout


    lateinit var loadingDialog: LoadingDialog


    private var nativeAdTop: NativeAd? = null

    private var nativeAdBottom: NativeAd? = null

    var interstitialAd: InterstitialAd? = null


    private var nativeAdManager: NativeAdManager? = null

    private var interstitialAdManager: InterstitialAdManager? = null



    // service
    var state = BaseService.State.Idle


    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {

        Timber.tag("--->").e("ShadowsocksConnection.Callback stateChanged  msg ==  $msg  profileName == $profileName")
        onVpnStateChange(state, profileName, msg)
    }

    override fun trafficUpdated(profileId: Long, stats: TrafficStats) {}
    override fun trafficPersisted(profileId: Long) {}
    override fun onServiceConnected(service: IShadowsocksService) {}

    override fun onServiceDisconnected() {
        onVpnStateChange(BaseService.State.Idle, null)
    }


    override fun onBinderDied() {
        shadowsocksConnection.disconnect(this)
        shadowsocksConnection.connect(this, this)
    }


    private fun onVpnStateChange(state: BaseService.State, profileName: String?, msg: String? = null) {

        Timber.tag("--->").e("changeState  $msg  profileName == $profileName")
        this.state = state

        when (state) {
            BaseService.State.Connecting -> vpnStateConnecting()
            BaseService.State.Connected -> vpnStateConnected()
            BaseService.State.Stopping -> vpnStateStopping()
            BaseService.State.Stopped -> vpnStateDisconnected()
            else -> {}
        }
    }


    private fun vpnStateConnecting() {
        ivToggle.setImageResource(R.drawable.ic_vpn_disconnected)
    }

    private fun vpnStateConnected() {

        ivToggle.isEnabled = true
        ivToggle.setImageResource(R.drawable.ic_vpn_connected)
        tvVpnConnectStatus.text = "Status: Connected"

        if (changeSererConnectFlag) {
            changeSererConnectFlag = false
        } else {


//            goToResultPage()

            //  先弹广告，再跳转

            checkInterstitialAdConfigAndLoad()
            checkNativeAdConfigAndLoad()
        }

        tester.testConnection()
    }


    private fun vpnStateStopping() {
        ivToggle.setImageResource(R.drawable.ic_vpn_disconnected)
    }

    private fun vpnStateDisconnected() {

        ivToggle.isEnabled = true
        ivToggle.setImageResource(R.drawable.ic_vpn_disconnected)

        tvVpnConnectStatus.text = "Status: Not Connected"


        //  断开连接，如果不是换IP重连（即手动断开连接），就要弹插页广告，然后跳结果页
        if (!changeSererConnectFlag) {
            //            goToResultPage()

            //   先弹广告  再跳结果页
            checkInterstitialAdConfigAndLoad()
        }
    }



    private fun toggle() = if (state.canStop) Core.stopService() else connect.launch(null)


    private val shadowsocksConnection = ShadowsocksConnection(true)



    private val connect = registerForActivityResult(StartService()) {

    }

    private val selectServerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ActivityResultCallback {

        //   选择 server 后返回
        if (it.resultCode == RESULT_OK) {

            var serializableExtra = it.data?.getSerializableExtra(SelectServerActivity.VPN_SERVERS)

            if (serializableExtra != null) {
                serversList = (serializableExtra as Array<ServerBean>).toMutableList()
            }

            changeSelectedVpnProfile()
        }
    })


    private fun changeSelectedVpnProfile() {

        //  获取当前被选中的 vpn

        val selectedShareCode = MMKV.defaultMMKV().decodeString(ConstantSharedPreference.SP_SELECTED_VPN_SHARE_CODE, null)

        selectedShareCode?.let {

            val activeProfileList = ProfileManager.getActiveProfiles()?.toMutableList() ?: mutableListOf()

            //  查看是否已经存在
            var selectedProfile: Profile? = activeProfileList.firstOrNull { it.toString() == selectedShareCode}

            if (selectedProfile != null) {

                Core.switchProfile(selectedProfile.id)

                //  如果之前是连接状态, 则重连新选择的IP
                if (state == BaseService.State.Connected) {
                    changeSererConnectFlag = true
                }
                Core.reloadService()
            }
        }


        val desc = MMKV.defaultMMKV().decodeString(ConstantSharedPreference.SP_SELECTED_VPN_DESC, null)
        tvServerCountry.text = desc

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        initUI()

        initListener()

        nativeAdManager = NativeAdManager(this)
        interstitialAdManager = InterstitialAdManager(this)

        shadowsocksConnection.connect(this, this)

        //  测试连接状态回调
        tester.status.observe(this) {
            it.retrieve(this::testStatusCallback, this::testErrorCallback)
        }



//        firebaseViewModel.vpnServers.observe(this) {
//
//            Timber.tag("--->").e("MainActivity configVpn == $it")
//
//            //  保存数据
//            if (!TextUtils.isEmpty(it)) {
//
//                MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_FIREBASE_CONFIG_SERVERS, it)
//                initVpnServers(it)
//            }
//
//            loadingDialog.dismiss()
//        }

//        val serverConfig = MMKV.defaultMMKV().decodeString(ConstantSharedPreference.SP_FIREBASE_CONFIG_SERVERS, null)
//        if (!TextUtils.isEmpty(serverConfig)) {
//            if (serverConfig != null) {
//                initVpnServers(serverConfig)
//            }
//        }
//
//        val adConfig = MMKV.defaultMMKV().decodeString(ConstantSharedPreference.SP_FIREBASE_CONFIG_ADS, null)
//        if (!TextUtils.isEmpty(adConfig)) {
//            if (adConfig != null) {
//                initConfig(adConfig)
//            }
//        }

        initData()

        firebaseViewModel.dataLoaded.observe(this) {
            if (it) {
                initData()
            }
        }

//        firebaseViewModel.adConfig.observe(this) {
//
//            Timber.tag("--->").e("MainActivity configAds == $it")
//            if (!TextUtils.isEmpty(it)) {
//
//                MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_FIREBASE_CONFIG_ADS, it)
//
//                initConfig(it)
//            }
//        }

//        loadingDialog.show()
//        firebaseViewModel.loadData()

    }


    private fun initData() {


        val serverConfig = MMKV.defaultMMKV().decodeString(ConstantSharedPreference.SP_FIREBASE_CONFIG_SERVERS, null)
        if (!TextUtils.isEmpty(serverConfig)) {
            if (serverConfig != null) {
                initVpnServers(serverConfig)
            }
        }

        val adConfig = MMKV.defaultMMKV().decodeString(ConstantSharedPreference.SP_FIREBASE_CONFIG_ADS, null)
        if (!TextUtils.isEmpty(adConfig)) {
            if (adConfig != null) {
                initConfig(adConfig)
            }
        }
    }

    private fun initUI() {
        fltSelectLocation = findViewById(R.id.flt_select_server)
        ivToggle = findViewById(R.id.iv_vpn_connection)
        adsTopContainer = findViewById(R.id.ads_main_native_top)
        tvVpnConnectStatus = findViewById(R.id.tv_vpn_connect_status)
        adsBottomContainer = findViewById(R.id.ads_main_native_bottom)
        tvServerCountry = findViewById(R.id.tv_server_desc)
        loadingDialog = LoadingDialog(this)
    }

    private fun initVpnServers(vpnServers: String) {

        try {
            val vpnList =  DataUtil.getServerList(vpnServers)

            Timber.tag("--->").e("vpnList == $vpnList")

            vpnList?.let {
                initDefaultVpn(vpnList)
            }
        } catch (e: Exception) {
        }

    }


    private fun testStatusCallback(text: CharSequence, code: Int) {

        when (code) {
            HttpsTest.Status.CODE_TESTING -> {}

            HttpsTest.Status.CODE_SUCCESS -> {
                ivToggle.isEnabled = true

                //   如果连接成功，则重新加载 NativeAd
                if (ConfigUtil.getAdStatus(ConfigUtil.TYPE_AD_NATIVE)) {

                    ConfigUtil.getOutlookingNativeAdId()?.let { nativeAdManager?.loadNativeAdTop(it) }

                    ConfigUtil.getOutlookingNativeAdId()?.let { nativeAdManager?.loadNativeAdBottom(it) }
                }
            }

            HttpsTest.Status.CODE_ERROR -> {

                ivToggle.isEnabled = true
                //  连接失败
                Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
            }
        }

    }


    private fun testErrorCallback(error: String) {
        //  测试连接失败
        ivToggle.isEnabled = true
        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
    }


    private fun initConfig(adConfig: String) {
        try {

            var configBean = DataUtil.getConfigData(adConfig!!)!!

            configBean.refreshTime?.let { it1 ->

                MMKV.defaultMMKV().encode(ConstantSharedPreference.AD_CONFIG_REFRESH_TIME, it1)
            }

            Timber.tag("--->").e("configBean == $configBean")

            //  初始化 FB
            val fbInitFlag =  MMKV.defaultMMKV().decodeBool(ConstantSharedPreference.SP_FB_INIT_FLAG, false)

            if (!fbInitFlag && configBean != null && !TextUtils.isEmpty(configBean.fbId)) {

                Timber.tag("--->").e("初始化 Facebook id == ${configBean.fbId}")

                FacebookSdk.setApplicationId(configBean.fbId)
                FacebookSdk.sdkInitialize(applicationContext)
                MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_FB_INIT_FLAG, true)
            }

            //   加载 主页 原生广告
            nativeAdManager?.startNativeGame()

        } catch (e: Exception) {

        }
    }



    private fun initDefaultVpn(list: MutableList<ServerBean>) {


        var defaultVpnId = MMKV.defaultMMKV().decodeInt(ConstantSharedPreference.SP_SELECTED_VPN_ID, -1)

        //  判断用户类型   24小时以内新用户    0-新用户    1-老用户

        val firstUseTime = MMKV.defaultMMKV().decodeLong(ConstantSharedPreference.SP_FIRST_USE_TIME, -1L)

//        var userType = Constant.USER_TYPE_NEW

        var userType = 0

        if (firstUseTime > 0L) {
            val timeSpan = System.currentTimeMillis().minus(firstUseTime)

            if (timeSpan >= 24 * 60 * 60 * 1000) {
//                userType = Constant.USER_TYPE_OLD

                userType = 1
            }
        } else {
            MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_FIRST_USE_TIME, System.currentTimeMillis())
        }



        Timber.tag("--->").e("userType == %s", userType)

        //  根据用户类型筛选数据，并且打乱顺序
        val filteredServers = list.filter {
            it.type == userType
        }

        val shuffledServers = filteredServers.shuffled().toMutableList()


        if (shuffledServers != null) {

            //  随机一个作为 推荐
            val randomInt =  ThreadLocalRandom.current().nextInt(0, shuffledServers.size)
            val randomVpn = shuffledServers[randomInt]

            //  默认 server id 为 -1
            val defaultVpn = ServerBean(-1, randomVpn.countryCode, "Fast Smart", randomVpn.shareCode, randomVpn.icon, randomVpn.recommend, randomVpn.selected, randomVpn.type)

            shuffledServers.add(0, defaultVpn)

            defaultVpn.id?.let {

                MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_SELECTED_VPN_ID, it)

                defaultVpnId = it
            }

            MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_SELECTED_VPN_DESC, defaultVpn.name)

            MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_SELECTED_VPN_CNT, defaultVpn.countryCode)

            MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_SELECTED_VPN_SHARE_CODE, defaultVpn.shareCode)
        }

        shuffledServers?.forEach {

            it.selected = it.id == defaultVpnId
            it.recommend = it.id == defaultVpnId
        }

        Timber.tag("--->").e("shuffledServers == $shuffledServers")

        if (shuffledServers != null) {
            serversList = shuffledServers

            saveServers()
        }


        changeSelectedVpnProfile()


    }


    private fun saveServers() {

        serversList?.let {


            //  循环 每条数据，如果数据已存在，则不加入，不存在，则把获取的vpn加入
            serversList.forEach { vpnInfoEntity ->
                run {

                    //  根据 shareCode 查找数据
                    val profiles = Profile.findAllUrls(
                        vpnInfoEntity.shareCode,
                        Core.currentProfile?.main
                    ).toList()


//                    Timber.tag("--->").e("profiles == $profiles")

                    //  获取已存在的VPN列表
                    val activeProfileList = ProfileManager.getActiveProfiles()?.toMutableList() ?: mutableListOf()

                    //  查看是否已经存在
                    var profileTemp: Profile? =
                        activeProfileList.firstOrNull { it.toString() == vpnInfoEntity.shareCode }

                    if (profileTemp == null) {

                        if (profiles.isNotEmpty()) {

                            Timber.tag("--->").e("Server 不存在 添加 ----------------------------- ")

                            //  vpn 不存在 创建一条
                            profileTemp = ProfileManager.createProfile(profiles[0])

                        }
                    }

                    if (vpnInfoEntity.recommend == true && profileTemp != null) {

                        Core.switchProfile(profileTemp.id)
                        Core.reloadService()
                    }
                }
            }
        }

    }



    override fun onStart() {
        super.onStart()
        shadowsocksConnection.bandwidthTimeout = 500
    }



    private fun initListener() {

        fltSelectLocation.setOnClickListener {

            try {
                if (serversList == null) {


                    firebaseViewModel.loadData()

                    return@setOnClickListener
                }

                val intent = Intent(this@MainActivity, SelectServerActivity::class.java)
                intent.putExtra(SelectServerActivity.VPN_SERVERS, serversList.toTypedArray())
                selectServerLauncher.launch(intent)
            } catch (e: Exception) {

                Timber.tag("--->").e("exception ---> ${e.message}")
                firebaseViewModel.loadData()
            }


        }

        ivToggle.setOnClickListener {
            toggle()
        }
    }



    fun goToResultPage() {

        val intent = Intent(this, ResultActivity::class.java)

        when (state) {
            BaseService.State.Connected -> intent.putExtra(ResultActivity.INTENT_STATUS, Constant.STATUS_VPN_CONNECTED)
            BaseService.State.Stopped -> intent.putExtra(ResultActivity.INTENT_STATUS, Constant.STATUS_VPN_DISCONNECTED)
        }

        startActivity(intent)
    }




    override fun onStop() {
        shadowsocksConnection.bandwidthTimeout = 0
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()



        try {
            nativeAdManager?.onDestroy()
            interstitialAdManager?.onDestroy()
        } catch (e: Exception) {

        }


        shadowsocksConnection.disconnect(this)
    }


    private fun checkNativeAdConfigAndLoad() {

        if (ConfigUtil.getAdStatus(ConfigUtil.TYPE_AD_NATIVE)) {

//            Timber.tag("--->").e("MainActivity 加载 Native 广告 -----------------------------------")

            ConfigUtil.getOutlookingNativeAdId()?.let {
                nativeAdManager?.loadNativeAdTop(it)
            }

            ConfigUtil.getOutlookingNativeAdId()?.let { nativeAdManager?.loadNativeAdBottom(it) }

        } else {

            //  超过次数，移除 native ad

                try {
                    nativeAdTop?.destroy()
                } catch (e: Exception) {}

            adsTopContainer.removeAllViews()

            try {
                nativeAdBottom?.destroy()
            } catch (e : Exception){}

            adsBottomContainer.removeAllViews()
        }

    }



    fun showNativeAdBottom(nativeAd: NativeAd) {


        Timber.tag("--->").e("MainActivity showNativeAdBottom nativeAd == $nativeAd")

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
        try {
            nativeAdBottom?.destroy()
        } catch (e:Exception) {
        }

        nativeAdBottom = nativeAd


        var adTemplateView: TemplateView =
            LayoutInflater.from(this).inflate(R.layout.ad_nat_small_bottom, null) as TemplateView


        adTemplateView.setNativeAd(nativeAd)

        adsBottomContainer.removeAllViews()
        adsBottomContainer.addView(adTemplateView)

    }




    fun showNativeAdTop(nativeAd: NativeAd) {


        Timber.tag("--->").e("MainActivity showNativeAdTop nativeAd == $nativeAd")

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
        try {
            nativeAdTop?.destroy()
        } catch (e: Exception) {
        }

        nativeAdTop = nativeAd


        var adTemplateView: TemplateView =
            LayoutInflater.from(this).inflate(R.layout.ad_nat_small_top, null) as TemplateView


        adTemplateView.setNativeAd(nativeAd)

        adsTopContainer.removeAllViews()
        adsTopContainer.addView(adTemplateView)

    }


    private fun checkInterstitialAdConfigAndLoad() {

        if (ConfigUtil.getAdStatus(ConfigUtil.TYPE_AD_INTERSTITIAL)) {
            loadingDialog?.show()

            interstitialAdManager?.startGame()
        } else {
            goToResultPage()
        }

    }



    // Show the ad if it's ready. Otherwise toast and restart the game.
    fun showInterstitial() {

        Timber.tag("--->").e( "showInterstitial ======================================")

        if (interstitialAd != null) {

            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {

                    interstitialAd = null
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {

                    Timber.tag("--->").d( "Ad failed to show.")

                    interstitialAd = null
                }

                override fun onAdShowedFullScreenContent() {

//                    Log.d("--->", "Ad showed fullscreen content.")
                    // Called when ad is dismissed.
                }
            }

            interstitialAd?.show(this)

        } else {

            interstitialAdManager?.startGame()

        }
    }



    // Resume the game if it's in progress.
    public override fun onResume() {
        super.onResume()

        interstitialAdManager?.onResume()

        nativeAdManager?.onResume()

    }

    // Cancel the timer if the game is paused.
    public override fun onPause() {

        interstitialAdManager?.onPause()

        nativeAdManager?.onPause()

        super.onPause()
    }



    fun removeAllAdsIfCannotShow() {

        if (ConfigUtil.getAdStatus(ConfigUtil.TYPE_AD_NATIVE)) {
            adsTopContainer.removeAllViews()
            adsBottomContainer.removeAllViews()

            try {
                nativeAdTop?.destroy()
                nativeAdBottom?.destroy()
            } catch (e : Exception) {

            }

        }
    }



}