package com.propertycrm.app

import com.propertycrm.app.util.PhoneUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneUtilsTest {

    @Test
    fun testNormalize() {
        assertEquals("9876543210", PhoneUtils.normalize(" 9876 543 210 "))
        assertEquals("9876543210", PhoneUtils.normalize("(987)-654-3210"))
        assertEquals("+919876543210", PhoneUtils.normalize("+91 98765-43210"))
    }

    @Test
    fun testIsValidPhone() {
        assertTrue(PhoneUtils.isValidPhone("9876543210"))
        assertTrue(PhoneUtils.isValidPhone("+919876543210"))
        assertTrue(PhoneUtils.isValidPhone("0112345678"))

        assertFalse(PhoneUtils.isValidPhone(""))
        assertFalse(PhoneUtils.isValidPhone("123")) // Too short
        assertFalse(PhoneUtils.isValidPhone("abcdefghij")) // Not numeric
    }

    @Test
    fun testAreMatching() {
        assertTrue(PhoneUtils.areMatching("9876543210", "+919876543210"))
        assertTrue(PhoneUtils.areMatching("09876543210", "9876543210"))
        assertTrue(PhoneUtils.areMatching("(987) 654-3210", "9876543210"))

        assertFalse(PhoneUtils.areMatching("9876543210", "1234567890"))
    }
}
