package com.github.shadowsocks.bean

import java.io.Serializable

data class ConfigBean(var interval : Long? = -1, var clickLimit : Int? = -1,
                      var showLimit : Int? = -1, var fbId : String,
                      var closeAd : Boolean? = true, var closeInter: Boolean? = true,
                      var refreshTime: Long? = 30, var closeVersions : ArrayList<String>) : Serializable {
}