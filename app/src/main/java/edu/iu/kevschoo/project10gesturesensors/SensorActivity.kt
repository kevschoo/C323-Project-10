package edu.iu.kevschoo.project10gesturesensors


import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import kotlin.math.absoluteValue
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SensorActivity : ComponentActivity(), SensorEventListener, LocationListener
{
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private lateinit var geocoder: Geocoder
    private var temperature: Float = 0f
    private var airPressure: Float = 0f
    private var temperatureState = mutableStateOf(0f)
    private var airPressureState = mutableStateOf(0f)
    private var locationState = mutableStateOf("")
    private var temperatureSensorEnabled = false

    companion object
    {
        private const val PERMISSIONS_REQUEST_LOCATION = 100
    }

    /**
     * Called when the activity is starting.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down
     *                           this Bundle contains the data it most recently supplied in onSaveInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        geocoder = Geocoder(this, Locale.getDefault())

        val temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (temperatureSensor == null)
        {
            temperatureSensorEnabled = false
        }
        else
        {
            sensorManager.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL)
            temperatureSensorEnabled = true
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)}

        setupLocationUpdates()

        setContent {
            SensorScreen(
                viewModel,
                location = locationState.value,
                temperature = temperatureState.value,
                airPressure = airPressureState.value,
                temperatureSensorEnabled = temperatureSensorEnabled
            )
        }
    }

    /**
     * Sets up location updates, checking for the necessary permissions.
     */
    private fun setupLocationUpdates()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
        }
        else
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_LOCATION)
        }
    }

    /**
     * Callback for the result from requesting permissions. Handles the case of the request for location permission
     * @param requestCode The request code passed in requestPermissions(android.app.Activity, String[], int)
     * @param permissions The requested permissions
     * @param grantResults The grant results for the corresponding permissions
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_LOCATION)
        {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED))
            {
                setupLocationUpdates()
            }
            //  if user denies the permissions
        }
    }

    /**
     * Called when there is a new sensor event, updating the state variables for temperature and air pressure
     * @param event The SensorEvent
     */
    override fun onSensorChanged(event: SensorEvent)
    {
        when (event.sensor.type)
        {
            Sensor.TYPE_AMBIENT_TEMPERATURE -> temperatureState.value = event.values[0]
            Sensor.TYPE_PRESSURE -> airPressureState.value = event.values[0]
        }
    }

    /**
     * Called when the location has changed
     * @param location The new location, as a Location object
     */
    override fun onLocationChanged(location: Location)
    {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        val address = addresses?.firstOrNull()
        locationState.value = "${address?.locality}, ${address?.adminArea}"
    }

    /**
     * Called when the accuracy of the registered sensor has changed
     * @param sensor The sensor whose accuracy changed
     * @param accuracy The new accuracy of this sensor
     */
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
/**
 * Composable function to render the sensor activity screen
 * Displays the location, temperature, air pressure, and a button to navigate to the gesture activity
 * @param viewModel The shared view model for the application
 * @param location The current location
 * @param temperature The current temperature reading
 * @param airPressure The current air pressure reading
 */
@Composable
fun SensorScreen(viewModel: SharedViewModel, location: String, temperature: Float, airPressure: Float,temperatureSensorEnabled: Boolean)
{
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Text(text = "Sensor Playground", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Location: $location")
        Text(text = "Temperature Sensor Working: ${if (temperatureSensorEnabled) "True" else "False"}")
        Text(text = "Temperature: ${"%.1f".format(temperature)}°C")
        Text(text = "Air Pressure: ${"%.1f".format(airPressure)} hPa")

        Spacer(modifier = Modifier.height(20.dp))

        FlingGestureButton(context = context, onFling = {
            val intent = Intent(context, GestureActivity::class.java)
            context.startActivity(intent)
        })
    }
}

/**
 * Composable function for a button that triggers navigation on a fling gesture
 * @param context The current context
 * @param onFling The action to perform when a fling gesture is detected
 */
@Composable
fun FlingGestureButton(context: Context, onFling: () -> Unit)
{
    var offset by remember { mutableStateOf(Offset.Zero) }

    val gestureModifier = Modifier.pointerInput(Unit)
    {
        detectDragGestures { change, dragAmount ->
            offset += dragAmount
            val thresholdValue = 10
            if (offset.x.absoluteValue > thresholdValue) {onFling()}
        }
    }

    Button(onClick = { }, modifier = gestureModifier)
    {Text("Gesture Playground") }
}