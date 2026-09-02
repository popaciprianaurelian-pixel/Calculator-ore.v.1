package com.example.calculatorore;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    DatabaseHelper db;
    Calendar currentMonth = Calendar.getInstance();
    TextView tvMonth, tvStats;
    CalendarView calendarView;
    int currentNorm = 160; 
    Uri currentPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DatabaseHelper(this);
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 40, 30, 40);
        scroll.addView(root);

        // Selector Luna
        LinearLayout monthRow = new LinearLayout(this);
        monthRow.setOrientation(LinearLayout.HORIZONTAL);
        monthRow.setGravity(Gravity.CENTER);
        Button btnPrev = new Button(this); btnPrev.setText("<<");
        tvMonth = new TextView(this); tvMonth.setTextSize(20); tvMonth.setPadding(40, 0, 40, 0); tvMonth.setTextColor(Color.BLACK);
        Button btnNext = new Button(this); btnNext.setText(">>");
        monthRow.addView(btnPrev); monthRow.addView(tvMonth); monthRow.addView(btnNext);
        root.addView(monthRow);

        btnPrev.setOnClickListener(v -> { currentMonth.add(Calendar.MONTH, -1); updateUI(); });
        btnNext.setOnClickListener(v -> { currentMonth.add(Calendar.MONTH, 1); updateUI(); });

        // Statistica Lunara
        tvStats = new TextView(this);
        tvStats.setTextSize(16);
        tvStats.setPadding(0, 30, 0, 30);
        tvStats.setTextColor(Color.parseColor("#006400"));
        root.addView(tvStats);

        // Calendar
        calendarView = new CalendarView(this);
        root.addView(calendarView);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            openDayEditor(date);
        });

        // Butoane Actiuni Rapoarte
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.VERTICAL);
        
        Button btnPDF = new Button(this); btnPDF.setText("📊 Export Raport Lunar (PDF)");
        Button btnCSV = new Button(this); btnCSV.setText("📊 Export Raport Lunar (Excel)");
        Button btnNorm = new Button(this); btnNorm.setText("⚙️ Setează Norma Lunară");
        Button btnBackup = new Button(this); btnBackup.setText("💾 Backup Bază de Date");
        Button btnRestore = new Button(this); btnRestore.setText("🔄 Restaurare Bază de Date");

        actionsRow.addView(btnNorm); actionsRow.addView(btnPDF); actionsRow.addView(btnCSV);
        actionsRow.addView(btnBackup); actionsRow.addView(btnRestore);
        root.addView(actionsRow);

        btnPDF.setOnClickListener(v -> exportPDF());
        btnCSV.setOnClickListener(v -> exportCSV());
        btnBackup.setOnClickListener(v -> backupDB());
        btnRestore.setOnClickListener(v -> restoreDB());
        btnNorm.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setText(String.valueOf(currentNorm));
            new AlertDialog.Builder(this).setTitle("Norma lunară (ore)")
                .setView(input)
                .setPositiveButton("OK", (d, w) -> { currentNorm = Integer.parseInt(input.getText().toString()); updateUI(); })
                .show();
        });

        setContentView(scroll);
        updateUI();
    }

    void updateUI() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("ro"));
        tvMonth.setText(sdf.format(currentMonth.getTime()).toUpperCase());

        String monthPrefix = String.format(Locale.US, "%04d-%02d", currentMonth.get(Calendar.YEAR), currentMonth.get(Calendar.MONTH) + 1);
        Cursor c = db.getReadableDatabase().rawQuery("SELECT total_mins, overtime_mins, type FROM shifts WHERE date LIKE ?", new String[]{monthPrefix + "%"});
        int totalMins = 0, overMins = 0, daysWorked = 0;
        
        while (c.moveToNext()) {
            if (!c.getString(2).equals("LIBER") && !c.getString(2).equals("CONCEDIU")) daysWorked++;
            totalMins += c.getInt(0);
            overMins += c.getInt(1);
        }
        c.close();

        int workedHours = totalMins / 60;
        int diff = workedHours - currentNorm;
        String difText = diff >= 0 ? ("+" + diff + "h peste normă") : (diff + "h sub normă");

        tvStats.setText(
            "📋 Zile lucrate: " + daysWorked + "\n" +
            "🎯 Norma setată: " + currentNorm + "h\n" +
            "⏳ Total lucrat: " + workedHours + "h " + (totalMins % 60) + "m\n" +
            "🔥 Ore Suplimentare (>8h/zi): " + (overMins / 60) + "h " + (overMins % 60) + "m\n" +
            "📈 Diferență: " + difText
        );
    }

    void openDayEditor(String date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        scroll.addView(layout);

        TextView tvDate = new TextView(this); tvDate.setText("Data: " + date); tvDate.setTextSize(20);
        layout.addView(tvDate);

        RadioGroup rgType = new RadioGroup(this);
        rgType.setOrientation(LinearLayout.HORIZONTAL);
        String[] types = {"ZI", "NOAPTE", "LIBER", "CONCEDIU"};
        for (String t : types) {
            RadioButton rb = new RadioButton(this); rb.setText(t); rgType.addView(rb);
            if (t.equals("ZI")) rb.setChecked(true);
        }
        layout.addView(rgType);

        LinearLayout intervalsLayout = new LinearLayout(this);
        intervalsLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(intervalsLayout);

        Button btnAddInterval = new Button(this);
        btnAddInterval.setText("+ Adaugă interval multiplu (Start - Stop)");
        layout.addView(btnAddInterval);

        EditText etBreak = new EditText(this); etBreak.setHint("Pauză totală (în minute)"); etBreak.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etBreak);

        EditText etNotes = new EditText(this); etNotes.setHint("Observații / Număr Tren");
        layout.addView(etNotes);

        Button btnPhoto = new Button(this); btnPhoto.setText("📷 Adaugă Poză Foaie Parcurs");
        layout.addView(btnPhoto);

        currentPhotoUri = null;
        Cursor c = db.getReadableDatabase().rawQuery("SELECT * FROM shifts WHERE date=?", new String[]{date});
        String existingId = null;
        if (c.moveToFirst()) {
            existingId = c.getString(0);
            String type = c.getString(2);
            for (int i = 0; i < rgType.getChildCount(); i++) {
                if (((RadioButton) rgType.getChildAt(i)).getText().toString().equals(type)) ((RadioButton) rgType.getChildAt(i)).setChecked(true);
            }
            etBreak.setText(c.getString(4));
            etNotes.setText(c.getString(7));
            String intervalsStr = c.getString(8);
            if (intervalsStr != null && !intervalsStr.isEmpty()) {
                for (String intv : intervalsStr.split(",")) addIntervalView(intervalsLayout, intv);
            }
        } else {
            addIntervalView(intervalsLayout, "");
        }
        c.close();

        btnAddInterval.setOnClickListener(v -> addIntervalView(intervalsLayout, ""));

        btnPhoto.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = null;
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                photoFile = File.createTempFile("FOAIE_" + timeStamp + "_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            } catch (IOException ex) {}
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(this, "com.example.calculatorore.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(takePictureIntent, 101);
            }
        });

        final String fExistingId = existingId;
        builder.setView(scroll)
            .setPositiveButton("Salvează", (dialog, which) -> {
                String type = "ZI";
                for (int i = 0; i < rgType.getChildCount(); i++) {
                    if (((RadioButton) rgType.getChildAt(i)).isChecked()) type = ((RadioButton) rgType.getChildAt(i)).getText().toString();
                }
                int brk = etBreak.getText().toString().isEmpty() ? 0 : Integer.parseInt(etBreak.getText().toString());
                
                StringBuilder intervalsBuilder = new StringBuilder();
                int totalMins = 0;
                for (int i = 0; i < intervalsLayout.getChildCount(); i++) {
                    LinearLayout row = (LinearLayout) intervalsLayout.getChildAt(i);
                    EditText start = (EditText) row.getChildAt(0);
                    EditText end = (EditText) row.getChildAt(1);
                    String s = start.getText().toString().trim();
                    String e = end.getText().toString().trim();
                    if (!s.isEmpty() && !e.isEmpty()) {
                        intervalsBuilder.append(s).append("-").append(e).append(",");
                        totalMins += calcMins(s, e);
                    }
                }
                
                totalMins -= brk;
                if (totalMins < 0) totalMins = 0;
                int overtime = totalMins > 480 ? (totalMins - 480) : 0; 
                
                SQLiteDatabase wdb = db.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("date", date); cv.put("type", type); cv.put("break_mins", brk);
                cv.put("total_mins", totalMins); cv.put("overtime_mins", overtime);
                cv.put("notes", etNotes.getText().toString()); cv.put("intervals", intervalsBuilder.toString());
                if(currentPhotoUri != null) cv.put("photo_uri", currentPhotoUri.toString());
                
                if (fExistingId != null) wdb.update("shifts", cv, "id=?", new String[]{fExistingId});
                else wdb.insert("shifts", null, cv);
                
                updateUI();
                Toast.makeText(this, "Ziuă salvată cu succes!", Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Șterge", (dialog, which) -> {
                if (fExistingId != null) {
                    db.getWritableDatabase().delete("shifts", "id=?", new String[]{fExistingId});
                    updateUI();
                    Toast.makeText(this, "Înregistrare ștearsă!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Anulează", null)
            .show();
    }

    void addIntervalView(LinearLayout parent, String preset) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        EditText etStart = new EditText(this); etStart.setHint("Start (ex: 07:00)"); etStart.setEms(6);
        EditText etEnd = new EditText(this); etEnd.setHint("Stop (ex: 19:30)"); etEnd.setEms(6);
        if (!preset.isEmpty() && preset.contains("-")) {
            String[] p = preset.split("-");
            etStart.setText(p[0]); if (p.length > 1) etEnd.setText(p[1]);
        }
        row.addView(etStart); row.addView(etEnd);
        parent.addView(row);
    }

    int calcMins(String start, String end) {
        try {
            String[] s = start.split(":"); String[] e = end.split(":");
            int st = Integer.parseInt(s[0].trim()) * 60 + Integer.parseInt(s[1].trim());
            int en = Integer.parseInt(e[0].trim()) * 60 + Integer.parseInt(e[1].trim());
            int diff = en - st;
            if (diff < 0) diff += 24 * 60; // Sare peste miezul noptii!
            return diff;
        } catch (Exception e) { return 0; }
    }

    void exportPDF() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        
        paint.setTextSize(16);
        canvas.drawText("Raport Lunar: " + tvMonth.getText().toString(), 10, 30, paint);
        
        paint.setTextSize(12);
        int y = 60;
        String monthPrefix = String.format(Locale.US, "%04d-%02d", currentMonth.get(Calendar.YEAR), currentMonth.get(Calendar.MONTH) + 1);
        Cursor c = db.getReadableDatabase().rawQuery("SELECT date, type, intervals, total_mins, notes FROM shifts WHERE date LIKE ? ORDER BY date", new String[]{monthPrefix + "%"});
        
        while (c.moveToNext()) {
            String line = c.getString(0) + " | " + c.getString(1) + " | Int: " + c.getString(2) + " | Ore: " + (c.getInt(3) / 60) + "h " + (c.getInt(3) % 60) + "m | Obs: " + c.getString(4);
            canvas.drawText(line, 10, y, paint);
            y += 20;
        }
        c.close();
        document.finishPage(page);
        
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Raport_" + monthPrefix + ".pdf");
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "Raport PDF salvat in folderul Downloads!", Toast.LENGTH_LONG).show();
        } catch (IOException e) { Toast.makeText(this, "Eroare PDF", Toast.LENGTH_SHORT).show(); }
        document.close();
    }

    void exportCSV() {
        String monthPrefix = String.format(Locale.US, "%04d-%02d", currentMonth.get(Calendar.YEAR), currentMonth.get(Calendar.MONTH) + 1);
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Raport_" + monthPrefix + ".csv");
            FileWriter fw = new FileWriter(file);
            fw.append("Data,Tip,Intervale,Total_Min,Suplimentare_Min,Observatii\n");
            Cursor c = db.getReadableDatabase().rawQuery("SELECT date, type, intervals, total_mins, overtime_mins, notes FROM shifts WHERE date LIKE ? ORDER BY date", new String[]{monthPrefix + "%"});
            while (c.moveToNext()) {
                fw.append(c.getString(0)).append(",").append(c.getString(1)).append(",").append(c.getString(2)).append(",")
                  .append(String.valueOf(c.getInt(3))).append(",").append(String.valueOf(c.getInt(4))).append(",").append(c.getString(5)).append("\n");
            }
            c.close(); fw.flush(); fw.close();
            Toast.makeText(this, "Fisier Excel (CSV) salvat in folderul Downloads!", Toast.LENGTH_LONG).show();
        } catch (IOException e) { Toast.makeText(this, "Eroare Excel", Toast.LENGTH_SHORT).show(); }
    }

    void backupDB() {
        try {
            File src = getDatabasePath("OreMunca.db");
            File dst = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Backup_CalculatorOre.db");
            copyFile(src, dst);
            Toast.makeText(this, "Baza de date salvata in Downloads!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {}
    }

    void restoreDB() {
        try {
            File src = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Backup_CalculatorOre.db");
            File dst = getDatabasePath("OreMunca.db");
            if (src.exists()) {
                copyFile(src, dst); updateUI();
                Toast.makeText(this, "Datele au fost restaurate cu succes!", Toast.LENGTH_LONG).show();
            } else { Toast.makeText(this, "Nu s-a gasit niciun backup anterior.", Toast.LENGTH_SHORT).show(); }
        } catch (Exception e) {}
    }

    void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024]; int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close(); out.close();
    }

    class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context ctx) { super(ctx, "OreMunca.db", null, 3); }
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE shifts (id INTEGER PRIMARY KEY, date TEXT, type TEXT, start TEXT, break_mins INTEGER, total_mins INTEGER, overtime_mins INTEGER, notes TEXT, intervals TEXT, photo_uri TEXT)");
        }
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            db.execSQL("DROP TABLE IF EXISTS shifts"); onCreate(db);
        }
    }
}
