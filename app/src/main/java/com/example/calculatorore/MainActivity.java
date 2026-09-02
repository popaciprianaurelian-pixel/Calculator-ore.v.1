package com.example.calculatorore;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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

        // Butoane Rapoarte
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.VERTICAL);
        
        Button btnPDF = new Button(this); btnPDF.setText("📊 Export Raport Lunar (PDF)");
        Button btnCSV = new Button(this); btnCSV.setText("📊 Export Raport Lunar (Excel)");
        Button btnNorm = new Button(this); btnNorm.setText("⚙️ Setează Norma Lunară");
        Button btnBackup = new Button(this); btnBackup.setText("💾 Backup Bază de Date");

        actionsRow.addView(btnNorm); actionsRow.addView(btnPDF); actionsRow.addView(btnCSV); actionsRow.addView(btnBackup);
        root.addView(actionsRow);

        btnPDF.setOnClickListener(v -> exportPDF());
        btnCSV.setOnClickListener(v -> exportCSV());
        btnBackup.setOnClickListener(v -> backupDB());
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
            "⏳ Total adunat: " + workedHours + "h " + (totalMins % 60) + "m\n" +
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
        tvDate.setPadding(0,0,0,20);
        layout.addView(tvDate);

        // Butoane Ture Rapide
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        LinearLayout presetLayout = new LinearLayout(this);
        presetLayout.setOrientation(LinearLayout.HORIZONTAL);
        presetLayout.setPadding(0, 0, 0, 30);
        hScroll.addView(presetLayout);
        layout.addView(hScroll);

        EditText etShift = new EditText(this); etShift.setHint("Număr Tren / Manevră");
        layout.addView(etShift);

        EditText etIntervals = new EditText(this); etIntervals.setHint("Orare Start-Stop (doar informativ)");
        layout.addView(etIntervals);

        // CONTROL TOTAL: AICI TRECI TU MANUAL ORELE SI MINUTELE
        TextView tvManual = new TextView(this); 
        tvManual.setText("CÂT AI LUCRAT EFECTIV? (Treci manual):"); 
        tvManual.setTextColor(Color.parseColor("#D2691E"));
        tvManual.setPadding(0,30,0,10);
        layout.addView(tvManual);

        LinearLayout timeLayout = new LinearLayout(this);
        timeLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        EditText etTotalH = new EditText(this); etTotalH.setHint("Ore"); etTotalH.setInputType(InputType.TYPE_CLASS_NUMBER); etTotalH.setEms(5);
        TextView tvH = new TextView(this); tvH.setText(" h și  ");
        EditText etTotalM = new EditText(this); etTotalM.setHint("Min"); etTotalM.setInputType(InputType.TYPE_CLASS_NUMBER); etTotalM.setEms(5);
        TextView tvM = new TextView(this); tvM.setText(" m");
        
        timeLayout.addView(etTotalH); timeLayout.addView(tvH); timeLayout.addView(etTotalM); timeLayout.addView(tvM);
        layout.addView(timeLayout);

        EditText etNotes = new EditText(this); etNotes.setHint("Observații");
        layout.addView(etNotes);

        RadioGroup rgType = new RadioGroup(this);
        rgType.setOrientation(LinearLayout.HORIZONTAL);
        String[] types = {"ZI", "NOAPTE", "LIBER", "CONCEDIU"};
        for (String t : types) {
            RadioButton rb = new RadioButton(this); rb.setText(t); rgType.addView(rb);
            if (t.equals("ZI")) rb.setChecked(true);
        }
        layout.addView(rgType);

        Button btnPhoto = new Button(this); btnPhoto.setText("📷 Poză Foaie Parcurs");
        layout.addView(btnPhoto);

        // Logica butoanelor de autocompletare
        String[] shifts = {"11123", "11186", "11178", "R11190", "11127", "11182", "Manevra"};
        for (String s : shifts) {
            Button b = new Button(this);
            b.setText(s);
            b.setOnClickListener(v -> {
                etShift.setText(s);
                if(s.equals("11123")) { etIntervals.setText("07:25 - 19:30"); etTotalH.setText("12"); etTotalM.setText("5"); }
                else if(s.equals("11186")) { etIntervals.setText("15:35 - 16:00"); etTotalH.setText("24"); etTotalM.setText("25"); }
                else if(s.equals("11178")) { etIntervals.setText("10:30 - 20:30"); etTotalH.setText("10"); etTotalM.setText("0"); }
                else if(s.equals("R11190")) { etIntervals.setText("19:35 - 14:30"); etTotalH.setText("18"); etTotalM.setText("55"); }
                else if(s.equals("11127")) { etIntervals.setText("15:30 - 10:00"); etTotalH.setText("18"); etTotalM.setText("30"); }
                else if(s.equals("11182")) { etIntervals.setText("13:15 - 23:00"); etTotalH.setText("9"); etTotalM.setText("45"); }
                else if(s.equals("Manevra")) { etIntervals.setText("07:00 - 16:30"); etTotalH.setText("9"); etTotalM.setText("30"); }
            });
            presetLayout.addView(b);
        }

        currentPhotoUri = null;
        Cursor c = db.getReadableDatabase().rawQuery("SELECT * FROM shifts WHERE date=?", new String[]{date});
        String existingId = null;
        if (c.moveToFirst()) {
            existingId = c.getString(0);
            String type = c.getString(2);
            for (int i = 0; i < rgType.getChildCount(); i++) {
                if (((RadioButton) rgType.getChildAt(i)).getText().toString().equals(type)) ((RadioButton) rgType.getChildAt(i)).setChecked(true);
            }
            etTotalH.setText(String.valueOf(c.getInt(3) / 60));
            etTotalM.setText(String.valueOf(c.getInt(3) % 60));
            etNotes.setText(c.getString(5));
            etShift.setText(c.getString(7));
            etIntervals.setText(c.getString(8));
        }
        c.close();

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
                
                int h = etTotalH.getText().toString().isEmpty() ? 0 : Integer.parseInt(etTotalH.getText().toString());
                int m = etTotalM.getText().toString().isEmpty() ? 0 : Integer.parseInt(etTotalM.getText().toString());
                int finalTotalMins = (h * 60) + m; // IA EXACT CE AI SCRIS TU, FARA SA SCADA NIMIC!
                
                int overtime = finalTotalMins > 480 ? (finalTotalMins - 480) : 0; 
                
                SQLiteDatabase wdb = db.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("date", date); 
                cv.put("type", type); 
                cv.put("total_mins", finalTotalMins); 
                cv.put("overtime_mins", overtime);
                cv.put("notes", etNotes.getText().toString()); 
                cv.put("shift_code", etShift.getText().toString());
                cv.put("intervals", etIntervals.getText().toString());
                if(currentPhotoUri != null) cv.put("photo_uri", currentPhotoUri.toString());
                
                if (fExistingId != null) wdb.update("shifts", cv, "id=?", new String[]{fExistingId});
                else wdb.insert("shifts", null, cv);
                
                updateUI();
                Toast.makeText(this, "Salvat exact cum ai introdus!", Toast.LENGTH_SHORT).show();
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
        Cursor c = db.getReadableDatabase().rawQuery("SELECT date, type, total_mins, notes, shift_code, intervals FROM shifts WHERE date LIKE ? ORDER BY date", new String[]{monthPrefix + "%"});
        
        while (c.moveToNext()) {
            String line = c.getString(0) + " | " + c.getString(4) + " (" + c.getString(5) + ") | " + c.getString(1) + " | Total: " + (c.getInt(2) / 60) + "h " + (c.getInt(2) % 60) + "m";
            canvas.drawText(line, 10, y, paint);
            y += 20;
        }
        c.close();
        document.finishPage(page);
        
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Raport_Ore_" + monthPrefix + ".pdf");
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "Raport PDF salvat in Downloads!", Toast.LENGTH_LONG).show();
        } catch (IOException e) { Toast.makeText(this, "Eroare PDF", Toast.LENGTH_SHORT).show(); }
        document.close();
    }

    void exportCSV() {
        String monthPrefix = String.format(Locale.US, "%04d-%02d", currentMonth.get(Calendar.YEAR), currentMonth.get(Calendar.MONTH) + 1);
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Raport_Ore_" + monthPrefix + ".csv");
            FileWriter fw = new FileWriter(file);
            fw.append("Data,Tren,Interval,Tip,Total_Min,Suplimentare_Min,Observatii\n");
            Cursor c = db.getReadableDatabase().rawQuery("SELECT date, shift_code, intervals, type, total_mins, overtime_mins, notes FROM shifts WHERE date LIKE ? ORDER BY date", new String[]{monthPrefix + "%"});
            while (c.moveToNext()) {
                fw.append(c.getString(0)).append(",").append(c.getString(1)).append(",").append(c.getString(2)).append(",")
                  .append(c.getString(3)).append(",").append(String.valueOf(c.getInt(4))).append(",").append(String.valueOf(c.getInt(5))).append(",").append(c.getString(6)).append("\n");
            }
            c.close(); fw.flush(); fw.close();
            Toast.makeText(this, "Fisier Excel salvat in Downloads!", Toast.LENGTH_LONG).show();
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

    void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024]; int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close(); out.close();
    }

    class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context ctx) { super(ctx, "OreMunca.db", null, 5); }
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE shifts (id INTEGER PRIMARY KEY, date TEXT, type TEXT, total_mins INTEGER, overtime_mins INTEGER, notes TEXT, photo_uri TEXT, shift_code TEXT, intervals TEXT)");
        }
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            db.execSQL("DROP TABLE IF EXISTS shifts"); onCreate(db);
        }
    }
}
