package com.efraespada.motiondetector;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by efraespada on 13/07/2017.
 */

public class MotionDetector {

    public static Context context;
    private static ComponentName serviceComponent;
    public static Boolean debug;
    private static int jobId;
    public static float minAccuracy = 10f;
    public static String currentType;

    public static LocationManager locationManager;
    public static LocationListener locationListener;

    public static SensorManager sensorMan;
    public static Sensor accelerometer;
    public static Listener listener;
    public static float[] mGravity;
    public static float mAccel;
    public static float mAccelCurrent;
    public static float mAccelLast;
    public static Boolean deviceIsMoving = false;
    public static long minTime = 30 * 1000;                   // 5 seconds
    public static long minDistance = 3;                       // 3 meters
    public static final int SERVICE_LIVE = 30 * 60 * 1000;    // 30 min
    public static float MIN_ACCURACY = 10f;
    public static int accelerationTimes;
    public static boolean initialized = false;
    public static boolean isPositive;

    public static Location currentLocation;

    private static final String TAG = MotionDetector.class.getSimpleName();


    private MotionDetector() {
        // nothing to do here
    }

    public static void initialize(Context context) {
        MotionDetector.context = context;
        MotionDetector.debug = false;
        MotionDetector.serviceComponent = new ComponentName(context, MotionJob.class);
    }

    public static void debug(boolean debug) {
        MotionDetector.debug = debug;
    }

    public static void minAccuracy(float minAccuracy) {
        MotionDetector.minAccuracy = minAccuracy;
    }

    public static void end() {
        if (context != null) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancelAll();
        }
    }

    public static void start(Listener listener) {
        if (context != null) {
            setListener(context, listener, debug);
            JobInfo.Builder builder = new JobInfo.Builder(jobId++, serviceComponent);
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(builder.build());
        }
    }

    public static void setListener(Context context, Listener listener, boolean debug) {
        MotionDetector.context = context;
        MotionDetector.debug = debug;
        MotionDetector.listener = listener;
        MotionDetector.locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                if (MotionDetector.deviceIsMoving && location.getAccuracy() <= MotionDetector.MIN_ACCURACY) {
                    currentLocation = location;
                    MotionDetector.listener.locationChanged(location);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // nothing to do here
            }

            @Override
            public void onProviderEnabled(String provider) {
                // nothing to do here
            }

            @Override
            public void onProviderDisabled(String provider) {
                // nothing to do here ..
            }
        };
    }

    public static String getType() {
        return currentType;
    }

    public static boolean isServiceReady() {
        return initialized;
    }

    public static Location getLocation() throws SecurityException {
        return MotionDetector.locationManager.getLastKnownLocation(getProviderName());
    }

    public static String getProviderName() {
        LocationManager locationManager = (LocationManager) MotionDetector.context.getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_LOW); // Chose your desired power consumption level.
        criteria.setAccuracy(Criteria.ACCURACY_FINE); // Choose your accuracy requirement.
        criteria.setSpeedRequired(true);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(false);

        return locationManager.getBestProvider(criteria, true);
    }

    public static float getMinAccuracy() {
        return MotionDetector.MIN_ACCURACY;
    }

    public static void setMinAccuracy(float minAccuracy) {
        MotionDetector.MIN_ACCURACY = minAccuracy;
    }

}
