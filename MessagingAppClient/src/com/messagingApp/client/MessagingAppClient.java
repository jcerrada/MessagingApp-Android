package com.messagingApp.client;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class MessagingAppClient extends TabActivity {
	private Intent mServiceIntent = null;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab
	    if(!MessagingAppService.isRunning()) {
	    	intent = new Intent(this, Login.class);
	    	startActivityForResult(intent, Login.GET_NUMBER);
	    }

	    intent = new Intent().setClass(this, ChatsTab.class);
	    spec = tabHost.newTabSpec("chats").setIndicator("Chats", res.getDrawable(R.drawable.ic_tab_chats)).setContent(intent);
	    tabHost.addTab(spec);
	    
	    intent = new Intent().setClass(this, ContactsTab.class);
	    spec = tabHost.newTabSpec("contacts").setIndicator("Contacs", res.getDrawable(R.drawable.ic_tab_contacts)).setContent(intent);
	    tabHost.addTab(spec);
	    
	    intent = new Intent().setClass(this, GroupsTab.class);
	    spec = tabHost.newTabSpec("groups").setIndicator("Groups", res.getDrawable(R.drawable.ic_tab_groups)).setContent(intent);
	    tabHost.addTab(spec);
	    
	    tabHost.setCurrentTab(1);
    }
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(data != null) {
				int phoneNumber = data.getExtras().getInt(Login.PHONE_NUMBER);
				mServiceIntent = new Intent(this, MessagingAppService.class); 
				mServiceIntent.putExtra(Login.PHONE_NUMBER, phoneNumber);
				startService(mServiceIntent);
		}
		else
			finish();
	}    
}