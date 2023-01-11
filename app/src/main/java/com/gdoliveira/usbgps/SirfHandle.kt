package com.gdoliveira.usbgps

import android.util.Log
import kotlin.math.roundToLong

class SirfHandle() {
    var msgRec: String? = null
    var byteArrayReceived: ByteArray? = null


    fun msgReceived(rec: String): Pair<List<String>, List<Long>?> {
//        byteArrayReceived = byteArrayReceived?.plus(rec)
        var msgList = arrayListOf<String>()
        var data_msg2: List<Long>? = null
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
                        }
                        "28" -> {

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

        return Pair(msgList, data_msg2)

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

    fun msg2_parser(SirfMsg: String): List<Long>{
        val X = hex2dec4S(SirfMsg.substring(10,18))
        val Y = hex2dec4S(SirfMsg.substring(18,26))
        val Z = hex2dec4S(SirfMsg.substring(26,34))
        val week = SirfMsg.substring(52,56).toLong(radix = 16)
        val tow = (SirfMsg.substring(57, 64).toLong(radix = 16) / 100.0).roundToLong()
        val SVinFix = SirfMsg.substring(64,66).toLong(radix = 16)
//        Log.i("Sirf MID 2",SirfMsg)
//        Log.i("Sirf MID 2","X=$X - Y=$Y - Z=$Z - week=$week - tow=$tow -SVinFix=$SVinFix")
        return listOf<Long>(X, Y, Z, week, tow, SVinFix)
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