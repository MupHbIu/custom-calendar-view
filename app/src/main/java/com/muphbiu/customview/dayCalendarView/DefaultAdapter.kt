package com.muphbiu.customview.dayCalendarView

import java.util.*
import kotlin.collections.HashMap

class DefaultAdapter: DayCalendarView.Adapter() {


    override fun composeEvents(events: List<DayCalendarView.Event>): Map<Int, List<DayCalendarView.Event>> {
        if (events.isEmpty()) return emptyMap()
        if (events.size == 1) return mapOf(0 to events)

        val sortedEvents = events.sortedBy { it.startTime }
        val resultMap: MutableMap<Int, MutableList<DayCalendarView.Event>> = HashMap()

        for (index in sortedEvents.indices) {
            val event = sortedEvents[index]
            var column = 0

            while (true) {
                val eventList = getListByColumn(resultMap, column)
                if (canAddEventToList(event, eventList)) {
                    eventList.add(event)
                    break
                } else {
                    column++
                }
            }
        }

        return resultMap
    }

    private fun getListByColumn(map: MutableMap<Int, MutableList<DayCalendarView.Event>>, column: Int): MutableList<DayCalendarView.Event> =
        map[column] ?: let {
            map[column] = LinkedList()
            map[column]!!
        }

    private fun canAddEventToList(event: DayCalendarView.Event, list: List<DayCalendarView.Event>): Boolean {
        for (currentEvent in list) {
            if (event.isIntersect(currentEvent)) {
                return false
            }
        }
        return true
    }

    private fun DayCalendarView.Event.isIntersect(other: DayCalendarView.Event): Boolean =
        startTime in other.startTime..other.endTime
}