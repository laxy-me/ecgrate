package com.laxy.ecgrate

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.content.IntentFilter
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laxy.ecgrate.base.BaseActivity
import com.laxy.ecgrate.databinding.ActivityMainBinding
import com.laxy.ecgrate.databinding.ItemRateBinding
import com.laxy.ecgrate.entity.CurrencyRate
import com.laxy.ecgrate.global.RateTask
import com.laxy.ecgrate.global.RateTask.data
import com.laxy.ecgrate.receiver.RefreshBroadcastReceiver
import com.laxy.ecgrate.receiver.ScreenReceiver
import com.laxy.ecgrate.viewmodel.MainViewModel
import com.laxy.ecgrate.widget.RateWidget


class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    private val screenReceiver by lazy { ScreenReceiver() }
    private val refreshReceiver by lazy { RefreshBroadcastReceiver() }
    private val mainViewModel by lazy { ViewModelProvider(this)[MainViewModel::class.java] }
    private var tempCurrency: String? = null
    private val rateAdapter by lazy { RateAdapter(mutableListOf()) { tempCurrency = it } }

    override fun bindingView() {
        // Header 是深色渐变背景，强制状态栏图标为白色
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        // 记录 Header 的 XML 原始 paddingTop，避免 insets 多次触发时叠加
        val headerOriginalTop = binding.header.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 根 layout 只处理左右和底部
            v.setPadding(bars.left, 0, bars.right, bars.bottom)
            // 顶部 inset 加到 Header，让渐变背景延伸到状态栏后面
            binding.header.setPadding(
                binding.header.paddingLeft,
                headerOriginalTop + bars.top,
                binding.header.paddingRight,
                binding.header.paddingBottom
            )
            insets
        }

        binding.recycleView.adapter = rateAdapter
        binding.recycleView.layoutManager = GridLayoutManager(this, 2)

        binding.time.setOnClickListener { mainViewModel.getRate() }

        binding.edit.setOnClickListener {
            binding.complete.visibility = View.VISIBLE
            binding.edit.visibility = View.INVISIBLE
            binding.intervalLayout.visibility = View.VISIBLE
            rateAdapter.editMode = true
        }
        binding.complete.setOnClickListener {
            rateAdapter.editMode = false
            tempCurrency?.let { RateTask.selectedCurrency = it }
            mainViewModel.getRate()
            binding.edit.visibility = View.VISIBLE
            binding.complete.visibility = View.INVISIBLE
            binding.intervalLayout.visibility = View.GONE
        }
        binding.save.setOnClickListener {
            val value = binding.editText.text.toString().toIntOrNull() ?: return@setOnClickListener
            RateTask.interval = value
            binding.editText.setText("")
        }
        binding.power.setOnClickListener {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    })
                } else {
                    startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        data.observe(this) { list ->
            if (list.isEmpty()) return@observe
            binding.time.text = "${list.first().ratDat} ${list.first().ratTim}"
            rateAdapter.updateData(list)
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            onBackPressedDispatcher.addCallback(this) {
                moveTaskToBack(true)
            }
        }
    }

    override fun initData() {
        RateTask.initSp(this)
        mainViewModel.getRate()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(
                refreshReceiver,
                IntentFilter(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
            )
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(screenReceiver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            unregisterReceiver(refreshReceiver)
        }
    }

    override fun onStop() {
        super.onStop()
        sendBroadcast(Intent(this, RateWidget::class.java).apply {
            action = RateWidget.ACTION_REFRESH
        })
        RateTask.scheduleNextAlarm(this)
        RateTask.schedule()
    }
}


internal class RateAdapter(
    private val list: MutableList<CurrencyRate.Body>,
    var tempCurrency: String? = null,
    val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<RateAdapter.VH>() {
    var editMode: Boolean = false
    private var selectedPosition: Int? = null

    fun updateData(newList: List<CurrencyRate.Body>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): VH {
        return VH(ItemRateBinding.inflate(LayoutInflater.from(p0.context), p0, false))
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(p0: VH, p1: Int) {
        p0.bindData(list[p1], p1)
    }

    inner class VH(private var binding: ItemRateBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(body: CurrencyRate.Body, p1: Int) {
            binding.apply {
                currencyName.text = body.ccyNbrEng
                rthBid.text = body.rthBid
                rthOfr.text = body.rthOfr
                rtcBid.text = body.rtcBid
                rtcOfr.text = body.rtcOfr
                root.setOnClickListener {
                    if (!editMode) return@setOnClickListener
                    onItemClick.invoke(body.ccyNbr)
                    tempCurrency = body.ccyNbr
                    selectedPosition?.let { notifyItemChanged(it) }
                    notifyItemChanged(p1)
                }
                val selected = body.ccyNbr == (tempCurrency ?: RateTask.selectedCurrency)
                if (selected) selectedPosition = p1
                root.isSelected = selected
            }
        }
    }
}
