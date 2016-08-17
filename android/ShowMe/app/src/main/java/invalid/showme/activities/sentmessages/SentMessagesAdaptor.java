package invalid.showme.activities.sentmessages;

import android.content.Context;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;

import de.greenrobot.event.EventBus;
import invalid.showme.R;
import invalid.showme.model.IFriend;
import invalid.showme.model.UserProfile;
import invalid.showme.model.photo.SentPhoto;
import invalid.showme.model.photo.getthumbnail.PhotoGetThumbnailJob;
import invalid.showme.model.photo.getthumbnail.SentPhotoThumbnailLoadEvent;
import invalid.showme.util.TimeUtil;

class SentMessagesAdaptor extends ArrayAdapter<SentPhoto> {
    private final static String TAG = "SentMessagesAdaptor";

    private Context context;
    private final GridView container;
    private final UserProfile application;
    private HashSet<Long> thumbnailsRequested;

    public SentMessagesAdaptor(Context context, GridView container, UserProfile up, List<SentPhoto> values) {
        super(context, -1, values);
        this.context = context;
        this.container = container;
        this.application = up;
        this.thumbnailsRequested = new HashSet<>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.sentmessages_photo, parent, false);
        }
        SentPhoto photo = this.getItem(position);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.sentmessage_image);
        ImageView privateImageIcon = (ImageView) convertView.findViewById(R.id.sentmessage_privateimageicon);
        ProgressBar loadingIndicator = (ProgressBar) convertView.findViewById(R.id.sentmessage_loadingIndicator);
        TextView textName = (TextView)convertView.findViewById(R.id.sentmessage_name);
        TextView sentTime = (TextView)convertView.findViewById(R.id.sentmessage_senttime);
        TextView status = (TextView)convertView.findViewById(R.id.sentmessage_status);

        IFriend friend = application.findFriend(photo.FriendID);
        if(friend == null) {
            textName.setText("-- Deleted Friend --");
        } else {
            textName.setText(friend.getDisplayName());
        }

        sentTime.setText(TimeUtil.ToFriendlyWords(photo.Sent));

        if(photo.Status == SentPhoto.SentPhotoStatus.Encrypting)
            status.setBackgroundResource(R.drawable.circleyellow);
        else if(photo.Status == SentPhoto.SentPhotoStatus.Queued)
            status.setBackgroundResource(R.drawable.circleorange);
        else if(photo.Status == SentPhoto.SentPhotoStatus.Sent)
            status.setBackgroundResource(R.drawable.circleblue);
        else if(photo.Status == SentPhoto.SentPhotoStatus.Received)
            status.setBackgroundResource(R.drawable.circlegreen);
        else
            status.setBackgroundResource(R.drawable.circlered);

        Boolean photoLoaded;
        if(photo.PrivatePhoto)
            photoLoaded = photo.privateThumbnailLoaded(context);
        else
            photoLoaded = photo.realThumbnailLoaded(context);

        if(photoLoaded) {
            loadingIndicator.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
            if(photo.PrivatePhoto) {
                imageView.setImageBitmap(photo.getPrivateThumbnail(context));
                privateImageIcon.setVisibility(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_privacypleaseicons", true) ? View.INVISIBLE : View.VISIBLE);
            } else {
                imageView.setImageBitmap(photo.getRealThumbnail(context));
                privateImageIcon.setVisibility(View.INVISIBLE);
            }
        } else {
            imageView.setVisibility(View.INVISIBLE);
            privateImageIcon.setVisibility(View.INVISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);

            Rect scrollBounds = new Rect();
            this.container.getHitRect(scrollBounds);
            if(imageView.getLocalVisibleRect(scrollBounds) && !this.thumbnailsRequested.contains(photo.getID())) {
                if (thumbnailsRequested.size() == 0)
                    EventBus.getDefault().register(this);
                this.thumbnailsRequested.add(photo.getID());
                this.application.getJobManager().addJob(new PhotoGetThumbnailJob(photo));
            } else if(!imageView.getLocalVisibleRect(scrollBounds)) {
                Log.d(TAG, "Photo with id " + photo.getID() + " out of sight.");
            }
        }

        return convertView;
    }

    public void onEvent(SentPhotoThumbnailLoadEvent event) {
        this.thumbnailsRequested.remove(event.SentPhotoId);
        if(this.thumbnailsRequested.size() == 0)
            EventBus.getDefault().unregister(this);
    }
}
