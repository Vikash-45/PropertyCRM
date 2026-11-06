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

    /**
     * Opens WhatsApp chat directly with customer using wa.me deep-link.
     */
    fun openWhatsApp(context: Context, phoneNumber: String, message: String = "") {
        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        if (cleanNumber.isBlank()) {
            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intlNumber = if (cleanNumber.startsWith("+")) cleanNumber.substring(1) else cleanNumber
            val url = if (message.isNotBlank()) {
                "https://wa.me/$intlNumber?text=${Uri.encode(message)}"
            } else {
                "https://wa.me/$intlNumber"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launches SMS client with customer phone prefilled.
     */
    fun sendSms(context: Context, phoneNumber: String, body: String = "") {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isBlank()) {
            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(cleanNumber)}")
                if (body.isNotBlank()) {
                    putExtra("sms_body", body)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open SMS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
