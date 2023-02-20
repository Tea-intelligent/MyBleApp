package com.dinkar.blescanner.Filter

class KalmanFilter {
    constructor(processNoise: Double, sensorNoise: Double, estimatedError: Double, initialValue: Double) {
        q = processNoise;
        r = sensorNoise;
        p = estimatedError;
        x = initialValue;

        print("Kalman Filter initialised");
    }

    private val TRAINING_PREDICTION_LIMIT = 500
    var q: Double = 0.0
    var r: Double = 0.0
    var x: Double = 0.0
    var p: Double = 0.0
    var k: Double = 0.0
    var predictionCycles: Double = 0.0

    fun getFilteredValue(measurement: Double): Double{
        // prediction phrase
        p = p+q

        // measurement update
        k = p / (p+r)
        x += k * (measurement - x)
        p *= (1 - k)

        return x
    }
}
