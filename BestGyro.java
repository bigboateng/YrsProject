package apps.boatengyeboah.com.testgyro;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.larswerkman.holocolorpicker.ColorPicker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;

public class BestGyro extends AppCompatActivity {
    TextView mTextView_azimuth;
    TextView mTextView_pitch;
    TextView mTextView_roll;
    TextView mTextView_filtered_azimuth;
    TextView mTextView_filtered_pitch;
    TextView mTextView_filtered_roll;

    ImageView imageView;
    float mAngle0_azimuth = 0;
    float mAngle1_pitch = 0;
    float mAngle2_roll = 0;

    float mAngle0_filtered_azimuth = 0;
    float mAngle1_filtered_pitch = 0;
    float mAngle2_filtered_roll = 0;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;
    boolean shouldSend = false;
    Button mDraw;
    private SensorManager sensorManager;
    //sensor calculation values
    float[] mGravity = null;
    float[] mGeomagnetic = null;
    float Rmat[] = new float[9];
    float Imat[] = new float[9];
    float orientation[] = new float[3];
    int width = 0;
    int height = 0;
    private final static int INTERVAL = 1000;
    Timer mTimer;
    Firebase myFirebaseRef;
    Handler mHandler;
    ColorPicker mMainColorPicker;
    SensorEventListener mAccelerometerListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity = event.values.clone();
                processSensorData();
                update();
            }
        }
    };
    SensorEventListener mMagnetometerListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic = event.values.clone();
                processSensorData();
                update();


            }
        }
    };

    private float restrictAngle(float tmpAngle) {
        while (tmpAngle >= 180) tmpAngle -= 360;
        while (tmpAngle < -180) tmpAngle += 360;
        return tmpAngle;
    }

    //x is a raw angle value from getOrientation(...)
    //y is the current filtered angle value
    private float calculateFilteredAngle(float x, float y) {
        final float alpha = 0.3f;
        float diff = x - y;

        //here, we ensure that abs(diff)<=180
        diff = restrictAngle(diff);

        y += alpha * diff;
        //ensure that y stays within [-180, 180[ bounds
        y = restrictAngle(y);

        return y;
    }


    public void processSensorData() {
        if (mGravity != null && mGeomagnetic != null) {
            boolean success = SensorManager.getRotationMatrix(Rmat, Imat, mGravity, mGeomagnetic);
            if (success) {
                SensorManager.getOrientation(Rmat, orientation);
                mAngle0_azimuth = (float) Math.toDegrees((double) orientation[0]); // orientation contains: azimut, pitch and roll
                mAngle1_pitch = (float) Math.toDegrees((double) orientation[1]); //pitch
                mAngle2_roll = -(float) Math.toDegrees((double) orientation[2]); //roll
                mAngle0_filtered_azimuth = calculateFilteredAngle(mAngle0_azimuth, mAngle0_filtered_azimuth);
                mAngle1_filtered_pitch = calculateFilteredAngle(mAngle1_pitch, mAngle1_filtered_pitch);
                mAngle2_filtered_roll = calculateFilteredAngle(mAngle2_roll, mAngle2_filtered_roll);
                if(shouldSend)
                    updateFirebase();

            }
            mGravity = null; //oblige full new refresh
            mGeomagnetic = null; //oblige full new refresh
        }
    }


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_best_gyro);
        Firebase.setAndroidContext(this);
        mDraw = (Button) findViewById(R.id.bDraw);
        mDraw.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        myFirebaseRef.child("calibrate").setValue("1");
                        shouldSend = true;
                        myFirebaseRef.child("calibrate").setValue("1");
                        break;
                    case MotionEvent.ACTION_UP:
                        myFirebaseRef.child("calibrate").setValue("0");
                        shouldSend = false;
                        Log.e("touch", "up");
                        break;
                }
                return false;
            }
        });
        mMainColorPicker = (ColorPicker) findViewById(R.id.picker);
        mMainColorPicker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int i) {
                myFirebaseRef.child("color").setValue(Integer.valueOf(String.valueOf(i), 16));
            }
        });
        myFirebaseRef = new Firebase("https://coolbean.firebaseio.com/datapoints/");
        mTextView_azimuth = (TextView) findViewById(R.id.normAzimuth);
        mTextView_pitch = (TextView) findViewById(R.id.normPitch);
        mTextView_roll = (TextView) findViewById(R.id.normRoll);
        mTextView_filtered_azimuth = (TextView) findViewById(R.id.filterAzimuth);
        mTextView_filtered_pitch = (TextView) findViewById(R.id.filterPitch);
        mTextView_filtered_roll = (TextView) findViewById(R.id.filteredRoll);
        imageView = (ImageView) findViewById(R.id.imageView);
        bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        mHandler = new Handler();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(mAccelerometerListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(mMagnetometerListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
        //startRepeatingTask();


    }


    private void sendDataToFirebase() {
        new CountDownTimer(11000, 10000) {
            @Override
            public void onTick(long millisUntilFinished) {

                // Display Data by Every Ten Second
                myFirebaseRef.child("roll").setValue(String.valueOf(Math.toRadians(mAngle2_filtered_roll)));
                myFirebaseRef.child("pitch").setValue(String.valueOf(Math.toRadians(mAngle1_filtered_pitch)));
                myFirebaseRef.child("yaw").setValue(String.valueOf(Math.toRadians(mAngle0_filtered_azimuth)));
            }

            @Override
            public void onFinish() {
                sendDataToFirebase();
            }

        }.start();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(mAccelerometerListener);
        sensorManager.unregisterListener(mMagnetometerListener);
    }


    public void updateFirebase() {
        myFirebaseRef.child("roll").setValue(String.valueOf(Math.toRadians(mAngle2_filtered_roll)));
        myFirebaseRef.child("pitch").setValue(String.valueOf(Math.toRadians(mAngle1_filtered_pitch)));
        myFirebaseRef.child("yaw").setValue(String.valueOf(Math.toRadians(mAngle0_filtered_azimuth)));
    }

    private void update() {

        mTextView_azimuth.setText("Azimuth: " + String.valueOf(mAngle0_azimuth));
        mTextView_pitch.setText("Pitch: " + String.valueOf(mAngle1_pitch));
        mTextView_roll.setText("Roll: " + String.valueOf(mAngle2_roll));

        mTextView_filtered_azimuth.setText("Filtered Azimuth: " + String.valueOf(mAngle0_filtered_azimuth));
        mTextView_filtered_pitch.setText("Filtered Pitch: " + String.valueOf(mAngle1_filtered_pitch));
        mTextView_filtered_roll.setText("Filtered Roll: " + String.valueOf(mAngle2_filtered_roll));
//
//        myFirebaseRef.child("roll").setValue(String.valueOf(Math.toRadians(mAngle2_filtered_roll)));
//        myFirebaseRef.child("pitch").setValue(String.valueOf(Math.toRadians(mAngle1_filtered_pitch)));
//        myFirebaseRef.child("yaw").setValue(String.valueOf(Math.toRadians(mAngle0_filtered_azimuth)));


    }

    private void updateCanvas() {
        final String s = "{\n" +
                "  \"x\":" + mAngle1_filtered_pitch + ",\n" +
                "  \"y\":" + mAngle2_filtered_roll + "\n" +
                "}";
        DatagramSocket clientSocket = null;
        try {
            clientSocket = new DatagramSocket();
            InetAddress IPAddress = InetAddress.getByName("176.31.126.78");
            byte[] sendData = new byte[1024];
            byte[] receiveData = new byte[1024];
            sendData = s.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 5000);
            clientSocket.send(sendPacket);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String modifiedSentence = new String(receivePacket.getData());
            Toast.makeText(getApplicationContext(), "FROM SERVER:" + modifiedSentence, Toast.LENGTH_SHORT).show();
            clientSocket.close();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e("SENT", "message sent");
    }


    double map(double x, double in_min, double in_max, double out_min, double out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }


    public class SendUDP extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            updateCanvas();
            Log.e("SENT", "message sent");
            return null;
        }
    }

}
