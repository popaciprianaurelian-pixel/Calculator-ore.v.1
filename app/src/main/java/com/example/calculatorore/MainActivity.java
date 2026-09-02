package com.example.calculatorore;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(40, 60, 40, 40);
        mainLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Calculator Ture Manevră");
        tvTitle.setTextSize(24);
        tvTitle.setTextColor(Color.BLACK);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, 40);
        mainLayout.addView(tvTitle);

        LinearLayout presetLayout = new LinearLayout(this);
        presetLayout.setOrientation(LinearLayout.HORIZONTAL);
        presetLayout.setGravity(Gravity.CENTER);
        presetLayout.setPadding(0, 0, 0, 30);

        Button btn1 = new Button(this);
        btn1.setText("11123");
        presetLayout.addView(btn1);

        Button btn2 = new Button(this);
        btn2.setText("11186");
        presetLayout.addView(btn2);

        Button btn3 = new Button(this);
        btn3.setText("11127");
        presetLayout.addView(btn3);

        mainLayout.addView(presetLayout);

        final EditText etStart = new EditText(this);
        etStart.setHint("Ora început (ex: 07:25)");
        etStart.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        etStart.setTextColor(Color.BLACK);
        mainLayout.addView(etStart);

        final EditText etEnd = new EditText(this);
        etEnd.setHint("Ora sfârșit (ex: 19:30)");
        etEnd.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        etEnd.setTextColor(Color.BLACK);
        mainLayout.addView(etEnd);

        Button btnCalc = new Button(this);
        btnCalc.setText("Calculează Totalul");
        btnCalc.setBackgroundColor(Color.parseColor("#007BFF"));
        btnCalc.setTextColor(Color.WHITE);
        mainLayout.addView(btnCalc);

        final TextView tvResult = new TextView(this);
        tvResult.setTextSize(20);
        tvResult.setTextColor(Color.parseColor("#28A745"));
        tvResult.setGravity(Gravity.CENTER);
        tvResult.setPadding(0, 40, 0, 0);
        mainLayout.addView(tvResult);

        setContentView(mainLayout);

        btn1.setOnClickListener(v -> { etStart.setText("07:25"); etEnd.setText("19:30"); });
        btn2.setOnClickListener(v -> { etStart.setText("15:35"); etEnd.setText("16:00"); });
        btn3.setOnClickListener(v -> { etStart.setText("15:30"); etEnd.setText("10:00"); });

        btnCalc.setOnClickListener(v -> {
            try {
                String[] s = etStart.getText().toString().trim().split(":");
                String[] e = etEnd.getText().toString().trim().split(":");
                int start = Integer.parseInt(s[0]) * 60 + Integer.parseInt(s[1]);
                int end = Integer.parseInt(e[0]) * 60 + Integer.parseInt(e[1]);

                int diff = end - start;
                if (diff < 0) {
                    diff += 24 * 60; 
                }

                tvResult.setText("Total lucrat: " + (diff / 60) + "h " + (diff % 60) + "m");
                tvResult.setTextColor(Color.parseColor("#28A745"));
            } catch (Exception ex) {
                tvResult.setText("Eroare! Folosește formatul HH:MM");
                tvResult.setTextColor(Color.RED);
            }
        });
    }
}
