package com.propertycrm.app.util

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    private const val MAGIC_HEADER = "PCRM_ENC_V2"
    private const val ITERATIONS = 65536
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    /**
     * Hashes a 4-digit or custom PIN with a random salt.
     */
    fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a random alphanumeric salt.
     */
    fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Encrypts plaintext JSON bytes to an output stream with AES-256-GCM.
     */
    fun encryptBackup(
        plainBytes: ByteArray,
        password: String,
        outputStream: OutputStream
    ) {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

        // Derive secret key
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        // Initialize AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        // Write Header
        outputStream.write(MAGIC_HEADER.toByteArray(Charsets.UTF_8))
        outputStream.write(salt)
        outputStream.write(iv)

        val encryptedBytes = cipher.doFinal(plainBytes)
        outputStream.write(encryptedBytes)
        outputStream.flush()
    }

    /**
     * Decrypts AES-256-GCM encrypted backup input stream back to plain JSON bytes.
     * Throws SecurityException or IllegalArgumentException on wrong password, corrupt data, or invalid magic header.
     */
    fun decryptBackup(
        inputStream: InputStream,
        password: String
    ): ByteArray {
        val headerBytes = ByteArray(MAGIC_HEADER.length)
        val readHeader = inputStream.read(headerBytes)
        if (readHeader != MAGIC_HEADER.length || String(headerBytes, Charsets.UTF_8) != MAGIC_HEADER) {
            throw IllegalArgumentException("Invalid backup file format: Missing or invalid encryption header.")
        }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        if (inputStream.read(salt) != SALT_LENGTH_BYTES) {
            throw IllegalArgumentException("Corrupted backup file: Truncated salt.")
        }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        if (inputStream.read(iv) != GCM_IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Corrupted backup file: Truncated IV.")
        }

        val encryptedData = inputStream.readBytes()
        if (encryptedData.isEmpty()) {
            throw IllegalArgumentException("Backup payload is empty.")
        }

        // Derive key with same salt
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        return try {
            cipher.doFinal(encryptedData)
        } catch (e: Exception) {
            throw SecurityException("Decryption failed. Incorrect password or backup file has been corrupted/tampered with.")
        }
    }
}
