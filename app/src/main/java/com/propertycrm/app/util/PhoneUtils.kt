package com.propertycrm.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object PhoneUtils {

    /**
     * Normalizes phone string by stripping whitespace, dashes, and parentheses.
     */
    fun normalize(phone: String?): String {
        if (phone == null) return ""
        return phone.replace("[\\s\\-\\(\\)]".toRegex(), "").trim()
    }

    /**
     * Validates if string looks like a legitimate phone number.
     */
    fun isValidPhone(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        val normalized = normalize(phone)
        val regex = "^[+]?[0-9]{7,15}$".toRegex()
        return regex.matches(normalized)
    }

    /**
     * Checks if two phone numbers match.
     */
    fun areMatching(phone1: String?, phone2: String?): Boolean {
        val n1 = normalize(phone1)
        val n2 = normalize(phone2)
        if (n1.isEmpty() || n2.isEmpty()) return false
        // Match exact or match last 10 digits (ignoring country code prefixes like +91 or 0)
        if (n1 == n2) return true
        val p1Sub = if (n1.length > 10) n1.takeLast(10) else n1
        val p2Sub = if (n2.length > 10) n2.takeLast(10) else n2
        return p1Sub == p2Sub
    }

    /**
     * Launches the phone dialer using ACTION_DIAL.
     * Does NOT initiate calls automatically (respects user safety & prompt requirement).
     */
    fun dialCustomer(context: Context, phoneNumber: String) {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isBlank()) {
            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(cleanNumber)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
