package com.matfess.app.ui.common

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.matfess.app.data.MatfessRepository
import com.matfess.app.data.Prefs

/**
 * Fragment base class with the shared repository and lifecycle logging.
 */
abstract class BaseFragment : Fragment() {

    protected val repo: MatfessRepository by lazy { MatfessRepository(requireContext()) }
    protected val prefs: Prefs by lazy { Prefs(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MatfessLifecycle", "${javaClass.simpleName} → onCreate()")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("MatfessLifecycle", "${javaClass.simpleName} → onViewCreated()")
    }

    override fun onStart() {
        super.onStart()
        Log.d("MatfessLifecycle", "${javaClass.simpleName} → onStart()")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MatfessLifecycle", "${javaClass.simpleName} → onResume()")
    }

    override fun onPause() {
        Log.d("MatfessLifecycle", "${javaClass.simpleName} → onPause()")
        super.onPause()
    }

    override fun onStop() {
        Log.d("MatfessLifecycle", "${javaClass.simpleName} → onStop()")
        super.onStop()
    }

    override fun onDestroyView() {
        Log.d("MatfessLifecycle", "${javaClass.simpleName} → onDestroyView()")
        super.onDestroyView()
    }

    protected fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
