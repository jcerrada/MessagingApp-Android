package com.messagingApp.client;

import java.util.Calendar;
import java.util.Vector;

import android.app.ListActivity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class MessagesChat extends ListActivity implements ServiceConnection, Callback{
	private static final int CHATS_ACTIVITY         = 0;
	private static final int CONTACTS_ACTIVITY      = 1;
	private static final int GROUPMEMBERS_ACTIVITY  = 2;
	private static final int COMMONFRIENDS_ACTIVITY = 3;
	private static final int SERVICE                = 4;
	private static final int DELETE_ID              = Menu.FIRST;
	
	private MessagesDB mMessagesDB;
	private ChatsDB    mChatsDB;
	private ContactsDB mContactsDB;
	private long       mIdChat;
	private int        mFromActivity;
	private long[]     mReceivers;
	private Messenger  mService;
	private Messenger  mActivity;
	private int        mSender = MessagingAppService.getPhoneNumber();
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.messages_list);
	    mActivity     = new Messenger(new Handler(this));
	    Bundle extras = getIntent().getExtras();
	    mIdChat = extras.getLong(ChatsTab.ID_CHAT);
	    if(mIdChat == 0L) {
	    	mIdChat    = extras.getLong(ContactsTab.ID_CONTACT);
	    	if(mIdChat == 0L) {
	    		mIdChat = extras.getLong(ContactsTab.ID_COMMON_FRIENDS);
	    		if(mIdChat == 0L) {
	    			mIdChat = extras.getInt(MessagesDB.KEY_RECEIVERID);
	    			if(mIdChat == 0) {
	    				mReceivers = extras.getLongArray(MembersGroupDB.KEY_MEMBER_PHONE);
	    				mFromActivity = GROUPMEMBERS_ACTIVITY;
	    			}
	    			else
	    				mFromActivity = SERVICE;
	    		}
	    		else
	    			mFromActivity = COMMONFRIENDS_ACTIVITY;
	    	}
	    	else
	    		mFromActivity = CONTACTS_ACTIVITY;
	    }
	    else
	    	mFromActivity = CHATS_ACTIVITY;
	    mMessagesDB = new MessagesDB(this);
	    mChatsDB    = new ChatsDB(this);
	    mContactsDB = new ContactsDB(this);
	    registerForContextMenu(getListView());
	 }
	
	private void openDatabases() {
		mChatsDB.open();
		mMessagesDB.open();
		mContactsDB.open();
	}
	
	private void closeDatabases() {
		if(mChatsDB != null)
			mChatsDB.close();
		if(mMessagesDB != null)
			mMessagesDB.close();
		if(mContactsDB != null)
			mContactsDB.close();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		connectToService();
		openDatabases();
		fillData();
	}
	
	@Override
	protected void onPause() {
		disconnectFromService();
		closeDatabases();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
    		case DELETE_ID:
    			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    			mMessagesDB.deleteMessage(info.id);
    			fillData();
    			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.delete_message);
	}

	private void connectToService() {
		Intent intent = new Intent(this,MessagingAppService.class);
		intent.putExtra(MessagingAppService.ACTIVITY_ID, MessagingAppService.ACT_MESSAGESCHATTAB);
		if(!getApplicationContext().bindService(intent, this, Service.BIND_AUTO_CREATE))
			Log.v("ConnectService", "Impossible connect to the service");
	}
	
	private void disconnectFromService() {
		if(mService != null) {
			Message message = new Message();
			message.what    = MessagingAppService.DISCONNECT;
			try {
				mService.send(message);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			getApplicationContext().unbindService(this);
		}
	}

	protected void fillData() {
		if(mFromActivity != GROUPMEMBERS_ACTIVITY && mFromActivity != COMMONFRIENDS_ACTIVITY) {
			Cursor messagesCursor;
			if(mFromActivity == CHATS_ACTIVITY)
				messagesCursor = mMessagesDB.getMessagesChat(mIdChat);
			else
				messagesCursor = mMessagesDB.getMessagesReceiver(mIdChat);
			startManagingCursor(messagesCursor);
			String[] from = new String[]{MessagesDB.KEY_SENDER_NAME, MessagesDB.KEY_DATE, MessagesDB.KEY_TIME, MessagesDB.KEY_MESSAGE};
			int[]    to   = {R.id.android_textview_message_name, R.id.android_message_date_textview, 
							 R.id.android_message_time_textview, R.id.android_message_text_textview};
			SimpleCursorAdapter messages = new SimpleCursorAdapter(this, R.layout.message_row, messagesCursor, from, to);
			setListAdapter(messages);
		}
	}
	
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.android_send_message_button:
				EditText messageEditText  = (EditText)findViewById(R.id.android_write_message_edittext);
				String   message          = messageEditText.getText().toString();
				ProtocolMessage protMess  = null;
				Vector<Integer> receivers = new Vector<Integer>();
				if(message.length() > 0) {
					switch(mFromActivity) {
						case SERVICE: 
						case CHATS_ACTIVITY: 
							long receiver = mChatsDB.getSender(mIdChat);
							mMessagesDB.addMessageToChat(mIdChat, receiver, "Me", mSender, message, Calendar.getInstance());
							mChatsDB.updateChat(mIdChat, message, Calendar.getInstance());
							if(receiver == 0L) {
								String groupName = mChatsDB.getChatName(mIdChat);
								protMess = new ProtocolMessage(ProtocolMessage.SMG, mSender, new Vector<Integer>(), 
								                               Calendar.getInstance(), groupName, message);
							}
							else {
								receivers.add(new Integer((int)receiver));
								protMess = new ProtocolMessage(ProtocolMessage.SSM, mSender, receivers, 
	                                                           Calendar.getInstance(), null, message);
							}
							break;
						case CONTACTS_ACTIVITY:
							long idChat = mChatsDB.getIdChatBySender(mIdChat);
							if(idChat == ChatsDB.NO_CHAT)
								idChat = mChatsDB.createChat(mContactsDB.getContactName(mIdChat), mIdChat);
							mMessagesDB.addMessageToChat(idChat, mIdChat, "Me", mSender, message, Calendar.getInstance());
							mChatsDB.updateChat(idChat, message, Calendar.getInstance());
							receivers.add(new Integer((int)mIdChat));
							protMess = new ProtocolMessage(ProtocolMessage.SSM, mSender, receivers, 
							 		                       Calendar.getInstance(), null, message);
							break;
						case COMMONFRIENDS_ACTIVITY:
							receivers.add(new Integer((int)mIdChat));
							protMess = new ProtocolMessage(ProtocolMessage.SMC, mSender, receivers, 
							 		                       Calendar.getInstance(), null, message);
							break;
						case GROUPMEMBERS_ACTIVITY:
							for(int i = 0; i < mReceivers.length; i++)
								receivers.add((int)mReceivers[i]);
							protMess = new ProtocolMessage(ProtocolMessage.SSM, mSender, receivers, 
		 		                       Calendar.getInstance(), null, message);
							break;
					}
					fillData();
					Message mess = new Message();
					mess.what    = MessagingAppService.MESSAGE;
					if(protMess != null) {
						mess.obj     = protMess.messToString();
						try {
							mService.send(mess);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
					messageEditText.setText("");
					if(mFromActivity == COMMONFRIENDS_ACTIVITY || mFromActivity == GROUPMEMBERS_ACTIVITY)
						finish();
				}
				else
					Toast.makeText(this, "You have to write a message", Toast.LENGTH_LONG).show();
		}
	}

	public boolean handleMessage(Message msg) {
		ProtocolMessage message = new ProtocolMessage((String)msg.obj);
		long chatId;
		switch(message.getType()) {
			case ProtocolMessage.SSM:
				chatId = mChatsDB.getIdChatBySender(message.getSender());
				if(chatId == ChatsDB.NO_CHAT)
					chatId = mChatsDB.createChat(mContactsDB.getContactName(message.getSender()), message.getSender());
				mMessagesDB.addMessageToChat(chatId, message.getSender(), mContactsDB.getContactName(message.getSender()),
						                     message.getSender(), message.getMessage(), message.getDate());
				mChatsDB.updateChat(chatId, message.getMessage(), message.getDate());
				break;
			case ProtocolMessage.ACG:
				 chatId = mChatsDB.getIdChat(message.getGroupName());
				 if(chatId == ChatsDB.NO_CHAT)
					 chatId = mChatsDB.createChat(message.getGroupName(), 0L);
				 mMessagesDB.addMessageToChat(chatId, 0L, mContactsDB.getContactName(message.getSender()), message.getSender(), 
						                      mContactsDB.getContactName(message.getReceivers().get(0)) + " has been added to the group", 
						                      message.getDate());
				 break;
			case ProtocolMessage.DCG:
				 chatId = mChatsDB.getIdChat(message.getGroupName());
				 if(chatId == ChatsDB.NO_CHAT)
					 chatId = mChatsDB.createChat(message.getGroupName(), 0L);
				 mMessagesDB.addMessageToChat(chatId, 0L, mContactsDB.getContactName(message.getSender()), message.getSender(), 
						                      mContactsDB.getContactName(message.getReceivers().get(0)) + " has been deleted from the group", 
						                      message.getDate());
				 break;
			case ProtocolMessage.SMG:
				 chatId = mChatsDB.getIdChat(message.getGroupName());
				 if(chatId == ChatsDB.NO_CHAT)
					 chatId = mChatsDB.createChat(message.getGroupName(), 0L);
				 mMessagesDB.addMessageToChat(chatId, 0L, mContactsDB.getContactName(message.getSender()), message.getSender(), 
						                      message.getMessage(), message.getDate());
				 mChatsDB.updateChat(chatId, message.getMessage(), message.getDate());
				 break;
		}
		fillData();
		return true;
	}

	public void onServiceConnected(ComponentName name, IBinder service) {
		mService        = new Messenger(service);
		Message message = new Message();
		message.what    = MessagingAppService.CONNECT;
		message.arg1    = MessagingAppService.ACT_MESSAGESCHATTAB;
		message.replyTo = mActivity;
		try {
			mService.send(message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}		
	}

	public void onServiceDisconnected(ComponentName name) {
		mService = null;
	}
}
