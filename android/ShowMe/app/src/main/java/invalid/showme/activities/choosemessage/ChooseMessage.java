
package invalid.showme.activities.choosemessage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.acra.ACRA;

import invalid.showme.R;
import invalid.showme.activities.SharedActivityLogic;
import invalid.showme.exceptions.IntentDataException;
import invalid.showme.model.UserProfile;
import invalid.showme.model.photo.DraftPhoto;
import invalid.showme.util.BitmapUtils;
import invalid.showme.util.IOUtil;

public class ChooseMessage extends Activity {
    private final String TAG = "ViewPhotoNormal";

    private static final int NONE = 0;
    private static final int HAVEDRAFT = 1;
    private static final int HAVEPHOTO = 2;
    private int mode = NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedActivityLogic.ConfirmInitialization(getApplicationContext(), true);
        setContentView(R.layout.activity_choose_message);

        UserProfile up = (UserProfile)getApplication();
        Intent intent = getIntent();

        String message = intent.getStringExtra("message");
        EditText edit = (EditText)findViewById(R.id.choose_message_message);
        edit.selectAll();
        if(message != null && message.length() > 0)
            edit.setText(message);



        long draftID = intent.getLongExtra("draftToSend", -1);
        if(draftID != -1)
            mode = HAVEDRAFT;



        String thumbnailPath = intent.getStringExtra("thumbnailPath");
        if(thumbnailPath != null && mode != NONE) {
            String msg = "Somehow have multiple modes!";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(intent));
            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
            ACRA.getErrorReporter().removeCustomData("intentData");
            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        }
        else if(thumbnailPath != null)
            mode = HAVEPHOTO;



        if(mode == NONE) {
            String msg = "Somehow didn't get any mode!";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(getIntent()));
            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
            ACRA.getErrorReporter().removeCustomData("intentData");
        }


        ImageView imageView = (ImageView) findViewById(R.id.choose_message_image);
        if(mode == HAVEDRAFT) {
            DraftPhoto draft = up.findDraft(draftID);
            imageView.setImageBitmap(draft.getRealThumbnail(getApplicationContext()));
        } else if(mode == HAVEPHOTO) {
            BitmapUtils.setImageForView(imageView, thumbnailPath);
        }
    }

    public void doneWithMessage(View view) {
        EditText edit = (EditText)findViewById(R.id.choose_message_message);
        String message = edit.getText().toString();

        Intent result = new Intent();
        result.putExtra("message", message);
        setResult(1, result);
        finish();
    }
}
