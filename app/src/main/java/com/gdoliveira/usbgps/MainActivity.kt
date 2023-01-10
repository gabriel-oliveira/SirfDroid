package com.gdoliveira.usbgps

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private var connectedState: Boolean = false

    lateinit var m_usbManager: UsbManager
    var m_device: UsbDevice? = null
    var m_serial: UsbSerialDevice? = null
    var m_connection: UsbDeviceConnection? = null
    var tcpServer: TcpServer? = null
    var sirfHandle = SirfHandle()
    var receivedMsgsId = arrayListOf<Int>()
    var data_msg2: List<Long>? = null

    private val ACTION_USB_PERMISSION = "com.gdoliveira.usbgps.USB_PERMISSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        m_usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        registerReceiver(broadcastReceiver, filter)

        val rollButton: Button = findViewById(R.id.buttonConectar)
        rollButton.setOnClickListener {
            if (!connectedState) {
                startUsbConnect()
                if (connectedState) {
                    rollButton.text = getString(R.string.desconectar)
                }
            } else {
                usbDisconnect()
                if (!connectedState) {
                    rollButton.text = getString(R.string.conectar)
                }
            }
        }

        val clearButton: ImageButton = findViewById(R.id.cButton)
        clearButton.setOnClickListener {
            clearTextView()
        }

        val togButton: ToggleButton = findViewById(R.id.toggleButton)
        val changeButton: Button = findViewById(R.id.buttonChangeProtocol)
        val navMsgButton: Button = findViewById(R.id.button)

//        when (togButton.text as String) {
//            "NMEA 4800" -> {
//                changeButton.append(": to SIRF 115200")
//            }
//            "SIRF 115200" -> {
//                changeButton.append(": to NMEA 4800")
//            }
//            else -> {
//                Log.e("GPS", "Erro ao definir NMEA x Sirf em buttonChangeProtocol")
//            }
//        }

        // TCP SERVER
        startTcpServer()

        changeButton.setOnClickListener {
            val protocol: String = togButton.text as String
            if (connectedState) {
                val changeMsg: ByteArray = when (protocol) {
                        "NMEA 4800" -> {
                            "\$PSRF100,0,115200,8,1,0*04\r\n".toByteArray()
                    }
                        "SIRF 115200" -> {
                            "A0A200188102010100010101050101010001000100010001000112C00167B0B3".decodeHex() + "\r\n".toByteArray()
                    }
                        else -> {
                            Log.e("GPS", "Erro ao definir NMEA x Sirf em buttonChangeProtocol")
                            return@setOnClickListener
                    }
                }
                // send changeMsg to device
                m_serial!!.syncWrite(changeMsg,0)
                Log.i("Serial", "Sent: $changeMsg")
                usbDisconnect()
                togButton.toggle()
                rollButton.text = getString(R.string.conectar)
            } else {
                Toast.makeText(this, "Device is disconnected!", Toast.LENGTH_SHORT).show()
            }
        }

        // reset, hot, enable nav and debug.
        navMsgButton.setOnClickListener {
            val protocol: String = togButton.text as String
            if (connectedState && protocol == "SIRF 115200") {

                val sirfMsgToSend: ByteArray = "A0A2001980000000000000000000000000000000000000000000000C10009CB0B3".decodeHex() + "\r\n".toByteArray()

                // send changeMsg to device
                m_serial!!.syncWrite(sirfMsgToSend,0)
                Log.i("GPS", "NAV MSG ENABLE - Msg Sent: $sirfMsgToSend")

            } else {
                Toast.makeText(this, "Device is not connected with Sirf 115200", Toast.LENGTH_SHORT).show()
                Log.e("GPS", "NAV_MSG_ENABLE: Device is not connected with Sirf Binary protocol 115200 baudrate!")
                return@setOnClickListener
            }
        }
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        if(m_serial != null){
//            m_serial!!.close()
//            unregisterReceiver(broadcastReceiver)
//        }
//        connectedState = false
//    }

    private fun startUsbConnect() {
        val usbDevices: HashMap<String, UsbDevice>? = m_usbManager.deviceList
        if (usbDevices?.isNotEmpty()!!) {
            usbDevices.forEach { entry ->
                m_device = entry.value
                Log.i("Serial", "See vendorID: ${m_device?.vendorId}")
//                Toast.makeText(this, "See vendorID: ${m_device?.vendorId}", Toast.LENGTH_SHORT).show()
                if (m_device?.vendorId == 1659) {
                    val intent: PendingIntent =
                        PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
                    m_usbManager.requestPermission(m_device, intent)
                    connectedState = true
                    Log.i("Serial", "Connection successful to VID${m_device?.vendorId}")
                    Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show()
                } else {
                    Log.i("Serial", "Did not connect to vendorID: ${m_device?.vendorId}")
                    Toast.makeText(this, "Did not connect to vendorID: ${m_device?.vendorId}", Toast.LENGTH_SHORT).show()
                    m_connection = null
                    m_device = null
                }
            }
        } else {
            Log.i("Serial", "No USB devices found")
            Toast.makeText(this, "No USB devices found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun usbDisconnect() {
        if (m_serial != null) {
            m_serial!!.close()
            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
            connectedState = false
        }
    }

    private fun clearTextView() {
        val resultTextView: TextView = findViewById(R.id.textView)
        resultTextView.text = " "
        Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show()
    }

    private fun startRead(protocol: String) {
        CoroutineScope(IO).launch { readSyncData(protocol) }
    }

    private fun startTcpServer() {
        CoroutineScope(IO).launch {
            tcpServer = TcpServer()
            tcpServer!!.startServer()
        }
    }

    private fun readSyncData(protocol: String) {

        val resultTextView: TextView = findViewById(R.id.textView)
        val sv: ScrollView = findViewById(R.id.scrollview)

        while(connectedState){

            val buf = ByteArray(1024)
            val n: Int? = m_serial?.syncRead(buf, 0)
            var msg = ""

            if (n != null && n > 0) {

                val rec = ByteArray(n)
                buf.copyInto(rec,0,0,n)

                when (protocol) {
                    "NMEA 4800" -> {
                        msg = String(rec)
                    }
                    "SIRF 115200" -> {
                        msg = rec.toHexString()
                        var (msgList, msg2) = sirfHandle.msgReceived(msg)
                        msgList.forEach {
//                            Log.i("GPS", it)
                            if (it.toInt(16) !in receivedMsgsId) {
                                receivedMsgsId.add(it.toInt(16))
                            }
                        }
                        data_msg2 = msg2
                    }
                    else -> {
                        Toast.makeText(this, "Erro ao definir NMEA x Sirf em ReadSyncData", Toast.LENGTH_LONG).show()
                        return
                    }
                }

                if (tcpServer!!.isAlive) {
//                    tcpServer!!.sendData(rec,n)
                    tcpServer!!.sendString(msg)
                }

                CoroutineScope(Main).launch {
//                    Log.i("GPS", "Len: ${n} - msg: ${msg} ")
                    when (protocol) {
                        "NMEA 4800" -> {
                            resultTextView.append(msg)
                            sv.fullScroll(View.FOCUS_DOWN)
                            //Exibição desta forma está travando o APP para SirfBinary
                        }
                        "SIRF 115200" -> {
//                            Log.i("GPS", "Len: ${n} - msg: ${msg} ")
                            resultTextView.text = receivedMsgsId.sorted().toString()

                            var receivedMsgsIdAux = arrayListOf<Int>()
                            receivedMsgsIdAux.addAll(receivedMsgsId)
//                            receivedMsgsIdAux.forEach{
////                                resultTextView.append("$it, ")
//                                Log.i("Sirf MSG", "MSG_ID: $it")
//                            }

                            updateCoordTextView(data_msg2)

                        }
                        else -> {
                            Log.e("GPS", "Erro ao definir NMEA x Sirf em ReadSyncData display result")
                            return@launch
                        }
                    }
                }
            } else {
                break
            }
        }
    }

    private fun updateCoordTextView(data: List<Long>?){

        if (data != null) {
            val coordXTextView: TextView = findViewById(R.id.textView5)
            val coordYTextView: TextView = findViewById(R.id.textView6)
            val coordZTextView: TextView = findViewById(R.id.textView7)
            val weekTextView: TextView = findViewById(R.id.textView11)
            val towTextView: TextView = findViewById(R.id.textView12)
            val svinfixTextView: TextView = findViewById(R.id.textView13)

            coordXTextView.text = data!![0].toString()
            coordYTextView.text = data!![1].toString()
            coordZTextView.text = data!![2].toString()
            weekTextView.text = data!![3].toString()
            towTextView.text = data!![4].toString()
            svinfixTextView.text = data!![5].toString()
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action!! == ACTION_USB_PERMISSION) {
                val granted: Boolean =
                    intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    m_connection = m_usbManager.openDevice(m_device)
                    m_serial = UsbSerialDevice.createUsbSerialDevice(m_device, m_connection)
                    if (m_serial != null) {
                        if (m_serial!!.syncOpen()) {

                            val togButton: ToggleButton = findViewById(R.id.toggleButton)
                            val protocol: String = togButton.text as String

                            Log.i("Serial", protocol)
                            val baudrate: Int = when (protocol) {
                                "NMEA 4800" -> 4800
                                "SIRF 115200" -> 115200
                                else -> {
                                    Toast.makeText(context, "Erro ao definir NMEA x Sirf onReceive", Toast.LENGTH_LONG).show()
                                    return
                                }
                            }
                            Log.i("Serial", "Set baudrate to $baudrate")

                            m_serial!!.setBaudRate(baudrate)  //NMEA-4800 Sirf-115200
                            m_serial!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                            m_serial!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
                            m_serial!!.setParity(UsbSerialInterface.PARITY_NONE)
                            m_serial!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)

                            startRead(protocol)

                        } else {
                            Log.i("Serial", "Port is not open")
                            Toast.makeText(context, "Port is not open", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.i("Serial", "Port is null")
                        Toast.makeText(context, "Port is null", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.v("Serial", "No extra permission")
                    Toast.makeText(context, "No extra permission", Toast.LENGTH_SHORT).show()
                }
            } else if (intent.action!! == UsbManager.ACTION_USB_ACCESSORY_ATTACHED) {
                startUsbConnect()
            } else if (intent.action!! == UsbManager.ACTION_USB_ACCESSORY_DETACHED) {
                usbDisconnect()
            }
        }
    }
}

// ByteArray to HexString
fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

// HexString to ByteArray
fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) {
        Log.e("DecodeHex", "Must have an pair length")
    }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}