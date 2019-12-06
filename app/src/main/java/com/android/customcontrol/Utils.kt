package com.android.customcontrol

import android.util.Log
import android.view.MotionEvent
import kotlin.math.pow

fun MotionEvent.inCircle(cx: Int, cy: Int, r: Int) = (x - cx).pow(2) + (y - cy).pow(2) < (r * r)

fun log(s: String) = Log.d("Controller", s)

fun <T, V> T?.retrieveAttrOrDefault(default: V, attr: T.(V) -> V) = if (this == null) default else attr(default)
