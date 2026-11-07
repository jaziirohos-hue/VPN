package com.xalane.vpn

/** Feature switches. Tests can change them before rendering. */
object AppFlags {
    /** Shows Demo locations and the Premium screen. False in release builds. */
    var demoContent: Boolean = BuildConfig.DEMO_CONTENT
}
