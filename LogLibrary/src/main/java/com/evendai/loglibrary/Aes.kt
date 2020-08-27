package com.evendai.loglibrary

import android.annotation.SuppressLint
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/** AES加密、解密 */
object Aes {

    /** 使用AES进行加密，加密后的数据使用Base64编码为String */
    fun encrypt(rawData: String): String = Base64.encodeToString(getCipher(Cipher.ENCRYPT_MODE).doFinal(rawData.toByteArray()), Base64.NO_WRAP)

    /** 把AES加密并通过Base64编码的String进行解密，还原为原始的String */
    fun decrypt(base64Data: String): String = String(getCipher(Cipher.DECRYPT_MODE).doFinal(Base64.decode(base64Data, Base64.NO_WRAP)))

    @SuppressLint("GetInstance")
    private fun getCipher(mode: Int) = Cipher.getInstance("AES/ECB/PKCS5Padding").apply { init(mode, SecretKeySpec("abc3efgabcdef119".toByteArray(), "AES")) }

}