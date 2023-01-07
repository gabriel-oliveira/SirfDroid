package com.gdoliveira.usbgps

import android.util.Log

class SirfHandle() {
    var msgRec: String? = null
    var byteArrayReceived: ByteArray? = null


    fun msgReceived(rec: String): List<String> {
//        byteArrayReceived = byteArrayReceived?.plus(rec)
        var msgList = arrayListOf<String>()
        msgRec += rec

        while (true){

            var indexes = msgRec.indexesOf("b0b3a0a2")

            if( indexes.size > 1 ){

                var startIndexMsg = indexes[0]
                var endIndexMsg = indexes[1]

                val newMsg = msgRec!!.substring(startIndexMsg + 4,endIndexMsg + 4)

//                Log.i("Sirf Msg", newMsg)

                if ( checkSirfMsg(newMsg) ) {
                    msgList.add(msgRec!!.substring(startIndexMsg + 12, startIndexMsg + 14)) // MSG ID
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
}

// ByteArray to HexString
//fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

public fun String?.indexesOf(substr: String, ignoreCase: Boolean = true): List<Int> {
    return this?.let {
        val regex = if (ignoreCase) Regex(substr, RegexOption.IGNORE_CASE) else Regex(substr)
        regex.findAll(this).map { it.range.start }.toList()
    } ?: emptyList()
}