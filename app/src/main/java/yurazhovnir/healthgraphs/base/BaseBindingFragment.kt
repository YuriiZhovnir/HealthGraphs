package yurazhovnir.healthgraphs.base

import android.content.Context
import android.os.*
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import yurazhovnir.healthgraphs.R
import yurazhovnir.healthgraphs.base.utils.LayoutChangeListener
import yurazhovnir.healthgraphs.base.utils.OnBackPressed

typealias Inflate<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

abstract class BaseBindingFragment<VB : ViewDataBinding>(private val inflate: Inflate<VB>) : Fragment(), OnBackPressed {

    abstract val TAG: String

    private var _binding: VB? = null
    val binding get() = _binding!!

    private var instanceStateSaved = false

    private val layoutDelegate = object : LayoutChangeListener.Delegate {
        private val uiHandler = Handler(Looper.getMainLooper())

        override fun layoutDidChange(oldHeight: Int, newHeight: Int, tempBottom: Int) {
            uiHandler.post {
//                if (tempBottom <= NAVIGATION_BAR_HEIGHT_F) return@post
//                onKeyboardVisibilityChanged(newHeight > oldHeight)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = inflate.invoke(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onKeyboardVisibilityChanged(view)
        setupKeyboardDismissOnTouchOutside(binding.root)
    }

    override fun onResume() {
        super.onResume()
        instanceStateSaved = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        instanceStateSaved = true
    }

    override fun onDestroyView() {
        hideKeyboard()
        _binding = null
        super.onDestroyView()
    }

    private fun onKeyboardVisibilityChanged(view: View) {
        view.addOnLayoutChangeListener(LayoutChangeListener().apply {
            delegate = layoutDelegate
        })
    }

    private fun setupKeyboardDismissOnTouchOutside(view: View) {
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                hideKeyboard()
                false
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setupKeyboardDismissOnTouchOutside(view.getChildAt(i))
            }
        }
    }

    open fun alertClosed() {}

    fun hideKeyboard() {
        try {
            val inputMethodManager =
                activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(
                activity?.currentFocus?.windowToken ?: binding.root.windowToken,
                0
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected fun setStatusBarColor(colorResId: Int) {
        activity?.window?.apply {
            statusBarColor = ContextCompat.getColor(requireContext(), colorResId)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    protected fun replaceFragment(
        @IdRes hostId: Int = android.R.id.content,
        fragment: BaseBindingFragment<out ViewDataBinding>,
        isAddToBackStack: Boolean = true,
    ) {
        activity?.supportFragmentManager?.beginTransaction()?.apply {
            setCustomAnimations(
                R.anim.fragment_enter_bottom,
                0,
                0,
                R.anim.fragment_exit_bottom
            )
            replace(hostId, fragment, fragment.TAG)
            if (isAddToBackStack) addToBackStack(fragment.TAG)
            setReorderingAllowed(true)
            commit()
        }
    }

    fun addFragment(
        @IdRes hostId: Int = android.R.id.content,
        fragment: BaseBindingFragment<out ViewDataBinding>,
    ) {
        activity?.supportFragmentManager?.beginTransaction()?.apply {
            setCustomAnimations(
                R.anim.fragment_enter_bottom,
                0,
                0,
                R.anim.fragment_exit_bottom
            )
            add(hostId, fragment, fragment.TAG)
            addToBackStack(fragment.TAG)
            setReorderingAllowed(true)
            commit()
        }
    }

    open fun onBackClick() {
        activity?.supportFragmentManager?.popBackStack()
    }

    override fun onBackPressed(): Boolean = false

    open fun keyboardStateChanged(isShown: Boolean) {}

    companion object {
        private const val TAG = "BaseBindingFragment"
        private const val NAVIGATION_BAR_HEIGHT_F = 82
    }
}
