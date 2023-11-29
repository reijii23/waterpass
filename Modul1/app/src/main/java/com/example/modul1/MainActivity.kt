//MainActivity.kt
package com.example.modul1

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Surface
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt
import android.view.View
import android.widget.FrameLayout


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var rotationMatrix: FloatArray
    private lateinit var orientationValues: FloatArray
    private lateinit var orientationSensor: Sensor
    private lateinit var textViewAzimuth: TextView
    private lateinit var textViewPitch: TextView
    private lateinit var textViewRoll: TextView
    private lateinit var circleHorizontal: ImageView
    private lateinit var circleVertical: ImageView
    private lateinit var customViewH: CustomViewHorizontal
    private lateinit var customViewV: CustomViewVertical



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Lock the orientation to portrait initially
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Initialize UI elements
        textViewAzimuth = findViewById(R.id.textAzimuth)
        textViewPitch = findViewById(R.id.textPitch)
        textViewRoll = findViewById(R.id.textRoll)
        circleHorizontal = findViewById(R.id.circleHorizontal)
        circleVertical = findViewById(R.id.circleVertical)

        // Set circle dimensions based on line dimensions
        val horizontalLine = findViewById<FrameLayout>(R.id.horizontalLine)
        val verticalLine = findViewById<FrameLayout>(R.id.verticalLine)

        val horizontalCircle = findViewById<ImageView>(R.id.circleHorizontal)
        val verticalCircle = findViewById<ImageView>(R.id.circleVertical)

        horizontalCircle.layoutParams.width = horizontalLine.width
        horizontalCircle.layoutParams.height = horizontalLine.width

        verticalCircle.layoutParams.width = verticalLine.height
        verticalCircle.layoutParams.height = verticalLine.height

        // Request a layout update to reflect the changes
        horizontalCircle.requestLayout()
        verticalCircle.requestLayout()

        // Initialize sensor components
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (rotationVectorSensor == null) {
            // Handle the case where the sensor is not available on this device
            // You may want to show an error message or disable certain features
        } else {
            // Register the sensor listener if the sensor is available
            sensorManager.registerListener(
                this,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            rotationMatrix = FloatArray(9)
            orientationValues = FloatArray(3)
        }
        customViewH = CustomViewHorizontal(this)
        customViewV = CustomViewVertical(this)
        val Hlayout = findViewById<View>(R.id.horizontalLine) as FrameLayout
        val Vlayout = findViewById<View>(R.id.verticalLine) as FrameLayout
        Hlayout.addView(customViewH)
        Vlayout.addView(customViewV)
    }

    inner class CustomViewHorizontal(context: Context) : View(context) {

        private var circlePaint: Paint = Paint()

        init {
            circlePaint.isAntiAlias = true
            circlePaint.color = resources.getColor(R.color.purple_500) // Change color as needed
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val horizontalRadius = height / 2f
            canvas.drawCircle(width / 2f, height / 2f, horizontalRadius, circlePaint)
        }
    }

    inner class CustomViewVertical(context: Context) : View(context) {

        private var circlePaint: Paint = Paint()

        init {
            circlePaint.isAntiAlias = true
            circlePaint.color = resources.getColor(R.color.purple_500) // Change color as needed
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val verticalRadius = width / 2f
            canvas.drawCircle(width / 2f, height / 2f, verticalRadius, circlePaint)
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the sensor listener
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if(rotationVectorSensor != null){
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        val magnetoMeterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if(magnetoMeterSensor != null){
            sensorManager.registerListener(this, magnetoMeterSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the sensor listener to save power when the app is in the background
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            // Get rotation matrix
            val rotationMatrix = FloatArray(9)
            val rotationOK = SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // Adjust rotation matrix based on screen rotation
            var rotationMatrixAdjusted = FloatArray(9)
            val rotation = windowManager.defaultDisplay.rotation
            when (rotation) {
                Surface.ROTATION_0 -> rotationMatrixAdjusted = rotationMatrix.clone()
                Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                    rotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, rotationMatrixAdjusted
                )
                Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                    rotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, rotationMatrixAdjusted
                )
                Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                    rotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, rotationMatrixAdjusted
                )
            }

            // Get orientation values in radians
            val orientationValues = FloatArray(3)
            SensorManager.getOrientation(rotationMatrixAdjusted, orientationValues)

            // Convert orientation values to degrees
            val azimuth = Math.toDegrees(orientationValues[0].toDouble())
            val pitch = Math.toDegrees(orientationValues[1].toDouble())
            val roll = Math.toDegrees(orientationValues[2].toDouble())

            // Update TextViews
            textViewAzimuth.text = "Azimuth: $azimuth°"
            textViewPitch.text = "Pitch: $pitch°"
            textViewRoll.text = "Roll: $roll°"

            // Update circle positions
            updateCirclePosition(azimuth.toFloat(), pitch.toFloat(), roll.toFloat())
        }
    }


    private fun updateCirclePosition(azimuth: Float, pitch: Float, roll: Float) {
        // Update the position of circles based on azimuth, pitch, and roll
        val horizontalLine = findViewById<FrameLayout>(R.id.horizontalLine)
        val verticalLine = findViewById<FrameLayout>(R.id.verticalLine)

        val horizontalCircle = findViewById<ImageView>(R.id.circleHorizontal)
        val verticalCircle = findViewById<ImageView>(R.id.circleVertical)

        val maxHorizontalTranslation = horizontalLine.width - horizontalCircle.width
        val maxVerticalTranslation = verticalLine.height - verticalCircle.height

        val horizontalTranslation =
            ((azimuth / 90) * maxHorizontalTranslation).coerceIn(0f,
                maxHorizontalTranslation.toFloat()
            )
        val verticalTranslation =
            ((pitch / 90) * maxVerticalTranslation).coerceIn(0f, maxVerticalTranslation.toFloat())

        // Adjustments to ensure circles are positioned correctly
        horizontalCircle.translationX = horizontalLine.x + horizontalTranslation
        verticalCircle.translationY = verticalLine.y + verticalTranslation
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientationValues()
    }

    private fun updateOrientationValues() {
        // Update rotation matrix and orientation values when screen rotates
        SensorManager.getRotationMatrixFromVector(rotationMatrix, orientationValues)
        SensorManager.getOrientation(rotationMatrix, orientationValues)

        // Update UI elements as needed
    }
}
