package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public boolean loginForUser(String username, String password) throws SQLException {

        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Users", "root", "root");
        PreparedStatement ps = con.prepareStatement("SELECT * FROM Users WHERE username = ? AND password = ?");
        ps.setString(1, username);
        ps.setString(2, password);
        ResultSet resultSet = ps.executeQuery();
        if (resultSet.next()) {

            return resultSet.getInt(1) > 0;
        } else {

            return false;
        }

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

}
