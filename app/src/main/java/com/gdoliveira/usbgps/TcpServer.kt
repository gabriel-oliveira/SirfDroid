package com.gdoliveira.usbgps

import android.util.Log
import kotlinx.coroutines.delay
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class TcpServer {
    val PORT = 9876
    val TAG = "TCP"
    var isAlive = false
    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    var dataOutputStream: DataOutputStream? = null
    var dataInputStream: DataInputStream? = null

    suspend fun startServer(){
        try {

            serverSocket = ServerSocket(PORT)
            Log.i("TCP", "TCP Server start on: $serverSocket")

            socket = serverSocket!!.accept()
            Log.i("TCP", "New client: $socket")

            isAlive = true

            dataInputStream = DataInputStream(socket!!.getInputStream())
            dataOutputStream = DataOutputStream(socket!!.getOutputStream())

            val buf = ByteArray(1024)

            while (true) {
                try {
                    if(dataInputStream!!.available() > 0){
                        val n: Int? = dataInputStream!!.read(buf)
                        val rec = ByteArray(n!!)
                        buf.copyInto(rec,0,0,n)
                        Log.i(TAG, "Received: " + String(rec))

//                        dataOutputStream!!.writeUTF("Received: " + String(rec))
                    }
                    delay(1000)
                } catch (e: IOException) {
                    e.printStackTrace()
                    try {
                        dataInputStream!!.close()
                        dataOutputStream!!.close()
                    } catch (ex: IOException) {
                        ex.printStackTrace()
                    }
                    isAlive = false
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    try {
                        dataInputStream!!.close()
                        dataOutputStream!!.close()
                    } catch (ex: IOException) {
                        ex.printStackTrace()
                    }
                    isAlive = false
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            try {
                socket?.close()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
            isAlive = false
        }
    }

    fun stopServer(){
        socket?.close()
        dataOutputStream!!.close()
        dataInputStream!!.close()
        isAlive = false
    }

    fun sendData(byteArray: ByteArray,n: Int){
        if (isAlive) {
            dataOutputStream!!.write(byteArray, 0, n)
            dataOutputStream!!.flush()
        }
    }

    fun sendString(s: String){
        if (isAlive) {
            dataOutputStream!!.writeBytes(s)
            dataOutputStream!!.flush()
        }
    }
}