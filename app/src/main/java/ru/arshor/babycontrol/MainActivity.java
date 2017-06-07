package ru.arshor.babycontrol;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int NOTIFY_ID = 101;
    private LocationManager locationManager;
    private String svcName = Context.LOCATION_SERVICE;
    private String phoneSMS = "";
    private int ref_min = 0;
    private boolean bSMS;
    private int flag = 0;
    private String latLongString = "";
    private String adressString = "";
    private Timer mTimer;
    private MyTimerTask mMyTimerTask;
    private DatabasePlaces mDatabasePlaces;
    private SQLiteDatabase mSqLiteDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(svcName);
        TextView myLocationText;
        myLocationText = (TextView) findViewById(R.id.myLocationText);
        myLocationText.setText("Твое текущее расположение:\n" + latLongString + "\n\n" +
                "Ты находишся по адресу:\n" + adressString);

        mDatabasePlaces = new DatabasePlaces(this, "mydatabase.db", null, 1);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Сообщи маме, где ты!!!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            showSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        TextView myLocationText;
        myLocationText = (TextView) findViewById(R.id.myLocationText);
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.walk) {
            if (mTimer != null) {
                mTimer.cancel();
            }
            mTimer = new Timer();
            mMyTimerTask = new MyTimerTask();

            SQLiteDatabase db = mDatabasePlaces.getWritableDatabase();
            db.delete("Places", null, null);

            startLocation();
            mTimer.schedule(mMyTimerTask, 1000, ref_min * 60 * 1000);

        } else if (id == R.id.stop_tracking) {
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }

            locationManager.removeUpdates(locationListener);
            myLocationText.setText("Слежение остановлено!");
        } else if (id == R.id.court) {

        } else if (id == R.id.my_walking) {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        } else if (id == R.id.my_settings) {
            showSettings();
        } else if (id == R.id.i_am_here) {
            flag = 1;
            startLocation();
            myLocationText.setText("Твое текущее расположение:\n" + latLongString + "\n\n" +
                    "Ты находишся по адресу:\n" + adressString);
            locationManager.removeUpdates(locationListener);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void updateWithNewLocation(Location location) {
        latLongString = "Расположение на найдено!";
        adressString = "Адрес не найден!";
        if (location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            latLongString = "Lat:" + lat + "\nLong:" + lng;

            Geocoder gc = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = gc.getFromLocation(lat, lng, 1);
                StringBuilder sb = new StringBuilder();

                if (addresses.size() > 0) {
                    Address address = addresses.get(0);
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++)
                        sb.append(address.getAddressLine(i)).append("\n");
                }
                adressString = sb.toString();
            } catch (IOException e) {
                adressString = e.toString();
            }
        }
        onNotifi();
        if (bSMS) {
            sendSMS(phoneSMS, adressString.substring(0,69));
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    public void startLocation() {

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);

        String provider = locationManager.getBestProvider(criteria, true);
        if (flag == 1){
            Location l = locationManager.getLastKnownLocation(provider);
            updateWithNewLocation(l);
            flag = 0;
        }
        locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);
    }

    public void onNotifi() {
        Context context = getApplicationContext();
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Resources res = context.getResources();
        Notification.Builder builder = new Notification.Builder(context);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                // большая картинка
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                //.setTicker(res.getString(R.string.warning)) // текст в строке состояния
                .setTicker("Предупреждение!")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                //.setContentTitle(res.getString(R.string.notifytitle)) // Заголовок уведомления
                .setContentTitle("Напоминание!")
                //.setContentText(res.getString(R.string.notifytext))
                .setContentText("Ваше расположение в мире изменилось! СМС отправлено!"); // Текст уведомления

        // Notification notification = builder.getNotification(); // до API 16
        Notification notification = builder.build();
        Uri ringURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notification.sound = ringURI;
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFY_ID, notification);
    }

    public void showSettings() {
        Intent intent = new Intent(this, settingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        phoneSMS = prefs.getString(getString(R.string.phone), "");
        ref_min = Integer.valueOf(prefs.getString(getString(R.string.ref_minutes), "30"));
        bSMS = prefs.getBoolean("SMS", false);
        TextView myText;
        myText = (TextView) findViewById(R.id.myText);
        if (bSMS){
            myText.setText("\nСМС о твоем расположении будет отправлено на телефон: " + phoneSMS
                    + "\n\nСМС будет отправляться каждые " + ref_min + " минут.");
        } else {
            myText.setText("\nСМС отправляться не будет.");
        };

    }

    class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setSpeedRequired(false);
            criteria.setCostAllowed(true);

            String provider = locationManager.getBestProvider(criteria, true);
            Location l = locationManager.getLastKnownLocation(provider);
            updateWithNewLocation(l);

            mSqLiteDatabase = mDatabasePlaces.getWritableDatabase();
            ContentValues values = new ContentValues();
            // Задайте значения для каждого столбца
            Calendar c = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd:MMMM:yyyy HH:mm:ss a");
            String strDate = sdf.format(c.getTime());
            values.put(DatabasePlaces.TIME_COLUMN, strDate);
            values.put(DatabasePlaces.LAT_COLUMN, l.getLatitude());
            values.put(DatabasePlaces.LNG_COLUMN, l.getLongitude());
            // Вставляем данные в таблицу
            mSqLiteDatabase.insert("Places", null, values);

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    TextView myLocationText;
                    myLocationText = (TextView) findViewById(R.id.myLocationText);
                    myLocationText.setText("Слежение запущено!\n\nТвое текущее расположение:\n" + latLongString + "\n\n" +
                            "Ты находишся по адресу:\n" + adressString);
                }
            });
        }
    }
}
