import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;


public class ServerMA {
	private static final int PORT     = 11325;
	private static final int MAX_CONN = 50;
	private ServerSocket                   mServer;
	private Socket                         mClient;
	private boolean                        mExit;
	private HashMap<Integer, ClientThread> mMapClients = new HashMap<Integer, ClientThread>();
	private MessagesDB					   mMessagesDB;
	private GroupsDB 	 				   mGroupsDB;
	private UsersDB						   mUsersDB;
	
	public ServerMA() {
		try {
			mServer = new ServerSocket(PORT, MAX_CONN);
			mExit   = false;
			mMessagesDB = new MessagesDB();
			mUsersDB    = new UsersDB();
			mGroupsDB   = new GroupsDB();
			while(!mExit) {
				mClient = mServer.accept();
				new ClientThread(mMapClients, mClient, mMessagesDB, mUsersDB, mGroupsDB);
			}
			mServer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void closeConnection() {
		mExit = true;	
	}
	
	public static void main(String[] args) {
		ServerMA server = new ServerMA();
	}
}
