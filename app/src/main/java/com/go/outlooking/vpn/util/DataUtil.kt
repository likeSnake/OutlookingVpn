package com.go.outlooking.vpn.util


import com.github.shadowsocks.bean.ConfigBean
import com.go.outlooking.vpn.bean.ServerBean
import com.go.outlooking.vpn.constant.FirebaseConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DataUtil {


    fun getConfigData(configAds: String): ConfigBean? {


        val configData = AesUtil.decrypt(configAds, FirebaseConfig.secretKey)

        return Gson().fromJson(configData, ConfigBean::class.java)
    }


    fun getServerList(configServer: String): MutableList<ServerBean> {


        val serverParam = AesUtil.decrypt(configServer, FirebaseConfig.secretKey)

        return Gson().fromJson(
            serverParam,
            object : TypeToken<MutableList<ServerBean>>() {}.type
        ) as MutableList<ServerBean>


    }









}