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

/**
 * A class representing the 2D point at the coordinates [x] and [y].
 * The class offers some functions for translating points in different ways.
 */
data class Point(val x: Float, val y: Float) {

    /**
     * Calculates the angle formed by drawing lines from this [Point] to two other ones.
     */
    fun getAngleWith(p2: Point, p3: Point): Float {
        val p1 = this
        val a = p3.relativeTo(p1)
        val b = p2.relativeTo(p1)
        return (atan2(a.y, a.x) - atan2(b.y, b.x)).toDegrees().let { if (it >= 0) it else it + 360 }
    }

    /**
     * Returns a new [Point] whose relative position to (0|0) is the same as this [Point]'s relative
     * position to [other]
     */
    fun relativeTo(other: Point) = Point(x - other.x, y - other.y)

    /**
     * Returns the length of the straight line between this [Point] and [other]
     */
    fun distanceTo(other: Point) = sqrt(sqr(x - other.x) + sqr(y - other.y))

    /**
     * Returns a new [Point] that represents the translation of this [Point] by moving it [distance]
     * Units in the direction given by [angle].
     */
    fun translate(distance: Double, angle: Double): Point {
        val a = Math.toRadians(angle)
        val x = distance * Math.cos(a) + x
        val y = distance * Math.sin(a) + y
        return Point(x.toFloat(), y.toFloat())
    }

    private fun sqr(n: Float) = n * n
    private fun sqrt(f: Float) = Math.sqrt(f.toDouble())
    private fun atan2(y: Float, x: Float) = Math.atan2(y.toDouble(), x.toDouble())
    private fun Double.toDegrees() = Math.toDegrees(this).toFloat()
}