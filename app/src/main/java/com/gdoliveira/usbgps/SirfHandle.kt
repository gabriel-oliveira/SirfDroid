package com.gdoliveira.usbgps

import android.util.Log
import java.lang.Double
import kotlin.math.round

open class SirfHandle() {
    var msgRec: String? = null
    var byteArrayReceived: ByteArray? = null


    fun msgReceived(rec: String): List<String> {
//        byteArrayReceived = byteArrayReceived?.plus(rec)
        var msgList = arrayListOf<String>()
        var data_msg2: List<Any>? = null
        var data_msg7: List<Any>? = null
        var data_msg28: List<Any>? = null
        msgRec += rec

        while (true){

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
                            data_msg2 = msg2_parser(newMsg)
                            updateCoordTextView(data_msg2)
                        }
                        "07" -> {
                            data_msg7 = msg7_parser(newMsg)
                        }
                        "1c" -> {
                            data_msg28 = msg28_parser(newMsg)
                        }

                    }

                } else {
                    Log.e("Check Sirf MSG", "$newMsg")
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
            Log.e("Check Sirf MSG", "Payload: $payloadLength - Msg length: ${msg.length}")
            false
        }
    }

    fun msg2_parser(SirfMsg: String): List<Any>{
        val X = hex2dec4S(SirfMsg.substring(10,18))
        val Y = hex2dec4S(SirfMsg.substring(18,26))
        val Z = hex2dec4S(SirfMsg.substring(26,34))
        val week = SirfMsg.substring(52,56).toLong(radix = 16)
        val tow = SirfMsg.substring(57, 64).toLong(radix = 16) / 100.0
        val SVinFix = SirfMsg.substring(64,66).toLong(radix = 16)
        val CH1 = SirfMsg.substring(66,68).toLong(radix = 16)
        val CH2 = SirfMsg.substring(68,70).toLong(radix = 16)
        val CH3 = SirfMsg.substring(70,72).toLong(radix = 16)
        val CH4 = SirfMsg.substring(72,74).toLong(radix = 16)
        val CH5 = SirfMsg.substring(74,76).toLong(radix = 16)
        val CH6 = SirfMsg.substring(76,78).toLong(radix = 16)
        val CH7 = SirfMsg.substring(78,80).toLong(radix = 16)
        val CH8 = SirfMsg.substring(80,82).toLong(radix = 16)
        val CH9 = SirfMsg.substring(82,84).toLong(radix = 16)
        val CH10 = SirfMsg.substring(84,86).toLong(radix = 16)
        val CH11= SirfMsg.substring(86,88).toLong(radix = 16)
        val CH12 = SirfMsg.substring(88,90).toLong(radix = 16)
        Log.i("Sirf MID 2",SirfMsg)
        Log.i("Sirf MID 2","X=$X Y=$Y Z=$Z week=$week tow=$tow SVinFix=$SVinFix")
        return listOf<Any>(X, Y, Z, week, tow, SVinFix)
    }

    fun msg28_parser(SirfMsg: String): List<Any>{
        val Channel = SirfMsg.substring(10,12).toLong(radix = 16)
        val TimeTag = SirfMsg.substring(12,20).toLong(radix = 16)
        val PRN = SirfMsg.substring(20,22).toLong(radix = 16)
        val GPS_STime = Double.longBitsToDouble( parseUnsignedHex( invSirfDbl(SirfMsg.substring(22,38) ) ) )
        val tow = round(GPS_STime)
        val PD = Double.longBitsToDouble( parseUnsignedHex( invSirfDbl(SirfMsg.substring(38,54) ) ) )
        val Cfreq = invSirfSgl(SirfMsg.substring(54,62)).toLong(radix = 16)
        val Cfase = Double.longBitsToDouble( parseUnsignedHex( invSirfDbl(SirfMsg.substring(62,78) ) ) )
        Log.i("Sirf MID 28",SirfMsg)
        Log.i("Sirf MID 28","Channel=$Channel TimeTag=$TimeTag PRN=$PRN GPS_STime=$GPS_STime tow=$tow PD=$PD Cfreq=$Cfreq Cfase=$Cfase")
        return listOf<Any>(Channel, TimeTag, PRN, GPS_STime, tow, PD, Cfreq, Cfase)
    }

    fun msg7_parser(SirfMsg: String): List<Any>{
        val week = SirfMsg.substring(10,14).toLong(radix = 16)
        val tow = SirfMsg.substring(14,22).toLong(radix = 16) / 100.0
        val SVs = SirfMsg.substring(22,24).toLong(radix = 16)
        val Clk_drift = SirfMsg.substring(24,32).toLong(radix = 16)       //Hz
        val Clk_bias = SirfMsg.substring(32,40).toLong(radix = 16)        //ns
        val GPS_Time = SirfMsg.substring(40,48).toLong(radix = 16)
        Log.i("Sirf MID 7",SirfMsg)
        Log.i("Sirf MID 7","week=$week tow=$tow SVs=$SVs Clock_Drift=$Clk_drift Clock_Bias $Clk_bias GPS_Time=$GPS_Time")
        return listOf<Any>(week, tow, SVs, Clk_drift, Clk_bias, GPS_Time)
    }

    public open fun updateCoordTextView(data: List<Any>?){
        // TODO: overide this fun in MainActivity
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