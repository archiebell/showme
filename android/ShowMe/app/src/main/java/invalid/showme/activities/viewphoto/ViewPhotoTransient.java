package invalid.showme.activities.viewphoto;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.acra.ACRA;

import de.greenrobot.event.EventBus;
import invalid.showme.R;
import invalid.showme.activities.SharedActivityLogic;
import invalid.showme.exceptions.IntentDataException;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.photo.Photo;
import invalid.showme.model.photo.ReceivedPhoto;
import invalid.showme.util.IOUtil;

public class ViewPhotoTransient extends Activity implements View.OnTouchListener {
    private final String TAG = "ViewPhotoTransient";

    private final static int NONE = 0;
    private final static int RECEIVEDPHOTO = 1;
    private final static int SENTPHOTO = 2;
    private int mode;

    private TextView coverupTextView;

    private Countdown handler;
    private static class Countdown extends Handler
    {
        private TextView countdownTextView;
        private int MessageCode;

        public Countdown(TextView c)
        {
            countdownTextView = c;
        }

        @Override
        public void handleMessage(Message msg) {
            CharSequence currently = countdownTextView.getText();
            Integer sec = Integer.parseInt(currently.toString());
            if (sec == 1)
                EventBus.getDefault().post(new PhotoViewTimeout());
            else {
                countdownTextView.setText(Integer.toString(sec - 1));
                Message nxt = this.obtainMessage();
                this.MessageCode = nxt.what;
                this.sendMessageDelayed(nxt, 1000);
            }
        }
    }

    private static Integer SECONDS = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedActivityLogic.ConfirmInitialization(getApplicationContext(), true);
        setContentView(R.layout.activity_view_phototransient);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        hideStatusBars();

        UserProfile up = (UserProfile)getApplication();

        Intent intent = getIntent();
        long photoId = intent.getLongExtra("photoToView", -1);
        if(photoId != -1)
            mode = RECEIVEDPHOTO;

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

        Photo photo;
        if(mode == RECEIVEDPHOTO) {
            photo = up.findPhoto(photoId);
            ((ReceivedPhoto) photo).SetAsSeen(DBHelper.getInstance(getApplicationContext()));
        }
        else
            photo = up.findSent(sentId);

        ImageView imageView = (ImageView)findViewById(R.id.view_phototransient_photo);
        //could show smaller resolution photo, but...
        //decryptedPhoto = photo.getDecryptedPhoto(BitmapUtils.GetScreenWidth(getWindowManager()), BitmapUtils.GetScreenHeight(getWindowManager()));
        Bitmap decryptedPhoto = photo.getDecryptedPhoto();
        imageView.setImageBitmap(decryptedPhoto);
        imageView.setOnTouchListener(this);

        TextView textView = (TextView)findViewById(R.id.view_phototransient_message);
        if(photo.Message != null && photo.Message.length() > 0)
            textView.setText(photo.Message);
        else
            textView.setVisibility(View.INVISIBLE);

        TextView countdownTextView = (TextView) findViewById(R.id.view_phototransient_countdown);
        countdownTextView.setText(SECONDS.toString());

        coverupTextView = (TextView)findViewById(R.id.view_phototransient_coverup);

        handler = new Countdown(countdownTextView);
        EventBus.getDefault().register(this);
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            coverupTextView.setVisibility(View.INVISIBLE);
            Message msg = handler.obtainMessage();
            handler.sendMessageDelayed(msg, 1000);
        } else if(event.getAction() == MotionEvent.ACTION_UP) {
            //Close activity
            finish();
        }
        return true;
    }

    @Override
    public void finish() {
        super.finish();

        this.handler.removeMessages(this.handler.MessageCode);
    }

    @SuppressWarnings({"unused", "UnusedParameters"})
    public void onEvent(PhotoViewTimeout event)
    {
        finish();
    }
}
