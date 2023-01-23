package com.gdoliveira.usbgps
// Handle with RTCM 3.2

import android.util.Log
import java.nio.ByteBuffer
import kotlin.math.floor
import kotlin.math.round

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

fun msg1002encode(GPS_STime: Double, PRN: Long, PD: Double, Cfase: Double): ByteArray {

    var msg = ByteArray(0)

    //Message Number (e.g.,“1002”= 0011 1110 1010) DF002 uint12 12
    //Reference Station ID DF003 uint12 12
    msg += byteArrayOf(0b00111110.toByte(), 0b10100000.toByte(),0b00000000.toByte())
//    GPS Epoch Time (TOW) DF004 uint30 30 // millisecond
    val tow = floor(GPS_STime).toLong()
    val towMS = tow * 1000
    msg += ByteBuffer.allocate(8).putLong(towMS shl 2).array().copyOfRange(4,8) //32bits
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
    val dtr = GPS_STime - tow
    val C1 = PD - (c * dtr)
    val PD_RTCM = round( (C1 % 299792.458) / 0.02 ).toLong()
    val dif = PD - Cfase

    if (dif < 0.0){
        msg += ByteBuffer.allocate(8).putLong( (PD_RTCM shl 1) or 1 ).array().copyOfRange(5,8)
    } else {
        msg += ByteBuffer.allocate(8).putLong(PD_RTCM shl 1).array().copyOfRange(5,8)
    }

//    GPS L1 PhaseRange – L1 Pseudorange DF012 int20 20
//    GPS L1 Lock time Indicator DF013 uint7 7
    //dif = (PD - Cfase) - (c * dtr * 1.0e-9)
    val dif_RTCM = round(dif/0.0005).toLong()
    msg += ByteBuffer.allocate(8).putLong(dif_RTCM shl 5).array().copyOfRange(5,8) // 24 bits

//    GPS Integer L1 Pseudorange Modulus Ambiguity DF014 uint8 8
//    GPS L1 CNR DF015 uint8 8
    val amb_RTCM = floor( (C1 / 299792.458) ).toLong()
    msg += ByteBuffer.allocate(8).putLong(amb_RTCM shl 6).array().copyOfRange(6,8) // 16 bits
    msg += byteArrayOf(0b00000000.toByte()) // + 4bits to complete msg

//    val msgSize = msg.size
//    Log.d("RTCM MSG 1002","Message Size: ${msg.size}") // 17,25 -> 18 but add zeros in the end
//    TOTAL 74
//    Log.d("RTCM MSG 1002","TOW:$towMS PRN:$PRN PD:$PD_RTCM DIF:$dif_RTCM AMB:$amb_RTCM")

    return header(1002) + msg + CRC24.crc24gen(header(1002) + msg)
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
        1002 -> {
            //len = 15
            header += 0b00010010.toByte()
        }
        1001 -> {
            //    val len = ByteBuffer.allocate(8)
            //    len.putInt(msg.size * 8) // ???
            //    header += len.array()

            //len = 15
            header += 0b00001111.toByte()
        }
        else -> {
            Log.e("RTCM Server","Error in RTCM Message build! ID $msgId not implemented.")
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