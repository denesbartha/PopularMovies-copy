package com.denesb.popularmovies.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class FavouritesProvider extends ContentProvider {
    public static final int CODE_FAVOURITES = 100;
    public static final int CODE_FAVOURITES_WITH_MOVIEID = 100;

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private FavouritesDbHelper mOpenHelper;

    public static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = FavouritesContract.CONTENT_AUTHORITY;
        // content::/com.denesb.popularmovies.data/favourites/ */
        matcher.addURI(authority, FavouritesContract.PATH_FAVOURITES, CODE_FAVOURITES);
        // content::/com.denesb.popularmovies.data/favourites/#
        matcher.addURI(authority, FavouritesContract.PATH_FAVOURITES + "/#",
                CODE_FAVOURITES_WITH_MOVIEID);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new FavouritesDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        Cursor cursor;
        switch (sUriMatcher.match(uri)) {
            case CODE_FAVOURITES:
                cursor = mOpenHelper.getReadableDatabase().query(
                        FavouritesContract.FavouritesEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        Uri retUri; // URI to be returned

        switch (match) {
            case CODE_FAVOURITES_WITH_MOVIEID:
                long id = db.insert(FavouritesContract.FavouritesEntry.TABLE_NAME, null, values);
                if (id > 0) {
                    retUri = ContentUris.withAppendedId(
                            FavouritesContract.FavouritesEntry.CONTENT_URI, id);
                }
                else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return retUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int tasksDeleted; // starts as 0

        switch (match) {
            // Handle the single item case, recognized by the ID included in the URI path
            case CODE_FAVOURITES_WITH_MOVIEID:
                String id = uri.getPathSegments().get(1);
                tasksDeleted = db.delete(
                        FavouritesContract.FavouritesEntry.TABLE_NAME,
                        FavouritesContract.FavouritesEntry.COLUMN_MOVIE_ID + "=?",
                        new String[]{id});
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (tasksDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return tasksDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        throw new RuntimeException("We don't support update operation...");
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new RuntimeException("We don't support getType operation...");
    }
}
