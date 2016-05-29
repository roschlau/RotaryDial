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
 * A class representing a C#-style event that can be subscribed to.
 * You can subscribe to an event by adding a function to it that takes exactly one parameter and
 * returns Unit.
 * An Event can be triggered by calling it directly as a function.
 * ```kotlin
 * val messageReceived = Event<String>()
 * messageReceived += { message -> print(message) }
 * messageReceived("Message 1") // will print "Message 1"
 * ```
 */
open class Event<T> {

    private val handlers = mutableListOf<(T) -> Unit>()

    /**
     * Adds a new handler to the event.
     */
    open operator fun plusAssign(handler: (T) -> Unit) {
        handlers.add(handler)
    }

    /**
     * Triggers the event and calls all registered event handlers. This call is blocking and will
     * not return until all handlers were called! Might be changed in the future, but it's good for
     * now as anko's async{ uiThread{ } } had some problems.
     */
    open operator fun invoke(value: T) {
        // Creating a copy of the list before iterating to prevent concurrent modification issues
        handlers.toList().forEach {
            it(value)
        }
    }
}