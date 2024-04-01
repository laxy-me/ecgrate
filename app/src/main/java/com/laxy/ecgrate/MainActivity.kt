package com.laxy.ecgrate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.IntentCompat
import androidx.core.content.UnusedAppRestrictionsConstants.API_30
import androidx.core.content.UnusedAppRestrictionsConstants.API_30_BACKPORT
import androidx.core.content.UnusedAppRestrictionsConstants.API_31
import androidx.core.content.UnusedAppRestrictionsConstants.DISABLED
import androidx.core.content.UnusedAppRestrictionsConstants.ERROR
import androidx.core.content.UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE
import androidx.core.view.ViewCompat
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
    private val mainViewModel by lazy { ViewModelProvider(this)[MainViewModel::class.java] }
    private var tempCurrency: String? = null
    override fun bindingView() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        data.observe(this) { list ->
            if (list.isEmpty()) {
                return@observe
            }
            binding.apply {
                time.text = "${list.first().ratDat} ${list.first().ratTim}"
                time.setOnClickListener {
                    mainViewModel.getRate()
                }
                val rateAdapter = RateAdapter(list) {
                    tempCurrency = it
                }
                recycleView.adapter = rateAdapter
                recycleView.layoutManager = GridLayoutManager(root.context, 2)
                edit.setOnClickListener {
                    complete.visibility = View.VISIBLE
                    edit.visibility = View.INVISIBLE
                    intervalLayout.visibility = View.VISIBLE
                    rateAdapter.editMode = true
                }
                complete.setOnClickListener {
                    rateAdapter.editMode = false
                    tempCurrency?.let {
                        RateTask.selectedCurrency = it
                    }
                    mainViewModel.getRate()
                    edit.visibility = View.VISIBLE
                    complete.visibility = View.INVISIBLE
                    intervalLayout.visibility = View.GONE
                }
                save.setOnClickListener {
                    try {
                        RateTask.interval = editText.text.toString().toInt()
                        editText.setText("")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                power.setOnClickListener {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        } else {
                            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
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
        registerReceiver(RefreshBroadcastReceiver(), IntentFilter().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                addAction(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
            }
        })
    }

    override fun onStop() {
        super.onStop()
        val intent = Intent(this, RateWidget::class.java)
        intent.setAction(RateWidget.ACTION_REFRESH)
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        sendBroadcast(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scheduleAlarm()
            RateTask.schedule()
        } else {
            RateTask.schedule()
        }
    }

    private fun onResult(appRestrictionsStatus: Int) {
        when (appRestrictionsStatus) {
            // Couldn't fetch status. Check logs for details.
            ERROR -> {}

            // Restrictions don't apply to your app on this device.
            FEATURE_NOT_AVAILABLE -> {}

            // The user has disabled restrictions for your app.
            DISABLED -> {}

            // If the user doesn't start your app for a few months, the system will
            // place restrictions on it. See the API_* constants for details.
            API_30_BACKPORT, API_30, API_31 -> handleRestrictions(appRestrictionsStatus)
        }
    }

    private fun handleRestrictions(appRestrictionsStatus: Int) {
        // If your app works primarily in the background, you can ask the user
        // to disable these restrictions. Check if you have already asked the
        // user to disable these restrictions. If not, you can show a message to
        // the user explaining why permission auto-reset or app hibernation should be
        // disabled. Then, redirect the user to the page in system settings where they
        // can disable the feature.
        val intent = IntentCompat.createManageUnusedAppRestrictionsIntent(this, packageName)

        // You must use startActivityForResult(), not startActivity(), even if
        // you don't use the result code returned in onActivityResult().
        startActivityForResult(intent, 0)
    }

    private fun scheduleAlarm() {
        // 获取AlarmManager实例
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        // 创建一个Intent，用于启动BroadcastReceiver
        val intent = Intent(this, RefreshBroadcastReceiver::class.java)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAction(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
                }
            }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val intervalMillis = (3 * 1000).toLong()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, intervalMillis, pendingIntent)
//                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), intervalMillis, pendingIntent)
                alarmManager?.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + intervalMillis,
                    intervalMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, intervalMillis, pendingIntent)
        }
    }
}


internal class RateAdapter(
    private val list: List<CurrencyRate.Body>,
    var tempCurrency: String? = null,
    val onItemClick: (String) -> Unit
) :
    RecyclerView.Adapter<RateAdapter.VH>() {
    var editMode: Boolean = false;
    private var selectedPosition: Int? = null
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): VH {
        return VH(ItemRateBinding.inflate(LayoutInflater.from(p0.context), p0, false))
    }

    override fun getItemCount(): Int {
        return list.count()
    }

    override fun onBindViewHolder(p0: VH, p1: Int) {
        p0.bindData(list[p1], p1)
    }

    inner class VH(private var binding: ItemRateBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(body: CurrencyRate.Body, p1: Int) {
            binding.apply {
                currencyName.text = body.ccyNbrEng
                rate.text = "现汇卖出价:${body.rthOfr}\n现钞卖出价:${body.rtcOfr}\n现汇买入价:${body.rthBid}\n现钞买入价:${body.rtcBid}"
                root.setOnClickListener {
                    if (!editMode) {
                        return@setOnClickListener
                    }
                    onItemClick.invoke(body.ccyNbr)
                    tempCurrency = body.ccyNbr
                    selectedPosition?.let {
                        notifyItemChanged(it)
                    }
                    notifyItemChanged(p1)
                }
                val selected = body.ccyNbr == (tempCurrency ?: RateTask.selectedCurrency)
                if (selected) {
                    selectedPosition = p1
                }
                root.isSelected = selected
            }
        }
    }
}


