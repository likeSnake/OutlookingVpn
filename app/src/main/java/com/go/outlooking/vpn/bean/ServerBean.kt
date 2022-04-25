package com.go.outlooking.vpn.bean




import java.io.Serializable

//import kotlinx.android.parcel.Parcelize


data class ServerBean(var id: Int?, var countryCode: String?,
                      var name: String?, var shareCode: String?,
                      var icon: String?, var recommend: Boolean? = false,
                      var selected : Boolean = false, var type: Int = 0) : Serializable {



}