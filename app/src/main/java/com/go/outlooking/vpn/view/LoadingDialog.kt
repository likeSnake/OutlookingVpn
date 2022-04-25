package com.go.outlooking.vpn.view

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialog
import com.go.outlooking.vpn.R


class LoadingDialog(context: Context?) : AppCompatDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.dialog_loading)

        setCancelable(false)
    }

}