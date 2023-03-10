package com.dinkar.blescanner

import android.util.Log
import com.dinkar.blescanner.Filter.KalmanFilter
import kotlin.math.pow


val kf: KalmanFilter = KalmanFilter(0.065, 1.4, 0.0, 0.0)
open class Beacon(mac: String?) {
    enum class beaconType {
        iBeacon, eddystoneUID, any
    }

    val macAddress = mac
    var manufacturer: String? = null
    var type: beaconType = beaconType.any
    open var uuid: String? = null
    var major: Int? = null
    var minor: Int? = null
    // var namespace: String? = null
    // var instance: String? = null
    var rssi: Int? = null

    /**addition field**/
    var x: Int = -1
    var y: Int = -1

    /*****************/
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Beacon) return false

        if (macAddress != other.macAddress) return false

        return true
    }

    override fun hashCode(): Int {
        return macAddress?.hashCode() ?: 0
    }

    private val  referenceRssi: Double = -55.0
    private val envParameter: Double = 3.5
    fun getCalculatedDistance(): Double{
        var distance: Double = 0.0 /************/
        // var FilteredRssi = kf.getFilteredValue(rssi!!.toDouble())
        // var rssiDiff = referenceRssi - FilteredRssi
        var rssiDiff = referenceRssi - rssi!!.toDouble()
        distance = 10.0.pow(rssiDiff / (10 * envParameter))
        return distance
    }
}

class anchors(){
    val number = 3
    val uuidList:List<String> = listOf<String>(
        "644F76F76A5242BCE911FD902C9BB987", //(0,430)
        "020012AC42021D86ED11EF1374360E91", // TODO set this up later 7777772E6B6B6D636E2E636F6D000001
        "020012AC42021D86ED11F0132064F146"
    )
    val xList = listOf<Double>(0.0, 3.9, 0.0)
    val yList = listOf<Double>(4.3, 0.0, 0.0)

    // var anchor1Rssi: Int? = null
    // var anchor2Rssi: Int? = null
    // var anchor3Rssi: Int? = null

    var distance1 = -1.0
    var distance2 = -1.0
    var distance3 = -1.0
}


