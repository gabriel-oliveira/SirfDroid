package com.gdoliveira.usbgps
// Handle with RTCM 3.2

import android.util.Log
import java.nio.ByteBuffer
import kotlin.math.abs

const val c = 299792458

fun msg1006encode(X: Double, Y: Double, Z: Double): ByteArray{

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
    msg += coordEncode(X)

    //Single Receiver Oscillator Indicator (0) DF142 bit(1) 1
    //Reserved DF001 (0) bit(1) 1
    //Antenna Reference Point ECEF-Y DF026 int38 38
    msg += coordEncode(Y)

    //Quarter Cycle Indicator (00) DF364 bit(2) 2
    //Antenna Reference Point ECEF-Z DF027 int38 38
    msg += coordEncode(Z)

    //Antenna Height DF028 uint16 16
    msg += byteArrayOf(0b00000000.toByte(), 0b00000000.toByte())

//    Log.d("RTCM MSG 1006","Message Size: ${msg.size}") // 21
    //TOTAL 168

    return header(1006) + msg + CRC24.crc24gen(header(1006) + msg)
}

fun msg1005encode(X: Double, Y: Double, Z: Double): ByteArray{

    var msg = ByteArray(0)

    //Message Number (“1005”= 0011 1110 1101) DF002 uint12 12
    //Reference Station ID (2003 = 0111 1101 0011) DF003 uint12 12
    msg += byteArrayOf(0b00111110.toByte(), 0b11010111.toByte(),0b11010011.toByte())

    //Reserved for ITRF Realization Year (0000 00) DF021 uint6 6
    //GPS Indicator (1) DF022 bit(1) 1
    //GLONASS Indicator (0) DF023 bit(1) 1
    msg += byteArrayOf(0b00000010.toByte())

    //Reserved for Galileo Indicator (0) DF024 bit(1) 1
    //Reference-Station Indicator (0) DF141 bit(1) 1
    //Antenna Reference Point ECEF-X DF025 int38 38
    msg += coordEncode(X)

    //Single Receiver Oscillator Indicator (0) DF142 bit(1) 1
    //Reserved DF001 (0) bit(1) 1
    //Antenna Reference Point ECEF-Y DF026 int38 38
    msg += coordEncode(Y)

    //Quarter Cycle Indicator (00) DF364 bit(2) 2
    //Antenna Reference Point ECEF-Z DF027 int38 38
    msg += coordEncode(Z)

//    Log.d("RTCM MSG 1005","Message Size: ${msg.size}")

    return header(1005) + msg + CRC24.crc24gen(header(1005) + msg)
}

fun msg1001encode(GPS_STime: Double, PRN: Long, PD: Double, Cfase: Double): ByteArray {

    var msg = ByteArray(0)

    //Message Number (e.g.,“1001”= 0011 1110 1001) DF002 uint12 12
    //Reference Station ID DF003 uint12 12
    msg += byteArrayOf(0b00111110.toByte(), 0b10010000.toByte(),0b00000000.toByte())
//    GPS Epoch Time (TOW) DF004 uint30 30 // TODO: millisecond
    val tow = abs(GPS_STime).toLong()
    msg += ByteBuffer.allocate(8).putLong(tow shl 2).array().copyOfRange(4,8) //32bits
//    Synchronous GNSS Flag DF005 bit(1) 1 // TODO: 0 - Data can be processed; 1 - Next message will contain observables of the same tow
//    No. of GPS Satellite Signals Processed DF006 uint5 5 //Number of satellites in the message = 1
//    GPS Divergence-free Smoothing Indicator (0) DF007 bit(1) 1
//    GPS Smoothing Interval (000) DF008 bit(3) 3
    msg += byteArrayOf(0b00010000.toByte())
//    TOTAL 64
//    GPS Satellite ID DF009 uint6 6
      msg += ByteBuffer.allocate(8).putLong(PRN shl 2).array().copyOfRange(7,8)
//    GPS L1 Code Indicator DF010 (0 - CA) bit(1) 1
//    GPS L1 Pseudorange DF011 uint24 24
    //dtr = GPS_STime - tow
    //C1 = PD - (c * dtr * 1.0e-9);
//    GPS L1 PhaseRange – L1 Pseudorange DF012 int20 20
    //dif = (PD - Cfase) - (c * dtr * 1.0e-9)
//    GPS L1 Lock time Indicator DF013 uint7 7

//    Log.d("RTCM MSG 1001","Message Size: ${msg.size}") // 15 -> 16 but add zeros in the end
//    TOTAL 58

    return header(1001) + msg + CRC24.crc24gen(header(1001) + msg)
}

fun header(msgId: Int): ByteArray {

    var header = ByteArray(0)
    header += 0b11010011.toByte()  //Preamble 8 bits 11010011
    //Reserved 6 bits 000000
    header += 0b00000000.toByte()
    //Message Length 10 bits Message length in bytes
    when (msgId) {
        1005 -> {
            //len = 19
            header += 0b00010011.toByte()
        }
        1006 -> {
            //len = 21
            header += 0b00010101.toByte()
        }
        1001 -> {
            //    val len = ByteBuffer.allocate(8)
            //    len.putInt(msg.size * 8) // ???
            //    header += len.array()

            //len = 15
            header += 0b00001111.toByte()
        }
    }
    return header
}


fun coordEncode(coord: Double): ByteArray { //coord: Double

    var byteCoord = ByteBuffer.allocate(8)
    var longCoord = (coord * 10000).toLong()

    if (longCoord < 0) {
        longCoord += 274877906943 // 0x3F_FFFF_FFFF  38bits
    }
    byteCoord.putLong(longCoord)
    val byteArrayCoord = byteCoord.array().copyOfRange(3,8)

    //val debugCoord = byteArrayCoord.toHexString()

//    Log.i("RTCM Coord Encode","Coord: $coord ByteArray: $byteCoord")
    return byteArrayCoord
}