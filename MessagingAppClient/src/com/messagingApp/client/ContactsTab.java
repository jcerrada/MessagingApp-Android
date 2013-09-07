package com.messagingApp.client;

import java.util.Vector;

import android.app.ListActivity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ContactsTab extends ListActivity implements ServiceConnection, Callback {
	public  static final String ID_CONTACT         = "idContact";
	public  static final String ID_COMMON_FRIENDS  = "commonFriends";
	private static final int    SEND_COMMON        = Menu.FIRST;
	private ContactsDB mDbHelper;
	private String     mSearchByName;
	private Messenger  mService;
	private Messenger  mActivity;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts_list);
		mActivity = new Messenger(new Handler(this));
		mDbHelper = new ContactsDB(this);
		Bundle extras = getIntent().getExtras();
		if(extras != null)
			mSearchByName = extras.getString(ContactsDB.KEY_NAME);
		else
			mSearchByName = null;
		registerForContextMenu(getListView());
	}
	
	@Override
	protected void onPause() {
		Log.v("ContactsTab", "Pausing activity");
		disconnectFromService();
		if(mDbHelper != null)
			mDbHelper.close();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v("ContactsTab", "Resuming activity");
		connectToService();
		mDbHelper.open();
		fillData();
	}
	
	@Override
	protected void onDestroy() {
		Log.v("ContactsTab", "Destroying activity");
		super.onDestroy();
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
    	case SEND_COMMON:
    		AdapterContextMenuInfo info    = (AdapterContextMenuInfo) item.getMenuInfo();
    		Intent intent = new Intent(this, MessagesChat.class);
    		intent.putExtra(ID_COMMON_FRIENDS, info.id);
    		startActivity(intent);
    		return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, SEND_COMMON, 0, R.string.send_common_friends);
	}

	private void connectToService() {
		Intent intent = new Intent(this,MessagingAppService.class);
		intent.putExtra(MessagingAppService.ACTIVITY_ID, MessagingAppService.ACT_CONTACTSTAB);
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
	
	private Vector<String> getContactNames(Vector<Integer> numbers) {
		Vector<String> names = new Vector<String>();
		Uri uri;
		Cursor cursor;
		for(int i = 0; i < numbers.size(); i++) {
			uri    = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numbers.get(i).toString()));
			cursor = getContentResolver().query(uri, new String[] {PhoneLookup.DISPLAY_NAME}, null, null, null);
			cursor.moveToFirst();
			names.add(cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME)));
			cursor.close();
		}
		return names;
	}

	protected void fillData() {
		Cursor contactsCursor;
		if(mSearchByName == null)
			contactsCursor = mDbHelper.getAllContacts();
		else
			contactsCursor = mDbHelper.getContactsByName(mSearchByName);
		startManagingCursor(contactsCursor);
		String[] from = new String[]{ContactsDB.KEY_NAME};
		int[]    to   = {android.R.id.text1};
		SimpleCursorAdapter messages = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, contactsCursor, from, to);
		setListAdapter(messages);
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent = new Intent(this,MessagesChat.class);
        intent.putExtra(ID_CONTACT,id);
        startActivity(intent);
    }
	
	public void onClickSearch(View v) {
		switch(v.getId()) {
			case R.id.android_search_contact_button:
				EditText contactName = (EditText)findViewById(R.id.android_search_contact_edittext);
				String   contact     = contactName.getText().toString();
				if(contact.length() != 0) {
					Intent intent = new Intent(getBaseContext(),ContactsTab.class);
					intent.putExtra(ContactsDB.KEY_NAME, contact);
					startActivity(intent);
					contactName.setText("");
					if(mSearchByName != null)
						finish();
				}
				else
					Toast.makeText(this, "You need to type the name of the contact", Toast.LENGTH_LONG).show();
				break;
		}
	}

	public boolean handleMessage(Message msg) {
		ProtocolMessage message = new ProtocolMessage((String)msg.obj);
		Vector<String>  names   = getContactNames(message.getReceivers());
		mDbHelper.deleteAllContacts();
		mDbHelper.addContacts(names, message.getReceivers());
		fillData();
		return true;
	}

	public void onServiceConnected(ComponentName arg0, IBinder service) {
		mService        = new Messenger(service);
		Message message = new Message();
		message.what    = MessagingAppService.CONNECT;
		message.arg1    = MessagingAppService.ACT_CONTACTSTAB;
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
