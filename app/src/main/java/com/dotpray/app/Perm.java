package com.dotpray.app;

/**
 * Permission strings that are not present in the API 26 SDK.
 *
 * <p>{@code android.Manifest.permission.POST_NOTIFICATIONS} only exists from
 * API 33, so the literal is kept here instead. Every use site is guarded by
 * a {@code Build.VERSION.SDK_INT} check.
 */
public final class Perm {

    public static final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";

    private Perm() {
    }
}
