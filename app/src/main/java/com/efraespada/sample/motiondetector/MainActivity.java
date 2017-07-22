package com.efraespada.sample.motiondetector;

import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.efraespada.motiondetector.Listener;
import com.efraespada.motiondetector.MotionDetector;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView speed;
    private TextView acceleration;
    private TextView type;
    private TextView steps;
    private ImageButton stepsReset;

    private static Integer count;
    private static String currentType;

    private static float lastSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (count == null) {
            count = 0;
        }

        if (currentType == null) {
            currentType = "not detected";
        }

        speed = (TextView) findViewById(R.id.speed);

        acceleration = (TextView) findViewById(R.id.acceleration);

        type = (TextView) findViewById(R.id.type);
        type.setText(currentType);

        steps = (TextView) findViewById(R.id.steps);
        steps.setText(String.valueOf(count));

        stepsReset = (ImageButton) findViewById(R.id.steps_reset);
        stepsReset.setOnClickListener(this);

        MotionDetector.initialize(getApplicationContext());
        MotionDetector.debug(true);

        MotionDetector.start(new Listener() {

            @Override
            public void locationChanged(Location location) {
                lastSpeed = location.getSpeed() * 3.6F;
                speed.setText(String.valueOf(lastSpeed));
            }

            @Override
            public void accelerationChanged(float acceleration) {
                MainActivity.this.acceleration.setText(String.valueOf(acceleration));
            }

            @Override
            public void step() {
                count++;
                steps.setText(String.valueOf(count));
            }

            @Override
            public void type(String type) {
                currentType = type;
                MainActivity.this.type.setText(type);
            }

        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.steps_reset:
                count = 0;
                steps.setText(String.valueOf(count));
                break;

            default:
                // nothing to do here
                break;
        }
    }
}
