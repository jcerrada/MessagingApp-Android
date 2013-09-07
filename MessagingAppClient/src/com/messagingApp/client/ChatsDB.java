package com.messagingApp.client;

import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ChatsDB {
	public static final String KEY_ROWID         = "_id";
	public static final String KEY_NAME          = "name";
	public static final String KEY_NUMBER        = "number";
	public static final String KEY_LAST_MESSAGE  = "message";
	public static final String KEY_DATE          = "date";
	public static final String KEY_TIME          = "time";
	
	public static final long   NO_CHAT           = -1;
	
	private static final String DATABASE_NAME    = "messagingAppDB";
    private static final String DATABASE_TABLE   = "chatsDB";
	private static final String TAG              = "ChatsDB";
	private static final String DATABASE_CREATE  = "CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE + " (" 
												 + KEY_ROWID         + " integer PRIMARY KEY autoincrement, "
												 + KEY_NAME          + " text    NOT NULL, "
												 + KEY_NUMBER        + " integer NOT NULL, "
												 + KEY_LAST_MESSAGE  + " text, "
												 + KEY_DATE          + " date, "
												 + KEY_TIME          + " time);";
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
	
	public ChatsDB(Context ctx) {
		mCtx = ctx;
	}
	
	public ChatsDB open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        mDbHelper.createTable(mDb);
        return this;
    }
	
	public void close() {
        mDbHelper.close();
    }
	
	public long createChat(String name, long phoneNumber) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_NUMBER, phoneNumber);
		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}
	
	public boolean deleteChat(long id) {
		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + id, null) > 0;
	}
	
	public boolean deleteChatByName(String name) {
		mDb.rawQuery("DELETE FROM " + DATABASE_TABLE + " WHERE " + KEY_NAME + " = ?", new String[]{name}).close();
		return true;
	}
	
	public Cursor getAllChats() {
		return mDb.rawQuery("SELECT * FROM " + DATABASE_TABLE + " ORDER BY " + KEY_DATE + " DESC, " + KEY_TIME + " DESC", null);
	}
	
	public Cursor getChat(long id) {
		Cursor cursor = mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NAME, KEY_NUMBER, KEY_LAST_MESSAGE, KEY_DATE, KEY_TIME},
								  KEY_ROWID + "=" + id, null, null, null, null);
		if(cursor != null)
			cursor.moveToFirst();
		return cursor;
	}
	
	public Cursor getChatByName(String name) {
		Cursor cursor = mDb.rawQuery("SELECT * FROM " + DATABASE_TABLE + " WHERE " + KEY_NAME + " = ?", new String[] {name});
		if(cursor != null)
			cursor.moveToFirst();
		return cursor;
	}
	
	public long getIdChat(String name) {
		Cursor cursor = mDb.rawQuery("SELECT * FROM " + DATABASE_TABLE + " WHERE " + KEY_NAME + " = ?", new String[]{name});
		if(cursor != null)
			cursor.moveToFirst();
		long idChat = NO_CHAT;
		if(cursor.getCount() != 0)
			idChat = cursor.getLong(cursor.getColumnIndex(KEY_ROWID));
		cursor.close();
		return idChat;
	}
	
	public long getIdChatBySender(long number) {
		Cursor cursor = mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NAME, KEY_NUMBER, KEY_LAST_MESSAGE, KEY_DATE, KEY_TIME},
				  KEY_NUMBER + "=" + number, null, null, null, null);
		if(cursor != null)
			cursor.moveToFirst();
		long idChat = NO_CHAT;
		if(cursor.getCount() != 0)
			idChat = cursor.getLong(cursor.getColumnIndex(KEY_ROWID));
		cursor.close();
		return idChat;
	}
	
	public String getChatName(long idChat) {
		Cursor cursor = mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NAME, KEY_NUMBER, KEY_LAST_MESSAGE, KEY_DATE, KEY_TIME},
				  KEY_ROWID + "=" + idChat, null, null, null, null);
		cursor.moveToFirst();
		String chatName = cursor.getString(cursor.getColumnIndex(KEY_NAME));
		cursor.close();
		return chatName;
	}
	
	public long getSender(long idChat) {
		Cursor cursor = mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NAME, KEY_NUMBER, KEY_LAST_MESSAGE, KEY_DATE, KEY_TIME},
				  KEY_ROWID + "=" + idChat, null, null, null, null);
		cursor.moveToFirst();
		long sender = cursor.getLong(cursor.getColumnIndex(KEY_NUMBER));
		cursor.close();
		return sender;
	}
	
	public boolean updateChat(long id, String lastMessage, Calendar calendar) {
		ContentValues values = new ContentValues();
		values.put(KEY_LAST_MESSAGE, lastMessage);
		values.put(KEY_DATE, calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DAY_OF_MONTH));
		values.put(KEY_TIME, calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND));
		return mDb.update(DATABASE_TABLE, values, KEY_ROWID + "=" + id, null) > 0;
	}
}
