import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;


public class ClientThread implements Runnable {
	private Integer						   mPhoneNumber;
	private HashMap<Integer, ClientThread> mMapClients;
	private Socket                         mClient;
	private BufferedReader                 mIn;
	private PrintWriter		           	   mOut;
	private boolean                        mExit;
	private ProtocolMessage				   mMessage;
	private MessagesDB					   mMessagesDB;
	private GroupsDB 	 				   mGroupsDB;
	private UsersDB						   mUsersDB;
	
	public ClientThread(HashMap<Integer, ClientThread> mapClients, Socket client, MessagesDB messagesDB, UsersDB usersDB, GroupsDB groupsDB) {
		try {
			mMapClients = mapClients;
			mClient     = client;
			mMessagesDB = messagesDB;
			mUsersDB    = usersDB;
			mGroupsDB   = groupsDB;
			mIn         = new BufferedReader(new InputStreamReader(mClient.getInputStream()));
			mOut        = new PrintWriter(mClient.getOutputStream(), true);
			mOut.flush();
			mExit       = false;
			
			Thread thread = new Thread(this);
			thread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void closeConnection() {
		mMapClients.remove(mPhoneNumber);
		mExit = true;
	}
	
	private void sendMessage(String message) {
		mOut.println(message);
		System.out.println("Enviando server: " + message);
	}
	
	private void notifyUsers(Vector<Integer> users) {
		for(int i=0; i < users.size(); i++) {
			ClientThread ct = mMapClients.get(users.get(i));
			if(ct != null)
				ct.sendMessage(mMessage.messToString());
			else
				mMessagesDB.addMessage(users.get(i), mMessage.messToString());
		}
	}
	
	private void sendSimpleMessage() {
		Vector<String>  messages  = mMessage.toStringVector();
		Vector<Integer> receivers = mMessage.getReceivers();
		for(int i=0; i < mMessage.getNumReceivers(); i++) {
			ClientThread ct = mMapClients.get(receivers.get(i));
			if(ct != null)
				ct.sendMessage(messages.get(i));
			else
				mMessagesDB.addMessage(receivers.get(i), messages.get(i));
		}
	}
	
	private Vector<Integer> sendContacts() {
		Vector<Integer> contacts = mUsersDB.areRegistered(mMessage.getReceivers());
		ProtocolMessage message = new ProtocolMessage(ProtocolMessage.SCN, mMessage.getSender(),contacts,Calendar.getInstance(),null,null);
		sendMessage(message.messToString());
		return contacts;
	}
	
	private void createGroup() {
		mGroupsDB.createGroup(mMessage.getGroupName() + "_" + mMessage.getSender(), mMessage.getReceivers());
		Vector<Integer> receivers = mMessage.getReceivers();
		receivers.removeElement(new Integer (mMessage.getSender()));
		notifyUsers(receivers);
	}
	
	private void deleteGroup() {
		Vector<Integer> members = mGroupsDB.getMembersGroup(mMessage.getGroupName());
		members.remove(new Integer(mMessage.getSender()));
		mGroupsDB.deleteGroup(mMessage.getGroupName());
		notifyUsers(members);
	}
	
	private void addMemberToGroup() {
		mGroupsDB.addMemberToGroup(mMessage.getGroupName(), mMessage.getReceivers().get(0));
		Vector<Integer> members = mGroupsDB.getMembersGroup(mMessage.getGroupName());
		members.remove(new Integer(mMessage.getSender()));
		notifyUsers(members);
	}
	
	private void deleteMemberFromGroup() {
		Vector<Integer> members = mGroupsDB.getMembersGroup(mMessage.getGroupName());
		mGroupsDB.deleteMemberFromGroup(mMessage.getGroupName(), mMessage.getReceivers().get(0));
		members.remove(mMessage.getSender());
		notifyUsers(members);
	}
	
	private void sendMessageToGroup() {
		Vector<Integer> members = mGroupsDB.getMembersGroup(mMessage.getGroupName());
		for(int i=0; i < mMessage.getNumReceivers(); i++)
			members.remove(mMessage.getReceivers().get(i));
		members.remove(new Integer(mMessage.getSender()));
		notifyUsers(members);
	}
	
	private void sendMessageToCommonFriends() {
		HashMap<Integer, Void> user1 = mUsersDB.getContactsUser(mPhoneNumber);
		Set<Integer>           user2 = mUsersDB.getContactsUser(mMessage.getReceivers().get(0)).keySet();
		Iterator<Integer>      it    = user2.iterator();
		Vector<Integer>        commonFriends = new Vector<Integer>();
		while(it.hasNext()) {
			Integer phoneNumber = it.next();
			if(user1.containsKey(phoneNumber));
				commonFriends.add(phoneNumber);
		}
		mMessage.setType(ProtocolMessage.SSM);
		notifyUsers(commonFriends);
	}
	
	public void run() {
		try {
			while(!mExit) {
				String message = mIn.readLine();
				if(message != null) {
					System.out.println("Recibiendo server: " + message);
					mMessage = new ProtocolMessage(message);
					switch(mMessage.getType()) {
						case ProtocolMessage.CNT: mPhoneNumber = mMessage.getSender();
						  			  	  mMapClients.put(mPhoneNumber, this); 
						  			  	  break;
						case ProtocolMessage.DSC: closeConnection(); break;
						case ProtocolMessage.SCN: mUsersDB.addUpdateUser(mPhoneNumber.intValue(), sendContacts()); break;
						case ProtocolMessage.SSM: sendSimpleMessage(); break;
						case ProtocolMessage.CGR: createGroup(); break;
						case ProtocolMessage.DGR: deleteGroup(); break;
						case ProtocolMessage.ACG: addMemberToGroup(); break;
						case ProtocolMessage.DCG: deleteMemberFromGroup(); break;
						case ProtocolMessage.SMG: sendMessageToGroup(); break;
						case ProtocolMessage.SMC: sendMessageToCommonFriends(); break;
					}
					if(mMessage.getType() != ProtocolMessage.DSC) {
						Vector<String> messages = mMessagesDB.getMessagesReceiver(mPhoneNumber);
						for(int i=0; i < messages.size(); i++)
							sendMessage(messages.get(i));
					}
				}
			}			
			mIn.close();
			mOut.close();
			mClient.close();
			mMapClients.remove(mPhoneNumber);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
