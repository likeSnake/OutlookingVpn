package com.go.outlooking.vpn.data


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.go.outlooking.vpn.constant.ConstantSharedPreference
import com.go.outlooking.vpn.constant.FirebaseConfig

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.get
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

//import timber.log.Timber

class FirebaseData : ViewModel() {


//    val vpnServers = MutableLiveData<String>()
//    val adConfig = MutableLiveData<String>()

    val dataLoaded = MutableLiveData<Boolean>()

    private var running: Job? = null

    fun loadData() {

        dataLoaded.value = false

        val remoteConfig = Firebase.remoteConfig

        running = GlobalScope.launch(Dispatchers.Main.immediate) {

            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        val firebaseDataOne = remoteConfig[FirebaseConfig.KEY_FIRE_BASE_VPN_SERVER_CONFIG].asString()
                        val firebaseDataTwo = remoteConfig[FirebaseConfig.KEY_FIRE_BASE_AD_CONFIG].asString()

                        MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_FIREBASE_CONFIG_SERVERS, firebaseDataOne)
                        MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_FIREBASE_CONFIG_ADS, firebaseDataTwo)

                        Timber.tag("--->").e("FirebaseData configVpn == $firebaseDataOne")
                        Timber.tag("--->").e("FirebaseData configAds == $firebaseDataTwo")

                        dataLoaded.value = true

                    } else {

//                        vpnServers.value = null
//                        adConfig.value = null

                        Timber.tag("--->").e("FirebaseData remoteConfig Fetch failed")

                        MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_FIREBASE_CONFIG_SERVERS, FirebaseConfig.vpnServers)
                        MMKV.defaultMMKV().encode(ConstantSharedPreference.SP_FIREBASE_CONFIG_ADS, FirebaseConfig.adConfig)

                        dataLoaded.value = true

                    }

                    task.addOnFailureListener {
                        Timber.tag("--->").e("FirebaseData remoteConfig Fetch exception ${it.message}" )
                        it.printStackTrace()
                    }




                }


        }

    }





}