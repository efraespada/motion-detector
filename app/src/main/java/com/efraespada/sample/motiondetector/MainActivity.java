package com.efraespada.sample.motiondetector;

import android.location.Location;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.efraespada.motiondetector.Listener;
import com.efraespada.motiondetector.MotionDetector;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.context.IconicsLayoutInflater;
import com.mikepenz.iconics.view.IconicsTextView;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;

import static com.efraespada.motiondetector.MotionService.BIKE;
import static com.efraespada.motiondetector.MotionService.CAR;
import static com.efraespada.motiondetector.MotionService.JOGGING;
import static com.efraespada.motiondetector.MotionService.METRO;
import static com.efraespada.motiondetector.MotionService.MOTO;
import static com.efraespada.motiondetector.MotionService.PLANE;
import static com.efraespada.motiondetector.MotionService.RUN;
import static com.efraespada.motiondetector.MotionService.SIT;
import static com.efraespada.motiondetector.MotionService.WALK;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private static final String ms = " m/s2";
    private static final String kmh = " km/h";
    private TextView speed;
    private TextView acceleration;
    private TextView accelerationMax;
    private ImageButton accelerationReset;
    private IconicsTextView type;
    private TextView steps;
    private ImageButton stepsReset;

    private static Integer count;
    private static String currentType;

    private static float lastSpeed;
    private static float maxAcceleration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory(getLayoutInflater(), new IconicsLayoutInflater(getDelegate()));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        if (count == null) {
            count = 0;
        }

        if (currentType == null) {
            currentType = "not detected";
        }

        speed = (TextView) findViewById(R.id.speed);
        speed.setText(String.valueOf(lastSpeed) + kmh);

        acceleration = (TextView) findViewById(R.id.acceleration);
        accelerationMax = (TextView) findViewById(R.id.acceleration_max);
        accelerationMax.setText(String.valueOf(maxAcceleration) + ms);

        accelerationReset = (ImageButton) findViewById(R.id.acceleration_reset);
        accelerationReset.setOnClickListener(this);

        type = (IconicsTextView) findViewById(R.id.type);
        printIcon(currentType);

        steps = (TextView) findViewById(R.id.steps);
        steps.setText(String.valueOf(count));

        stepsReset = (ImageButton) findViewById(R.id.steps_reset);
        stepsReset.setOnClickListener(this);

        MotionDetector.initialize(this);
        MotionDetector.minAccuracy(30);
        MotionDetector.debug(true);

        MotionDetector.start(new Listener() {

            @Override
            public void locationChanged(Location location) {
                lastSpeed = location.getSpeed() * 3.6F;
                speed.setText(String.valueOf(lastSpeed) + kmh);
            }

            @Override
            public void accelerationChanged(float acceleration) {
                if (maxAcceleration < acceleration) {
                    maxAcceleration = acceleration;
                    MainActivity.this.accelerationMax.setText(String.valueOf(maxAcceleration) + ms);

                }
                MainActivity.this.acceleration.setText(String.valueOf(acceleration) + ms);
            }

            @Override
            public void step() {
                count++;
                steps.setText(String.valueOf(count));
            }

            @Override
            public void type(String type) {
                currentType = type;
                MainActivity.this.printIcon(type);
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MotionDetector.isServiceReady()) {
            lastSpeed = MotionDetector.getLocation().getSpeed() * 3.6F;
            speed.setText(String.valueOf(lastSpeed) + kmh);
            printIcon(MotionDetector.getType());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MotionDetector.end();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.steps_reset:
                count = 0;
                steps.setText(String.valueOf(count));
                break;

            case R.id.acceleration_reset:
                maxAcceleration = 0.0f;
                accelerationMax.setText(String.valueOf(maxAcceleration) + ms);
                break;

            default:
                // nothing to do here
                break;
        }
    }

    private void printIcon(String value) {
        switch (value) {

            case SIT:
                type.setText(CommunityMaterial.Icon.cmd_seat_recline_normal.getFormattedName());
                break;

            case WALK:
                type.setText(MaterialDesignIconic.Icon.gmi_directions_walk.getFormattedName());
                break;

            case JOGGING:
                type.setText(MaterialDesignIconic.Icon.gmi_directions_run.getFormattedName());
                break;

            case RUN:
                type.setText(CommunityMaterial.Icon.cmd_run_fast.getFormattedName());
                break;

            case BIKE:
                type.setText(MaterialDesignIconic.Icon.gmi_directions_bike.getFormattedName());
                break;

            case METRO:
                type.setText(MaterialDesignIconic.Icon.gmi_subway.getFormattedName());
                break;

            case CAR:
                type.setText(MaterialDesignIconic.Icon.gmi_car.getFormattedName());
                break;

            case MOTO:
                type.setText(CommunityMaterial.Icon.cmd_motorbike.getFormattedName());
                break;

            case PLANE:
                type.setText(MaterialDesignIconic.Icon.gmi_airplane.getFormattedName());
                break;

            default:
                type.setText(GoogleMaterial.Icon.gmd_local_pizza.getFormattedName());
                break;
        }

    }
}
