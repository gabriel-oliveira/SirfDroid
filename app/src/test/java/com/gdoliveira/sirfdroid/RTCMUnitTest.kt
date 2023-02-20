package com.gdoliveira.sirfdroid

import org.junit.Assert
import org.junit.Test
import java.util.BitSet
import kotlin.experimental.or
import kotlin.math.floor

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

    @Test
    fun rtcm1002_isCorret() {

        val c = "d300123ea0000bb3ba40104078405087a6a0140000612061"

        val GPS_STime = 49082.00777058164
        val PRN = 16
        val PD = 2.6391766051384903E7
        val Cfase = 2.639190495792381E7
        val epoch = Epoch(0)
        epoch.arrayMID28 = arrayOf<MID28>(MID28(0,0,PRN,GPS_STime,PD,0,Cfase))
        epoch.MID2 = MID2(0,0,0,0,GPS_STime,1, listOf(PRN))
        epoch.MID7 = MID7(0,GPS_STime,1,0,((GPS_STime - (floor(GPS_STime))) * 1E9).toLong(),(floor(GPS_STime) * 1000).toLong())

        val m = msg1002encode(epoch).toHexString().lowercase()
//        print("msg = RTCMReader.parse(b\"")
//        m.forEach { print("\\x%02x".format(it)) }
//        println("\")")
        Assert.assertEquals(c, m)
    }

    @Test
    fun testToBooleanToBytearray() {
        val a = 0b1100_0000
        val b = 0b0000_0000
        val ba = ByteArray(2)
        ba[0] = a.toByte()
        ba[1] = b.toByte()
        val b_size = ba.size
        ba[b_size - 1] = ba[b_size - 1] or (a shr 6).toByte()
//        println(ba.toHexString())
//        ba.toBooleanArray().forEach {
//            if(it){
//                print("1")
//            } else {
//                print("0")
//            }
//        }
//        println(" ")
//        println(ba.toBooleanArray().toByteArray().toHexString())
//        val t = (1 shl 4).toByte()
//        println("%02x".format(t))
        Assert.assertEquals(ba.toHexString(), ba.toBooleanArray().toByteArray().toHexString())
    }

    @Test
    fun testAddBooleanArray() {
        val a = 0b1100_0000
        val b = 0b0000_0011
        var ba = BooleanArray(16)
        ba.toAdd(0,8, byteArrayOf(a.toByte()))
        ba.toAdd(8,8, byteArrayOf(b.toByte()))
        Assert.assertEquals("c003", ba.toByteArray().toHexString())
    }

    @Test
    fun testHalfByte() {
        val ba = byteArrayOf(0b1000_0100.toByte(),0b0011_0000.toByte())
        val bs = BooleanArray(12)
        bs[0] = true
        bs[5] = true
        bs[10] = true
        bs[11] = true

        Assert.assertEquals(ba.toHexString(), bs.toByteArray().toHexString())
    }
}