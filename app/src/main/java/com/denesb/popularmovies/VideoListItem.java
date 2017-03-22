package com.denesb.popularmovies;

import android.os.Parcel;
import android.os.Parcelable;

public class VideoListItem implements Parcelable {
    private String key;

    /**
     * getter for key
     * @return key
     */
    public String getKey() { return key; }

    public VideoListItem(String key) {
        this.key = key;
    }

    private VideoListItem(Parcel in) {
        key = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(key);
    }

    /**
     * Creator for Parcelable.
     */
    public final Parcelable.Creator<VideoListItem> CREATOR = new Parcelable.Creator<VideoListItem>() {
        @Override
        public VideoListItem createFromParcel(Parcel parcel) {
            return new VideoListItem(parcel);
        }

        @Override
        public VideoListItem[] newArray(int i) {
            return new VideoListItem[i];
        }

    };
}
