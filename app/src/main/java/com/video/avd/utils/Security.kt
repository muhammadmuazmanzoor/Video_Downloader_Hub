package com.video.avd.utils

import android.text.TextUtils
import java.io.IOException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

class Security {
    private val KEY_FACTORY_ALGORITHM="RSA"
    private val SIGNATURE_ALGORITHM="SHA1withRSA"
    @Throws(IOException::class)
    fun verifyPurchase(LICENSE_KEY:String?,signedData:String,
                       signature:String?):Boolean{
        if(TextUtils.isEmpty(signedData)|| TextUtils.isEmpty(LICENSE_KEY) || TextUtils.isEmpty(signature)){

            return false
        }
        val key = generatedPublicKey(LICENSE_KEY)
        return verify(key,signedData,signature)
    }

    private fun verify(publicKey: PublicKey, signedData: String, signature: String?) : Boolean{
        val signatureBytes:ByteArray=try {
            android.util.Base64.decode(signature,android.util.Base64.DEFAULT)
        }
        catch (e:java.lang.IllegalArgumentException){
            return false
        }
        try {
            val signatureAlgorithm=Signature.getInstance(SIGNATURE_ALGORITHM)
            signatureAlgorithm.initVerify(publicKey)
            signatureAlgorithm.update(signedData.toByteArray())
            return signatureAlgorithm.verify(signatureBytes)
        }
        catch (e:java.lang.Exception){

        }
        return false
    }
    @Throws(IOException::class)
    fun generatedPublicKey(encodedPublicKey: String?):PublicKey {
        return try {
            val decodeKey=android.util.Base64.decode(encodedPublicKey,android.util.Base64.DEFAULT)
            val keyFactory= KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            keyFactory.generatePublic(X509EncodedKeySpec(decodeKey))
        }
        catch (e: NoSuchAlgorithmException){
            throw java.lang.RuntimeException(e)
        }
        catch (e:InvalidKeySpecException){
            val msg="Invalid key Specification: $e"
            throw IOException(msg)
        }
    }
}