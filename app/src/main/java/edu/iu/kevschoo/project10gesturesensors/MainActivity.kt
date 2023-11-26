package edu.iu.kevschoo.project10gesturesensors

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, SensorActivity::class.java)
        startActivity(intent)
        finish()
    }
}