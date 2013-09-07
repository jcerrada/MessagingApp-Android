package com.messagingApp.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class Login extends Activity {
	public static final String PHONE_NUMBER = "Phone Number"; 
	public static final int    GET_NUMBER   = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
	}
	
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.android_login_button:
				EditText text  = (EditText)findViewById(R.id.android_login_edittext);
				String   phone = text.getText().toString();
				if(phone.length() > 0) {
					Intent   intent = new Intent();
					intent.putExtra(PHONE_NUMBER,Integer.parseInt(phone));
					setResult(RESULT_OK, intent);
					finish();
				}
				else
					Toast.makeText(this, "You have to insert a phone number", Toast.LENGTH_LONG).show();
				break;
		}
	}
}
