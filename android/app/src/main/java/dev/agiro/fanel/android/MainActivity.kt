package dev.agiro.fanel.android

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.agiro.fanel.android.sync.CalendarSyncScheduler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<CalendarViewModel> {
        CalendarViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CalendarSyncScheduler.enqueuePeriodic(this, BuildConfig.DEFAULT_HOUSEHOLD_ID)

        val statusView = TextView(this)
        val refreshButton = Button(this).apply {
            text = getString(R.string.sync_now)
            setOnClickListener { viewModel.syncNow() }
        }
        val sampleButton = Button(this).apply {
            text = getString(R.string.create_sample_event)
            setOnClickListener { viewModel.createSampleEvent() }
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(statusView)
            addView(refreshButton)
            addView(sampleButton)
        }
        setContentView(container)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    statusView.text = buildString {
                        appendLine(getString(R.string.household_status, state.householdId.ifBlank { getString(R.string.configure_household_id) }))
                        appendLine(getString(R.string.visible_events_status, state.events.size))
                        appendLine(getString(R.string.last_sync_status, state.lastSyncLabel))
                    }
                }
            }
        }
    }
}
