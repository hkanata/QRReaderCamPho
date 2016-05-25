package br.com.opba;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

import br.com.tfleet.qrreader.R;

public class MainActivity extends AppCompatActivity {

    //Log
    private static final String TAG = "LOGCAT";

    //TAG to Share Pref
    private static final String MyPREFERENCES = "MAIN_PREF";

    //You Can Use CameraSource.CAMERA_FACING_FRONT to Front Camera
    //You Can Use CameraSource.CAMERA_FACING_BACK to Back Camera
    private static final int CAMERA_FACE = CameraSource.CAMERA_FACING_BACK;

    //Request Camera
    private static final int REQUEST_CAMERA = 1;

    //Textview from activity
    TextView barcodeInfo;

    //Bar detector
    BarcodeDetector barcodeDetector;

    //Surface View
    SurfaceView cameraView;

    //Camera Source
    CameraSource cameraSource;

    //Global SharePref
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView  = (SurfaceView)findViewById(R.id.camera_view);
        barcodeInfo = (TextView)findViewById(R.id.code_info);

        /******************************/
        /****INITIAL FROM CAMERA*******/
        /******************************/
            sharedpreferences   = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
            Boolean startCamera = sharedpreferences.getBoolean("startCamera", Boolean.FALSE);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean("startCamera", Boolean.TRUE);
            editor.commit();

            //Check phone have camera
            if(!isCameraAvailable()){
                Log.i(TAG, "Cameras doenst exists FACING = " + CAMERA_FACE);
                return;
            }

            //API >= 23 - NEED Permission on Runtime application
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                askPermissionCamera();
            }

            //START THE CAMERA
            if( startCamera ) {
                readFromCamera();
            }
        /******************************/
        /******FINAL FROM CAMERA*******/
        /******************************/


        /******************************/
        /***INITIAL FROM PHOTO*********/
        /******************************/
            //readFromPhoto();
        /******************************/
        /*****FINAL FROM PHOTO*********/
        /******************************/
    }

    /*
    * Read QRCODE from anyphoto of Celphone
    * The Pic must have a QRCODE parts
    * */
    private void readFromPhoto() {
        Log.i(TAG, "Start Read Photo");

        //IMAGE VIEW
        ImageView imageView = (ImageView) findViewById(R.id.imageView);

        /*
        * You can use from Galery or from drawable
        * */

        //1 OPTION: Decode FROM DRAWABLE
        Bitmap myQRCode = BitmapFactory.decodeResource(getResources(), R.drawable.qr);

        //2 OPTION: Decode FROM GALERY
        //String photoDir = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/Camera/";
        //String imagem   = "MY_QR_CODE_PHOTO.jpg";
        //Bitmap myQRCode = BitmapFactory.decodeFile( photoDir + imagem );

        //Setting BITMAP
        imageView.setImageBitmap(myQRCode);

        //BarDetector
        BarcodeDetector barcodeDetector =
                new BarcodeDetector.Builder(this)
                        .setBarcodeFormats(Barcode.QR_CODE)
                        .build();

        //Frame
        Frame myFrame = new Frame.Builder()
                .setBitmap(myQRCode)
                .build();

        //Codes
        SparseArray<Barcode> barcodes = barcodeDetector.detect(myFrame);
        if(barcodes.size() != 0) {
            Log.d(TAG, barcodes.valueAt(0).displayValue);
            barcodeInfo.setText(""+barcodes.valueAt(0).displayValue);
        }
    }

    /*
    * Starts camera to READ QRCode
    * */
    private void readFromCamera() {
        Log.i(TAG, "Start Camera");

        //bar detector
        barcodeDetector =
                new BarcodeDetector.Builder(this)
                        .setBarcodeFormats(Barcode.QR_CODE)
                        .build();

        //Camera source
        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CAMERA_FACE)
                .build();

        /*
        * Surface Holder
        * */
        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    //Start camera on preview
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ie) {
                    Log.e(TAG, ie.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        /*
        * Processor barcode
        * */
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {}

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() != 0) {
                    barcodeInfo.post(new Runnable() {
                        public void run() {
                            Log.i(TAG, ""+barcodes.valueAt(0).displayValue);
                            barcodeInfo.setText(""+barcodes.valueAt(0).displayValue);
                        }
                    });
                }
            }
        });
    }

    /*
    * Ask permissions Camera
    * */
    private void askPermissionCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA},
                    REQUEST_CAMERA);

            //Default Denied
            setStartCameraPermission(Boolean.FALSE);
        }
    }

    /*
    * Check CAMERAS FACES
    * */
    private boolean isCameraAvailable() {
        int cameraCount = 0;
        boolean isFrontCameraAvailable = false;
        cameraCount = Camera.getNumberOfCameras();
        while (cameraCount > 0) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            cameraCount--;
            Camera.getCameraInfo(cameraCount, cameraInfo);
            if (cameraInfo.facing == CAMERA_FACE) {
                isFrontCameraAvailable = true;
                break;
            }
        }
        return isFrontCameraAvailable;
    }

    /*
    * Permissions denied SharePref
    * */
    private void setStartCameraPermission(Boolean type) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean("startCamera", type);
        editor.commit();
    }

    /*
    * Result permission RunTime
    * */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permission Camera Granted
                    setStartCameraPermission(Boolean.TRUE);

                    //umplements method here
                    //readFromCamera();

                    //Call Again THIS Activity
                    finish();
                    startActivity(getIntent());

                } else {
                    //Denied
                    setStartCameraPermission(Boolean.FALSE);

                    Toast.makeText(getApplicationContext(),
                            "Permission Camera Denied - Cannot Reader QRCode", Toast.LENGTH_LONG).show();

                }
                return;
            }
        }
    }
}

