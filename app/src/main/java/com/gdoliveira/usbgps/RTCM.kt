package com.gdoliveira.usbgps
// Handle with RTCM 3.2

import android.util.Log
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.ceil
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

fun msg1002encode(msgs28: List<MID28>): ByteArray {

    val numSats = msgs28.size

    val bytesSize = 8 + ceil(numSats * 9.25).toInt()
    var msg = ByteArray(bytesSize)

    val bitSize = 8 * ceil(numSats * 9.25).toInt()
    var bmsg = BooleanArray(bitSize)

    var satHeaderMsg = ByteArray(0)

    //HEADER
    //Message Number (e.g.,“1002”= 0011 1110 1010) DF002 uint12 12
    //Reference Station ID DF003 uint12 12
    satHeaderMsg += byteArrayOf(0b00111110.toByte(), 0b10100000.toByte(),0b00000000.toByte())

    //GPS Epoch Time (TOW) DF004 uint30 30 // millisecond
    val tow = msgs28[0].tow
    val towMS = tow * 1000
    satHeaderMsg += ByteBuffer.allocate(8).putLong(towMS shl 2).array().copyOfRange(4,8) //32bits

    //Synchronous GNSS Flag DF005 bit(1) 1
    //No. of GPS Satellite Signals Processed DF006 uint5 5 //Number of satellites in the message = 1
    //GPS Divergence-free Smoothing Indicator (0) DF007 bit(1) 1
    //GPS Smoothing Interval (000) DF008 bit(3) 3
    satHeaderMsg += byteArrayOf((numSats shl 4).toByte())

    //TOTAL 64

    var sat = 0
    for (mid28 in msgs28) {

        var satmsg = BooleanArray(74)

        //GPS Satellite ID DF009 uint6 6
        satmsg.toAdd( 0, 6,
            ByteBuffer.allocate(8).putLong(mid28.PRN shl 2).array().copyOfRange(7,8) )

        //GPS L1 Code Indicator DF010 (0 - CA) bit(1) 1
        satmsg.toAdd(6,1,BooleanArray(1))

        //GPS L1 Pseudorange DF011 uint24 24
        val dtr = mid28.GPS_STime - tow
        val C1 = mid28.PD - (c * dtr)
        val PD_RTCM = round( (C1 % 299792.458) / 0.02 ).toLong()
        satmsg.toAdd( 7, 24,
            ByteBuffer.allocate(8).putLong(PD_RTCM).array().copyOfRange(5,8) )

        //GPS L1 PhaseRange – L1 Pseudorange DF012 int20 20
        val dif = mid28.PD - mid28.Cfase
        val dif_RTCM = round(dif/0.0005).toLong()
        satmsg.toAdd( 31, 20,
            ByteBuffer.allocate(8).putLong(dif_RTCM).array().copyOfRange(5,8).toBooleanArray().copyOfRange(4,24) )

        //GPS L1 Lock time Indicator DF013 uint7 7
        satmsg.toAdd(51,7,BooleanArray(7))

        //GPS Integer L1 Pseudorange Modulus Ambiguity DF014 uint8 8
        val amb_RTCM = floor( (C1 / 299792.458) ).toLong()
        satmsg.toAdd(58, 8,
            ByteBuffer.allocate(8).putLong(amb_RTCM).array().copyOfRange(7,8) )

        //GPS L1 CNR DF015 uint8 8
        satmsg.toAdd(66, 8, BooleanArray(8) )

        //TOTAL 74

        bmsg.toAdd(sat*74, 74, satmsg)
        sat += 1
    }

    msg = satHeaderMsg + bmsg.toByteArray()
    val payload = header(msg) + msg

    return payload + CRC24.crc24gen(payload)
}

fun header(msg: ByteArray): ByteArray {

    var header = ByteArray(0)

    //Preamble 8 bits 11010011
    header += 0b11010011.toByte()

    //Reserved 6 bits 000000
    header += 0b00000000.toByte()

    //Message Length 10 bits Message length in bytes
    header += msg.size.toByte()

    return header

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
            //len = 18
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


fun coordEncode(coord: Double): ByteArray {

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


fun ByteArray.toBooleanArray(): BooleanArray {
    val s = size * 8
    var i = 0
    var l = BooleanArray(s)
    forEach {
        if ( ((it.toInt() shr 7) and 1) == 1) l[i] = true
        if ( ((it.toInt() shr 6) and 1) == 1) l[i+1] = true
        if ( ((it.toInt() shr 5) and 1) == 1) l[i+2] = true
        if ( ((it.toInt() shr 4) and 1) == 1) l[i+3] = true
        if ( ((it.toInt() shr 3) and 1) == 1) l[i+4] = true
        if ( ((it.toInt() shr 2) and 1) == 1) l[i+5] = true
        if ( ((it.toInt() shr 1) and 1) == 1) l[i+6] = true
        if ( (it.toInt() and 1) == 1) l[i+7] = true
        i += 8
    }
    return l
}

fun BooleanArray.toByteArray(): ByteArray {
    val s = ceil(size.toDouble() / 8.0).toInt()
    var i = 0
    var ba = ByteArray(s)

    toList().chunked(8).map {
        var bs = BitSet(8)
        var e = 7
        for (b in it) {
            if(b) { bs[e] = true }
            e-=1
        }
        if (!bs.isEmpty) { ba[i] = bs.toByteArray()[0] }
        i+=1
    }
    return ba
}

fun BooleanArray.toAdd(startIndex: Int, length: Int, ba: BooleanArray): BooleanArray {
    val endIndex = startIndex + length - 1
    var e = 0
    for (i in startIndex..endIndex){
        this[i] = ba[e]
        e += 1
    }
    return this
}

fun BooleanArray.toAdd(index: Int, length: Int, ba: ByteArray): BooleanArray {
    return this.toAdd(index, length, ba.toBooleanArray())
}