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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*

class MainActivity : AppCompatActivity(), AnkoLogger {

    val number: String
        get() = numberTextView.text.toString()

    var backspaceCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dialerView.numberConfirmed += {
            backspaceCounter = 0
            numberTextView.text.append(it.toString())
        }
        backspace.onClick {
            onBackspaceClicked()
        }
        backspace.onLongClick {
            onBackspaceLongClicked()
            true
        }
        callButton.onClick {
            callButtonClicked()
        }
    }

    private fun callButtonClicked() {
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);
        when(permissionCheck) {
            PackageManager.PERMISSION_GRANTED ->
                    makeCall(number)
            PackageManager.PERMISSION_DENIED ->
                    requestCallPermission()
        }
    }

    private val requestCallPermissionCode = 0
    private fun requestCallPermission() {
        ActivityCompat.requestPermissions(this,
                                          arrayOf(Manifest.permission.CALL_PHONE),
                                          requestCallPermissionCode)
    }

    override fun onRequestPermissionsResult(requestCode:  Int,
                                            permissions:  Array<out String>,
                                            grantResults: IntArray) {
        if(requestCode == requestCallPermissionCode) {
            if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, yay!
                makeCall(number)
            } else {
                // Permission not granted. Well, just open the default dialer then.
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:" + number)
                startActivity(intent)
            }
        }
    }

    private fun onBackspaceLongClicked() {
        vibrator.vibrate(50)
        numberTextView.text.clear()
        // todo: remember that the user learned to use the hold function
    }

    private fun onBackspaceClicked() {
        if (number.isNotEmpty()) {
            val old = number
            numberTextView.text.clear()
            numberTextView.text.append(old.substring(0..old.length - 2))
        }
        if (++backspaceCounter >= MIN_BACKSPACE_HINT_TRIGGER
                && number.length <= CHARS_LEFT_HINT_TRIGGER) {
            // todo restrict this so the user doesn't get spammed if he doesn't get the hint immediately or chooses to ignore it
            toast(R.string.hold_backspace_hint)
        }
    }

    companion object {
        const val MIN_BACKSPACE_HINT_TRIGGER = 3
        const val CHARS_LEFT_HINT_TRIGGER = 3
    }
}
