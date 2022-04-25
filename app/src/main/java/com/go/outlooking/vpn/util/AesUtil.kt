package com.go.outlooking.vpn.util



import android.util.Base64
import timber.log.Timber
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object AesUtil {


    @Throws(Exception::class)
    fun decrypt(sSrc: String?, sKey: String?): String? {

        return try {
            // 判断Key是否正确
            if (sKey == null) {
//                print("Key为空null")
                return null
            }

            // 判断Key是否为16位
            if (sKey.length != 16) {
//                print("Key长度不是16位")
                return null
            }

            val raw = sKey.toByteArray(charset("utf-8"))
            val skeySpec = SecretKeySpec(raw, "AES")
            val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, skeySpec)
            val encrypted1: ByteArray = Base64.decode(sSrc, Base64.NO_WRAP)
            try {
                val original: ByteArray = cipher.doFinal(encrypted1)
                String(original, Charset.forName("utf-8"))
            } catch (e: Exception) {
                println(e.toString())
                null
            }
        } catch (ex: Exception) {
            println(ex.toString())
//            Timber.tag("--->").e("decrypt exception: ${ex.message}")
            null
        }
    }

}