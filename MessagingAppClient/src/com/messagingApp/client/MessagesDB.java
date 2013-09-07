package com.messagingApp.client;

import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MessagesDB {
	public static final String KEY_ROWID         = "_id";
	public static final String KEY_CHATID        = "id_chat";
	public static final String KEY_RECEIVERID    = "id_receiver";
	public static final String KEY_SENDER_NAME   = "sender_name";
	public static final String KEY_SENDER_NUMBER = "sender";
	public static final String KEY_MESSAGE       = "message";
	public static final String KEY_DATE          = "date";
	public static final String KEY_TIME          = "time";
	
	private static final String DATABASE_NAME    = "messagingAppDB";
    private static final String DATABASE_TABLE   = "messagesDB";
	private static final String TAG              = "MessagesDB";
	private static final String DATABASE_CREATE  = "CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE + " (" 
												 + KEY_ROWID         + " integer PRIMARY KEY autoincrement, "
												 + KEY_CHATID        + " integer NOT NULL, "
												 + KEY_RECEIVERID    + " integer NOT NULL, "
												 + KEY_SENDER_NAME   + " text    NOT NULL, "
												 + KEY_SENDER_NUMBER + " integer NOT NULL, "
												 + KEY_MESSAGE       + " text    NOT NULL, "
												 + KEY_DATE          + " date    NOT NULL, "
												 + KEY_TIME          + " time    NOT NULL);";
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
	
	public MessagesDB(Context ctx) {
		mCtx = ctx;
	}
	
	public MessagesDB open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        mDbHelper.createTable(mDb);
        return this;
    }
	
	public void close() {
        mDbHelper.close();
    }
	
	public long addMessageToChat(long idChat, long receiver, String senderName, int senderPhone, String message, Calendar calendar) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_CHATID, idChat);
		initialValues.put(KEY_RECEIVERID, receiver);
		initialValues.put(KEY_SENDER_NAME, senderName);
		initialValues.put(KEY_SENDER_NUMBER, senderPhone);
		initialValues.put(KEY_MESSAGE, message);
		initialValues.put(KEY_DATE, calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DAY_OF_MONTH));
		initialValues.put(KEY_TIME, calendar.get(Calendar.HOUR_OF_DAY) + ":" +calendar.get(Calendar.MINUTE));
		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}
	
	public boolean deleteMessage(long id) {
		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + id, null) > 0;
	}
	
	public boolean deleteMessagesChat(long idChat) {
		return mDb.delete(DATABASE_TABLE, KEY_CHATID + "=" + idChat, null) > 0;
	}
	
	public Cursor getMessagesChat(long idChat) {
		return mDb.rawQuery("SELECT * FROM " + DATABASE_TABLE + " WHERE " + KEY_CHATID + " = ? ORDER BY " + KEY_DATE + ", " + KEY_TIME, new String[]{Long.toString(idChat)});
		/*return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_CHATID, KEY_RECEIVERID, KEY_SENDER_NAME, KEY_SENDER_NUMBER, KEY_MESSAGE, KEY_DATE, KEY_TIME},
						 KEY_CHATID + "=" + idChat, null, null, null, null);*/
	}
	
	public Cursor getMessagesReceiver(long idReceiver) {
		return mDb.rawQuery("SELECT * FROM " + DATABASE_TABLE + " WHERE " + KEY_RECEIVERID + " = ? ORDER BY " + KEY_DATE + ", " + KEY_TIME, new String[]{Long.toString(idReceiver)});
		/*return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_CHATID, KEY_RECEIVERID, KEY_SENDER_NAME, KEY_SENDER_NUMBER, KEY_MESSAGE, KEY_DATE, KEY_TIME},
						 KEY_RECEIVERID + "=" + idReceiver, null, null, null, KEY_DATE + "," + KEY_TIME);*/
	}
}
