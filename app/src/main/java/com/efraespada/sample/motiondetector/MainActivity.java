package com.efraespada.sample.motiondetector;

import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.efraespada.motiondetector.Listener;
import com.efraespada.motiondetector.MotionDetector;

public class MainActivity extends AppCompatActivity {

    private static Integer count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MotionDetector.initialize(getApplicationContext());
        MotionDetector.debug(true);

        if (count == null) {
            count = 0;
        }

        MotionDetector.start(new Listener() {
            @Override
            public void locationChanged(Location location) {

            }

            @Override
            public void step() {
                count++;
                ((TextView) findViewById(R.id.steps)).setText(String.valueOf(count));
            }

            @Override
            public void car() {

            }
        });
    }
}
