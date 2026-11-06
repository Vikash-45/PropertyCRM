package com.propertycrm.app

import com.propertycrm.app.util.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SecurityUtilsTest {

    @Test
    fun testPinHashing() {
        val pin = "1234"
        val salt1 = SecurityUtils.generateSalt()
        val salt2 = SecurityUtils.generateSalt()

        val hash1 = SecurityUtils.hashPin(pin, salt1)
        val hash2 = SecurityUtils.hashPin(pin, salt2)
        val hash1Repeat = SecurityUtils.hashPin(pin, salt1)

        // Different salts produce different hashes
        assertNotEquals(hash1, hash2)
        // Same salt produces identical hash
        assertEquals(hash1, hash1Repeat)
        // Wrong pin produces different hash
        assertNotEquals(hash1, SecurityUtils.hashPin("4321", salt1))
    }

    @Test
    fun testEncryptionAndDecryptionWithCorrectPassword() {
        val originalPayload = "{\"customers\": [{\"name\": \"Vikash\", \"phone\": \"9876543210\"}]}"
        val password = "SuperSecretPassword123"

        val outputStream = ByteArrayOutputStream()
        SecurityUtils.encryptBackup(originalPayload.toByteArray(Charsets.UTF_8), password, outputStream)

        val encryptedBytes = outputStream.toByteArray()
        assertTrue("Encrypted output should not be empty", encryptedBytes.isNotEmpty())
        // Encrypted bytes should start with magic header
        val header = String(encryptedBytes.sliceArray(0 until 11), Charsets.UTF_8)
        assertEquals("PCRM_ENC_V2", header)

        // Decrypt with correct password
        val inputStream = ByteArrayInputStream(encryptedBytes)
        val decryptedBytes = SecurityUtils.decryptBackup(inputStream, password)
        val decryptedString = String(decryptedBytes, Charsets.UTF_8)

        assertEquals(originalPayload, decryptedString)
    }

    @Test
    fun testDecryptionFailsWithWrongPassword() {
        val originalPayload = "Confidential CRM Records"
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword456"

        val outputStream = ByteArrayOutputStream()
        SecurityUtils.encryptBackup(originalPayload.toByteArray(Charsets.UTF_8), correctPassword, outputStream)

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        try {
            SecurityUtils.decryptBackup(inputStream, wrongPassword)
            fail("Decryption should have thrown SecurityException on wrong password")
        } catch (e: SecurityException) {
            assertTrue("Expected SecurityException on wrong password", e.message?.contains("Decryption failed") == true)
        }
    }

    @Test
    fun testDecryptionFailsOnCorruptedPayload() {
        val originalPayload = "Confidential CRM Records"
        val password = "CorrectPassword123"

        val outputStream = ByteArrayOutputStream()
        SecurityUtils.encryptBackup(originalPayload.toByteArray(Charsets.UTF_8), password, outputStream)

        val bytes = outputStream.toByteArray()
        // Corrupt ciphertext byte
        bytes[bytes.size - 5] = (bytes[bytes.size - 5] + 1).toByte()

        val inputStream = ByteArrayInputStream(bytes)
        try {
            SecurityUtils.decryptBackup(inputStream, password)
            fail("Decryption should have thrown exception on tampered ciphertext")
        } catch (e: SecurityException) {
            // Expected
        }
    }
}
