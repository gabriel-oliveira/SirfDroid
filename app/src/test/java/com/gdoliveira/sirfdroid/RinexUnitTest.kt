package com.gdoliveira.sirfdroid

import org.junit.Test
import org.junit.Assert.*

class RinexUnitTest {

    @Test
    fun tgps2Date_isCorret(){

        val rinex = Rinex()

        assertEquals(" 23  1 28 16 10 38,0200000",
            rinex.tgps2date(2246.0,576638.02) )

    }

    @Test
    fun dayOfYear_isCorret(){
        val rinex = Rinex()
        assertEquals(33, rinex.tow2dayOfYear(2247, 345601))
    }
}