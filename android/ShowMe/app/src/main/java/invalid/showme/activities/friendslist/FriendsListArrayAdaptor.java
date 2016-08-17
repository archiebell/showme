package invalid.showme.activities.friendslist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import invalid.showme.R;
import invalid.showme.layoutobjects.TextDrawable;
import invalid.showme.model.IFriend;

public class FriendsListArrayAdaptor extends ArrayAdapter<IFriend> {
    private final Context context;
    private final List<IFriend> values;

    public FriendsListArrayAdaptor(Context context, List<IFriend> values) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.friendslist_compactlist_friends_row, parent, false);
        }
        IFriend f = this.getItem(position);

        TextView nameView;
        nameView = (TextView) convertView.findViewById(R.id.friendslist_compactlist_friends_row_main);
        nameView.setText(f.getDisplayName());

        ImageView imageView;
        imageView = (ImageView) convertView.findViewById(R.id.friendslist_compactlist_friends_row_icon);

        if(!f.getDisplayName().equals("Save For Later")) {
            String letter = f.getDisplayName().substring(0, 1).toUpperCase();
            TextDrawable.ColorGenerator generator = TextDrawable.MATERIAL;

            TextDrawable icon;
            if (f.hasUnseenPhotos()) {
                icon = TextDrawable.builder()
                        .beginConfig()
                        .withBorder(10)
                        .bold()
                        .endConfig()
                        .buildRound(letter, generator.getColor(f.getDisplayName()));
            } else {
                icon = TextDrawable.builder()
                        .buildRound(letter, generator.getColor(f.getDisplayName()));
            }
            imageView.setImageDrawable(icon);
        } else {
            imageView.setImageResource(R.drawable.ic_camera_front_black_48dp);
        }

        return convertView;
    }
}
