package com.gdoliveira.usbgps

import android.util.Log
import java.io.File
import kotlin.math.floor
import android.os.Environment.getExternalStorageDirectory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period

const val DIRECTORY = "RINEX"

class Rinex(){

    private var rfile: File? = null
    var isStarted = false

    fun start(stationName: String){

        val dir = File(getExternalStorageDirectory(), DIRECTORY)

        if (!dir.isDirectory){
            if (!dir.mkdirs()){
                Log.e("RINEX","RINEX DIR NOT CREATED!")
            }
        }

        if (!isStarted) {

            checkFileName(stationName)
            rfile!!.createNewFile()
            isStarted = true

        } else {
            Log.e("RINEX","Rinex builder already started!")
        }
    }

    fun stop(){
        if (isStarted) {
            isStarted = false
        } else {
            Log.e("RINEX","Rinex builder already stopped!")
        }
    }

    fun write(text: String){
        if (isStarted){
            rfile!!.appendText(text)
        }
    }

    fun headerBuild(epoch: Epoch){

        val coordX: Double = epoch.MID2!!.X.toDouble()
        val coordY: Double = epoch.MID2!!.Y.toDouble()
        val coordZ: Double = epoch.MID2!!.Z.toDouble()
        val week: Double = epoch.MID7!!.week.toDouble()
        val tow: Double = epoch.MID7!!.tow

        val firstObs = tgps2date(week,tow+1.0).replace("  "," ").split(" ") //delay in 1s to first obs
        val dt = "%s-%02d-%02d %02d:%02d".format(
            firstObs[3],firstObs[2].toInt(),firstObs[1].toInt(),firstObs[4].toInt(),firstObs[5].toInt())

        write("     2.11           OBSERVATION DATA    G                   RINEX VERSION / TYPE\n")
        write("Sirf                Gabriel             %s      PGM / RUN BY / DATE\n".format(dt) )
        write("The file contains C1L1 pseudorange data of the SIRFSTART IV COMMENT\n")
        write("USER                                                        MARKER NAME\n")
        write("1                                                           MARKER NUMBER\n")
        write("GABRIEL DINIZ                                               OBSERVER / AGENCY\n")
        write("0                   SIRFSTARIV          4.0.4               REC # / TYPE / VERS\n")
        write("0                   <NONE>                                  ANT # / TYPE\n")
        write(" %12.3f  %12.3f  %12.3f                   APPROX POSITION XYZ\n".format(
            coordX,coordY,coordZ).replace(",",".") )
        write("         .0000         .0000         .0000                  ANTENNA: DELTA H/E/N\n")
        write("     1     0                                                WAVELENGTH FACT L1/2\n")
        write("     2    C1    L1                                          # / TYPES OF OBSERV\n")
        write("     1.000                                                  INTERVAL\n")
        write("  20%s    %2s    %2s    %2s    %2s   %10s                 TIME OF FIRST OBS\n".format(
            firstObs[1],firstObs[2],firstObs[3],firstObs[4],firstObs[5],firstObs[6]).replace(",",".") )
        write("                                                            END OF HEADER\n")

    }

    fun writeEpoch(epoch: Epoch){

        write( "%s  0".format( tgps2date( epoch.MID7!!.week.toDouble(), epoch.MID7!!.tow ) ).replace(",",".") )

        write( "%3d".format( epoch.MID2!!.SVinFix ) )

        var msgs28: List<MID28> = listOf<MID28>()
        epoch.arrayMID28.forEach {
            if (it.PRN in epoch.MID2!!.listPRN){
                if(it.GPS_STime.toLong() == epoch.MID2!!.tow.toLong()){
                    msgs28 += it
                }
            }
        }

        for (msg28 in msgs28) {
            write( "G%2d".format(msg28.PRN) )
        }

        for (msg28 in msgs28) {

            val Phase = msg28.Cfase
            val PD = msg28.PD

            val L1 = if (Phase != 0.0) {
                "%16.3f".format( (Phase / lightSpeed - epoch.MID7!!.Clk_bias * 1.0e-9) * f )
            } else {
                "%16s".format("")
            }

            val C1 = if ( PD != 0.0 ) {
                "%14.3f".format( (PD - (lightSpeed * epoch.MID7!!.Clk_bias * 1.0e-9) ) )
            } else {
                "%14s".format("")
            }

            write( "\n%14s%16s".format(C1, L1).replace(",",".") )

        }

        write("\n")

    }

    fun tgps2date( gweek: Double, wsec: Double ): String {
        //TGPS2DATA Retorna uma string com a data referente ao tempo GPS (GPS_Week e GPS_Time_of_week)

        val jd: Double = (gweek * 7.0) + (wsec / 86400.0) + 2444244.5
//        val MJD: Double = jd - 2400000.5
        val a: Double = floor(jd + 0.5)
        val b: Double = a + 1537.0
        val c: Double = floor((b - 122.1) / 365.25)
        val d: Double = floor(365.25 * c)
        val e: Double = floor((b - d) / 30.6001)
//        val f: Double = jd + 0.5
        val day: Double = b - d - floor(30.6001 * e)
        val month: Double = e - 1.0 - ( 12.0 * floor(e / 14.0) )
        val year: Double = c - 4715.0 - floor((7.0 + month) / 10.0)
        val dweek: Double = floor(wsec / 86400.0)
        val pom: Double = (wsec / 3600.0) - (dweek * 24.0)
        val hour: Double = floor(pom)
//        pom = (pom - hour) * 60.0
        var sec: Double = wsec - (dweek * 86400.0) - (hour * 3600.0)
        val min: Double = floor(sec / 60.0)
        sec -= min * 60.0

        return "%3.0f%3.0f%3.0f%3.0f%3.0f%11.7f".format( (year - 2000.0), month, day, hour, min, sec )
    }

    fun tow2dayOfYear(week: Long, tow: Long): Int{

        val initGPSTime = LocalDateTime.of(1980,1,6,0,0)
        val GPSTime = initGPSTime.plusDays(week * 7).plusSeconds(tow)

        return GPSTime.dayOfYear

    }

    fun checkFileName(stationName: String) {

        val stationPartOfFileName = if (stationName.length > 4) {
            stationName.substring(0, 4).uppercase()
        } else if (stationName.isEmpty()) {
            "SIRF"
        } else if (stationName.length in 1..4) {
            stationName + IntArray(4 - stationName.length){ _ -> 0}.joinToString("")
        } else {
            stationName.uppercase()
        }

        val datePartOfFileName = "%03d0.%02dO".format(LocalDate.now().dayOfYear, LocalDate.now().year - 2000)
        var fileName = stationPartOfFileName + datePartOfFileName

        val dir = File(getExternalStorageDirectory(), DIRECTORY)

        var i = 1
        while (true) {

            rfile = File(dir.absolutePath, fileName)

            if (!rfile!!.isFile) {

                break

            } else {

                val n = i.toString()
                fileName = stationPartOfFileName.substring(0,4 - n.length) + n + datePartOfFileName
                i += 1

            }
        }

    }

}

