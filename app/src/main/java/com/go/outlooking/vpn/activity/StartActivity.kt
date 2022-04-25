package com.go.outlooking.vpn.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.go.outlooking.vpn.R
import com.go.outlooking.vpn.data.FirebaseData


class StartActivity : AppCompatActivity() {


    private val firebaseViewModel by viewModels<FirebaseData>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_start)


        firebaseViewModel.dataLoaded.observe(this) {
            if (it) {
                startActivity(Intent(this@StartActivity, MainActivity::class.java))
                finish()
            }
        }


        firebaseViewModel.loadData()

    }





//    private fun loadFireBaseData() {
//
//        val remoteConfig = Firebase.remoteConfig
//
//        remoteConfig.fetchAndActivate()
//            .addOnCompleteListener(this) { task ->
//
//                if (task.isSuccessful) {
//
//                    var firebaseConfigServers = remoteConfig[FirebaseConfig.FIRE_BASE_VPN_SERVER_CONFIG].asString()
//                    var firebaseConfigAds = remoteConfig[FirebaseConfig.FIRE_BASE_AD_CONFIG].asString()
//
//                    //  保存数据
//                    if (!TextUtils.isEmpty(firebaseConfigAds)) {
//                        MMKV.defaultMMKV().encode(ConstantSharedPreference.FIREBASE_CONFIG_ADS, firebaseConfigAds)
//                    }
//
//                    if (!TextUtils.isEmpty(firebaseConfigServers)) {
//                        MMKV.defaultMMKV().encode(ConstantSharedPreference.FIREBASE_CONFIG_SERVERS, firebaseConfigServers)
//                    }
//
//                    Timber.tag("--->").e("LaunchActivity configVpn == $firebaseConfigServers")
//
//                    Timber.tag("--->").e("LaunchActivity configAds == $firebaseConfigAds")
//
//
//                    if (!TextUtils.isEmpty(firebaseConfigAds)) {
//
//                        firebaseConfigAds?.let {
//
//                            val configEntity = DataUtil.getAdConfigData(firebaseConfigAds!!)!!
//
//                            //  初始化 FB
//                            if (configEntity != null && !TextUtils.isEmpty(configEntity.fbId)) {
//                                FacebookSdk.setApplicationId(configEntity.fbId)
//                                FacebookSdk.sdkInitialize(applicationContext)
//
//                                MMKV.defaultMMKV().encode(ConstantSharedPreference.FB_INIT_FLAG, true)
//                            }
//
//                        }
//                    }
//
//                } else {
//
//                    Timber.tag("--->").e("LaunchActivity remoteConfig Fetch failed")
//
//                }
//
//
//                startActivity(Intent(this@StartActivity, MainActivity::class.java))
//                finish()
//
//            }
//
//    }


}