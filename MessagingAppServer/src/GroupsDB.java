import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;
import java.util.Vector;


public class GroupsDB {
	private static final String mGroupsTable = "CREATE TABLE IF NOT EXISTS groups ( "
											 + "_id          integer PRIMARY KEY autoincrement, "
											 + "group_name   text    NOT NULL, " 
											 + "members      text    NOT NULL);";
	private static final String mGroupNameKey      = "group_name";
	private static final String mMembersKey = "members";
	private Connection mConn;
	
	public GroupsDB() {
		try {
			Class.forName("org.sqlite.JDBC");
			mConn = DriverManager.getConnection("jdbc:sqlite:messagingApp.db");
			Statement stat = mConn.createStatement();
			stat.execute(mGroupsTable);
			stat.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private String membersToString(Vector<Integer> members) {
		StringBuilder sb = new StringBuilder();
		sb.append(members.get(0));
		for(int i=1; i<members.size(); i++)
			sb.append('#').append(members.get(i));
		return sb.toString();
	}
	
	public void createGroup(String name, Vector<Integer> members) {
		try {
			PreparedStatement pstmt = mConn.prepareStatement("INSERT INTO groups (" + mGroupNameKey + "," + mMembersKey + ") " +
					   										 "VALUES (?, ?);");
			pstmt.setString(1, name);
			pstmt.setString(2, membersToString(members));
			pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void deleteGroup(String groupName) {
		try {
			PreparedStatement pstmt = mConn.prepareStatement("DELETE FROM groups WHERE " + mGroupNameKey + " = ?;");
			pstmt.setString(1, groupName);
			pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public Vector<Integer> getMembersGroup(String name) {
		Vector<Integer> members = new Vector<Integer>();
		try {
			PreparedStatement pstmt = mConn.prepareStatement("SELECT " + mMembersKey + " FROM groups " +
											   				 "WHERE " + mGroupNameKey + " LIKE ?;");
			pstmt.setString(1, name);
			ResultSet res  = pstmt.executeQuery();
			res.next();
			StringTokenizer st = new StringTokenizer(res.getString(mMembersKey), "#");
			while(st.hasMoreTokens())
				members.add(Integer.valueOf(st.nextToken()));
			res.close();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return members;
	}
	
	public void addMemberToGroup(String groupName,Integer member) {
		try {
			if(!getMembersGroup(groupName).contains(member)) {
				Statement stat = mConn.createStatement();
				ResultSet res  = stat.executeQuery("SELECT " + mMembersKey + " FROM groups " +
				  	   						       "WHERE " + mGroupNameKey + "=" + groupName + ";");
				res.next();
				stat.executeUpdate("UPDATE groups " +
								   "SET " + mMembersKey + "=" + res.getString(mMembersKey) + "#" + member + " " +
					               "WHERE " + mGroupNameKey + "=" + groupName + ";");
				res.close();
				stat.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void deleteMemberFromGroup(String groupName, Integer member) {
		Vector<Integer> vMembers = getMembersGroup(groupName);
		vMembers.remove(member);
		try {
			Statement stat = mConn.createStatement();
			stat.executeUpdate("UPDATE groups " +
			             	   "SET " + mMembersKey + "=" + membersToString(vMembers) + " " +
			             	   "WHERE " + mGroupNameKey + "=" + groupName + ";");
			stat.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
