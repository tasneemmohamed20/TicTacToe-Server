package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import models.UserModel;
import org.apache.derby.jdbc.ClientDriver;

public class DAO {

    private Connection connection;
    private static boolean isConnected = false;

    public DAO() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection("jdbc:derby://localhost:1527/Users", "root", "root");
                System.out.println("Database connected successfully.");
                isConnected = true;
            }
        } catch (SQLException e) {
            Logger.getLogger(DAO.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            Logger.getLogger(DAO.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static boolean registerForUser(String username, String password) throws SQLException {

        if (isUsernameTaken(username)) {
            System.out.println("Username already exists.");
            return false;
        }

        int result = 0;
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Users", "root", "root");
        PreparedStatement ps = con.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, username);
        ps.setString(2, password);
        result = ps.executeUpdate();
        //con.close();
        ps.close();
        return result > 0;

    }

    public boolean updateUserStatus(String username, String status) throws SQLException {
    System.out.println("Updating user status for: " + username + " to " + status);
    DriverManager.registerDriver(new ClientDriver());
    try (Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Users", "root", "root");
         PreparedStatement ps = con.prepareStatement("UPDATE users SET status = ? WHERE username = ?")) {
        ps.setString(1, status);
        ps.setString(2, username);
        int result = ps.executeUpdate();
        System.out.println("Update result: " + result);
        return result > 0;
    }
}



    public UserModel loginForUser(String username, String password) throws SQLException {

        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Users", "root", "root");
        PreparedStatement ps = con.prepareStatement("SELECT * FROM Users WHERE username = ? AND password = ?");
        ps.setString(1, username);
        ps.setString(2, password);
        ResultSet resultSet = ps.executeQuery();
        UserModel user = null;
        while (resultSet.next()) {
            user = new UserModel(
                    resultSet.getInt("userid"),
                    resultSet.getString("username"),
                    resultSet.getString("password"),
                    resultSet.getString("score"),
                    resultSet.getString("status")
            );
        }
        return user;

    }

    private static boolean isUsernameTaken(String username) throws SQLException {

        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Users", "root", "root");
        PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, username);

        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;

    }

    public static Vector<String> getAllInlineUsers() throws SQLException {
        Vector<String> onlineUsers = new Vector<String>();
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Users", "root", "root");
        PreparedStatement ps = con.prepareStatement("SELECT username FROM users WHERE status = 'online'", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet res = ps.executeQuery();
        while (res.next()) {
            onlineUsers.add(res.getString("username"));
        }
        con.close();
        ps.close();
        return onlineUsers;
    }


    public static Vector<String> getAllUsers() throws SQLException {
        Vector<String> allUsers = new Vector<>();
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Users", "root", "root");
        PreparedStatement ps = con.prepareStatement("SELECT username FROM users", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet res = ps.executeQuery();
        while (res.next()) {
            allUsers.add(res.getString("username"));
        }
        con.close();
        ps.close();
        return allUsers;
    }
    
        public static boolean updateScoreByUsername(String username) throws SQLException {
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Users", "root", "root");

        PreparedStatement ps = con.prepareStatement("UPDATE users SET score = score + 5 WHERE username = ?");
        ps.setString(1, username);

        int result = ps.executeUpdate();
        ps.close();
        con.close();

        return result > 0;
    }
        
    public static Vector<String> getInGameUsers() throws SQLException {
        Vector<String> inGameUsers = new Vector<>();
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Users", "root", "root");
        PreparedStatement ps = con.prepareStatement("SELECT username FROM users WHERE status = 'ingame'", 
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet res = ps.executeQuery();
        while (res.next()) {
            inGameUsers.add(res.getString("username"));
        }
        con.close();
        ps.close();
        return inGameUsers;
    }

}