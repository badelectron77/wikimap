package com.medeozz.wikimap;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WikiMapDB";

    public static final String TABLE_ARTICLES_NAME = "articles_v05"; // auch unten aendern!!!!!

    static final String MARKER_ID            = "marker_id";
    static final String LAT                  = "lat";
    static final String LNG                  = "lng";
    static final String TITLE                = "title";
    static final String SUMMARY              = "summary";
    static final String WIKIPEDIA_URL        = "wikipedia_url";
    static final String ELEVATION            = "elevation";
    static final String LANG                 = "lang";
    static final String THUMBNAIL_IMAGE_NAME = "thumbnail_img_name";
    static final String LIST_IMAGE_URL       = "list_img_url";
    static final String TEMP_DISTANCE        = "temp_distance";

    public DbHelper(Context context) {

        super(context, DATABASE_NAME, null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Tabelle anlegen wenn nicht existent
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ARTICLES_NAME + " (" +
                MARKER_ID +             " TEXT PRIMARY KEY NOT NULL, " +
                LAT +                   " REAL, " +
                LNG +                   " REAL, " +
                TITLE +                 " TEXT, " +
                SUMMARY +               " TEXT, " +
                WIKIPEDIA_URL +         " TEXT, " +
                ELEVATION +             " INTEGER, " +
                LANG +                  " TEXT, " +
                THUMBNAIL_IMAGE_NAME +  " TEXT, " +
                LIST_IMAGE_URL +        " TEXT, " +
                TEMP_DISTANCE +         " REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.w("Constants", "Upgrading database, which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTICLES_NAME);
        onCreate(db);
    }
}