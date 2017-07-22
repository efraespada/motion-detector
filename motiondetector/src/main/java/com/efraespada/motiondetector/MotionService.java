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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.Date;

import static android.location.LocationProvider.AVAILABLE;

/**
 * Created by efraespada on 16/07/2017.
 */

public class MotionService extends Service implements SensorEventListener {

    private static final String TAG = MotionService.class.getSimpleName();

    private static final String SIT =       "sit";
    private static final String WALK =      "walking";
    private static final String JOGGING =   "jogging";
    private static final String RUN =       "running";
    private static final String BIKE =      "biking";
    private static final String CAR =       "car";
    private static final String MOTO =      "moto";
    private static final String METRO =     "metro";
    private static final String PLANE =     "plane";

    private static final String[] ORDER = new String[]{SIT, WALK, JOGGING, RUN, BIKE, CAR, MOTO, METRO, PLANE};

    private static Date lastTypeTime;
    private static final long maxInterval = 10 * 60 * 1000; // 10 min - millis

    private static String currentType;

    private static Properties SIT_PROPERTIES;
    private static Properties WALK_PROPERTIES;
    private static Properties JOGGING_PROPERTIES;
    private static Properties RUN_PROPERTIES;
    private static Properties BIKE_PROPERTIES;
    private static Properties CAR_PROPERTIES;
    private static Properties MOTO_PROPERTIES;
    private static Properties METRO_PROPERTIES;
    private static Properties PLANE_PROPERTIES;

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

    private static Location currentLocation;

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
                    currentLocation = location;
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

        SIT_PROPERTIES =        new Properties(0,   0.99f,      0,      1.38f,  1.4f);
        WALK_PROPERTIES =       new Properties(1,   4.99f,      0,      1.38f,  1.4f);
        JOGGING_PROPERTIES =    new Properties(5,   9.99f,      0.55f,  2.77f,  1.85f);
        RUN_PROPERTIES =        new Properties(10,  19.99f,     1,      5.55f,  3.47f);
        BIKE_PROPERTIES =       new Properties(10,  29.99f,     1,      3.77f,  2.7f);
        CAR_PROPERTIES =        new Properties(10,  249.99f,    1,      12.77f, 2);
        MOTO_PROPERTIES =       new Properties(10,  249.99f,    1,      12.77f, 1.4f);
        METRO_PROPERTIES =      new Properties(10,  109,        1,      6.05f,  1.4f);
        PLANE_PROPERTIES =      new Properties(20,  900,        1.8f,   3.5f,   1.4f);
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

            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            DecimalFormat decimalFormat = new DecimalFormat("#.#");
            float twoDigitsF = Float.valueOf(decimalFormat.format(mAccel));

            checkAcceleration(twoDigitsF);
            if (!deviceIsMoving && twoDigitsF > 1){
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

    public void stopService() {
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

            if (currentLocation == null) {
                currentLocation = locationManager.getLastKnownLocation(getProviderName());
            }

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
        listener.accelerationChanged(mAccel);
        if (Math.abs(mAccel) > 0.3) {
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

            if (accelerationTimes <= 5 && accelerationTimes > 1 && currentLocation != null) {
                if (Math.abs(mAccel) <= SIT_PROPERTIES.getMaxAcceleration() && Math.abs(mAccel) > SIT_PROPERTIES.getMinAcceleration()
                        && currentLocation.getSpeed() / 3.6f < SIT_PROPERTIES.getMaxSpeed() && currentLocation.getSpeed() / 3.6f >= SIT_PROPERTIES.getMinSpeed()) {

                    if (priority(SIT)) {
                        currentType = SIT;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                }
            } else if (accelerationTimes > 5 && currentLocation != null) {
                if (Math.abs(mAccel) <= WALK_PROPERTIES.getMaxAcceleration() && Math.abs(mAccel) > WALK_PROPERTIES.getMinAcceleration()
                        && currentLocation.getSpeed() / 3.6f < WALK_PROPERTIES.getMaxSpeed() && currentLocation.getSpeed() / 3.6f >= WALK_PROPERTIES.getMinSpeed()) {

                    if (priority(WALK)) {
                        currentType = WALK;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (Math.abs(mAccel) <= JOGGING_PROPERTIES.getMaxAcceleration() && Math.abs(mAccel) > JOGGING_PROPERTIES.getMinAcceleration()
                        && currentLocation.getSpeed() / 3.6f < JOGGING_PROPERTIES.getMaxSpeed() && currentLocation.getSpeed() / 3.6f >= JOGGING_PROPERTIES.getMinSpeed()) {

                    if (priority(JOGGING)) {
                        currentType = JOGGING;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (Math.abs(mAccel) <= RUN_PROPERTIES.getMaxAcceleration() && Math.abs(mAccel) > RUN_PROPERTIES.getMinAcceleration()
                        && currentLocation.getSpeed() / 3.6f < RUN_PROPERTIES.getMaxSpeed() && currentLocation.getSpeed() / 3.6f >= RUN_PROPERTIES.getMinSpeed()) {

                    if (priority(RUN)) {
                        currentType = RUN;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (Math.abs(mAccel) <= BIKE_PROPERTIES.getMaxAcceleration() && Math.abs(mAccel) > BIKE_PROPERTIES.getMinAcceleration()
                        && currentLocation.getSpeed() / 3.6f < BIKE_PROPERTIES.getMaxSpeed() && currentLocation.getSpeed() / 3.6f >= BIKE_PROPERTIES.getMinSpeed()) {

                    if (priority(BIKE)) {
                        currentType = BIKE;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (Math.abs(mAccel) <= MOTO_PROPERTIES.getMaxAcceleration() && Math.abs(mAccel) > MOTO_PROPERTIES.getMinAcceleration()
                        && currentLocation.getSpeed() / 3.6f < MOTO_PROPERTIES.getMaxSpeed() && currentLocation.getSpeed() / 3.6f >= MOTO_PROPERTIES.getMinSpeed()) {

                    if (priority(MOTO)) {
                        currentType = MOTO;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (Math.abs(mAccel) <= METRO_PROPERTIES.getMaxAcceleration() && Math.abs(mAccel) > METRO_PROPERTIES.getMinAcceleration()
                        && currentLocation.getSpeed() / 3.6f < METRO_PROPERTIES.getMaxSpeed() && currentLocation.getSpeed() / 3.6f >= METRO_PROPERTIES.getMinSpeed()) {

                    if (priority(METRO)) {
                        currentType = METRO;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (Math.abs(mAccel) <= CAR_PROPERTIES.getMaxAcceleration() && Math.abs(mAccel) > CAR_PROPERTIES.getMinAcceleration()
                        && currentLocation.getSpeed() / 3.6f < CAR_PROPERTIES.getMaxSpeed() && currentLocation.getSpeed() / 3.6f >= CAR_PROPERTIES.getMinSpeed()) {

                    if (priority(CAR)) {
                        currentType = CAR;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (Math.abs(mAccel) <= PLANE_PROPERTIES.getMaxAcceleration() && Math.abs(mAccel) > PLANE_PROPERTIES.getMinAcceleration()
                        && currentLocation.getSpeed() / 3.6f < PLANE_PROPERTIES.getMaxSpeed() && currentLocation.getSpeed() / 3.6f >= PLANE_PROPERTIES.getMinSpeed()) {

                    if (priority(PLANE)) {
                        currentType = PLANE;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                }
            }
        }
    }

    private static boolean priority(String value) {
        if (currentType == null) {
            return true;
        }

        if (value.equals(currentType)) {
            return true;
        }

        for (String val : ORDER) {
            if (val.equals(currentType)) {
                return true;
            } else if (val.equals(value)) {
                return false;
            }
        }

        return true;
    }

    class Properties {

        private float maxSpeed;
        private float minSpeed;
        private float minAcceleration;
        private float maxAcceleration;
        private float maxSteps;

        public Properties() {
            // nothing to do here
        }

        public Properties(float minSpeed, float maxSpeed, float minAcceleration, float maxAcceleration, float maxSteps) {
            this.maxSpeed = maxSpeed;
            this.minSpeed = minSpeed;
            this.minAcceleration = minAcceleration;
            this.maxAcceleration = maxAcceleration;
            this.maxSteps = maxSteps;
        }

        public float getMaxSpeed() {
            return maxSpeed;
        }

        public void setMaxSpeed(float maxSpeed) {
            this.maxSpeed = maxSpeed;
        }

        public float getMinSpeed() {
            return minSpeed;
        }

        public void setMinSpeed(float minSpeed) {
            this.minSpeed = minSpeed;
        }

        public float getMaxAcceleration() {
            return maxAcceleration;
        }

        public void setMaxAcceleration(float maxAcceleration) {
            this.maxAcceleration = maxAcceleration;
        }

        public float getMinAcceleration() {
            return minAcceleration;
        }

        public void setMinAcceleration(float minAcceleration) {
            this.minAcceleration = minAcceleration;
        }

        public float getMaxSteps() {
            return maxSteps;
        }

        public void setMaxSteps(float maxSteps) {
            this.maxSteps = maxSteps;
        }
    }
}
