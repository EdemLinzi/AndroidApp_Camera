package com.example.cameraapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button btnTakePicture;
    TextureView textureView;

    String cameraId;
    CameraDevice cameraDevice;
    CameraCaptureSession cameraCaptureSessions;
    CaptureRequest.Builder captureRequest;
    Size imageSize;
    ImageReader imageReader;

    Intent intent;

    File file;
    public static final int CAMERA_REQUEST = 200;
    Handler backgroundHandler;
    HandlerThread handlerThread;

    String lastPicUri="";

    private static final SparseIntArray ORIENTATIONS=new SparseIntArray();
    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnTakePicture = findViewById(R.id.btn_take_picture);
        textureView = findViewById(R.id.texture_view);

        textureView.setSurfaceTextureListener(textureListener);

        intent = new Intent(this,Galery.class);
    }

    public void lastPic(View v){
        startActivity(intent);
    }

    public void takePicture(View v){
        if(cameraDevice == null) return;
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpgsize = null;
            if(cameraCharacteristics != null){
                jpgsize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(ImageFormat.JPEG);
            }

            int width = 1200;
            int height = 780;

            if(jpgsize != null && jpgsize.length >0){
                width = jpgsize[0].getWidth();
                height = jpgsize[0].getHeight();
            }

            ImageReader reader = ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
            List<Surface> outSurface =new ArrayList<>(2);
            outSurface.add(reader.getSurface());
            outSurface.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);


            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));

            String randomUUID = UUID.randomUUID().toString();
            file = new File(Environment.getExternalStorageDirectory()+"/"+randomUUID+".jpg");
            lastPicUri = Environment.getExternalStorageDirectory()+"/"+randomUUID+".jpg";

            intent.putExtra("imageUrl",lastPicUri);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;

                    image = reader.acquireLatestImage();
                    ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[byteBuffer.capacity()];
                    byteBuffer.get(bytes);
                    save(bytes);

                }


                private void save(byte[] bytes){
                    OutputStream outputStream = null;
                    try {
                        outputStream = new FileOutputStream(file);
                        outputStream.write(bytes);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener,backgroundHandler);
            final CameraCaptureSession.CaptureCallback captureLitener = new CameraCaptureSession.CaptureCallback(){
                @Override
                public void onCaptureCompleted( CameraCaptureSession session,CaptureRequest request,TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Log.i("MainActivity",""+file);
                    Toast.makeText(MainActivity.this,"Saved "+file,Toast.LENGTH_LONG).show();
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(),captureLitener,backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            },backgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void createCameraPreview(){
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageSize.getWidth(),imageSize.getHeight());
            Surface surface = new Surface(texture);

            captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequest.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if(cameraDevice == null) return;
                    cameraCaptureSessions = session;
                    updatePreview();

                }

                @Override
                public void onConfigureFailed( CameraCaptureSession session) {

                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void updatePreview(){
        captureRequest.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequest.build(),null,backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void openCampera(){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            imageSize = map.getOutputSizes(SurfaceTexture.class)[0];

            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },CAMERA_REQUEST);
                return;
            }
            cameraManager.openCamera(cameraId,stateCallback,null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == CAMERA_REQUEST){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"You can't use camera",Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCampera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroudThread();
        if(textureView.isAvailable()){
            openCampera();
        }
        else{
            textureView.setSurfaceTextureListener(textureListener);
        }

    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    private void stopBackgroundThread() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void startBackgroudThread() {
        handlerThread = new HandlerThread("Camera Background");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

    }
}
