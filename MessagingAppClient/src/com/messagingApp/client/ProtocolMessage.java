package com.messagingApp.client;
import java.util.Calendar;
import java.util.Vector;

import com.google.gson.Gson;

/**
 * *
 * @author Jose
 * CNT --> Connect
 * DSC --> Disconnect
 * SSM --> Send Simple Message
 * SCN --> Send Contacts
 * CGR --> Create Group
 * DGR --> Delete Group
 * ACG --> Add Contact to a Group
 * DCG --> Delete Contact from a Group
 * SMG --> Send Message to a group
 * SMC --> Send message to common friends
 */
public class ProtocolMessage {
	public static final int CNT = 1;
	public static final int DSC = 2;
	public static final int SSM = 3;
	public static final int SCN = 4;
	public static final int CGR = 5;
	public static final int DGR = 6;
	public static final int ACG = 7;
	public static final int DCG = 8;
	public static final int SMG = 9;
	public static final int SMC = 10;
	private int             mType;
	private int             mSender;
	private Vector<Integer> mReceivers = new Vector<Integer>();
	private Calendar        mDate;
	private String          mGroupName;
	private String          mMessage;
	
	public ProtocolMessage(int type, int sender, Vector<Integer> receivers, 
			               Calendar date, String groupName, String message) {
		mType      = type;
		mSender    = sender;
		mReceivers = receivers;
		mDate      = date;
		mDate      = Calendar.getInstance();
		mGroupName = groupName;
		mMessage   = message;
	}
	
	public ProtocolMessage(String message) {	
		Gson    g  = new Gson();
		ProtocolMessage m  = g.fromJson(message,ProtocolMessage.class);
		mType      = m.getType();
		mSender    = m.getSender();
		mReceivers = m.getReceivers();
		mDate      = m.getDate();
		mGroupName = m.getGroupName();
		mMessage   = m.getMessage();
	}
	
	public int getType() {
		return mType;
	}
	
	public int getSender() {
		return mSender;
	}
	
	public int getNumReceivers() {
		return mReceivers.size();
	}
	
	public Vector<Integer> getReceivers() {
		return mReceivers;
	}
	
	public Calendar getDate() {
		return mDate;
	}
	
	public String getGroupName() {
		return mGroupName;
	}
	
	public String getMessage() {
		return mMessage;
	}
	
	public void setType(int type) {
		mType = type;
	}
	
	public void printAll() {
		System.out.println("Type: " + mType);
		System.out.println("Sender: " + mSender);
		for(int i=0; i<mReceivers.size();i++) {
			System.out.println("Receiver " + i + ": " + mReceivers.get(i));
		}
		System.out.println("Date: " + mDate.getTime());
		if(mGroupName != null)
			System.out.println("Group Name: " + mGroupName);
		if(mMessage != null)
			System.out.println("Message: " + mMessage);
	}
	
	public String messToString() {
		Gson g = new Gson();
		return g.toJson(this);
	}
	
	public Vector<String> toStringVector() {
		Vector<String> vString = new Vector<String>();
		for(int i = 0; i < mReceivers.size(); i++) {
			Vector<Integer> v = new Vector<Integer>();
			v.add(mReceivers.get(i));
			vString.add(new ProtocolMessage(mType, mSender,v,mDate, mGroupName, mMessage).messToString());
		}
		return vString;
	}
}
 