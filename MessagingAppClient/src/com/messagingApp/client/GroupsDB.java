package com.messagingApp.client;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class GroupsDB {
	public static final String KEY_ROWID        = "_id";
	public static final String KEY_NAME         = "name";
	
	private static final String DATABASE_NAME    = "messagingAppDB";
    private static final String DATABASE_TABLE   = "groupsDB";
	private static final String TAG              = "GroupsDB";
	private static final String DATABASE_CREATE  = "CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE + " (" 
												 + KEY_ROWID        + " integer PRIMARY KEY autoincrement, " 
												 + KEY_NAME         + " text    NOT NULL);";
    private static final int    DATABASE_VERSION = 2;

    private final Context  mCtx;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if(db.getVersion() != newVersion) {
            	Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
            	db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE + ";");
                onCreate(db);
            }
        }
        
        public void createTable(SQLiteDatabase db) {
        	db.execSQL(DATABASE_CREATE);
        }
    }
	
	public GroupsDB(Context ctx) {
		mCtx = ctx;
	}
	
	public GroupsDB open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        mDbHelper.createTable(mDb);
        return this;
    }
	
	public void close() {
        mDbHelper.close();
    }
	
	public long createGroup(String name) {
		ContentValues initialValues = new ContentValues(); 
		initialValues.put(KEY_NAME, name);
		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}
	
	public boolean deleteGroup(long id) {
		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + id, null) > 0;
	}
	
	public Cursor getAllGroups() {
		return mDb.rawQuery("SELECT * FROM " + DATABASE_TABLE + " ORDER BY " + KEY_NAME, null);
	}
	
	public Cursor getGroup(long rowID) {
		Cursor cursor = mDb.query(DATABASE_TABLE, new String [] {KEY_ROWID, KEY_NAME}, KEY_ROWID + "=" + rowID, null, null, null, null);
		if(cursor != null)
			cursor.moveToFirst();
		return cursor;
	}
	
	public Cursor getGroupsByName(String name) {
		return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NAME}, KEY_NAME + " LIKE \'%" + name + "%\'", null, null, null, KEY_NAME);
	}
}
