package com.example.atlantvpn

internal const val MIN_INTERFACE_SCALE=.8f
internal const val MAX_INTERFACE_SCALE=1.25f
internal fun normalizedInterfaceScale(value:Float):Float =
    if(value.isFinite())value.coerceIn(MIN_INTERFACE_SCALE,MAX_INTERFACE_SCALE) else 1f
