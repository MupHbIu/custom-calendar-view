package com.muphbiu.customview.dayCalendarView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.annotation.*
import com.muphbiu.customview.R
import com.muphbiu.customview.dayCalendarView.DayCalendarView.Event.Companion.END_DAY_TIME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Для более удобного использования необходимо добавить методы изменения параметров стиля для
// возможности программно кастомизировать DayCalendarView
// Для расширения возможностей использования DayCalendarView, необходимо конкретный класс Event
// заменить на generic и переместить логику получения данных в адаптер
// (для чего его и необходимо будет переопределять с указанием типа и реализацией
// алгоритма компоновки данных для указанного типа)

/**
 * Данный класс позволяет отображать события в зависимости от времени начала и окончания события
 */
class DayCalendarView(
    context: Context,
    attributeSet: AttributeSet?,
    @AttrRes defStyleAttr: Int,
    @StyleRes defStyleRes: Int
) : View(context, attributeSet, defStyleAttr, defStyleRes) {

    private companion object DefaultValues {
        const val TITLE_HEIGHT = R.dimen.dayCalendarTitleHeight
        const val TIME_LINE_WIDTH = R.dimen.dayCalendarTimeLineWidth
        const val TITLE_BACKGROUND_COLOR = R.color.dayCalendarTitleBackgroundColor
        const val TITLE_TEXT_COLOR = R.color.dayCalendarTitleTextColor
        const val TITLE_TEXT_SIZE = R.dimen.dayCalendarTitleTextSize
        const val EVENT_BACKGROUND_DEFAULT_COLOR = R.color.dayCalendarEventBackgroundDefaultColor
        const val EVENT_TEXT_COLOR = R.color.dayCalendarEventTextColor
        const val EVENT_TEXT_SIZE = R.dimen.dayCalendarEventTextSize
        const val EVENT_CORNER_RADIUS = R.dimen.dayCalendarEventCornerRadius
        const val TIME_LINE_COLOR = R.color.dayCalendarTimeLineColor
        const val TIME_LINE_SIZE = R.dimen.dayCalendarTimeLineSize
        const val MIN_EVENT_WIDTH = R.dimen.dauCalendarMinEventWidth
        const val MIN_TITLE_WIDTH = R.dimen.dayCalendarMinTitleWidth
        const val MIN_TIME_LINE_BLOCK_HEIGHT = END_DAY_TIME.toFloat()
    }

    /**
     * Класс данных, используемый для отображения событий в [DayCalendarView]
     */
    data class Event(
        val id: Long,
        val text: String,
        val startTime: Int,
        val endTime: Int,
        @ColorInt val color: Int? = null
    ) {
        companion object {
            const val START_DAY_TIME = 0
            const val END_DAY_TIME = 60 * 24
        }
    }

    /**
     * Интерфейс для обратного вызова, который вызывается при клике на событие.
     */
    interface OnEventClickListener {
        fun onClick(event: Event)

        companion object {
            operator fun invoke(action: (Event) -> Unit) = object : OnEventClickListener {
                override fun onClick(event: Event) = action(event)
            }
        }
    }

    /**
     * Базовый класс Адаптера
     *
     * Адаптеры обеспечивают привязку набора данных к их отображению в [DayCalendarView].
     * Так же, адаптеры позволяют определить свой алгоритм компоновки данных для отображения.
     */
    abstract class Adapter {
        companion object {
            val DEFAULT = DefaultAdapter()
        }

        var data: Map<Int, List<Event>> = HashMap()
            private set

        val columns: Int get() = data.keys.size
        var title: String = ""
            set(value) {
                field = value
                updateListeners()
            }

        private val listeners: MutableSet<(Adapter) -> Unit> = mutableSetOf()

        fun addOnEvensChangeListener(listener: (Adapter) -> Unit) {
            listeners.add(listener)
        }

        fun removeOnEvensChangeListener(listener: (Adapter) -> Unit) {
            listeners.remove(listener)
        }

        private fun updateListeners() = CoroutineScope(Dispatchers.Main).launch {
            listeners.forEach {
                it?.invoke(this@Adapter)
            }
        }

        fun setEvents(events: Map<Int, List<Event>>) {
            data = events
            updateListeners()
        }

        fun setEvents(events: List<Event>) {
            CoroutineScope(Dispatchers.Default).launch {
                val composedEvents = composeEvents(events)
                withContext(Dispatchers.Main) {
                    setEvents(composedEvents)
                }
            }
        }

        protected abstract fun composeEvents(events: List<Event>): Map<Int, List<Event>>
    }

    var adapter: Adapter = Adapter.DEFAULT
        set(value) {
            field.removeOnEvensChangeListener(onEventsChangeListener)
            field = value
            updateViewSize()
            invalidate()
        }

    private val onEventsChangeListener: (Adapter) -> Unit = {
        updateViewSize()
        invalidate()
    }

    private var listener: OnEventClickListener? = null

    fun setOnEventClickListener(l: OnEventClickListener?) { listener = l }

    private var currentPosition: Pair<Float, Float> = Pair(0f, 0f)

    // Values
    private var titleHeight = resources.getDimension(TITLE_HEIGHT)
    private var timeLineWidth = resources.getDimension(TIME_LINE_WIDTH)
    @ColorInt private var titleBackgroundColor: Int = context.getColor(TITLE_BACKGROUND_COLOR)
    @ColorInt private var titleTextColor: Int = context.getColor(TITLE_TEXT_COLOR)
    private var titleTextSize = resources.getDimension(TITLE_TEXT_SIZE)
    @ColorInt private var eventTextColor: Int = context.getColor(EVENT_TEXT_COLOR)
    @ColorInt private var eventBackgroundColor: Int = context.getColor(EVENT_BACKGROUND_DEFAULT_COLOR)
    private var eventTextSize = resources.getDimension(EVENT_TEXT_SIZE)
    private var eventCornerRadius = resources.getDimension(EVENT_CORNER_RADIUS)
    @ColorInt private var timeLineColor: Int = context.getColor(TIME_LINE_COLOR)
    private var timeLineSize = resources.getDimension(TIME_LINE_SIZE)

    // Draw params
    private val boardRect = RectF()
    private val eventBoardRect = RectF()
    private val drawRect = RectF()
    private var eventWidth: Float = 0f
    private var eventPaddingVertical: Float = 15f
    private var eventPaddingHorizontal: Float = 5f

    private var oneMinuteSize: Float = 1f

    // Paints
    private val titleBgPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = titleBackgroundColor
        style = Paint.Style.FILL
    }
    private val eventBgPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = eventBackgroundColor
        style = Paint.Style.FILL
    }
    private val textPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = titleTextColor
        textAlign = Paint.Align.CENTER
        textSize = titleTextSize
    }
    private val timeLinePaint: Paint = Paint().apply {
        color = timeLineColor
        strokeWidth = timeLineSize
    }

    constructor(context: Context, attributeSet: AttributeSet?, @AttrRes defStyleAttr: Int): this(context, attributeSet, defStyleAttr, R.style.DefaultDayCalendarStyle)
    constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, R.attr.dayCalendarStyle)
    constructor(context: Context): this(context, null)

    init {
        if (attributeSet != null) {
            initAttributes(attributeSet, defStyleAttr, defStyleRes)
        }
        if (isInEditMode) {
            adapter = Adapter.DEFAULT.apply {
                // events: Map<Int, List<DayCalendarView.Event>> or List<DayCalendarView.Event>
                setEvents(DayCalendarViewEditModeData.events)
                title = "S U N D A Y"
            }
        }
    }

    private fun initAttributes(attributeSet: AttributeSet, defStyleAttr: Int, defStyleRes: Int) {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.DayCalendarView, defStyleAttr, defStyleRes)

        titleBackgroundColor = typedArray.getColor(R.styleable.DayCalendarView_titleBackgroundColor, titleBackgroundColor)
        titleTextColor = typedArray.getColor(R.styleable.DayCalendarView_titleTextColor, titleTextColor)
        titleTextSize = typedArray.getDimension(R.styleable.DayCalendarView_titleTextSize, titleTextSize)
        eventBackgroundColor = typedArray.getColor(R.styleable.DayCalendarView_eventBackgroundDefaultColor, eventBackgroundColor)
        eventTextColor = typedArray.getColor(R.styleable.DayCalendarView_eventTextColor, eventTextColor)
        eventTextSize = typedArray.getDimension(R.styleable.DayCalendarView_eventTextSize, eventTextSize)
        eventCornerRadius = typedArray.getDimension(R.styleable.DayCalendarView_eventCornerRadius, eventCornerRadius)
        timeLineColor = typedArray.getColor(R.styleable.DayCalendarView_timeLineColor, timeLineColor)
        timeLineSize = typedArray.getDimension(R.styleable.DayCalendarView_timeLineSize, timeLineSize)

        typedArray.recycle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        adapter.addOnEvensChangeListener(onEventsChangeListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        adapter.removeOnEvensChangeListener(onEventsChangeListener)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val minHeight = suggestedMinimumHeight + paddingTop + paddingBottom

        val minTimeLineBlockHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            MIN_TIME_LINE_BLOCK_HEIGHT,
            resources.displayMetrics
        ).toInt()
        val minTitleWidthPx = resources.getDimension(MIN_TITLE_WIDTH).toInt()
        val minEventWidthPx = resources.getDimension(MIN_EVENT_WIDTH).toInt()

        val desiredWidth = maxOf(minWidth,
            paddingLeft + paddingRight + minTitleWidthPx,
            paddingLeft + paddingRight + timeLineWidth.toInt() + (adapter.columns) * minEventWidthPx
        )
        val desiredHeight = maxOf(minHeight,
            titleHeight.toInt() + minTimeLineBlockHeightPx + paddingTop + paddingBottom
        )

        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateViewSize()
    }

    private fun updateViewSize() {
        boardRect.left = paddingLeft.toFloat()
        boardRect.top = paddingTop.toFloat()
        boardRect.right = width - paddingRight.toFloat()
        boardRect.bottom = height - paddingBottom.toFloat()

        eventBoardRect.left = boardRect.left + timeLineWidth
        eventBoardRect.top = boardRect.top + titleHeight
        eventBoardRect.right = boardRect.right
        eventBoardRect.bottom = boardRect.bottom

        eventWidth = (boardRect.left + boardRect.right - timeLineWidth) / adapter.columns
        eventPaddingVertical = (eventBoardRect.bottom - eventBoardRect.top) / END_DAY_TIME
        eventPaddingHorizontal = (boardRect.left + boardRect.right - timeLineWidth) * 0.001f

        oneMinuteSize = (eventBoardRect.bottom - eventBoardRect.top) / END_DAY_TIME
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackground(canvas)
        drawEvents(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        titleBgPaint.color = titleBackgroundColor
        drawTimeLines(canvas)
        drawTitle(canvas)
    }

    private fun drawTimeLines(canvas: Canvas) {
        timeLinePaint.apply {
            color = timeLineColor
            strokeWidth = timeLineSize
        }
        drawRect.apply {
            left = boardRect.left
            top = boardRect.top
            right = boardRect.left + timeLineWidth
            bottom = boardRect.bottom
        }
        canvas.drawRect(drawRect, titleBgPaint)

        val lineStartX = boardRect.left
        val lineEndX = boardRect.left + timeLineWidth
        var currentLine = boardRect.top + titleHeight
        val lineBottom = boardRect.bottom
        val step = (lineBottom - currentLine) / END_DAY_TIME * 30
        while(currentLine <= lineBottom) {
            canvas.drawLine(lineStartX, currentLine, lineEndX, currentLine, timeLinePaint)
            currentLine += step
        }
    }

    private fun drawTitle(canvas: Canvas) {
        textPaint.apply {
            color = titleTextColor
            textAlign = Paint.Align.CENTER
            textSize = titleTextSize
        }
        drawRect.apply {
            left = boardRect.left
            top = boardRect.top
            right = boardRect.right
            bottom = boardRect.top + titleHeight
        }
        canvas.drawRect(drawRect, titleBgPaint)
        val textPosY = ((drawRect.height() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2))
        val text = getAbbreviatedText(adapter.title, drawRect.width())
        canvas.drawText(text, drawRect.centerX(), drawRect.top + textPosY, textPaint)
    }

    private fun getAbbreviatedText(text: String, width: Float): String {
        val numOfChars = textPaint.breakText(text, true, width, null)
        return if(text.length > numOfChars) {
             if(numOfChars <= 3) "" else text.substring(0, numOfChars - 3) + "..."
        } else text
    }

    private fun drawEvents(canvas: Canvas) {
        textPaint.apply {
            color = eventTextColor
            textAlign = Paint.Align.CENTER
            textSize = eventTextSize
        }

        for(column in adapter.data.keys) {
            val events = adapter.data[column] ?: continue
            for(event in events) {
                drawEvent(canvas, column, event)
            }
        }
    }

    private fun drawEvent(canvas: Canvas, column: Int, event: Event) {
        eventBgPaint.color = event.color ?: eventBackgroundColor

        drawRect.apply {
            left = eventBoardRect.left + column * eventWidth + eventPaddingHorizontal
            right = left + eventWidth - eventPaddingHorizontal * 2
            val topAndBottom = calculateEventHeight(eventBoardRect.top, event)
            top = topAndBottom.first
            bottom = topAndBottom.second
        }
        canvas.drawRoundRect(drawRect, eventCornerRadius, eventCornerRadius, eventBgPaint)

        val textPosY = drawRect.top - (textPaint.descent() + textPaint.ascent())
        val text = getAbbreviatedText(event.text, drawRect.width() - eventPaddingHorizontal * 2)
        canvas.drawText(
            text,
            drawRect.centerX(),
            textPosY + eventPaddingVertical * 10,
            textPaint
        )
    }

    private fun calculateEventHeight(
        boardTop: Float,
        event: Event
    ): Pair<Float, Float> {
        val actuallyTop = boardTop + event.startTime * oneMinuteSize
        val actuallyBoolean = boardTop + event.endTime * oneMinuteSize
        return Pair(actuallyTop + eventPaddingVertical, actuallyBoolean - eventPaddingVertical)
    }


    // Touch
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (adapter.data.isEmpty()) return false
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> updateCurrentPosition(event)
            MotionEvent.ACTION_MOVE -> updateCurrentPosition(event)
            MotionEvent.ACTION_UP -> performClick()
            else -> false
        }
    }

    private fun updateCurrentPosition(event: MotionEvent): Boolean {
        currentPosition = Pair(event.x, event.y)
        return event.x in boardRect.left..boardRect.right && event.y in boardRect.top..boardRect.bottom
    }

    override fun performClick(): Boolean {
        super.performClick()
        val event = getEventInColumn(currentPosition.second, getColumn(currentPosition.first))
        return if(event != null) {
            listener?.onClick(event)
            true
        } else {
            false
        }
    }

    private fun getColumn(positionX: Float): Int =
        ((positionX - eventBoardRect.left) / eventWidth).toInt()

    private fun getEventInColumn(positionY: Float, column: Int): Event? {
        val list = adapter.data[column] ?: return null
        return list.find {
            val topAndBottom = calculateEventHeight(eventBoardRect.top, it)
            positionY in topAndBottom.first..topAndBottom.second
        }
    }

}