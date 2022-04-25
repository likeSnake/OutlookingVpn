package com.go.outlooking.vpn.adapter

import com.go.outlooking.vpn.bean.ServerBean


interface OnItemClickListener {

    fun onItemClick(serverBean: ServerBean, position: Int)


}