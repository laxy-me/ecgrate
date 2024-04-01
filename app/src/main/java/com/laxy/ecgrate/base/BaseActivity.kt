package com.laxy.ecgrate.base

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 *
 * @author laxy
 * @date 2024/3/30
 */

inline fun <B : ViewBinding> Activity.viewBinder(
    crossinline inflater: (LayoutInflater) -> B
) = lazy(LazyThreadSafetyMode.NONE) {
    inflater.invoke(layoutInflater)
}

abstract class BaseActivity<B : ViewBinding>(bindingInflater: (LayoutInflater) -> B) : AppCompatActivity() {
    protected val binding by viewBinder<B>(bindingInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding
        beforeSetContent()
        setContentView(binding.root)
        bindingView()
        initData()
    }

    protected fun beforeSetContent() {
        enableEdgeToEdge()
    }

    abstract fun bindingView()

    abstract fun initData()
}

