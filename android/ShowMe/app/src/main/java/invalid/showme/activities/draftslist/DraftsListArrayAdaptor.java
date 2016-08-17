package invalid.showme.activities.draftslist;

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
import android.widget.RelativeLayout;

import java.util.HashSet;
import java.util.List;

import de.greenrobot.event.EventBus;
import invalid.showme.R;
import invalid.showme.model.UserProfile;
import invalid.showme.model.photo.DraftPhoto;
import invalid.showme.model.photo.getthumbnail.DraftThumbnailLoadEvent;
import invalid.showme.model.photo.getthumbnail.PhotoGetThumbnailJob;

class DraftsListArrayAdaptor extends ArrayAdapter<DraftPhoto> {
    private final static String TAG = "DraftsListArrayAdaptor";

    private final Context context;
    private final GridView container;
    private final UserProfile application;
    private HashSet<Long> thumbnailsRequested;

    public DraftsListArrayAdaptor (Context context, GridView container, UserProfile up, List<DraftPhoto> values) {
        super(context, -1, values);
        this.context = context;
        this.application = up;
        this.container = container;
        this.thumbnailsRequested = new HashSet<>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.draftslist_photo, parent, false);
        }

        DraftPhoto photo = this.getItem(position);
        RelativeLayout imageContainer = (RelativeLayout) convertView.findViewById(R.id.draftslist_container);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.draftslist_image);
        ImageView privateImageIcon = (ImageView) convertView.findViewById(R.id.draftslist_privateimageicon);
        ProgressBar loadingIndicator = (ProgressBar) convertView.findViewById(R.id.loadingIndicator);

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
            if(imageContainer.getLocalVisibleRect(scrollBounds) && !this.thumbnailsRequested.contains(photo.getID())) {
                if (thumbnailsRequested.size() == 0)
                    EventBus.getDefault().register(this);
                this.thumbnailsRequested.add(photo.getID());
                this.application.getJobManager().addJob(new PhotoGetThumbnailJob(photo));
            } else if(!imageView.getLocalVisibleRect(scrollBounds)) {
                Log.d(TAG, "Draft with id " + photo.getID() + " out of sight.");
            }
        }

        return convertView;
    }

    public void onEvent(DraftThumbnailLoadEvent event) {
        this.thumbnailsRequested.remove(event.DraftPhotoId);
        if(this.thumbnailsRequested.size() == 0)
            EventBus.getDefault().unregister(this);
    }
}
