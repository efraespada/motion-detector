package com.efraespada.motiondetector;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by efraespada on 13/07/2017.
 */

public class MotionDetector {

    private static Context context;
    private static MotionService motionService;
    private static Boolean isServiceBound;
    private static Boolean debug;

    private static final String TAG = MotionDetector.class.getSimpleName();


    private MotionDetector() {
        // nothing to do here
    }

    public static void initialize(Context context) {
        MotionDetector.context = context;
        MotionDetector.debug = false;
    }

    public static void debug(boolean debug) {
        MotionDetector.debug = debug;
    }

    public static void end() {
        if (isServiceBound != null && isServiceBound && motionService != null && motionService.getServiceConnection() != null) {
            motionService.stopService();
            try {
                context.unbindService(motionService.getServiceConnection());
            } catch (IllegalArgumentException e) {}

            if (debug) Log.e(TAG, "unbound");
            context.stopService(new Intent(context, MotionService.class));
            isServiceBound = false;
        }
    }

    public static void start(Listener listener) {
        if (isServiceBound == null || !isServiceBound) {
            Intent i = new Intent(context, MotionService.class);
            context.startService(i);
            MotionService motionService = new MotionService();
            motionService.setListener(context, listener, debug);
            context.bindService(i, getServiceConnection(motionService), Context.BIND_AUTO_CREATE);
            isServiceBound = true;
        } else {
            motionService.setListener(context, listener, debug);
            motionService.stopService();
            motionService.startService();
        }
    }

    private static ServiceConnection getServiceConnection(Object obj) {
        if (obj instanceof MotionService) return new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                if (service instanceof MotionService.MotionSensorBinder) {
                    motionService = ((MotionService.MotionSensorBinder) service).getService();
                    motionService.setServiceConnection(this);
                    if (debug) Log.e(TAG, "instanced motion service");
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                if (className.getClassName().equals(MotionService.class.getName())) motionService = null;
                if (debug) Log.e(TAG, "disconnected");
            }
        };
        return null;
    }

    public static Location getLocation() {
        return motionService.getLocation();
    }

    public static String getType() {
        return motionService.getType();
    }

    public static boolean isServiceReady() {
        return motionService != null;
    }

}
