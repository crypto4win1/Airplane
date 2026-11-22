package com.example.airplanetoggle;

import android.content.Context;
import android.provider.Settings;

import java.io.DataOutputStream;

/** Reads and changes the airplane mode state. */
final class AirplaneController {

    private static final String KEY = "airplane_mode_on";
    private final Context context;

    AirplaneController(Context context) {
        this.context = context.getApplicationContext();
    }

    boolean isOn() {
        return Settings.Global.getInt(context.getContentResolver(), KEY, 0) == 1;
    }

    /**
     * Sets airplane mode. Returns true when the system reports the requested state.
     * Direct write needs WRITE_SECURE_SETTINGS (granted via adb).
     * Otherwise the change is attempted through root (su).
     */
    boolean set(boolean on) {
        int value = on ? 1 : 0;
        try {
            Settings.Global.putInt(context.getContentResolver(), KEY, value);
        } catch (SecurityException ignored) {
            // Permission not granted; root fallback below.
        }
        if (isOn() == on) {
            // Send the broadcast too, so the radios and system UI react. Root only.
            runAsRoot("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state " + on);
            return true;
        }
        runAsRoot("settings put global " + KEY + " " + value
                + " && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state " + on);
        return isOn() == on;
    }

    private static boolean runAsRoot(String command) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            try (DataOutputStream os = new DataOutputStream(process.getOutputStream())) {
                os.writeBytes(command + "\nexit\n");
                os.flush();
            }
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
