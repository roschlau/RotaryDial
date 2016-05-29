/*
 * Copyright (C) 2016  Robin Roschlau
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rrpictureproductions.rotarydialer

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import org.jetbrains.anko.*

/**
 * A View displaying a classical rotary dial interface.
 */
class RotaryDialerView : View, AnkoLogger {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {init(attrs)}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context,
                                                                                  attrs,
                                                                                  defStyleAttr) {init(attrs, defStyleAttr)}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
            context,
            attrs,
            defStyleAttr,
            defStyleRes) {init(attrs, defStyleAttr, defStyleRes)}

    private fun init(attrs: AttributeSet, defStyleAttr: Int = 0, defStyleRes: Int = 0) {
        val a = context.theme.obtainStyledAttributes(attrs,
                                                     R.styleable.RotaryDialerView,
                                                     defStyleAttr, defStyleRes)
        try {
            foregroundColor = a.getColor(R.styleable.RotaryDialerView_dialColor,
                                         ContextCompat.getColor(context, R.color.colorPrimary))
            textColor = a.getColor(R.styleable.RotaryDialerView_textColor, Color.BLACK)
        } finally {
            a.recycle()
        }
    }

    val numberSelected = Event<Int>()
    val numberConfirmed = Event<Int>()

    var curRotation = 0.toFloat()
        private set

    private val outerPadding = dip(20)
    private val innerPadding = dip(15)

    private var center          = Point(0f, 0f)
    private var radius          = 0f
    private var numberRadius    = 0.0
    private var numberDistance  = 0.0

    private val numbers = (0..9).toSet()
    private val numberPositions: Map<Int, Point>
        get() = numbers.associate { it to getNumberPosition(it) }

    private var foregroundColor = ContextCompat.getColor(context, R.color.colorPrimary)
        set(value) {
            field = value
            foregroundPaint.color = field
        }
    private val foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = foregroundColor
        style = Paint.Style.FILL
    }

    private var textColor = Color.BLACK
        set(value) {
            field = value
            textPaint.color = field
        }
    private val textSize_ = sp(24).toFloat()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = textSize_
    }

    private val eraser = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private lateinit var osBitmap: Bitmap
    private lateinit var osCanvas: Canvas

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Account for padding
        val xpad = (paddingLeft + paddingRight).toFloat();
        val ypad = (paddingTop + paddingBottom).toFloat();

        val ww = w - xpad;
        val hh = h - ypad;

        center = Point(width / 2.toFloat(), height / 2.toFloat())

        // Figure out how big we can make the dialer.
        radius = Math.min(ww, hh) / 2;
        // Calculate the radius of the number holes.
        // So Math, much complicated, wow.
        numberRadius = (Math.PI*radius - Math.PI*outerPadding - 6*innerPadding) / (12 + Math.PI)
        // Calculate the distance of the center of the number holes from the center of the dials
        numberDistance = radius - numberRadius - outerPadding

        redrawOffscreenBitmap()
    }

    private fun redrawOffscreenBitmap() {
        osBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        osCanvas = Canvas(osBitmap)

        osCanvas.drawCircle(center.x, center.y, radius, foregroundPaint)

        numbers.forEach {
            // Draw holes
            val (x, y) = getNumberPosition(it)
            // if(it == 1) info { "1 at ($x|$y)" }
            osCanvas.drawCircle(x, y,
                                numberRadius.toFloat(),
                                eraser)
        }
    }

    private var fingerId: Int? = null
    private var firstTouch: Point? = null
    private var curTouch: Point? = null
    private var selectedNumber: Int? = null
        set(value) {
            field = value
            value?.let { numberSelected(value) }
        }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action) {
            MotionEvent.ACTION_DOWN -> if(fingerId == null) onTouchDown(event)
            MotionEvent.ACTION_MOVE -> if(fingerId != null) onTouchMove(event)
            MotionEvent.ACTION_UP   -> onTouchUp()
        }
        return true
    }

    private fun onTouchDown(event: MotionEvent) {
        val touch = Point(event.x, event.y)
        selectedNumber = getNumberAt(touch)
        if(selectedNumber != null) {
            info { "Down on $selectedNumber" }
            fingerId = event.getPointerId(0)
            firstTouch = Point(event.x, event.y)
            curTouch = firstTouch?.copy()
        } else {
            curTouch = null
        }
    }

    private fun onTouchMove(event: MotionEvent) {
        curTouch = Point(event.x, event.y)
        val newAngle = center.getAngleWith(firstTouch!!, curTouch!!)
        if (curRotation < newAngle && (newAngle - curRotation) < MAX_ROTATION_STEP_DISTANCE) {
            curRotation = newAngle
            if (newAngle >= getNumberMaxDegrees(selectedNumber!!)) {
                confirmNumber()
            }
            invalidate()
        }
    }

    private fun confirmNumber() {
        context.vibrator.vibrate(VIBRATION_LENGTH)
        info { "Number confirmed: $selectedNumber" }
        numberConfirmed(selectedNumber!!)
        fingerId = null
        rotateBack()
    }

    private var animator: ValueAnimator? = null

    private fun rotateBack() {
        if(animator != null) return
        animator = ValueAnimator.ofFloat(curRotation, 0f).apply {
            duration = 3L * curRotation.toLong() + 100L
            addUpdateListener { animator ->
                curRotation = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator) { }
                override fun onAnimationCancel(animation: Animator) { }
                override fun onAnimationStart(animation: Animator) { }
                override fun onAnimationEnd(animation: Animator) {
                    animator = null
                }
            })
            start()
        }
    }

    private fun onTouchUp() {
        fingerId = null
        if(curRotation != 0f) rotateBack()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        numbers.forEach {
            // Draw Numbers
            val (tx, ty) = getNumberPosition(it)
            val str = it.toString()
            canvas.drawTextCentered(str, tx, ty, textPaint)
        }
        canvas.rotate(curRotation, center.x, center.y)
        canvas.drawBitmap(osBitmap, 0f, 0f, null)
    }

    private fun getNumberAt(point: Point) =
            numberPositions.filter { it.value.distanceTo(point) <= numberRadius }.keys.firstOrNull()

    private fun getNumberPosition(n: Int) = center.translate(numberDistance,
                                                             getNumberDegrees(n))

    private fun getNumberMaxDegrees(n: Int): Int = when(n) {
        0    -> getNumberMaxDegrees(10)
        else -> (1 + n) * STEP_SIZE
    }

    fun getNumberDegrees(n: Int): Double = when(n) {
        0    -> getNumberDegrees(10)
        else -> (4.5 * STEP_SIZE + (n-1) * -STEP_SIZE + 180) % 360.0
    }

    companion object {
        const val STEP_SIZE = 360 / 12
        const val MAX_ROTATION_STEP_DISTANCE = 100
        const val VIBRATION_LENGTH = 50L
    }
}