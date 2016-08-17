package invalid.showme.activities.viewphoto;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.acra.ACRA;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import invalid.showme.R;
import invalid.showme.activities.SharedActivityLogic;
import invalid.showme.exceptions.IntentDataException;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.photo.Photo;
import invalid.showme.model.photo.ReceivedPhoto;
import invalid.showme.util.BitmapUtils;
import invalid.showme.util.IOUtil;
import invalid.showme.util.PhotoFileManager;

public class ViewPhotoNormal extends Activity {
    private final String TAG = "ViewPhotoNormal";

    private final static int NONE = 0;
    private final static int RECEIVEDPHOTO = 1;
    private final static int DRAFTPHOTO = 2;
    private final static int SENTPHOTO = 3;
    private final static int PLAINTEXTPHOTO = 4;
    private int mode;

    private Photo photo;
    private String thumbnailPath;

    private RelativeLayout controlContainer;
    private Countdown handler;
    private static class Countdown extends Handler
    {
        private RelativeLayout controlContainer;

        public Countdown(RelativeLayout c)
        {
            controlContainer = c;
        }

        @Override
        public void handleMessage(Message msg) {
            controlContainer.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedActivityLogic.ConfirmInitialization(getApplicationContext(), true);
        setContentView(R.layout.activity_view_photonormal);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        UserProfile up = (UserProfile)getApplication();
        Intent intent = getIntent();

        long photoId = intent.getLongExtra("photoToView", -1);
        if(photoId != -1)
            mode = RECEIVEDPHOTO;

        long draftId = intent.getLongExtra("draftToView", -1);
        if(draftId != -1 && mode != NONE) {
            String msg = "Somehow have both draft and photo to view??";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(getIntent()));
            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
            ACRA.getErrorReporter().removeCustomData("intentData");
        }
        else if(draftId != -1)
            mode = DRAFTPHOTO;

        long sentId = intent.getLongExtra("sentToView", -1);
        if(sentId != -1 && mode != NONE) {
            String msg = "Somehow have both another mode and sent photo??";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(getIntent()));
            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
            ACRA.getErrorReporter().removeCustomData("intentData");
        }
        else if(sentId != -1)
            mode = SENTPHOTO;

        this.thumbnailPath = intent.getStringExtra("thumbnailPath");
        if(this.thumbnailPath != null && mode != NONE) {
            String msg = "Somehow have another mode and image path??";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(getIntent()));
            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
            ACRA.getErrorReporter().removeCustomData("intentData");
        }
        else if(this.thumbnailPath != null)
            mode = PLAINTEXTPHOTO;

        if(mode == NONE) {
            String msg = "Somehow didn't get any mode!";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(getIntent()));
            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
            ACRA.getErrorReporter().removeCustomData("intentData");
        }

        ZoomableImageView imageView = (ZoomableImageView)findViewById(R.id.view_photonormal_photo);
        TextView textView = (TextView)findViewById(R.id.view_photonormal_message);

        if(mode == RECEIVEDPHOTO) {
            ReceivedPhoto photo = up.findPhoto(photoId);
            this.photo = photo;
            photo.SetAsSeen(DBHelper.getInstance(getApplicationContext()));

            Bitmap decryptedPhoto = this.photo.getDecryptedPhoto();
            imageView.setImageBitmap(decryptedPhoto);

            if(this.photo.Message != null && this.photo.Message.length() > 0)
                textView.setText(this.photo.Message);
            else
                textView.setVisibility(View.INVISIBLE);
        } else if(mode == DRAFTPHOTO) {
            this.photo = up.findDraft(draftId);
            imageView.setImageBitmap(this.photo.getDecryptedPhoto());

            if(this.photo.Message != null && this.photo.Message.length() > 0)
                textView.setText(this.photo.Message);
            else
                textView.setVisibility(View.INVISIBLE);
        } else if(mode == SENTPHOTO) {
            this.photo = up.findSent(sentId);
            imageView.setImageBitmap(this.photo.getDecryptedPhoto());

            if(this.photo.Message != null && this.photo.Message.length() > 0)
                textView.setText(this.photo.Message);
            else
                textView.setVisibility(View.INVISIBLE);
        } else if(mode == PLAINTEXTPHOTO) {
            this.photo = null;
            BitmapUtils.setImageForView(imageView, this.thumbnailPath);
            textView.setVisibility(View.INVISIBLE);
        }

        hideStatusBars();

        controlContainer = (RelativeLayout)findViewById(R.id.view_photonormal_controlContainer);
        handler = new Countdown(controlContainer);
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideStatusBars();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        hideStatusBars();
    }

    private void hideStatusBars()
    {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        ActionBar actionBar = getActionBar();
        if(actionBar != null)
            actionBar.hide();
    }

    public void showControls(View view)
    {
        controlContainer.setVisibility(View.VISIBLE);

        Message msg = handler.obtainMessage();
        handler.sendMessageDelayed(msg, 2000);
    }

    public void sharePhoto(View view)
    {
        controlContainer.setVisibility(View.INVISIBLE);

        File plaintextPhoto;
        try {
            if (mode == PLAINTEXTPHOTO)
                plaintextPhoto = PhotoFileManager.CreateReadablePhoto(getApplicationContext(), new FileInputStream(this.thumbnailPath));
            else
                plaintextPhoto = PhotoFileManager.CreateReadablePhoto(getApplicationContext(), this.photo.getDecryptedPhotoStream());

            Uri uriToImage = FileProvider.getUriForFile(getApplicationContext(), "invalid.showme.photosharer", plaintextPhoto);

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uriToImage);
            shareIntent.setType("image/jpeg");
            shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Photo"));
        } catch(IOException e) {
            ACRA.getErrorReporter().handleException(e);
        }
    }

    public void deletePhoto(View view)
    {
        controlContainer.setVisibility(View.VISIBLE);

        Message msg = handler.obtainMessage();
        handler.sendMessageDelayed(msg, 2000);
    }
}
