package com.messagingApp.client;

import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MembersGroupDB {
	public static final String KEY_ROWID        = "_id";
	public static final String KEY_IDGROUP      = "group_id";
	public static final String KEY_MEMBER_PHONE = "number";
	public static final String KEY_MEMBER_NAME  = "name";
	
	private static final String DATABASE_NAME    = "messagingAppDB";
    private static final String DATABASE_TABLE   = "membersGroupDB";
	private static final String TAG              = "MembersGroupDB";
	private static final String DATABASE_CREATE  = "CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE + " (" 
												 + KEY_ROWID        + " integer PRIMARY KEY autoincrement, " 
												 + KEY_IDGROUP      + " integer NOT NULL, "
												 + KEY_MEMBER_NAME  + " text    NOT NULL, "
												 + KEY_MEMBER_PHONE + " integer NOT NULL);";
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
	
	public MembersGroupDB(Context ctx) {
		mCtx = ctx;
	}
	
	public MembersGroupDB open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        mDbHelper.createTable(mDb);
        return this;
    }
	
	public void close() {
        mDbHelper.close();
    }
	
	public long addMember(long idGroup, String name, long number) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_IDGROUP, idGroup);
		initialValues.put(KEY_MEMBER_NAME, name);
		initialValues.put(KEY_MEMBER_PHONE, number);
		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}
	
	public void addMembersVector(long idGroup, Vector<String> names, Vector<Integer> numbers) {
		for(int i = 0; i < names.size(); i++)
			addMember(idGroup, names.get(i), numbers.get(i).intValue());
	}
	
	public boolean deleteMemberFromGroup(long id) {
		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + id, null) > 0;
	}
	
	public boolean deleteAllMembersGroup(long idGroup) {
		return mDb.delete(DATABASE_TABLE, KEY_IDGROUP + "=" + idGroup, null) > 0;
	}
	
	public Cursor getMembersGroup(long idGroup) {
		return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_IDGROUP, KEY_MEMBER_NAME,KEY_MEMBER_PHONE},
					 	 KEY_IDGROUP + "=" + idGroup, null, null, null, KEY_MEMBER_NAME);
	}
	
	public Cursor getMember(long id) {
		Cursor cursor = mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_IDGROUP, KEY_MEMBER_NAME,KEY_MEMBER_PHONE},
								  KEY_ROWID + "=" + id, null, null, null, null);
		if(cursor != null)
			cursor.moveToFirst();
		return cursor;
	}
}
