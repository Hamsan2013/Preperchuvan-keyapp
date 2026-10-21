package com.preperchuvan.keyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PreperchuvanIME extends InputMethodService {

    private Typeface customFont;
    private Vibrator vibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        try {
            // Must place copies in android/app/src/main/assets/fonts/
            customFont = Typeface.createFromAsset(getAssets(), "fonts/MyCustomFont.ttf");
        } catch (Exception e) {
            Log.e("PreperchuvanIME", "Font not found. Check assets/fonts/MyCustomFont.ttf");
            customFont = Typeface.DEFAULT;
        }
    }

    @Override
    public View onCreateInputView() {
        // Create root layout
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#ECEFF1"));
        root.setPadding(0, 10, 0, 10);

        try {
            // Load JSON layout (Must place copy in android/app/src/main/assets/config/)
            InputStream is = getAssets().open("config/keyboard-layout.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);

            JSONObject config = new JSONObject(jsonStr);
            JSONObject layout = config.getJSONObject("layout");

            // Row 1, 2, 3
            root.addView(createRow(layout.getJSONArray("row1")));
            root.addView(createRow(layout.getJSONArray("row2")));
            root.addView(createRow(layout.getJSONArray("row3")));

        } catch (Exception e) {
            Log.e("PreperchuvanIME", "Failed to load JSON layout", e);
        }

        // Bottom Action Row (Row 4)
        root.addView(createActionRow());

        return root;
    }

    private LinearLayout createRow(JSONArray keys) throws Exception {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        for (int i = 0; i < keys.length(); i++) {
            String hexCode = keys.getString(i);
            if (!hexCode.isEmpty()) {
                int codePoint = Integer.parseInt(hexCode, 16);
                String label = new String(Character.toChars(codePoint));
                row.addView(createKeyButton(label, label));
            }
        }
        return row;
    }

    private LinearLayout createActionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        row.addView(createSpecialButton("🌐", () -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
            }
        }));

        row.addView(createSpecialButton("SPACE", () -> commitText(" ")));
        row.addView(createSpecialButton("⌫", this::handleBackspace));
        row.addView(createSpecialButton("↵", this::handleEnter));

        return row;
    }

    private Button createKeyButton(String label, final String outputText) {
        Button btn = new Button(new ContextThemeWrapper(this, R.style.KeyButtonStyle), null, 0);
        btn.setText(label);
        btn.setTypeface(customFont);
        
        // Define layout weight
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 150, 1.0f);
        params.setMargins(4, 4, 4, 4);
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> {
            vibrate();
            commitText(outputText);
        });
        return btn;
    }

    private Button createSpecialButton(String label, Runnable action) {
        Button btn = new Button(new ContextThemeWrapper(this, R.style.KeyButtonStyle), null, 0);
        btn.setText(label);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 150, 1.5f);
        if (label.equals("SPACE")) params.weight = 3.0f; // Spacebar is wider
        
        params.setMargins(4, 4, 4, 4);
        btn.setLayoutParams(params);
        btn.setBackgroundColor(Color.parseColor("#CFD8DC"));

        btn.setOnClickListener(v -> {
            vibrate();
            action.run();
        });
        return btn;
    }

    private void commitText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
        }
    }

    private void handleBackspace() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence selectedText = ic.getSelectedText(0);
            if (selectedText == null || selectedText.length() == 0) {
                ic.deleteSurroundingText(1, 0);
            } else {
                ic.commitText("", 1);
            }
        }
    }

    private void handleEnter() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        }
    }

    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(20);
            }
        }
    }
          }
                  
