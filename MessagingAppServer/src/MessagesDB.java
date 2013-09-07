import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

public class MessagesDB {
	private static final String mTableMessages = "CREATE TABLE IF NOT EXISTS messages ( "
											   + "_id        integer PRIMARY KEY autoincrement, "
											   + "receiver   integer NOT NULL, "
											   + "message    text);";
	
	private static final String mIDKey        = "_id";
	private static final String mReceiverKey  = "receiver";
	private static final String mTypeKey      = "type";
	private static final String mSenderKey    = "sender";
	private static final String mReceiversKey = "receivers";
	private static final String mDateKey      = "date";
	private static final String mGroupNameKey = "group_name";
	private static final String mMessageKey   = "message";
	private Connection mConn;
	
	public MessagesDB() {
		try {
			Class.forName("org.sqlite.JDBC");
			mConn = DriverManager.getConnection("jdbc:sqlite:messagingApp.db");
			Statement stat = mConn.createStatement();
			stat.execute(mTableMessages);
			stat.close();			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void addMessage(int receiver, String message) {
		try {
			PreparedStatement pstmt = mConn.prepareStatement("INSERT INTO messages (" + mReceiverKey + "," + mMessageKey + ") " +
					           								 "VALUES ( ? , ?)");
			pstmt.setInt(1, receiver);
			pstmt.setString(2, message);
			pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public Vector<String> getMessagesReceiver(int receiver) {
		Vector<String> vMessages = new Vector<String>();
		try {
			Statement stat = mConn.createStatement();
			ResultSet res  = stat.executeQuery("SELECT * FROM messages " +
					                           "WHERE " + mReceiverKey + "=" + receiver + ";");
			while(res.next())
				vMessages.add(res.getString(mMessageKey));
			stat.executeUpdate("DELETE FROM messages WHERE " + mReceiverKey + "=" + receiver + ";");
			res.close();
			stat.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return vMessages;
	}
	
	public void closeDB() {
		try {
			mConn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		MessagesDB mess = new MessagesDB();
		ProtocolMessage message = new ProtocolMessage(0, 677084244, new Vector<Integer>(), Calendar.getInstance(), "hola", "me llamo jose luis");
		mess.addMessage(669238051, message.messToString());
		Vector<String> v = mess.getMessagesReceiver(669238051);
		for(int i = 0; i < v.size(); i++)
			System.out.println("Message " + i + ": " + v.get(i));
	}
}