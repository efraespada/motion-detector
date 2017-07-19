package com.efraespada.motiondetector;

import android.location.Location;

/**
 * Created by efraespada on 16/07/2017.
 */

public interface Listener {

    void locationChanged(Location location);
    void step();
    void car();

}
