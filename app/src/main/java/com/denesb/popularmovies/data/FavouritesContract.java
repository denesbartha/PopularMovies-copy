package com.denesb.popularmovies.data;

import android.net.Uri;
import android.provider.BaseColumns;

public class FavouritesContract {
    public static final String CONTENT_AUTHORITY = "com.denesb.popularmovies";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_FAVOURITES = "favourites";

    public static final class FavouritesEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_FAVOURITES)
                .build();

        public static final String TABLE_NAME = "favourites";
        public static final String COLUMN_MOVIE_ID = "movie_id";
    }
}
