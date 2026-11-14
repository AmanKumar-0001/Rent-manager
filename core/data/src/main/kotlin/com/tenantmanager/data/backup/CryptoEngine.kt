package com.tenantmanager.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {

    private const val HEADER_MAGIC = "TENANTMGR_ENC_V1" // Exactly 16 bytes ASCII
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 600_000
    private const val KEY_LENGTH_BITS = 256

    private const val GENERIC_ERROR_MESSAGE = "Failed to decrypt: invalid passphrase or corrupted file"

    private val secureRandom = SecureRandom()

    fun encrypt(plaintext: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

        val keySpec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        val headerBytes = HEADER_MAGIC.toByteArray(Charsets.US_ASCII)
        val result = ByteArray(headerBytes.size + salt.size + iv.size + ciphertext.size)

        var offset = 0
        System.arraycopy(headerBytes, 0, result, offset, headerBytes.size)
        offset += headerBytes.size

        System.arraycopy(salt, 0, result, offset, salt.size)
        offset += salt.size

        System.arraycopy(iv, 0, result, offset, iv.size)
        offset += iv.size

        System.arraycopy(ciphertext, 0, result, offset, ciphertext.size)

        return result
    }

    fun decrypt(encryptedBytes: ByteArray, passphrase: CharArray): ByteArray {
        val minLength = 16 + SALT_LENGTH_BYTES + IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)
        if (encryptedBytes.size < minLength) {
            throw SecurityException(GENERIC_ERROR_MESSAGE)
        }

        val headerBytes = HEADER_MAGIC.toByteArray(Charsets.US_ASCII)
        for (i in headerBytes.indices) {
            if (encryptedBytes[i] != headerBytes[i]) {
                throw SecurityException(GENERIC_ERROR_MESSAGE)
            }
        }

        var offset = headerBytes.size
        val salt = ByteArray(SALT_LENGTH_BYTES)
        System.arraycopy(encryptedBytes, offset, salt, 0, SALT_LENGTH_BYTES)
        offset += SALT_LENGTH_BYTES

        val iv = ByteArray(IV_LENGTH_BYTES)
        System.arraycopy(encryptedBytes, offset, iv, 0, IV_LENGTH_BYTES)
        offset += IV_LENGTH_BYTES

        val ciphertextLength = encryptedBytes.size - offset
        val ciphertext = ByteArray(ciphertextLength)
        System.arraycopy(encryptedBytes, offset, ciphertext, 0, ciphertextLength)

        return try {
            val keySpec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = keyFactory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            throw SecurityException(GENERIC_ERROR_MESSAGE)
        }
    }
}

