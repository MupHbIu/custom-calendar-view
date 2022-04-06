package com.muphbiu.customview

import com.muphbiu.customview.dayCalendarView.DayCalendarView.Event
import com.muphbiu.customview.dayCalendarView.DayCalendarViewData
import com.muphbiu.customview.dayCalendarView.DefaultAdapter
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    val events: MutableMap<Int, MutableList<Event>> = hashMapOf(
        0 to mutableListOf(
            Event(1, "1", 0, 500),
            Event(2, "2", 3000, 4000)
        ),
        1 to mutableListOf(
            Event(3, "3", 0, 1800),
            Event(4, "4", 2200, 4500)
        ),
        2 to mutableListOf(
            Event(5, "5", 0, 1000),
        ),
        3 to mutableListOf(
            Event(6, "6", 4000, 5000),
        ),
        4 to mutableListOf(
            Event(7, "7", 0, 1000),
        )
    )

    val testList = listOf(
        Event(0, "0", 1500, 2500),
        Event(1, "1", 1500, 2500),
        Event(2, "2", 1500, 2500),
        Event(3, "3", 1500, 2500),
        Event(4, "4", 1500, 2500),
        Event(5, "5", 1500, 2500)
    )

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun dayCalendarTest() {
        val defAdapter = DefaultAdapter()

        defAdapter.setEvents(testList)

        DayCalendarViewData.testEvents.forEach { entry ->
            print("${entry.key} : ")
            entry.value.forEach {
                print("$it ; ")
            }
            println("")
        }
    }
}