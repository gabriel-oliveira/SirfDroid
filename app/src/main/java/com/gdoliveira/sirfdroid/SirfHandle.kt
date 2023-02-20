package com.gdoliveira.sirfdroid

import android.util.Log
import java.lang.Double
import kotlin.math.floor
import kotlin.math.round

open class SirfHandle() {
    var msgRec: String? = null
//    var byteArrayReceived: ByteArray? = null
//    var msgs28: Array<MID28> = arrayOf()
    var epoch = Epoch(0)
    var lastEpoch = Epoch(0)
    var samplingRate = 1 //seconds

    fun msgReceived(rec: String): List<String> {

        var msgList = arrayListOf<String>()
//        var data_msg7: List<Any>? = null
        msgRec += rec

        while (true)
        {
            var indexes = msgRec.indexesOf("b0b3a0a2")

            if( indexes.size > 1 ){

                var startIndexMsg = indexes[0]
                var endIndexMsg = indexes[1]

                val newMsg = msgRec!!.substring(startIndexMsg + 4,endIndexMsg + 4)

//                Log.i("Sirf Msg", newMsg)

                if ( checkSirfMsg(newMsg) ) {

                    val msgid = msgRec!!.substring(startIndexMsg + 12, startIndexMsg + 14)
                    msgList.add( msgid )

                    when (msgid){
                        "02" -> {
                            val data_msg2 = msg2_parser(newMsg)
                            epoch.MID2 = data_msg2
                        }
                        "07" -> {

                            val data_msg7 = msg7_parser(newMsg)
                            epoch.MID7 = data_msg7
                            if (epoch.MID2 != null && epoch.arrayMID28.isNotEmpty()) {

                                val argsTextView: List<Any> = listOf<Any>(epoch.MID2!!.X, epoch.MID2!!.Y, epoch.MID2!!.Z,
                                    epoch.MID7!!.week, epoch.MID7!!.tow, epoch.MID2!!.listPRN)
                                updateCoordTextView(argsTextView)

                                if (epoch.MID2!!.SVinFix > 0){

                                    if ( data_msg7.tow.toInt() > epoch.oldTow){

                                        sendRTCMdata(msg1002encode(epoch))

                                        sendRTCMdata( msg1006encode( epoch.MID2!!.X.toDouble(),
                                            epoch.MID2!!.Y.toDouble(), epoch.MID2!!.Z.toDouble() ) )

                                        epoch2rinex(epoch)

                                        //reinitialize epoch
                                        lastEpoch = epoch
                                        epoch = Epoch(epoch.MID7!!.tow.toInt() + samplingRate - 1)

                                    } else {

                                        Log.i("Check Epoch","tow=${data_msg7.tow} oldTow=${epoch.oldTow}")

                                        //reinitialize epoch
                                        epoch = Epoch(epoch.oldTow)
                                    }
                                }
                            }
                        }
                        "1c" -> {
                            val data_msg28 = msg28_parser(newMsg)
                            epoch.arrayMID28 += data_msg28
                        }

                    }

                } else {
//                    Log.e("Check Sirf MSG", "$newMsg")
                }
                msgRec = msgRec!!.drop(endIndexMsg)

            } else {
                break
            }
        }

        return msgList

    }

    fun checkSirfMsg(msg: String): Boolean {
        val payloadLength = msg.substring(6,8).toInt(16) * 2 + 16   // 2x because hex +16 because start-end msg
        return if (payloadLength == msg.length) {
            true
        } else {
//            Log.e("Check Sirf MSG", "Payload: $payloadLength - Msg length: ${msg.length}")
            false
        }
    }

    fun msg2_parser(SirfMsg: String): MID2{
        val X = hex2dec4S(SirfMsg.substring(10,18))
        val Y = hex2dec4S(SirfMsg.substring(18,26))
        val Z = hex2dec4S(SirfMsg.substring(26,34))
        val week = SirfMsg.substring(52,56).toLong(radix = 16)
        val tow = SirfMsg.substring(57, 64).toLong(radix = 16) / 100.0
        val SVinFix = SirfMsg.substring(64,66).toInt(radix = 16)
        val CH1 = SirfMsg.substring(66,68).toInt(radix = 16)
        val CH2 = SirfMsg.substring(68,70).toInt(radix = 16)
        val CH3 = SirfMsg.substring(70,72).toInt(radix = 16)
        val CH4 = SirfMsg.substring(72,74).toInt(radix = 16)
        val CH5 = SirfMsg.substring(74,76).toInt(radix = 16)
        val CH6 = SirfMsg.substring(76,78).toInt(radix = 16)
        val CH7 = SirfMsg.substring(78,80).toInt(radix = 16)
        val CH8 = SirfMsg.substring(80,82).toInt(radix = 16)
        val CH9 = SirfMsg.substring(82,84).toInt(radix = 16)
        val CH10 = SirfMsg.substring(84,86).toInt(radix = 16)
        val CH11= SirfMsg.substring(86,88).toInt(radix = 16)
        val CH12 = SirfMsg.substring(88,90).toInt(radix = 16)

        val listPRN = listOf<Int>(CH1, CH2, CH3, CH4, CH5, CH6, CH7, CH8, CH9, CH10, CH11, CH12)

//        Log.i("Sirf MID 2",SirfMsg)
//        Log.i("Sirf MID 2","X=$X Y=$Y Z=$Z week=$week tow=$tow SVinFix=$SVinFix " +
//        "CH1=$CH1 CH2=$CH2 CH3=$CH3 CH4=$CH4 CH5=$CH5 CH6=$CH6 CH7=$CH7 CH8=$CH8 CH9=$CH9 CH10=$CH10 CH11=$CH11 CH12=$CH12")

        return MID2(X, Y, Z, week, tow, SVinFix, listPRN)
    }

    fun msg28_parser(SirfMsg: String): MID28{
        val Channel = SirfMsg.substring(10,12).toLong(radix = 16)
        val TimeTag = SirfMsg.substring(12,20).toLong(radix = 16)
        val PRN = SirfMsg.substring(20,22).toInt(radix = 16)
        val GPS_STime = Double.longBitsToDouble( parseUnsignedHex( invSirfDbl(SirfMsg.substring(22,38) ) ) )
//        val tow = floor(GPS_STime).toLong()
        val PD = Double.longBitsToDouble( parseUnsignedHex( invSirfDbl(SirfMsg.substring(38,54) ) ) )
        val Cfreq = invSirfSgl(SirfMsg.substring(54,62)).toLong(radix = 16)
        val Cfase = Double.longBitsToDouble( parseUnsignedHex( invSirfDbl(SirfMsg.substring(62,78) ) ) )
//        val timeInTrack = SirfMsg.substring(78,82).toLong(radix = 16)
//        val syncFlag = SirfMsg.substring(82,84)
//        val CH1 = SirfMsg.substring(84,86).toLong(radix = 16)
//        val CH2 = SirfMsg.substring(86,88).toLong(radix = 16)
//        val CH3 = SirfMsg.substring(88,90).toLong(radix = 16)
//        val CH4 = SirfMsg.substring(90,92).toLong(radix = 16)
//        val CH5 = SirfMsg.substring(92,94).toLong(radix = 16)
//        val CH6 = SirfMsg.substring(94,96).toLong(radix = 16)
//        val CH7 = SirfMsg.substring(96,98).toLong(radix = 16)
//        val CH8 = SirfMsg.substring(98,100).toLong(radix = 16)
//        val CH9 = SirfMsg.substring(100,102).toLong(radix = 16)
//        val CH10 = SirfMsg.substring(102,104).toLong(radix = 16)
//        val deltaRange = SirfMsg.substring(104,108).toLong(radix = 16)
//        val meanDelta = SirfMsg.substring(108,112).toLong(radix = 16)
//        val extrapolationTime = SirfMsg.substring(112,116).toLong(radix = 16)
//        val phaseError = SirfMsg.substring(116,118).toLong(radix = 16)
//        val lowPower = SirfMsg.substring(118,120).toLong(radix = 16)

//        Log.i("Sirf MID 28",SirfMsg)
//        Log.i("Sirf MID 28","Channel=$Channel TimeTag=$TimeTag PRN=$PRN GPS_STime=$GPS_STime PD=$PD Cfreq=$Cfreq Cfase=$Cfase")
//        "tow=$tow timeInTrack=$timeInTrack syncFlag=$syncFlag " +
//        "CH1=$CH1 CH2=$CH2 CH3=$CH3 CH4=$CH4 CH5=$CH5 CH6=$CH6 CH7=$CH7 CH8=$CH8 CH9=$CH9 CH10=$CH10 " +
//        "deltaRange=$deltaRange meanDelta=$meanDelta extrapolationTime=$extrapolationTime " +
//        "phaseError=$phaseError lowPower=$lowPower")

        return MID28(Channel, TimeTag, PRN, GPS_STime, PD, Cfreq, Cfase)
    }

    fun msg7_parser(SirfMsg: String): MID7{
        val week = SirfMsg.substring(10,14).toLong(radix = 16)
        val tow = SirfMsg.substring(14,22).toLong(radix = 16) / 100.0
        val SVs = SirfMsg.substring(22,24).toInt(radix = 16)
        val Clk_drift = SirfMsg.substring(24,32).toLong(radix = 16)       //Hz
        val Clk_bias = SirfMsg.substring(32,40).toLong(radix = 16)        //ns
        val GPS_Time = SirfMsg.substring(40,48).toLong(radix = 16)

//        Log.i("Sirf MID 7",SirfMsg)
//        Log.i("Sirf MID 7","week=$week tow=$tow SVs=$SVs Clock_Drift=$Clk_drift Clock_Bias $Clk_bias GPS_Time=$GPS_Time")

        return MID7(week, tow, SVs, Clk_drift, Clk_bias, GPS_Time)
    }

    open fun updateCoordTextView(data: List<Any>?){
        // Override this fun in MainActivity
    }
    open fun sendRTCMdata(data: ByteArray){
        // Override this fun in MainActivity
    }
    open fun epoch2rinex(epoch: Epoch){
        // Override this fun in MainActivity
    }
}

// ByteArray to HexString
//fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

public fun String?.indexesOf(substr: String, ignoreCase: Boolean = true): List<Int> {
    return this?.let {
        val regex = if (ignoreCase) Regex(substr, RegexOption.IGNORE_CASE) else Regex(substr)
        regex.findAll(this).map { it.range.start }.toList()
    } ?: emptyList()
}

public fun hex2dec4S( input_args: String ): Long {
//    hex2dec4S converte uma string hexadecimal negativa de 4 bits em um numero decimal

    if (input_args[0].toString().equals("F", true)) {
        return (input_args.toLong(radix = 16) - "FFFFFFFF".toLong(radix = 16) - 1)
    }
    else {
        return input_args.toLong(radix = 16)
    }
}

private fun invSirfSgl( a: String ): String {
//INVSIRFSGL Inverte a a variavel 4 Sgl do Sirf Binary Protocol
    return a.substring(6,8) + a.substring(4,6) + a.substring(2,4) + a.substring(0,2)
}

private fun invSirfDbl( a: String ): String {
//INVSIRFDBL Inverte a a variavel 8 Dbl do Sirf Binary Protocol
    return a.substring(8,16) + a.substring(0,8)
}

fun parseUnsignedHex(text: String): Long {
    return if (text.length == 16) {
        (parseUnsignedHex(text.substring(0, 1)) shl 60
                or parseUnsignedHex(text.substring(1)))
    } else text.toLong(16)
}

data class MID28(val Channel: Long, val TimeTag: Long, val PRN: Int, val GPS_STime: kotlin.Double,
                 val PD: kotlin.Double, val Cfreq: Long, val Cfase: kotlin.Double)
{
    fun copy(): MID28 {
        return MID28(Channel, TimeTag, PRN, GPS_STime, PD, Cfreq, Cfase)
    }
}

data class MID2( val X: Long, val Y: Long, val Z: Long, val week: Long, val tow: kotlin.Double, val SVinFix: Int, val listPRN: List<Int>)
{
    fun copy(): MID2 {
        return MID2(X, Y, Z, week, tow, SVinFix, listPRN)
    }
    fun toList(): List<Any> {
        return listOf<Any>(X, Y, Z, week, tow, SVinFix, listPRN)
    }
}

data class MID7( val week: Long, val tow: kotlin.Double, val SVs: Int, val Clk_drift: Long, val Clk_bias: Long, val GPS_Time: Long)
{
    fun copy(): MID7 {
        return MID7(week, tow, SVs, Clk_drift, Clk_bias, GPS_Time)
    }
}

class Epoch(tow: Int){
    var oldTow = tow
    var MID2: MID2? = null
    var MID7: MID7? = null
    var arrayMID28: Array<MID28> = arrayOf<MID28>()
}