package com.gdoliveira.usbgps

import org.junit.Assert
import org.junit.Test

class RTCMUnitTest {
    @Test
    fun rtcm1005_isCorret() {

        val c = "D300133ED7D30202980EDEEF34B4BD62AC0941986F33360B98"

        val x = 1114104.5999
        val y = -4850729.7108
        val z = 3975521.4643

        val m = msg1005encode(x, y, z).toHexString().uppercase()

        Assert.assertEquals(c, m)
    }
}