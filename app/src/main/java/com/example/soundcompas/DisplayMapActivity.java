package com.example.soundcompas;

import androidx.fragment.app.FragmentActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class DisplayMapActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    MainActivity.DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        dbHelper = new MainActivity.DBHelper(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    public double latitude;
    public double longitude;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // DB on read
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // делаем запрос всех данных из таблицы mytable, получаем Cursor
        Cursor c = db.query("location", null, null, null, null, null, null);

        // ставим позицию курсора на первую строку выборки
        // если в выборке нет строк, вернется false
        LatLng[] locations = new LatLng[11];
        int i = 0;
        if (c.moveToFirst()) {
            // определяем номера столбцов по имени в выборке
            int idColIndex = c.getColumnIndex("id");
            int nameColIndex = c.getColumnIndex("lat");
            int emailColIndex = c.getColumnIndex("long");

            do {
                mMap.clear();
                mMap.setMyLocationEnabled(true);
                latitude = c.getDouble(nameColIndex);
                longitude = c.getDouble(emailColIndex);
                locations[i] = new LatLng(latitude, longitude);
                i++;
            } while (c.moveToNext());
        } else
            Log.d("LOG_TAG", "0 rows");
        c.close();
        // filling empty locations with last seen
        while (i < 11){
            i++;
            locations[i] = new LatLng(latitude, longitude);
        }
        Polyline polyline = mMap.addPolyline(new PolylineOptions()
                .clickable(true)
                .add(locations));
        polyline.setJointType(JointType.ROUND);
        LatLng currentLocation = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(currentLocation).title("You are here"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15.0f));
    }

}
