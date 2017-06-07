package ru.arshor.babycontrol;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabasePlaces mDatabasePlaces;
    private SQLiteDatabase mSqLiteDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mDatabasePlaces = new DatabasePlaces(this, "mydatabase.db", null, 1);
        mSqLiteDatabase = mDatabasePlaces.getReadableDatabase();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Cursor cursor = mSqLiteDatabase.query("Places", new String[]{DatabasePlaces.TIME_COLUMN,
                        DatabasePlaces.LAT_COLUMN, DatabasePlaces.LNG_COLUMN},
                null, null,
                null, null, null);

        while (cursor.moveToNext()) {
            String time = cursor.getString(cursor.getColumnIndex(DatabasePlaces.TIME_COLUMN));
            double lat = cursor.getDouble(cursor.getColumnIndex(DatabasePlaces.LAT_COLUMN));
            double lng = cursor.getDouble(cursor.getColumnIndex(DatabasePlaces.LNG_COLUMN));

            LatLng pls = new LatLng(lat, lng);
            mMap.addMarker(new MarkerOptions().position(pls).title(time));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pls, 14));
        }
        cursor.close();

    }
}
