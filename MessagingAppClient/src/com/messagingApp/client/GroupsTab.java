package com.messagingApp.client;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
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

public class GroupsTab extends ListActivity {
	private static final int DELETE_ID = Menu.FIRST;
	private GroupsDB       mGroupsDB;
	private MembersGroupDB mMembersGroupDB;
	private String   mSearchGroupByName;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.group_list);
	    Bundle extras = getIntent().getExtras();
	    if(extras != null)
	    	mSearchGroupByName = extras.getString(GroupsDB.KEY_NAME);
	    else
	    	mSearchGroupByName = null;
	    mGroupsDB       = new GroupsDB(this);
	    mMembersGroupDB = new MembersGroupDB(this);
	    registerForContextMenu(getListView());
	 }
	
	@Override
	protected void onResume() {
		super.onResume();
		openDatabases();
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
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case DELETE_ID:
				AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
				mGroupsDB.deleteGroup(info.id);
				mMembersGroupDB.deleteAllMembersGroup(info.id);
				fillData();
				return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.delete_group);
	}

	private void openDatabases() {
		mGroupsDB.open();
		mMembersGroupDB.open();
	}
	
	private void closeDatabases() {
		if(mGroupsDB != null)
			mGroupsDB.close();
		if(mMembersGroupDB != null)
			mMembersGroupDB.close();
	}

	protected void fillData() {
		Cursor groupsCursor;
		if(mSearchGroupByName == null)
			groupsCursor = mGroupsDB.getAllGroups();
		else
			groupsCursor = mGroupsDB.getGroupsByName(mSearchGroupByName);
		startManagingCursor(groupsCursor);
		String[] from = new String[]{GroupsDB.KEY_NAME};
		int[]    to   = {android.R.id.text1};
		SimpleCursorAdapter groups = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, groupsCursor, from, to);
		setListAdapter(groups);
	}
	
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent = new Intent(this,GroupMembers.class);
        intent.putExtra(GroupsDB.KEY_ROWID,id);
        startActivity(intent);
     }

	public void onClick(View v) {
		Intent intent;
		switch(v.getId()) {
			case R.id.android_new_group_layout:
				intent = new Intent(this,CreateGroup.class);
				startActivity(intent); 
				break;
			case R.id.android_search_group_button:
				EditText groupName = (EditText)findViewById(R.id.android_search_group_edittext);
				String   group     = groupName.getText().toString();
				if(group.length() != 0) {
					groupName.setText("");
					intent = new Intent(this,GroupsTab.class);
					intent.putExtra(GroupsDB.KEY_NAME, group);
					startActivity(intent);
				}
				else
					Toast.makeText(this, "You need to type the name of the group", Toast.LENGTH_LONG).show();
				break;
		}
		if(mSearchGroupByName != null)
			finish();
	}
}
