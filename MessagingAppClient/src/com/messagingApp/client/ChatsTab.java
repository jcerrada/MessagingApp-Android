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
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ChatsTab extends ListActivity implements ServiceConnection, Callback {
	public  static final String ID_CHAT               = "idChat";
	public  static final int    ACTIVITY_CREATE_GROUP = Menu.FIRST;
	private static final int    DELETE_ID             = Menu.FIRST;
	private static final int    CREATE_CHAT_ID        = Menu.FIRST + 1;
	private ChatsDB    mChatsDB;
	private MessagesDB mMessagesChatDB;
	private ContactsDB mContactsDB;
	private Messenger  mService;
	private Messenger  mActivity;
	private int        mSender = 677084244;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.chats_list);
	    mActivity       = new Messenger(new Handler(this));
	    mChatsDB        = new ChatsDB(this);
	    mMessagesChatDB  = new MessagesDB(this);
	    mContactsDB     = new ContactsDB(this);
	    registerForContextMenu(getListView());
	 }
	
	private void openDatabases() {
		mChatsDB.open();
		mMessagesChatDB.open();
		mContactsDB.open();
	}
	
	private void closeDatabases() {
		if(mChatsDB != null)
			mChatsDB.close();
		if(mMessagesChatDB != null)
			mMessagesChatDB.close();
		if(mContactsDB != null)
			mContactsDB.close();
	}
	
	private void connectToService() {
		Intent intent = new Intent(this,MessagingAppService.class);
		intent.putExtra(MessagingAppService.ACTIVITY_ID, MessagingAppService.ACT_CHATSTAB);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, CREATE_CHAT_ID, 0, R.string.create_group_chat);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
        	case CREATE_CHAT_ID:
        		Intent intent = new Intent(this, CreateGroup.class);
        		intent.putExtra(ID_CHAT, 1L);
        		startActivityForResult(intent, ACTIVITY_CREATE_GROUP);
        		return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
        	case DELETE_ID:
        		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        		if(mChatsDB.getSender(info.id) == 0L) {
        			String   group_name = mChatsDB.getChatName(info.id);
        			String[] name_parts = group_name.split("_");
        			if(name_parts[1].equals(Integer.toString(mSender))) {
        				ProtocolMessage message = new ProtocolMessage(ProtocolMessage.DGR, mSender, 
        						                                      new Vector<Integer>(), Calendar.getInstance(), group_name, null);
        				Message mess = new Message();
        				mess.what    = MessagingAppService.MESSAGE;
        				mess.obj     = message.messToString();
        				try {
							mService.send(mess);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
        			}
        		}
        		mMessagesChatDB.deleteMessagesChat(info.id);
        		mChatsDB.deleteChat(info.id);
        		
        		fillData();
        		return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.delete_chat);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(data != null) {
			openDatabases();
			String          group_name    = data.getExtras().getString(CreateGroup.GROUP_NAME);
			long[]          phones        = data.getExtras().getLongArray(CreateGroup.GROUP_MEMBERS);
			Vector<Integer> group_members = new Vector<Integer>();
			for(int i = 0; i < phones.length; i++)
				group_members.add(new Integer ((int)phones[i]));
			group_members.add(new Integer(MessagingAppService.getPhoneNumber()));
			ProtocolMessage  message = new ProtocolMessage(ProtocolMessage.CGR, mSender, group_members, Calendar.getInstance(), group_name, null);
			mChatsDB.createChat(group_name + "_" + Integer.toString(mSender), 0L);
			fillData();
			Message          mess    = new Message();
			mess.what = MessagingAppService.MESSAGE;
			mess.obj  = message.messToString();
			closeDatabases();
			try {
				mService.send(mess);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	protected void fillData() {
		Cursor chatsCursor = mChatsDB.getAllChats();
		startManagingCursor(chatsCursor);
		String[] from = new String[]{ChatsDB.KEY_NAME, ChatsDB.KEY_LAST_MESSAGE, ChatsDB.KEY_DATE};
		int[]    to   = {R.id.row_chat_name, R.id.last_message_chat, R.id.date_chat};
		SimpleCursorAdapter chats = new SimpleCursorAdapter(this, R.layout.chat_row, chatsCursor, from, to);
		setListAdapter(chats);
	}
	
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent = new Intent(this,MessagesChat.class);
        intent.putExtra(ID_CHAT,id);
        startActivity(intent);
     }

	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.android_new_chat_layout:
				Intent intent = new Intent(this,ContactsTab.class);
				startActivity(intent); break;
		}
	}

	public boolean handleMessage(Message msg) {
		String          message     = (String) msg.obj;
		ProtocolMessage protMessage = new ProtocolMessage(message);
		long idChat;
		switch(protMessage.getType()) {
		case ProtocolMessage.SSM:
			idChat = mChatsDB.getIdChatBySender(protMessage.getSender());
			if(idChat == ChatsDB.NO_CHAT)
				idChat = mChatsDB.createChat(mContactsDB.getContactName(protMessage.getSender()), protMessage.getSender());
			mMessagesChatDB.addMessageToChat(idChat, protMessage.getSender(), mContactsDB.getContactName(protMessage.getSender()),
					protMessage.getSender(), protMessage.getMessage(), protMessage.getDate());
			mChatsDB.updateChat(idChat, protMessage.getMessage(), protMessage.getDate());
			break;
		case ProtocolMessage.ACG:
			 idChat = mChatsDB.getIdChat(protMessage.getGroupName());
			 if(idChat == ChatsDB.NO_CHAT)
				 idChat = mChatsDB.createChat(protMessage.getGroupName(), 0L);
			 mMessagesChatDB.addMessageToChat(idChat, 0L, mContactsDB.getContactName(protMessage.getSender()), protMessage.getSender(), 
					                      mContactsDB.getContactName(protMessage.getReceivers().get(0)) + " has been added to the group", 
					                      protMessage.getDate());
			 break;
		case ProtocolMessage.DCG:
			 idChat = mChatsDB.getIdChat(protMessage.getGroupName());
			 if(idChat == ChatsDB.NO_CHAT)
				 idChat = mChatsDB.createChat(protMessage.getGroupName(), 0L);
			 mMessagesChatDB.addMessageToChat(idChat, 0L, mContactsDB.getContactName(protMessage.getSender()), protMessage.getSender(), 
					                      mContactsDB.getContactName(protMessage.getReceivers().get(0)) + " has been deleted from the group", 
					                      protMessage.getDate());
			 break;
		case ProtocolMessage.SMG:
			 idChat = mChatsDB.getIdChat(protMessage.getGroupName());
			 if(idChat == ChatsDB.NO_CHAT)
				 idChat = mChatsDB.createChat(protMessage.getGroupName(), 0L);
			 mMessagesChatDB.addMessageToChat(idChat, 0L, mContactsDB.getContactName(protMessage.getSender()), protMessage.getSender(), 
					 protMessage.getMessage(), protMessage.getDate());
			 mChatsDB.updateChat(idChat, protMessage.getMessage(), protMessage.getDate());
			 break;
		case ProtocolMessage.CGR:
				idChat = mChatsDB.getIdChat(protMessage.getGroupName() + "_" + protMessage.getSender());
				if(idChat == ChatsDB.NO_CHAT)
					idChat = mChatsDB.createChat(protMessage.getGroupName() + "_" + protMessage.getSender(), 0L);
				String receiverName, contactName = mContactsDB.getContactName(protMessage.getSender());
				for(int i = 0; i < protMessage.getNumReceivers(); i++) {
					receiverName = mContactsDB.getContactName(protMessage.getReceivers().get(i));
					mMessagesChatDB.addMessageToChat(idChat, 0L, contactName, protMessage.getSender(), 
							                        receiverName + " has been added by " + contactName, protMessage.getDate());
				}
				Toast.makeText(this, "The group chat " + protMessage.getGroupName() + " has been created", Toast.LENGTH_LONG).show();
				break;
			case ProtocolMessage.DGR:
				idChat = mChatsDB.getIdChat(protMessage.getGroupName());
				mMessagesChatDB.deleteMessagesChat(idChat);
				mChatsDB.deleteChatByName(protMessage.getGroupName());
				Toast.makeText(this, "The group chat " + protMessage.getGroupName() + " has been deleted", Toast.LENGTH_LONG).show();
				break;
		}
		fillData();
		return true;
	}

	public void onServiceDisconnected(ComponentName arg0) {
		mService = null;
	}

	public void onServiceConnected(ComponentName name, IBinder service) {
		mService        = new Messenger(service);
		Message message = new Message();
		message.what    = MessagingAppService.CONNECT;
		message.arg1    = MessagingAppService.ACT_CHATSTAB;
		message.replyTo = mActivity;
		try {
			mService.send(message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
