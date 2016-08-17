package invalid.showme.activities.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.acra.ACRA;

import invalid.showme.exceptions.StrangeUsageException;

public class PreviewPhoto extends SurfaceView implements SurfaceHolder.Callback {
    private final static String TAG = "PreviewPhoto";

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Boolean currentCameraIsFront;

    public PreviewPhoto(Context context, Camera camera, Boolean cameraIsFront) {
        super(context);
        mCamera = camera;
        currentCameraIsFront = cameraIsFront;
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, "Error setting camera previews: " + e.getMessage());
            ACRA.getErrorReporter().handleException(e);
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        RefreshCamera(mCamera, null, false);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void RefreshCamera(Camera camera, Boolean cameraIsFront, Boolean wasJustReleased) {
        if(cameraIsFront != null)
            currentCameraIsFront = cameraIsFront;
        if (mHolder.getSurface() == null) {
            ACRA.getErrorReporter().handleException(new StrangeUsageException("Somehow got into RefreshCamera with null Surface"));
            return;
        }

        try {
            if(mCamera != null && !wasJustReleased) mCamera.stopPreview();
        } catch (Exception e) {
            ACRA.getErrorReporter().handleException(new Exception("Somehow caught exception while trying to stop preview", e));
        }

        mCamera = camera;
        try {
            //https://stackoverflow.com/questions/20559112/using-camera-in-landscape-mode-getting-preview-in-portrait
            Camera.Parameters parameters = mCamera.getParameters();
            if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                parameters.set("orientation", "portrait");
                mCamera.setDisplayOrientation(90);
                //Make it not-upside-down
                if(currentCameraIsFront)
                    parameters.setRotation(270);
                else
                    parameters.setRotation(90);
            }
            else {
                parameters.set("orientation", "landscape");
                mCamera.setDisplayOrientation(0);
                parameters.setRotation(0);
            }
            mCamera.setParameters(parameters);

            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera preview: " + e.getMessage());
            ACRA.getErrorReporter().handleException(new Exception("Error starting camera preview", e));
        }
    }
}
