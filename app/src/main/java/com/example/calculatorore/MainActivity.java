package com.example.calculatorore;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    DatabaseHelper dbHelper;
    LinearLayout historyLayout;
    TextView tvStats;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#F5F5F5"));

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dp(20), dp(30), dp(20), dp(20));
        scrollView.addView(mainLayout);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Foi de Parcurs & Rapoarte");
        tvTitle.setTextSize(22);
        tvTitle.setTextColor(Color.BLACK);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, dp(20));
        mainLayout.addView(tvTitle);

        final EditText etDate = new EditText(this);
        etDate.setHint("Data (ex: 02/09/2026)");
        etDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        etDate.setTextColor(Color.BLACK);
        mainLayout.addView(etDate);

        final EditText etShift = new EditText(this);
        etShift.setHint("Număr Tren / Manevră");
        etShift.setTextColor(Color.BLACK);
        mainLayout.addView(etShift);

        final EditText etStart = new EditText(this);
        etStart.setHint("Ora început (HH:MM)");
        etStart.setTextColor(Color.BLACK);
        mainLayout.addView(etStart);

        final EditText etEnd = new EditText(this);
        etEnd.setHint("Ora sfârșit (HH:MM)");
        etEnd.setTextColor(Color.BLACK);
        mainLayout.addView(etEnd);

        // Butoane pentru autocompletare rapida
        LinearLayout preset1 = new LinearLayout(this);
        preset1.setOrientation(LinearLayout.HORIZONTAL);
        preset1.setGravity(Gravity.CENTER);
        String[] shifts1 = {"11123", "11186", "11178"};
        for (String s : shifts1) {
            Button b = new Button(this);
            b.setText(s);
            b.setOnClickListener(v -> setShiftData(s, etShift, etStart, etEnd));
            preset1.addView(b);
        }
        mainLayout.addView(preset1);

        LinearLayout preset2 = new LinearLayout(this);
        preset2.setOrientation(LinearLayout.HORIZONTAL);
        preset2.setGravity(Gravity.CENTER);
        String[] shifts2 = {"11190", "11127", "11182"};
        for (String s : shifts2) {
            Button b = new Button(this);
            b.setText(s);
            b.setOnClickListener(v -> setShiftData(s, etShift, etStart, etEnd));
            preset2.addView(b);
        }
        mainLayout.addView(preset2);

        Button btnSave = new Button(this);
        btnSave.setText("Salvează Foaia de Parcurs");
        btnSave.setBackgroundColor(Color.parseColor("#007BFF"));
        btnSave.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, dp(15), 0, 0);
        btnSave.setLayoutParams(btnParams);
        mainLayout.addView(btnSave);

        tvStats = new TextView(this);
        tvStats.setTextSize(18);
        tvStats.setTextColor(Color.parseColor("#D2691E"));
        tvStats.setPadding(0, dp(20), 0, dp(10));
        mainLayout.addView(tvStats);

        historyLayout = new LinearLayout(this);
        historyLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(historyLayout);

        setContentView(scrollView);

        btnSave.setOnClickListener(v -> {
            String date = etDate.getText().toString();
            String shift = etShift.getText().toString();
            String start = etStart.getText().toString();
            String end = etEnd.getText().toString();

            if (shift.isEmpty() || start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Completați toate câmpurile!", Toast.LENGTH_SHORT).show();
                return;
            }

            int diff = calculateMinutes(start, end);
            if (diff < 0) {
                Toast.makeText(this, "Format oră invalid (folosiți HH:MM)!", Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.insertWaybill(date, shift, start, end, diff);
            Toast.makeText(this, "Foaie de parcurs salvată cu succes!", Toast.LENGTH_SHORT).show();
            refreshData();
        });

        refreshData();
    }

    private void setShiftData(String shift, EditText etShift, EditText etStart, EditText etEnd) {
        etShift.setText(shift);
        if (shift.equals("11123")) { etStart.setText("07:25"); etEnd.setText("18:30"); }
        else if (shift.equals("11186")) { etStart.setText("15:35"); etEnd.setText("16:00"); }
        else if (shift.equals("11178")) { etStart.setText("10:30"); etEnd.setText("20:30"); }
        else if (shift.equals("11190")) { etStart.setText("19:05"); etEnd.setText("10:30"); }
        else if (shift.equals("11127")) { etStart.setText("15:30"); etEnd.setText("10:00"); }
        else if (shift.equals("11182")) { etStart.setText("13:15"); etEnd.setText("23:00"); }
    }

    private int calculateMinutes(String startStr, String endStr) {
        try {
            String[] s = startStr.split(":");
            String[] e = endStr.split(":");
            int start = Integer.parseInt(s[0].trim()) * 60 + Integer.parseInt(s[1].trim());
            int end = Integer.parseInt(e[0].trim()) * 60 + Integer.parseInt(e[1].trim());
            int diff = end - start;
            if (diff < 0) diff += 24 * 60; // Trecerea peste miezul nopții
            return diff;
        } catch (Exception e) {
            return -1;
        }
    }

    private void refreshData() {
        historyLayout.removeAllViews();
        Cursor c = dbHelper.getAllWaybills();
        int totalMinutes = 0;

        while (c.moveToNext()) {
            String date = c.getString(1);
            String shift = c.getString(2);
            String start = c.getString(3);
            String end = c.getString(4);
            int mins = c.getInt(5);
            totalMinutes += mins;

            TextView tv = new TextView(this);
            tv.setText("📅 " + date + " | Tura: " + shift + "\n🕒 " + start + " - " + end + " (Total: " + (mins / 60) + "h " + (mins % 60) + "m)");
            tv.setPadding(dp(10), dp(10), dp(10), dp(10));
            tv.setTextColor(Color.DKGRAY);
            tv.setBackgroundColor(Color.parseColor("#E9ECEF"));
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(10));
            tv.setLayoutParams(params);
            
            historyLayout.addView(tv);
        }
        c.close();

        tvStats.setText("📊 Raport Total:\nTotal ore înregistrate: " + (totalMinutes / 60) + "h " + (totalMinutes % 60) + "m");
    }

    class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "OreMunca.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE waybills (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, shift TEXT, start TEXT, end TEXT, minutes INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        public void insertWaybill(String date, String shift, String start, String end, int minutes) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("date", date);
            cv.put("shift", shift);
            cv.put("start", start);
            cv.put("end", end);
            cv.put("minutes", minutes);
            db.insert("waybills", null, cv);
        }

        public Cursor getAllWaybills() {
            return this.getReadableDatabase().rawQuery("SELECT * FROM waybills ORDER BY id DESC", null);
        }
    }
}
