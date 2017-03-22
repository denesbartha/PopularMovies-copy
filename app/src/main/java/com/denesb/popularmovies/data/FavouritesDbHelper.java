package com.denesb.popularmovies.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.denesb.popularmovies.data.FavouritesContract.FavouritesEntry;

public class FavouritesDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "favourites.db";
    private static final int DATABASE_VERSION = 2;

    public FavouritesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Creates the database for the first time.
     * @param db
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_FAVOURITES_TABLE =
                "CREATE TABLE " + FavouritesEntry.TABLE_NAME +
                    " (" +
                        FavouritesEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        FavouritesEntry.COLUMN_MOVIE_ID + " INTEGER NOT NULL, "  +
                        "UNIQUE (" + FavouritesEntry.COLUMN_MOVIE_ID + ") ON CONFLICT REPLACE);";
        db.execSQL(SQL_CREATE_FAVOURITES_TABLE);
    }

    /**
     * Recreates the table if neccessary.
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FavouritesEntry.TABLE_NAME);
        onCreate(db);
    }
}
