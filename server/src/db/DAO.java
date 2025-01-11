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

    public DAO() {
        connect();
    }

    public void connect() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection("jdbc:derby://localhost:1527/User", "root", "root");
                System.out.println("Database connected successfully.");
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
        int result = 0;
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Users", "root", "root");
        PreparedStatement ps = con.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, username);
        ps.setString(2, password);
        result = ps.executeUpdate();
        con.close();
        ps.close();
        if (result > 0) {
            return true;// return respone to ui if positive added succ else -neg alert error 
        } else {
            return false;
        }

    }

    public static boolean loginForUser(String username, String password) throws SQLException {
        int result = 0;
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Users", "root", "root");
        PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, username);
        ps.setString(2, password);
        ResultSet resultSet = ps.executeQuery();
        return resultSet.next();

    }
    /*  
    public boolean loginUser(String username, String password) {
        String query = "SELECT * FROM Users WHERE username = ? AND password = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

  public boolean registerUser(String username, String password) {
        String query = "INSERT INTO Users (username, password,) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, password);
            
            statement.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Username already exists.");
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
     */

}
