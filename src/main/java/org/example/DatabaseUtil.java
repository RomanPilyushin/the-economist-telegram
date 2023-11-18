package org.example;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class DatabaseUtil {
    private static final Logger LOGGER = Logger.getLogger(DatabaseUtil.class.getName());
    private static final String DB_URL = "jdbc:sqlite:subscribed_users.db";

    static {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS SubscribedUsers (ChatId INTEGER PRIMARY KEY)";
            stmt.executeUpdate(sql);  // Use executeUpdate for CREATE TABLE
        } catch (SQLException e) {
            // Handle exceptions
        }
    }

    public static void addUser(long chatId) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT OR IGNORE INTO SubscribedUsers (ChatId) VALUES (?)")) {
            pstmt.setLong(1, chatId);
            pstmt.executeUpdate();  // Use executeUpdate for INSERT
        } catch (SQLException e) {
            // Handle exceptions
        }
    }

    public static void removeUser(long chatId) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM SubscribedUsers WHERE ChatId = ?")) {
            pstmt.setLong(1, chatId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                LOGGER.info("User removed: " + chatId);
            } else {
                LOGGER.info("No such user to remove: " + chatId);
            }
        } catch (SQLException e) {
            LOGGER.severe("Error removing user: " + e.getMessage());
        }
    }

    public static Set<Long> getAllUsers() {
        Set<Long> users = new HashSet<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT ChatId FROM SubscribedUsers");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(rs.getLong("ChatId"));
            }
        } catch (SQLException e) {
            LOGGER.severe("Error retrieving users: " + e.getMessage());
        }
        return users;
    }
}
