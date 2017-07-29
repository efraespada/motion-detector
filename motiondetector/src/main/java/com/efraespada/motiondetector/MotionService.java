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

import java.text.DecimalFormat;
import java.util.Date;

import static android.location.LocationProvider.AVAILABLE;

/**
 * Created by efraespada on 16/07/2017.
 */

public class MotionService extends Service implements SensorEventListener {

    private static final String TAG = MotionService.class.getSimpleName();

    public static final String SIT =       "sit";
    public static final String WALK =      "walking";
    public static final String JOGGING =   "jogging";
    public static final String RUN =       "running";
    public static final String BIKE =      "biking";
    public static final String CAR =       "car";
    public static final String MOTO =      "moto";
    public static final String METRO =     "metro";
    public static final String PLANE =     "plane";

    private static final String[] ORDER = new String[]{SIT, WALK, JOGGING, RUN, BIKE, METRO, CAR, MOTO, PLANE};

    private static Date lastTypeTime;
    private static final long maxInterval = 5 * 60 * 1000; // 5 min - millis

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
    private long minTime = 30 * 1000;                   // 5 seconds
    private long minDistance = 3;                       // 3 meters
    private final int SERVICE_LIVE = 30 * 60 * 1000;    // 30 min
    private static float MIN_ACCURACY = 10f;
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
        MotionService.context = context;
        MotionService.debug = debug;
        MotionService.listener = listener;
        MotionService.locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                if (deviceIsMoving && location.getAccuracy() <= MIN_ACCURACY) {
                    currentLocation = location;
                    MotionService.listener.locationChanged(location);
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

        SIT_PROPERTIES =        new Properties(0,   0.99f,      1,  2,  1.4f);
        WALK_PROPERTIES =       new Properties(1,   4.99f,      3,  4,  1.4f);
        JOGGING_PROPERTIES =    new Properties(5,   9.99f,      5,  6,  1.85f);
        RUN_PROPERTIES =        new Properties(10,  19.99f,     7,  8,  3.47f);
        BIKE_PROPERTIES =       new Properties(10,  29.99f,     8,  9,  2.7f);
        CAR_PROPERTIES =        new Properties(10,  249.99f,    9,  15,  2);
        MOTO_PROPERTIES =       new Properties(10,  249.99f,    18, 23,  1.4f);
        METRO_PROPERTIES =      new Properties(10,  50,         2,  4,  1.4f);
        PLANE_PROPERTIES =      new Properties(150,  1200,      60, 70,  1.4f);
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
        if (Math.abs(mAccel) > 0.01) {
            if (mAccel > 0 && isPositive) {
                accelerationTimes++;
            } else if (mAccel > 0 && !isPositive) {
                isPositive = true;
                accelerationTimes = 0;
                if (mAccel > 0.5s && currentLocation != null && (currentLocation.getSpeed() * 3.6f) <= RUN_PROPERTIES.getMaxSpeed() ) {
                    listener.step();
                }
            } else if (mAccel < 0 && isPositive) {
                isPositive = false;
                accelerationTimes = 0;
            } else if (mAccel < 0 && !isPositive) {
                accelerationTimes++;
            }

            if (currentLocation != null) {
                if (accelerationTimes >= SIT_PROPERTIES.getMinAccelerationTimes()
                        && accelerationTimes <= SIT_PROPERTIES.getMaxAccelerationTimes()
                        && currentLocation.getSpeed() * 3.6f < SIT_PROPERTIES.getMaxSpeed()
                        && currentLocation.getSpeed() * 3.6f >= SIT_PROPERTIES.getMinSpeed()) {

                    if (priority(SIT)) {
                        currentType = SIT;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (accelerationTimes >= WALK_PROPERTIES.getMinAccelerationTimes()
                        && accelerationTimes <= WALK_PROPERTIES.getMaxAccelerationTimes()
                        && currentLocation.getSpeed() * 3.6f < WALK_PROPERTIES.getMaxSpeed()
                        && currentLocation.getSpeed() * 3.6f >= WALK_PROPERTIES.getMinSpeed()) {

                    if (priority(WALK)) {
                        currentType = WALK;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (accelerationTimes >= JOGGING_PROPERTIES.getMinAccelerationTimes()
                        && accelerationTimes <= JOGGING_PROPERTIES.getMaxAccelerationTimes()
                        && currentLocation.getSpeed() * 3.6f < JOGGING_PROPERTIES.getMaxSpeed()
                        && currentLocation.getSpeed() * 3.6f >= JOGGING_PROPERTIES.getMinSpeed()) {

                    if (priority(JOGGING)) {
                        currentType = JOGGING;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (accelerationTimes >= RUN_PROPERTIES.getMinAccelerationTimes()
                        && accelerationTimes <= RUN_PROPERTIES.getMaxAccelerationTimes()
                        && currentLocation.getSpeed() * 3.6f < RUN_PROPERTIES.getMaxSpeed()
                        && currentLocation.getSpeed() * 3.6f >= RUN_PROPERTIES.getMinSpeed()) {

                    if (priority(RUN)) {
                        currentType = RUN;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (accelerationTimes >= BIKE_PROPERTIES.getMinAccelerationTimes()
                        && accelerationTimes <= BIKE_PROPERTIES.getMaxAccelerationTimes()
                        && currentLocation.getSpeed() * 3.6f < BIKE_PROPERTIES.getMaxSpeed()
                        && currentLocation.getSpeed() * 3.6f >= BIKE_PROPERTIES.getMinSpeed()) {

                    if (priority(BIKE)) {
                        currentType = BIKE;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (accelerationTimes >= MOTO_PROPERTIES.getMinAccelerationTimes()
                        && accelerationTimes <= MOTO_PROPERTIES.getMaxAccelerationTimes()
                        && currentLocation.getSpeed() * 3.6f < MOTO_PROPERTIES.getMaxSpeed()
                        && currentLocation.getSpeed() * 3.6f >= MOTO_PROPERTIES.getMinSpeed()) {

                    if (priority(MOTO)) {
                        currentType = MOTO;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (accelerationTimes >= METRO_PROPERTIES.getMinAccelerationTimes()
                        && accelerationTimes <= METRO_PROPERTIES.getMaxAccelerationTimes()
                        && currentLocation.getSpeed() * 3.6f < METRO_PROPERTIES.getMaxSpeed()
                        && currentLocation.getSpeed() * 3.6f >= METRO_PROPERTIES.getMinSpeed()) {

                    if (priority(METRO)) {
                        currentType = METRO;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (accelerationTimes >= CAR_PROPERTIES.getMinAccelerationTimes()
                        && accelerationTimes <= CAR_PROPERTIES.getMaxAccelerationTimes()
                        && currentLocation.getSpeed() * 3.6f < CAR_PROPERTIES.getMaxSpeed()
                        && currentLocation.getSpeed() * 3.6f >= CAR_PROPERTIES.getMinSpeed()) {

                    if (priority(CAR)) {
                        currentType = CAR;
                        lastTypeTime = new Date();
                        listener.type(currentType);
                    }

                } else if (accelerationTimes >= PLANE_PROPERTIES.getMinAccelerationTimes()
                        && accelerationTimes <= PLANE_PROPERTIES.getMaxAccelerationTimes()
                        && currentLocation.getSpeed() * 3.6f < PLANE_PROPERTIES.getMaxSpeed()
                        && currentLocation.getSpeed() * 3.6f >= PLANE_PROPERTIES.getMinSpeed()) {

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
        if (lastTypeTime != null && (lastTypeTime.getTime() - (new Date().getTime())) >= maxInterval) {
            return true;
        }

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
        private int minAccelerationTimes;
        private int maxAccelerationTimes;
        private float maxSteps;

        public Properties() {
            // nothing to do here
        }

        public Properties(float minSpeed, float maxSpeed, int minAccelerationTimes, int maxAccelerationTimes, float maxSteps) {
            this.maxSpeed = maxSpeed;
            this.minSpeed = minSpeed;
            this.minAccelerationTimes = minAccelerationTimes;
            this.maxAccelerationTimes = maxAccelerationTimes;
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

        public float getMaxSteps() {
            return maxSteps;
        }

        public void setMaxSteps(float maxSteps) {
            this.maxSteps = maxSteps;
        }

        public int getMinAccelerationTimes() {
            return minAccelerationTimes;
        }

        public void setMinAccelerationTimes(int minAccelerationTimes) {
            this.minAccelerationTimes = minAccelerationTimes;
        }

        public int getMaxAccelerationTimes() {
            return maxAccelerationTimes;
        }

        public void setMaxAccelerationTimes(int maxAccelerationTimes) {
            this.maxAccelerationTimes = maxAccelerationTimes;
        }
    }
}
