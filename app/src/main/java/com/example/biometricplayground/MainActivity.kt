package com.example.biometricplayground

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import com.example.biometricplayground.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import javax.crypto.Cipher

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val sharedPreferences by lazy {
        getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.saveButton.setOnClickListener {
            promptCredential(Cipher.ENCRYPT_MODE) { cipher ->
                val encryptedMessage = CryptographyUtil.encryptData(
                    cipher,
                    binding.messageField.editText?.text?.toString().orEmpty()
                )
                with(sharedPreferences.edit()) {
                    putString(KEY_SECRET_MESSAGE, encryptedMessage)
                    apply()
                }
                binding.messageField.editText?.setText("")
            }
        }

        binding.loadButton.setOnClickListener {
            promptCredential(Cipher.DECRYPT_MODE) { cipher ->
                val decryptedMessage = try {
                    CryptographyUtil.decryptData(
                        cipher,
                        sharedPreferences.getString(KEY_SECRET_MESSAGE, "").orEmpty()
                    )
                } catch (e: Exception) {
                    Log.e("TAG", "", e)
                    "No saved secret message found"
                }
                MaterialAlertDialogBuilder(this)
                    .setTitle("Secret Message")
                    .setPositiveButton("Ok") { dialog, _ -> dialog.dismiss() }
                    .setMessage(decryptedMessage)
                    .show()
            }
        }

        binding.clearButton.setOnClickListener {
            CryptographyUtil.clearKey()
        }
    }

    private fun promptCredential(cipherMode: Int, onAuthenticated: (Cipher) -> Unit) {
        val biometricPrompt = BiometricPrompt(this, object :
            BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(
                    this@MainActivity,
                    "Authentication Error: $errString\nError Code: $errorCode",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                result.cryptoObject?.cipher?.let { onAuthenticated(it) }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@MainActivity, "Authentication Failed", Toast.LENGTH_SHORT)
                    .show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Prompt Title")
            .setDescription("Biometric Prompt Description")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(
            promptInfo,
            BiometricPrompt.CryptoObject(CryptographyUtil.getCipher(cipherMode))
        )
    }

    companion object {

        private const val SHARED_PREFERENCES_NAME = "shared_preferences_name"
        private const val KEY_SECRET_MESSAGE = "key_secret_message"
    }
}