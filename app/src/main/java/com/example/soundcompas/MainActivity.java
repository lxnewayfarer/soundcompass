package com.example.soundcompas;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends Activity implements SensorEventListener {
    private FusedLocationProviderClient fusedLocationClient;
    TextToSpeech textToSpeech;
    //Объявляем картинку для компаса
    private ImageView HeaderImage;
    //Объявляем функцию поворота картинки
    private float RotateDegree = 0f;
    //Объявляем работу с сенсором устройства
    private SensorManager mSensorManager;
    //Объявляем объект TextView
    TextView CompOrient;
    TextView Locat;
    Handler handler = new Handler();
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Связываем объект ImageView с нашим изображением:
        HeaderImage = (ImageView) findViewById(R.id.Compas);
        //play("a");
        //TextView в котором будет отображаться градус поворота:
        CompOrient = (TextView) findViewById(R.id.Header);
        Locat = (TextView) findViewById(R.id.Footer);

        //Инициализируем возможность работать с сенсором устройства:
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            Locat.setText("Широта: " + Double.toString(location.getLatitude()) +
                                    "\nДолгота: " + Double.toString(location.getLongitude()));
                        } else {
                            Locat.setText("Не получается получить данные о геолокации");
                        }
                    }
                });
        dbHelper = new DBHelper(this);
    }

    public double latitude;
    public double longitude;

    public void LocatOnTime()
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            Locat.setText("Широта: " + Double.toString(latitude) +
                                    "\nДолгота: " + Double.toString(longitude));
                            ContentValues cv = new ContentValues();
                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            Log.d("DATABASE", "--- Insert in mytable: ---");
                            // подготовим данные для вставки в виде пар: наименование столбца - значение

                            cv.put("lat", latitude);
                            cv.put("long", longitude);
                            // вставляем запись и получаем ее ID
                            long rowID = db.insert("location", null, cv);
                            Log.d("DATABASE", "row inserted, ID = " + rowID);
                        } else {
                            Locat.setText("Не получается получить данные о геолокации");
                        }
                    }
                });
        Log.d("DATABASE", "--- Rows in mytable: ---");
        // делаем запрос всех данных из таблицы mytable, получаем Cursor
        Cursor c = db.query("location", null, null, null, null, null, null);

        // ставим позицию курсора на первую строку выборки
        // если в выборке нет строк, вернется false
        if (c.moveToFirst()) {
            // определяем номера столбцов по имени в выборке
            int idColIndex = c.getColumnIndex("id");
            int nameColIndex = c.getColumnIndex("lat");
            int emailColIndex = c.getColumnIndex("long");

            // deleting first entry
            if (c.isFirst())
                db.delete("location","id=?",new String[]{Integer.toString(c.getInt(idColIndex))});

            do {
                // получаем значения по номерам столбцов и пишем все в лог
                Log.d("LOG_TAG",
                        "ID = " + c.getInt(idColIndex) +
                                ", name = " + c.getString(nameColIndex) +
                                ", email = " + c.getString(emailColIndex));
                // переход на следующую строку
                // а если следующей нет (текущая - последняя), то false - выходим из цикла
            } while (c.moveToNext());
        } else
            Log.d("LOG_TAG", "0 rows");
        c.close();
    }

    static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            // конструктор суперкласса
            super(context, "Location.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d("DATABASE", "--- onCreate database ---");
            // создаем таблицу с полями
            db.execSQL("create table location ("
                    + "id INTEGER primary key autoincrement,"
                    + "lat REAL,"
                    + "long REAL" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            LocatOnTime();
            handler.postDelayed(this, 180000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(runnableCode);
        //Устанавливаем слушателя ориентации сенсора
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Останавливаем при надобности слушателя ориентации
        //сенсора с целью сбережения заряда батареи:
        mSensorManager.unregisterListener(this);
    }
    public int degree;
    @Override
    public void onSensorChanged(SensorEvent event) {

        //Получаем градус поворота от оси, которая направлена на север, север = 0 градусов:
        degree = Math.round(event.values[0]);
        CompOrient.setText("Текущий азимут:\n" + Integer.toString(360-degree) + " градусов");

        //Создаем анимацию вращения:
        RotateAnimation rotateAnimation = new RotateAnimation(
                RotateDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        //Продолжительность анимации в миллисекундах:
        rotateAnimation.setDuration(200);

        //Настраиваем анимацию после завершения подсчетных действий датчика:
        rotateAnimation.setFillAfter(true);

        //Запускаем анимацию:
        HeaderImage.startAnimation(rotateAnimation);
        RotateDegree = -degree;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Этот метод не используется, но без него программа будет ругаться
    }

    private void play(String nameOfFile){
        MediaPlayer mPlayer = MediaPlayer.create(this, getResources().getIdentifier(nameOfFile, "raw", getPackageName()));
        mPlayer.start();
    }

    public void Click(View view)
    {
        play("a"+Integer.toString(360-degree));
    }

    public void showMap(View view) {
        Intent intent = new Intent(this, DisplayMapActivity.class);
        startActivity(intent);
    }

}
