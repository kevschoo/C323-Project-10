package edu.iu.kevschoo.project10gesturesensors

import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.launch
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration


class GestureActivity : ComponentActivity()
{
    /**
     * Called when the activity is starting
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied in onSaveInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        setContent { GestureScreen(viewModel) }
    }
}
/**
 * Composable function that sets up the main screen layout based on the device orientation (landscape or portrait)
 * @param viewModel The shared view model for the application
 */
@Composable
fun GestureScreen(viewModel: SharedViewModel) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val position = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    var gestureLog by remember { mutableStateOf(listOf<String>()) }
    val coroutineScope = rememberCoroutineScope()

    if (isLandscape)
    {
        Row(modifier = Modifier.fillMaxSize())
        {
            GestureCanvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                position = position.value,
                onGestureDetected =
                { dx, dy ->
                    val description = describeSwipeMovement(dx, dy)
                    gestureLog = listOf(description) + gestureLog.take(14)
                    val targetPosition = Offset(position.value.x + dx, position.value.y + dy)
                    coroutineScope.launch { position.animateTo(targetPosition, animationSpec = tween(durationMillis = 600)) }
                }
            )
            GestureLog(gestureLog = gestureLog, modifier = Modifier.weight(1f))
        }
    }
    else
    {
        Column(modifier = Modifier.fillMaxSize())
        {
            GestureCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                position = position.value,

                onGestureDetected =
                { dx, dy ->
                    val description = describeSwipeMovement(dx, dy)
                    gestureLog = listOf(description) + gestureLog.take(14)
                    val targetPosition = Offset(position.value.x + dx, position.value.y + dy)
                    coroutineScope.launch { position.animateTo(targetPosition, animationSpec = tween(durationMillis = 600)) }
                }
            )
            GestureLog(gestureLog = gestureLog, modifier = Modifier.weight(1f))
        }
    }
}
/**
 * Composable function for rendering the gesture canvas and the draggable object
 * This function is responsible for detecting gestures on the canvas and updating the position of the draggable object
 * @param modifier Modifier for customizing the layout, appearance, and behavior of the canvas
 * @param position The current position of the draggable object
 * @param onGestureDetected A callback function that is invoked when a gesture is detected
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GestureCanvas(modifier: Modifier, position: Offset, onGestureDetected: (Float, Float) -> Unit)
{
    val density = LocalDensity.current
    var startDragPosition by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .background(Color.LightGray)
            .pointerInteropFilter
            { motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startDragPosition = Offset(motionEvent.x, motionEvent.y)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val endDragPosition = Offset(motionEvent.x, motionEvent.y)
                        val dx = endDragPosition.x - startDragPosition.x
                        val dy = endDragPosition.y - startDragPosition.y
                        onGestureDetected(dx / density.density, dy / density.density)
                        true
                    }
                    else -> false
                }
            }
    )
    {
        val canvasWidth = constraints.maxWidth
        val canvasHeight = constraints.maxHeight

        val ballSizePx = with(density) { 50.dp.toPx() }
        val initialX = (canvasWidth - ballSizePx) / 2f
        val initialY = (canvasHeight - ballSizePx) / 2f

        val wrappedX = wrapAround(position.x, initialX, canvasWidth.toFloat())
        val wrappedY = wrapAround(position.y, initialY, canvasHeight.toFloat())

        Box(
            modifier = Modifier
                .offset { IntOffset(wrappedX.roundToInt(), wrappedY.roundToInt()) }
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.Red)
        )
    }
}
/**
 * Function to calculate the wrapped-around position of an object on the canvas
 * Ensures that the object reappears on the opposite side of the canvas when it moves off the canvas bounds
 * @param value The current position value (x or y coordinate)
 * @param initial The initial position value (x or y coordinate)
 * @param max The maximum boundary of the canvas (width or height)
 * @return The new position value after applying the wrap-around logic
 */
private fun wrapAround(value: Float, initial: Float, max: Float): Float
{
    val offset = value + initial
    return when
    {
        offset < 0 -> max + offset % max
        offset > max -> offset % max
        else -> offset
    }
}
/**
 * Composable function for displaying the gesture log
 * Lists the recent gestures detected on the canvas
 * @param gestureLog The list of strings representing the detected gestures
 * @param modifier Modifier for customizing the layout, appearance, and behavior of the gesture log
 */
@Composable
fun GestureLog(gestureLog: List<String>, modifier: Modifier)
{
    Column(modifier = modifier.background(Color.White).padding(16.dp))
    {
        Text("Gesture Log", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(10.dp))
        gestureLog.forEach { gesture -> Text(gesture) }
    }
}
/**
 * Function to describe the direction of a swipe movement based on the change in x and y coordinates
 * @param dx The change in x-coordinate
 * @param dy The change in y-coordinate
 * @return A string describing the direction of the swipe
 */
private fun describeSwipeMovement(dx: Float, dy: Float): String {
    val tolerance = 0.5f
    val absDx = dx.absoluteValue
    val absDy = dy.absoluteValue

    return when {
        absDx > absDy + absDy * tolerance && dx > 0 -> "Swiped Right"
        absDx > absDy + absDy * tolerance && dx < 0 -> "Swiped Left"
        absDy > absDx + absDx * tolerance && dy > 0 -> "Swiped Down"
        absDy > absDx + absDx * tolerance && dy < 0 -> "Swiped Up"
        absDx > absDy - absDy * tolerance && absDx < absDy + absDy * tolerance && dx > 0 -> if (dy > 0) "Swiped Bottom-Right" else "Swiped Top-Right"
        absDx > absDy - absDy * tolerance && absDx < absDy + absDy * tolerance && dx < 0 -> if (dy > 0) "Swiped Bottom-Left" else "Swiped Top-Left"
        else -> "No Swipe"
    }
}