package com.messagingApp.client;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class CreateGroup extends ListActivity{
	public static final String GROUP_MEMBERS = "group_members";
	public static final String GROUP_NAME    = "group_name";
	
	private ContactsDB     mContactsDB;
	private GroupsDB       mGroupDB;
	private MembersGroupDB mGroupMembersDB;
	private boolean        mGroupChat = false;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.create_group);
	    Bundle extras   = getIntent().getExtras();
	    if(extras != null && extras.getLong(ChatsTab.ID_CHAT) == 1L)
	    	mGroupChat = true;
	    mContactsDB     = new ContactsDB(this);
	    mGroupDB        = new GroupsDB(this);
	    mGroupMembersDB = new MembersGroupDB(this);
	    registerForContextMenu(getListView());
	    getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	 }
	
	private void openDataBases() {
		mContactsDB.open();
		mGroupDB.open();
		mGroupMembersDB.open();
	}
	
	private void closeDatabases() {
		if(mContactsDB != null)
			mContactsDB.close();
		if(mGroupDB != null)
			mGroupDB.close();
		if(mGroupMembersDB != null)
			mGroupMembersDB.close();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		openDataBases();
		fillData();
	}
	
	@Override
	protected void onPause() {
		closeDatabases();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	protected void fillData() {
		Cursor contactsCursor = mContactsDB.getAllContacts();
		startManagingCursor(contactsCursor);
		String[] from = new String[]{ContactsDB.KEY_NAME};
		int[]    to   = {android.R.id.text1};
		SimpleCursorAdapter messages = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_multiple_choice, contactsCursor, from, to);
		setListAdapter(messages);
	}
	
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.android_confirm_button_create_group:
				EditText groupName = (EditText)findViewById(R.id.android_group_name_edittext);
				String   group     = groupName.getText().toString();
				String contactName;
				if(group.length() != 0) {
					ListView list       = getListView();
					ListAdapter adapter = getListAdapter();
					long ids[] = list.getCheckedItemIds();
					if(!mGroupChat) {
						long idGroup = mGroupDB.createGroup(group);
						SparseBooleanArray posChecked = list.getCheckedItemPositions();
						for(int j = 0,i = 0; i < list.getCount(); i++) {
							if(posChecked.get(i)) {
								contactName = mContactsDB.getContactName(adapter.getItemId(i));
								mGroupMembersDB.addMember(idGroup, contactName, ids[j]);
								j++;
							}
						}
					}
					else {
						Intent intent = new Intent();
						intent.putExtra(GROUP_NAME, group);
						intent.putExtra(GROUP_MEMBERS, ids);
						setResult(RESULT_OK, intent);
					}
					finish();
				}
				else
					Toast.makeText(this, "You have to type the name of the group", Toast.LENGTH_LONG).show();
				break;
		}
	}
}
