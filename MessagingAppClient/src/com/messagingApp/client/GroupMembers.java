package com.messagingApp.client;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class GroupMembers extends ListActivity{
	private MembersGroupDB mDbHelper;
	private long           idGroup;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.group_members);
	    Bundle extras = getIntent().getExtras();
	    idGroup = extras.getLong(MembersGroupDB.KEY_ROWID);
	    mDbHelper = new MembersGroupDB(this);
	    registerForContextMenu(getListView());
	    getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	 }
	
	@Override
	protected void onResume() {
		super.onResume();
		mDbHelper.open();
		fillData();
	}
	
	@Override
	protected void onPause() {
		if(mDbHelper != null)
			mDbHelper.close();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	protected void fillData() {
		Cursor membersCursor = mDbHelper.getMembersGroup(idGroup);
		startManagingCursor(membersCursor);
		String[] from = new String[]{MembersGroupDB.KEY_MEMBER_NAME};
		int[]    to   = {android.R.id.text1};
		SimpleCursorAdapter messages = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_multiple_choice, membersCursor, from, to);
		setListAdapter(messages);
	}
	
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.android_confirm_button_members_group:
				ListView list         = getListView();
				long[]   selectedIds  = list.getCheckedItemIds();
				if(selectedIds.length > 0) {
					long[]   membersPhone = new long[selectedIds.length];
					Cursor   member;
					for(int i = 0; i < selectedIds.length; i++) {
						member = mDbHelper.getMember(selectedIds[i]);
						membersPhone[i] = member.getLong(member.getColumnIndex(MembersGroupDB.KEY_MEMBER_PHONE));
						member.close();
					}
					Intent intent = new Intent(this, MessagesChat.class);
					intent.putExtra(MembersGroupDB.KEY_MEMBER_PHONE, membersPhone);
					startActivity(intent);
					finish();
				}
				else
					Toast.makeText(this, "You have to select at least one member", Toast.LENGTH_LONG).show();
				break;
		}
	}
}
