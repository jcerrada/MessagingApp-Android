package com.messagingApp.client;

import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ContactsDB {
	public static final String KEY_NUMBER = "_id";
	public static final String KEY_NAME   = "name";
	
	private static final String DATABASE_NAME    = "messagingAppDB";
    private static final String DATABASE_TABLE   = "contactsDB";
	private static final String TAG              = "ContactsDB";
	private static final String DATABASE_CREATE  = "CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE + " (" 
												 + KEY_NUMBER + " integer PRIMARY KEY," 
												 + KEY_NAME   + " text NOT NULL);";
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
            Log.v("ContactsDB", "Creating ContactsDB...");
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
	
	public ContactsDB(Context ctx) {
		mCtx = ctx;
	}
	
	public ContactsDB open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        mDbHelper.createTable(mDb);
        return this;
    }
	
	public void close() {
		if(mDb != null)
			mDb.close();
		mDbHelper.close();		
    }
	
	private boolean createContact(int phoneNumber, String name) {
		ContentValues initialValues = new  ContentValues();
		initialValues.put(KEY_NUMBER, phoneNumber);
		initialValues.put(KEY_NAME, name);
		return mDb.insert(DATABASE_TABLE, null, initialValues) != -1;
	}
	
	public void addContacts(Vector<String> names, Vector<Integer> phones) {
		for(int i = 0; i < names.size(); i++)
			createContact(phones.get(i).intValue(), names.get(i));
	}
	
	public void deleteAllContacts() {
		mDb.delete(DATABASE_TABLE, null, null);
	}
	
	public boolean deleteContact(long phoneNumber) {
		return mDb.delete(DATABASE_TABLE, KEY_NUMBER + "=" + phoneNumber, null) > 0;
	}
	
	public boolean updateContact(String name, long phoneNumber) {
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, name);
		return mDb.update(DATABASE_TABLE, values, KEY_NUMBER + "=" + phoneNumber, null) > 0;
	}
	
	public Cursor getAllContacts() {
		return mDb.query(DATABASE_TABLE, new String[] {KEY_NAME, KEY_NUMBER}, null, null, null, null, KEY_NAME);
	}
	
	public Cursor getContact(long phoneNumber) {
		Cursor cursor = mDb.query(DATABASE_TABLE, new String[] {KEY_NAME, KEY_NUMBER}, KEY_NUMBER + "=" + phoneNumber, null, null, null, null); 
		if(cursor != null)
			cursor.moveToFirst();
		return cursor;
	}
	
	public String getContactName(long phoneNumber) {
		Cursor cursor = mDb.query(DATABASE_TABLE, new String[] {KEY_NAME, KEY_NUMBER}, KEY_NUMBER + "=" + phoneNumber, null, null, null, null); 
		if(cursor != null)
			cursor.moveToFirst();
		String contactName = Long.toString(phoneNumber); 
		if(cursor.getCount() != 0)
			contactName = cursor.getString(cursor.getColumnIndex(KEY_NAME));
		cursor.close();
		return contactName;
	}
	
	public Cursor getContactsByName(String name) {
		return mDb.query(DATABASE_TABLE, new String[] {KEY_NAME, KEY_NUMBER}, KEY_NAME + " LIKE \'%" + name + "%\'", null, null, null, KEY_NAME);
	}
}
