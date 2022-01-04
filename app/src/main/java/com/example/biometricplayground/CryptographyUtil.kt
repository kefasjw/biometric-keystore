package com.example.biometricplayground

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.Key
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val KEY_ALIAS = "biometric_playground_key"

object CryptographyUtil {

    fun encryptData(cipher: Cipher, plainData: String): String {
        val encryptedData = cipher.doFinal(plainData.toByteArray())
        return Base64.encodeToString(encryptedData, Base64.DEFAULT)
    }

    fun decryptData(cipher: Cipher, encryptedData: String): String {
        val decodedEncryptedData = Base64.decode(encryptedData, Base64.DEFAULT)
        return String(cipher.doFinal(decodedEncryptedData))
    }

    fun getCipher(cipherMode: Int): Cipher {
        return Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}")
            .apply {
                init(cipherMode, getOrCreateKey(), GCMParameterSpec(128, ByteArray(12)))
            }
    }

    fun clearKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).also {
            it.load(null)
        }

        keyStore.deleteEntry(KEY_ALIAS)
    }

    private fun getOrCreateKey(): Key {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).also {
            it.load(null)
        }

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            initializeKey()
        }

        return keyStore.getKey(KEY_ALIAS, null)
    }

    private fun initializeKey() {
        with(KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)) {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setUserAuthenticationRequired(true)
                    .setRandomizedEncryptionRequired(false) // Not secure
                    .build()
            )
            generateKey()
        }
    }
}