package yurazhovnir.healthgraphs.base

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.ViewDataBinding
import yurazhovnir.healthgraphs.R
import yurazhovnir.healthgraphs.base.utils.LayoutChangeListener
import yurazhovnir.healthgraphs.base.utils.OnBackPressed

abstract class BaseBindingActivity<VB : ViewDataBinding>(
    private val inflate: (layoutInflater: android.view.LayoutInflater) -> VB
) : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    private val layoutDelegate = object : LayoutChangeListener.Delegate {
        private val uiHandler = Handler(Looper.getMainLooper())

        override fun layoutDidChange(oldHeight: Int, newHeight: Int, tempBottom: Int) {
            uiHandler.post {
                if (tempBottom <= NAVIGATION_BAR_HEIGHT) return@post
                onKeyboardVisibilityChanged(newHeight > oldHeight)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        _binding = inflate.invoke(layoutInflater)
        setContentView(binding.root)

        binding.root.addOnLayoutChangeListener(LayoutChangeListener().apply {
            delegate = layoutDelegate
        })

        hideKeyboard()
    }

    /**
     * Replaces the fragment in the given container with optional back stack.
     */
    protected fun replaceFragment(
        @IdRes containerId: Int,
        fragment: androidx.fragment.app.Fragment,
        addToBackStack: Boolean = true
    ) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_enter_bottom,
                0,
                0,
                R.anim.fragment_exit_bottom
            )
            .replace(containerId, fragment, fragment::class.java.simpleName)
            .apply {
                if (addToBackStack) addToBackStack(fragment::class.java.simpleName)
            }
            .commit()
    }

    protected fun addFragment(@IdRes containerId: Int, fragment: androidx.fragment.app.Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.fragment_enter_bottom,
                    0,
                    0,
                    R.anim.fragment_exit_bottom
                )
                .add(containerId, fragment, fragment::class.java.simpleName)
                .addToBackStack(fragment::class.java.simpleName)
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error adding fragment: ${fragment::class.java.simpleName}", e)
        }
    }

    open fun onKeyboardVisibilityChanged(isVisible: Boolean) {}

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(window?.decorView?.windowToken, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide keyboard", e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(android.R.id.content)
        if ((fragment as? OnBackPressed)?.onBackPressed() != true) {
            super.onBackPressed()
        }
    }

    companion object {
        private const val TAG = "BaseBindingActivity"
        private const val NAVIGATION_BAR_HEIGHT = 82
    }
}
