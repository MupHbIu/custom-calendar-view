package com.muphbiu.customview.dayCalendarView

import android.graphics.Color
import com.muphbiu.customview.dayCalendarView.DayCalendarView.Event

class DayCalendarViewData {

    companion object {
        // TEST EVENTS
        var testEvents = hashMapOf(
            0 to mutableListOf(
                Event(1, "1", 0, 30),
                Event(1, "1", 30, 60, Color.RED),
                Event(1, "1", 60, 90),
                Event(2, "2", 60*3, 60*5),
                Event(8, "8", 60*22, 60*23),
                Event(8, "8", 60*23, 60*24)
            ),
            1 to mutableListOf(
                Event(3, "3", 0, 60*2),
                Event(4, "4", (60*2.5).toInt(), 60*4),
            ),
            2 to mutableListOf(
                Event(5, "5", 0, 60*2),
            ),
            3 to mutableListOf(
                Event(6, "6", 60*10, 60*13),
            ),
            4 to mutableListOf(
                Event(7, "7", 60*5, 60*10),
            )
        )
    }


}