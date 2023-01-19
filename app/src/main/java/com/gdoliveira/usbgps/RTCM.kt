package com.gdoliveira.usbgps
// Handle with RTCM 3.2

import android.util.Log
import java.nio.ByteBuffer

fun msg1006encode(X: Long, Y: Long, Z: Long): ByteArray{ //X Y Z Double ???

    var msg = ByteArray(0)

    //Message Number (“1006”= 0011 1110 1110) DF002 uint12 12
    //Reference Station ID (0000 0000 0000) DF003 uint12 12
    msg += byteArrayOf(0b00111110.toByte(), 0b11100000.toByte(),0b00000000.toByte())

    //Reserved for ITRF Realization Year (0000 00) DF021 uint6 6
    //GPS Indicator (1) DF022 bit(1) 1
    //GLONASS Indicator (0) DF023 bit(1) 1
    msg += byteArrayOf(0b00000010.toByte())

    //Reserved for Galileo Indicator (0) DF024 bit(1) 1
    //Reference-Station Indicator (0) DF141 bit(1) 1
    //Antenna Reference Point ECEF-X DF025 int38 38
    coordEncode(X)

    //Single Receiver Oscillator Indicator (0) DF142 bit(1) 1
    //Reserved DF001 (0) bit(1) 1
    //Antenna Reference Point ECEF-Y DF026 int38 38
    coordEncode(Y)

    //Quarter Cycle Indicator (00) DF364 bit(2) 2
    //Antenna Reference Point ECEF-Z DF027 int38 38
    coordEncode(Z)

    //Antenna Height DF028 uint16 16
    msg += byteArrayOf(0b00000000.toByte(), 0b00000000.toByte())

    Log.d("RTCM MSG 1006","Message Size: ${msg.size}")
    //TOTAL 168

//Teste
//    var b = ByteArray(0)
//    val c = "D300133ED7D30202980EDEEF34B4BD62AC0941986F33360B98"
//    c.forEach{
//        b += it.toString().toInt(16).toByte()
//    }
//    return c.decodeHex()

    return header(msg) + msg + CRC24Q(msg)
}

fun msg1001encode(SirfMID28: List<Any>, SirfMID7: List<Any>?): ByteArray {

    var msg = ByteArray(0)

//    Message Number (e.g.,“1001”= 0011 1110 1001) DF002 uint12 12
//    Reference Station ID DF003 uint12 12
//    GPS Epoch Time (TOW) DF004 uint30 30
//    Synchronous GNSS Flag DF005 bit(1) 1
//    No. of GPS Satellite Signals Processed DF006 uint5 5
//    GPS Divergence-free Smoothing Indicator DF007 bit(1) 1
//    GPS Smoothing Interval DF008 bit(3) 3
//    TOTAL 64
//    GPS Satellite ID DF009 uint6 6
//    GPS L1 Code Indicator DF010 bit(1) 1
//    GPS L1 Pseudorange DF011 uint24 24
//    GPS L1 PhaseRange – L1 Pseudorange DF012 int20 20
//    GPS L1 Lock time Indicator DF013 uint7 7
//    TOTAL 58

    return header(msg) + msg + CRC24Q(msg)
}

fun header(msg: ByteArray): ByteArray {

    var header = ByteArray(0)
    header += "11010011".toLong(radix = 2).toByte()  //Preamble 8 bits 11010011
    //Reserved 6 bits 000000
    //Message Length 10 bits Message length in bytes
    return header
}

fun CRC24Q(msg: ByteArray): ByteArray {
    //CRC  24 bits
    var crc = ByteArray(0)
    return crc
}

fun addZerosLeft(msg: ByteArray, size: Int): ByteArray {
    return msg
}

fun coordEncode(coord: Long): ByteArray { //coord: Double

    var buf = ByteArray(2)
    var byteCoord = ByteBuffer.allocate(38)
    var longCoord = (coord * 10000)//.toLong()

    if (longCoord < 0) {
        longCoord = longCoord + 274877906943 + 1
    }

    byteCoord.putLong(longCoord)

    Log.i("RTCM Coord Encode","Coord: $coord ByteArray: $byteCoord")
    return buf + byteCoord.array()
}