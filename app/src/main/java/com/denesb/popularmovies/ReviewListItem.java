package com.denesb.popularmovies;

import android.os.Parcel;
import android.os.Parcelable;

public class ReviewListItem implements Parcelable {
    private String author;
    private String content;

    /**
     * getter for author
     * @return author
     */
    public String getAuthor() { return author; }

    /**
     * getter for content
     * @return
     */
    public String getContent() { return content; }

    public ReviewListItem(String author, String content) {
        this.author = author;
        this.content = content;
    }

    private ReviewListItem(Parcel in) {
        author = in.readString();
        content = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(author);
        parcel.writeString(content);
    }

    /**
     * Creator for Parcelable.
     */
    public final Parcelable.Creator<ReviewListItem> CREATOR = new Parcelable.Creator<ReviewListItem>() {
        @Override
        public ReviewListItem createFromParcel(Parcel parcel) {
            return new ReviewListItem(parcel);
        }

        @Override
        public ReviewListItem[] newArray(int i) {
            return new ReviewListItem[i];
        }

    };
}
