package apps.boatengyeboah.com.testgyro;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.DecimalFormat;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    TextView xText, yText, zText, bLarge, xLarge;
    String roll = "roll";
    private CanvasView customCanvas;
    int width = 0;
    int height = 0;
    private Path mPath;
    private Paint mPaint;
    private float mX, mY;
    private static final float TOLERANCE = 5;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);    // Register the sensor listeners
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        xText = (TextView) findViewById(R.id.tvX);
        yText = (TextView) findViewById(R.id.tvY);
        zText = (TextView) findViewById(R.id.tvZ);
        xLarge = (TextView)findViewById(R.id.xLarge);
        bLarge = (TextView)findViewById(R.id.yLarge);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;
        startActivity(new Intent(this, BestGyro.class));

    }



    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    float[] mGravity;
    float[] mGeomagnetic;

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
//                values[0]: azimuth, rotation around the Z axis.
//                values[1]: pitch, rotation around the X axis.
//                values[2]: roll, rotation around the Y axis.
                double Roll = orientation[2];
                double Pitch = orientation[1];
                xText.setText(String.valueOf(roundTwoDecimals(orientation[0]))); // orientation contains: azimut, pitch and roll
                yText.setText(String.valueOf(roundTwoDecimals(orientation[1])));
                zText.setText(String.valueOf(roundTwoDecimals(orientation[2])));
                roll = String.valueOf(roundTwoDecimals(orientation[2]));
                double rollLarge = map(Roll, -Math.PI / 2, Math.PI / 2, 0, width);
                double pitchLarge = map(Pitch,-Math.PI/2,Math.PI/2, 0, height);
                bLarge.setText("Roll: " + String.valueOf(roundTwoDecimals(rollLarge)));
                xLarge.setText("Pitch: " + String.valueOf(roundTwoDecimals(pitchLarge)));

//                Canvas grid = new Canvas(Bitmap.createBitmap(h, w, Bitmap.Config.ARGB_8888));
//                grid. drawColor(Color.WHITE);
//                Paint paint = new Paint();
//                paint.setStyle(Paint.Style.FILL);
//                grid.drawCircle(w/2, h/2 , w/2, paint);

            }
        }
    }

    double roundTwoDecimals(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(d));
    }

    double map(double x, double in_min, double in_max, double out_min, double out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }


}