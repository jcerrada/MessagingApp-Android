import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;


public class UsersDB {
	private static final String mTableUsers = "CREATE TABLE IF NOT EXISTS users ( " +
											  "_id  integer PRIMARY KEY ," +
											  "contacts      text);";
	private static final String mIDKey       = "_id";
	private static final String mContactsKey = "contacts";
	private Connection mConn;
	
	public UsersDB() {
		try {
			Class.forName("org.sqlite.JDBC");
			mConn = DriverManager.getConnection("jdbc:sqlite:messagingApp.db");
			Statement stat = mConn.createStatement();
			stat.execute(mTableUsers);
			stat.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void closeDB() {
		try {
			mConn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void addUpdateUser(int phoneNumber, Vector<Integer> contacts) {
		StringBuilder sb = new StringBuilder();
		sb.append("\'");
		if(contacts.size() > 0)
			sb.append(contacts.get(0));
		for(int i=1; i < contacts.size(); i++)
			sb.append('*').append(contacts.get(i));
		sb.append("\'");
		try {
			String users = sb.toString();
			Statement stat = mConn.createStatement();
			stat.executeUpdate("INSERT OR REPLACE INTO users (" + mIDKey + ", " + mContactsKey + " ) " +
							   "VALUES ( " + phoneNumber + ", " + users + " );");
			stat.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public Vector<Integer> areRegistered(Vector<Integer> contacts) {
		Vector<Integer> vUsers = new Vector<Integer>();
		try {
			StringBuilder sb = new StringBuilder();
			if(contacts.size() > 0) {
				sb.append('(');
				for(int i=0; i<(contacts.size() - 1); i++)
					sb.append(contacts.get(i) + ",");
				sb.append(contacts.lastElement()).append(')');
				Statement stat = mConn.createStatement();
				ResultSet res  = stat.executeQuery("SELECT " + mIDKey + " FROM users " +
												   "WHERE " + mIDKey + " IN " + sb.toString() + ";");
				while(res.next())
					vUsers.add(new Integer(res.getInt(mIDKey)));
				res.close();
				stat.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return vUsers;
	}
	
	public HashMap<Integer, Void> getContactsUser(int phoneNumber) {
		HashMap<Integer, Void> map = new HashMap<Integer, Void>();
		try {
			Statement stat = mConn.createStatement();
			ResultSet res  = stat.executeQuery("SELECT " + mContactsKey + " FROM users " +
											   "WHERE " + mIDKey + "=" + phoneNumber + ";");
			if(res.next()) {
				StringTokenizer st = new StringTokenizer(res.getString(mContactsKey),"*");
				while(st.hasMoreTokens())
					map.put(Integer.valueOf(st.nextToken()),null);
			}
			res.close();
			stat.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return map;
	}
	
	public void deleteAllUsers() {
		Statement stat;
		try {
			stat = mConn.createStatement();
			stat.executeUpdate("DELETE FROM users;");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		/*UsersDB usersDB = new UsersDB();
		Vector<Integer> receivers = new Vector<Integer>();
		usersDB.deleteAllUsers();
		usersDB.addUpdateUser(677084244, receivers);
		HashMap<Integer, Void> users = usersDB.getContactsUser(677084244);
		Set<Integer> u = users.keySet();
		Iterator<Integer> it = u.iterator();
		while(it.hasNext()) {
			System.out.println("hola");
			System.out.println(it.next());
		}*/
	}
}
