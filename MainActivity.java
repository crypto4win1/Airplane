package com.example.airplanetoggle;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private EditText intervalInput;
    private Button startButton;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(24);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("وضع الطيارة المتكرر");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(0));

        TextView label = new TextView(this);
        label.setText("الوقت بالثواني بين كل نقرة");
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        root.addView(label, matchWrap(dp(24)));

        intervalInput = new EditText(this);
        intervalInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        intervalInput.setSingleLine(true);
        intervalInput.setText("20");
        intervalInput.setGravity(Gravity.CENTER);
        root.addView(intervalInput, matchWrap(dp(8)));

        startButton = new Button(this);
        startButton.setText("ابدأ");
        startButton.setOnClickListener(v -> onStartStopClicked());
        root.addView(startButton, matchWrap(dp(24)));

        statusText = new TextView(this);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText, matchWrap(dp(16)));

        TextView hint = new TextView(this);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        hint.setGravity(Gravity.CENTER);
        hint.setText("ملاحظة: تغيير وضع الطيارة يحتاج صلاحية النظام WRITE_SECURE_SETTINGS "
                + "(تُمنح مرة واحدة عبر adb) أو هاتف مروّت (root). راجع ملف README.");
        root.addView(hint, matchWrap(dp(24)));

        setContentView(root);
        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void onStartStopClicked() {
        if (AirplaneService.running) {
            startService(new Intent(this, AirplaneService.class)
                    .setAction(AirplaneService.ACTION_STOP));
            startButton.postDelayed(this::refreshUi, 500);
            return;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(intervalInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            seconds = 0;
        }
        if (seconds < 1) {
            statusText.setText("أدخل رقماً أكبر من 0");
            return;
        }

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        Intent intent = new Intent(this, AirplaneService.class)
                .putExtra(AirplaneService.EXTRA_INTERVAL, seconds);
        startForegroundService(intent);
        startButton.postDelayed(this::refreshUi, 500);
    }

    private void refreshUi() {
        if (AirplaneService.running) {
            startButton.setText("إيقاف");
            statusText.setText("يعمل: يتم التبديل كل " + intervalInput.getText() + " ثانية");
            intervalInput.setEnabled(false);
        } else {
            startButton.setText("ابدأ");
            statusText.setText("متوقف");
            intervalInput.setEnabled(true);
        }
    }

    private LinearLayout.LayoutParams matchWrap(int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topMargin;
        return lp;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
