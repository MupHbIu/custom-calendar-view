package com.muphbiu.customview

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.muphbiu.customview.databinding.ActivityMainBinding
import com.muphbiu.customview.dayCalendarView.DayCalendarView
import kotlinx.coroutines.delay
import kotlin.random.Random

class DayCalendarExampleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val onEventClickListener = DayCalendarView.OnEventClickListener {
        Snackbar.make(binding.root, it.text, Snackbar.LENGTH_SHORT).show()
    }
    private val events: MutableList<DayCalendarView.Event> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDayCalendarView()
        initTestEvents()
    }

    private fun initDayCalendarView() = with (binding.dayCalendarView) {
        adapter.title = "S U N D A Y"
        setOnEventClickListener(onEventClickListener)
    }

    private fun initTestEvents() {
        lifecycleScope.launchWhenStarted {
            repeat(15) {
                delay(500)
                events += getRandomEvent()
                binding.dayCalendarView.adapter.setEvents(events)
            }
        }
    }

    private fun getRandomEvent(): DayCalendarView.Event {
        val eventId = Random.nextLong(0, Long.MAX_VALUE)
        val eventText = Random.nextLong(0x000000000, 0xFFFFFFFFFFF).toString(10)
        val timeStart = Random.nextInt(DayCalendarView.Event.START_DAY_TIME, DayCalendarView.Event.END_DAY_TIME - 60)
        var timeEnd = 0
        while (timeEnd < timeStart + 30) {
            timeEnd = Random.nextInt(DayCalendarView.Event.START_DAY_TIME + 60, DayCalendarView.Event.END_DAY_TIME)
        }
        val color = Color.argb(255, getRandomColorInt(), getRandomColorInt(), getRandomColorInt())
        return DayCalendarView.Event(eventId, eventText, timeStart, timeEnd, color)
    }

    private fun getRandomColorInt(): Int = Random.nextInt(0, 255)
}