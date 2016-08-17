package invalid.showme.activities.camera;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.acra.ACRA;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import invalid.showme.R;
import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.util.BitmapUtils;
import invalid.showme.util.PhotoFileManager;

//From http://examples.javacodegeeks.com/android/core/hardware/camera-hardware/android-camera-example/
//TODO: Replace with Camera2 class
public class TakePhoto extends Activity {
    private final static String TAG = "TakePhoto";

    private Camera mCamera;
    private PreviewPhoto mPreview;
    private byte[] photoBytes;
    private Bitmap previewBitmap;

    private boolean currentCameraIsFront = false;

    private ImageButton switchCameraButton;
    private ImageButton takePhotoButton;
    private ImageButton photoIsGoodButton;
    private ImageButton photoIsBadButton;
    private ImageView previewPhotoView;
    private ImageView previewPhotoBackground;
    private LinearLayout previewPhotoLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_takephoto);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        switchCameraButton = (ImageButton)findViewById(R.id.takephoto_button_switchcamera);
        takePhotoButton = (ImageButton)findViewById(R.id.takephoto_button_capture);
        photoIsGoodButton = (ImageButton)findViewById(R.id.takephoto_button_photoisgood);
        photoIsBadButton = (ImageButton)findViewById(R.id.takephoto_button_photoisbad);

        previewPhotoView = (ImageView)findViewById(R.id.takephoto_picture_preview);
        previewPhotoBackground = (ImageView)findViewById(R.id.takephoto_picture_background);

        mPreview = new PreviewPhoto(this, mCamera, null);

        previewPhotoLayout = (LinearLayout) findViewById(R.id.takephoto_camera_preview);
        previewPhotoLayout.addView(mPreview);

        //https://stackoverflow.com/questions/18460647/android-setfocusarea-and-auto-focus
        //  but with changes
        previewPhotoLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (mCamera != null) {
                    mCamera.cancelAutoFocus();
                    Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);

                    Camera.Parameters parameters = mCamera.getParameters();
                    if (!parameters.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                    if (parameters.getMaxNumFocusAreas() > 0) {
                        List<Camera.Area> mylist = new ArrayList<>();
                        mylist.add(new Camera.Area(focusRect, 1000));
                        parameters.setFocusAreas(mylist);
                    }

                    try {
                        mCamera.cancelAutoFocus();
                        mCamera.setParameters(parameters);
                        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                                /*if (camera.getParameters().getFocusMode() != Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
                                    Camera.Parameters parameters = camera.getParameters();
                                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                    if (parameters.getMaxNumFocusAreas() > 0) {
                                        parameters.setFocusAreas(null);
                                    }
                                    camera.setParameters(parameters);
                                }*/
                            }
                        });
                    } catch (Exception e) {
                        ACRA.getErrorReporter().handleException(new Exception("Strange exception in focusing code.", e));
                    }
                }
                return true;
            }
        });
    }
    private Rect calculateTapArea(float x, float y, float coefficient) {
        float focusAreaSize = 210;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

        int left = clamp((int) x - areaSize / 2, 0, previewPhotoLayout.getWidth() - areaSize, 1000-areaSize);
        int top = clamp((int) y - areaSize / 2, 0, previewPhotoLayout.getHeight() - areaSize, 1000-areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }
    private int clamp(int x, int min, int max, int otherMax) {
        if (x > otherMax)
            return otherMax;
        if (x > max)
            return max;
        if (x < min)
            return min;
        return x;
    }

    @Override
    public void onResume() {
        super.onResume();
        //TODO: Any camera.open call can fail, catch and handle exception.
        currentCameraIsFront = !PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("pref_currentcameraisback", true);
        if (mCamera == null) {
            if (findFrontFacingCamera() < 0) {
                switchCameraButton.setVisibility(View.GONE);
                mCamera = Camera.open(findBackFacingCamera());
            } else if(currentCameraIsFront) {
                mCamera = Camera.open(findFrontFacingCamera());
            } else {
                mCamera = Camera.open(findBackFacingCamera());
            }

            mPreview.RefreshCamera(mCamera, currentCameraIsFront, false);
        }

        if(currentCameraIsFront)
            switchCameraButton.setImageResource(R.drawable.ic_camera_rear_white_48dp);
        else
            switchCameraButton.setImageResource(R.drawable.ic_camera_front_white_48dp);

        SwapButtons(true);
        photoBytes = null; //not good, but don't know how to display data so might as well delete it
        if(previewBitmap != null) previewBitmap.recycle();
        previewBitmap = null;
    }

    private void SwapButtons(Boolean showTakePhotoButtons)
    {
        switchCameraButton.setVisibility(showTakePhotoButtons ? View.VISIBLE : View.INVISIBLE);
        takePhotoButton.setVisibility(showTakePhotoButtons ? View.VISIBLE : View.INVISIBLE);
        photoIsGoodButton.setVisibility(showTakePhotoButtons ? View.INVISIBLE : View.VISIBLE);
        photoIsBadButton.setVisibility(showTakePhotoButtons ? View.INVISIBLE : View.VISIBLE);
        previewPhotoBackground.setVisibility(showTakePhotoButtons ? View.INVISIBLE : View.VISIBLE);
        previewPhotoView.setVisibility(showTakePhotoButtons ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    public void switchCamera(View v) {
        int numberOfCameras = Camera.getNumberOfCameras();
        if (numberOfCameras > 1) {
            releaseCamera();
            int cameraId = -1;
            if (currentCameraIsFront) {
                cameraId = findBackFacingCamera();
                currentCameraIsFront = false;
                switchCameraButton.setImageResource(R.drawable.ic_camera_front_white_48dp);
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("pref_currentcameraisback", true).commit();
            } else {
                cameraId = findFrontFacingCamera();
                currentCameraIsFront = true;
                switchCameraButton.setImageResource(R.drawable.ic_camera_rear_white_48dp);
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("pref_currentcameraisback", false).commit();
            }

            if (cameraId >= 0) {
                mCamera = Camera.open(cameraId);
                mPreview.RefreshCamera(mCamera, currentCameraIsFront, true);
            } else {
                String msg = "Somehow had more than one camera but couldn't switch to other camera";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new StrangeUsageException(msg));
            }
        } else {
            String msg = "Somehow tried to switch camera when only have one camera.";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().handleException(new StrangeUsageException(msg));
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }



    public void capturePhoto(View v) {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                photoBytes = bytes;
                previewBitmap = BitmapUtils.DecodeByteArrayForSize(photoBytes, BitmapUtils.THUMBNAIL_SIZE, BitmapUtils.THUMBNAIL_SIZE);
                if(currentCameraIsFront) {
                    float[] mirrorY = { -1, 0, 0, 0, 1, 0, 0, 0, 1};
                    Matrix m = new Matrix();
                    m.setValues(mirrorY);
                    previewBitmap = Bitmap.createBitmap(previewBitmap, 0,0,previewBitmap.getWidth(), previewBitmap.getHeight(), m, true);
                }
                BitmapUtils.setImageForView(previewPhotoView, previewBitmap);
                SwapButtons(false);

                //TODO: hack. When rotate don't know how to re-display old preview
                //      (even if went and stashed data somewhere else, which don't know where)
                //      Instead, just don't allow orientation change while previewing
                if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });
    }

    public void photoIsGood(View v) {
        SwapButtons(true);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        try {
            File photoFile = PhotoFileManager.createTempImageFile(getApplicationContext());

            //thumbnail already flipped, and want to conserve memory, so write it out first
            File thumbnailFile = PhotoFileManager.GetThumbnailFile(photoFile.getAbsoluteFile());
            PhotoFileManager.createThumbnailFromThumbnailBitmapToFile(previewBitmap, thumbnailFile);
            previewBitmap.recycle();

            if(currentCameraIsFront) { //Need to flip image
                Bitmap img = null;
                img = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);

                photoBytes = null;

                float[] mirrorY = { -1, 0, 0, 0, 1, 0, 0, 0, 1};
                Matrix m = new Matrix();
                m.setValues(mirrorY);
                img = Bitmap.createBitmap(img, 0,0,img.getWidth(), img.getHeight(), m, true);

                PhotoFileManager.createTempImageFile(photoFile, img);
                img.recycle();
            }
            else { //No need to flip
                PhotoFileManager.createTempImageFile(photoFile, photoBytes);
            }
            photoBytes = null;

            Intent data = new Intent();
            data.putExtra("photoPath", photoFile.getAbsolutePath());
            data.putExtra("thumbnailPath", thumbnailFile.getAbsolutePath());
            setResult(Activity.RESULT_OK, data);
            finish();
        } catch(Exception e) {
            Toast.makeText(getApplicationContext(), "Error: Could not save photo.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Could not save photo");
            ACRA.getErrorReporter().handleException(e);

            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }
    public void photoIsBad(View v) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        previewBitmap.recycle();
        previewBitmap = null;
        mPreview.RefreshCamera(mCamera, null, false);
        SwapButtons(true);
    }



    private int findFrontFacingCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                return i;
        }
        return -1;
    }

    private int findBackFacingCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                return i;
        }
        return -1;
    }
}
