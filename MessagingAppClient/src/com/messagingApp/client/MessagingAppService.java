package com.messagingApp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Calendar;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

public class MessagingAppService extends Service implements Runnable, Callback {
	public  static final String ACTIVITY_ID         = "activity";
	public  static final int    ACT_CHATSTAB        = 0;
	public  static final int    ACT_CONTACTSTAB     = 1;
	public  static final int    ACT_MESSAGESCHATTAB = 2;
	public  static final int    CONNECT             = 6;
	public  static final int    DISCONNECT          = 7;
	public  static final int    MESSAGE             = 8;
	public  static final int    CONNECTION_PROBLEM  = 11;
	private static final String SERVER_IP           = "192.168.1.100";
	private static final int    PORT                = 11325;
	private static final int    NUM_ACTIVITIES      = 3;
	private static boolean         mExit  = true;
	private static int             mPhone;
	private Socket                 mSocket;
	private NotificationManager    mNM;
	private BufferedReader         mIn;
	private PrintWriter            mOut;
	private Messenger              mService;
	private Messenger              mActivity;
	private int                    mActivityID = -1;
	private Vector<Vector<String>> mActivityMessages = new Vector<Vector<String>>();
	
	@Override
	public void onCreate() {
		super.onCreate();
		try {		
			mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
			mSocket = new Socket(InetAddress.getByName(SERVER_IP),PORT);
			
			mIn     = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			mOut    = new PrintWriter(mSocket.getOutputStream(),true);
			mOut.flush();
			mExit   = false;
			
			for(int i = 0; i < NUM_ACTIVITIES; i++)
				mActivityMessages.add(new Vector<String>());
			mService = new Messenger(new Handler(this));
			
			Thread thread = new Thread(this);
			thread.start();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		ProtocolMessage message = new ProtocolMessage(ProtocolMessage.DSC, mPhone, new Vector<Integer>(), Calendar.getInstance(), null, null);
		sendMessage(message.messToString());
		try {
			mIn.close();
			mOut.close();
			mSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onDestroy();
		mExit = true; 
	}
	
	public void connectToServer() {
		ProtocolMessage  message  = new ProtocolMessage(ProtocolMessage.CNT, mPhone, new Vector<Integer>(), Calendar.getInstance(), null, null);
		sendMessage(message.messToString());
		Vector<Integer> receivers = getContacts();
		message  = new ProtocolMessage(ProtocolMessage.SCN, mPhone, receivers, Calendar.getInstance(), null, null);
		sendMessage(message.messToString());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Bundle extras = intent.getExtras();
		mPhone = extras.getInt(Login.PHONE_NUMBER);
		connectToServer();
		return super.onStartCommand(intent, flags, startId);
	}
	
	private Vector<Integer> getContacts() {
		Vector<Integer> contacts = new Vector<Integer>();
		Cursor cursor = getContentResolver().query(Phone.CONTENT_URI,new String[] {Phone.NUMBER}, null, null, null); 
		while(cursor.moveToNext()) {
			String phoneNumber = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
			phoneNumber = phoneNumber.replace("-", "");
			if(phoneNumber.startsWith("+34"))
				phoneNumber = phoneNumber.replaceFirst("\\+34", "");
			else {
				if(phoneNumber.startsWith("0034"))
					phoneNumber = phoneNumber.replaceFirst("0034", "");
				else {
					if(phoneNumber.startsWith("00353"))
						phoneNumber = phoneNumber.replaceFirst("00353", "");
				}
			}
			Log.v("Number",phoneNumber);
			contacts.add(new Integer(phoneNumber));
		}
		cursor.close();
		return contacts;
	}
	
	private boolean sendMessage(String message) {
		Log.v("Send Client", message);
		mOut.println(message);
		return true;
	}
	
	private void showNotification(String message) {
		ProtocolMessage protMessage = new ProtocolMessage(message);
		String tittle, mess;
		if(protMessage.getMessage() == null) {
			tittle = protMessage.getSender() + ": New";
			mess = "New Message!";
		}
		else {
			tittle = protMessage.getSender() + ": " + ((protMessage.getMessage().length() < 5) ? 
					 protMessage.getMessage() : protMessage.getMessage().substring(0, 5)+ "...");
			mess   = protMessage.getMessage();
		}
		Notification notification = new Notification(R.drawable.icon, tittle, System.currentTimeMillis()); 
		Intent intent;
		switch(protMessage.getType()) {
			case ProtocolMessage.CGR:
			case ProtocolMessage.DGR:
				intent = new Intent(this, ChatsTab.class); 
				break;
			case ProtocolMessage.SCN:
				intent = new Intent(this, ContactsTab.class); 
				break;
			case ProtocolMessage.SSM:
			case ProtocolMessage.ACG:
			case ProtocolMessage.DCG:
			case ProtocolMessage.SMG:
				intent = new Intent(this, MessagesChat.class);
				intent.putExtra(MessagesDB.KEY_RECEIVERID, protMessage.getSender()); 
				break;
			default:
				intent = new Intent();
		}
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		notification.setLatestEventInfo(this , "New message from " + protMessage.getSender(), mess, contentIntent);
		mNM.notify(message.hashCode(), notification);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mService.getBinder();
	}
	
	public static boolean isRunning() {
		return !mExit;
	}
	
	public static int getPhoneNumber() {
		return mPhone;
	}
	
	public void setPhoneNumber(int number) {
		mPhone = number;
	}

	public void run() {
		while(!mExit) {
			try {
				String message = mIn.readLine();
				if(message != null) {
					Log.v("Receive client", message);
					ProtocolMessage protMessage = new ProtocolMessage(message);
					Message         mess        = new Message();
					mess.obj = message;
					showNotification(message);
					switch(protMessage.getType()) {
						case ProtocolMessage.SSM:
						case ProtocolMessage.ACG:
						case ProtocolMessage.DCG:
						case ProtocolMessage.SMG:
							if(mActivity != null && (mActivityID == ACT_MESSAGESCHATTAB || mActivityID == ACT_CHATSTAB)) {
								mActivity.send(mess);
								mNM.cancel(message.hashCode());
							}
							else 
								mActivityMessages.get(ACT_CHATSTAB).add(message);
							break;
						case ProtocolMessage.SCN:
							if(mActivity != null && mActivityID == ACT_CONTACTSTAB) {
								mActivity.send(mess);
								mNM.cancel(message.hashCode());
							}
							else
								mActivityMessages.get(ACT_CONTACTSTAB).add(message);
							break;
						case ProtocolMessage.CGR:
						case ProtocolMessage.DGR:
							if(mActivity != null && mActivityID == ACT_CHATSTAB) {
								mActivity.send(mess);
								mNM.cancel(message.hashCode());
							}
							else
								mActivityMessages.get(ACT_CHATSTAB).add(message); 
							break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public boolean handleMessage(Message msg) {
		Message mess;
		switch(msg.what) {
			case CONNECT:
				mActivity   = msg.replyTo;
				mActivityID = msg.arg1;
				Log.v("handleMessage", "Binding to the activity " + mActivityID);
				for(int i = 0; i < mActivityMessages.get(mActivityID).size(); i++) {
					String message = (String)mActivityMessages.get(mActivityID).remove(i);
					mess      = new Message();
					mess.obj = message;
					Log.v("handleMessage", "Enviando mensaje pendiente " + i + ": " + (String)mess.obj);
					try {
						mActivity.send(mess);
						mNM.cancel(message.hashCode());
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					
				}
				break;
			case DISCONNECT:
				mActivity   = null;
				mActivityID = -1; 
				break;
			case MESSAGE:
				String message = (String) msg.obj;
				if(!sendMessage(message)) {
					mess      = new Message();
					mess.what = CONNECTION_PROBLEM;
					try {
						mActivity.send(mess);
					} catch (RemoteException e) {
						e.printStackTrace();
						return false;
					}
				}
		}
		return true;
	}

}
