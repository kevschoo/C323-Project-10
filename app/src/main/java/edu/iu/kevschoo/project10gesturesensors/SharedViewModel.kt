package edu.iu.kevschoo.project10gesturesensors

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
/**
 * ViewModel class for managing shared data between different components of the application
 * Extends AndroidViewModel and holds data related to gesture logs
 */
class SharedViewModel(application: Application) : AndroidViewModel(application)
{
    // LiveData holding a list of gesture logs.
    val gestureLog = MutableLiveData<List<String>>()

    /**
     * Updates the gesture log LiveData with a new list of gesture logs
     * @param log The new list of gesture logs to be set
     */
    fun updateGestureLog(log: List<String>)
    {
        gestureLog.value = log
    }
}