package com.dotpray.app;

import android.graphics.Insets;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

/**
 * Code that only exists on Android 11 (API 30) and newer.
 *
 * <p>Android verifies a class when it is first loaded, so a reference to an
 * API 30 member inside {@link MainActivity} would make the whole activity
 * unresolvable on older devices. Isolating the calls in a separate class means
 * only that class is ever touched on API 30+, exactly like the AndroidX
 * "ApiXXImpl" pattern.
 */
public final class Api30 {

    private Api30() {
    }

    /** Lets the app draw behind the system bars. */
    public static void setDecorFitsSystemWindows(Window window) {
        window.setDecorFitsSystemWindows(false);
    }

    /** Switches the status and navigation bar icons between dark and light. */
    public static void setLightSystemBars(Window window, boolean light) {
        // getDecorView() installs the decor if it does not exist yet, so this
        // is safe even if a caller reaches it before setContentView().
        WindowInsetsController controller = window.getDecorView().getWindowInsetsController();
        if (controller == null) return;
        int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        controller.setSystemBarsAppearance(light ? mask : 0, mask);
    }

    /**
     * @param insets  the insets handed to the listener
     * @param out     a two element array that receives {top, bottom} in pixels
     */
    public static void systemBarInsets(WindowInsets insets, int[] out) {
        Insets bars = insets.getInsets(
                WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        out[0] = bars.top;
        out[1] = bars.bottom;
    }

    /**
     * Four-sided variant for screens that meet a cutout in landscape.
     *
     * @param out receives {left, top, right, bottom} in pixels
     */
    public static void systemBarInsets4(WindowInsets insets, int[] out) {
        Insets bars = insets.getInsets(
                WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        out[0] = bars.left;
        out[1] = bars.top;
        out[2] = bars.right;
        out[3] = bars.bottom;
    }
}
