package com.efraespada.motiondetector;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import static android.location.LocationProvider.AVAILABLE;

/**
 * Created by efraespada on 16/07/2017.
 */

public class MotionService extends Service implements SensorEventListener {

    private static final String TAG = MotionService.class.getSimpleName();

    private static SensorManager sensorMan;
    private static Sensor accelerometer;
    private static Listener listener;
    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;
    private static Boolean deviceIsMoving = false;
    private long minTime = 30 * 1000; // 5 seconds
    private long minDistance = 3; // 3 meters
    private final int SERVICE_LIVE = 30 * 60 * 1000; // 30 min
    private float MIN_ACCURACY = 10f;
    private static int accelerationTimes;
    private static boolean initialized;
    private static boolean isPositive;
    private static boolean debug;
    private static Context context;

    private static LocationListener locationListener;

    private final IBinder motionSensorBinder = new MotionService.MotionSensorBinder();
    private ServiceConnection sc;

    public void setListener(Context context, Listener listener, boolean debug) {
        this.context = context;
        this.debug = debug;
        this.listener = listener;
        this.locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                if (deviceIsMoving && location.getAccuracy() <= MIN_ACCURACY) {
                    MotionService.this.listener.locationChanged(location);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                if (status != AVAILABLE) {
                    stopService();
                    startService();
                }
            }

            @Override
            public void onProviderEnabled(String provider) {
                stopService();
                startService();
            }

            @Override
            public void onProviderDisabled(String provider) {
                // nothing to do here ..
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sc = null;
        sensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        accelerationTimes = 0;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorMan.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        startService();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (debug) Log.d(TAG, "service bound");
        return motionSensorBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopService();
        sensorMan.unregisterListener(this);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (debug) Log.d(TAG, "service destroyed");
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mGravity = event.values.clone();
            // Shake detection
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            // Make this higher or lower according to how much
            // motion you want to detect
            //Log.e(TAG, String.valueOf(Math.abs(mAccel)));
            // if (!deviceIsMoving && mAccel > 0.0F){
            checkAcceleration(mAccel);
            if (!deviceIsMoving && mAccel > 1){
                startService();
                deviceIsMoving = true;
                Handler handler = new Handler();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        deviceIsMoving = false;
                    }
                };
                handler.postDelayed(runnable, 3000);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void stopService() {
        if (initialized) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(locationListener);
            initialized = false;
        }
    }

    public void startService() {
        if (!initialized) {
            initialized = true;
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(getProviderName(), minTime, minDistance, locationListener);
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    stopService();
                }
            };
            handler.postDelayed(runnable, SERVICE_LIVE);
        }
    }

    private String getProviderName() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_LOW); // Chose your desired power consumption level.
        criteria.setAccuracy(Criteria.ACCURACY_FINE); // Choose your accuracy requirement.
        criteria.setSpeedRequired(true);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(false);

        return locationManager.getBestProvider(criteria, true);
    }

    public void setServiceConnection(ServiceConnection sc) {
        this.sc = sc;
        if (debug) Log.d(TAG, "serviceConnection set");
    }

    public ServiceConnection getServiceConnection() {
        return this.sc;
    }

    public class MotionSensorBinder extends Binder {
        public MotionService getService() {
            return MotionService.this;
        }
    }

    private void checkAcceleration(float mAccel) {
        if (Math.abs(mAccel) > 2) {
            if (mAccel > 0 && isPositive) {
                accelerationTimes++;
            } else if (mAccel > 0 && !isPositive) {
                isPositive = true;
                accelerationTimes = 0;
                listener.step();
            } else if (mAccel < 0 && isPositive) {
                isPositive = false;
                accelerationTimes = 0;
            } else if (mAccel < 0 && !isPositive) {
                accelerationTimes++;
            }

            if (accelerationTimes > 10) {
                // TODO car acceleration detected
            }
        }
    }
}
