package org.blacksmith.blacksmith;

import java.lang.*;

import java.io.File;
import java.sql.*;

public class SimpleSqlDatabase {

    public Connection conn;
    private String tableName;

    public SimpleSqlDatabase(Blacksmith plugin, String tableName) {
        this.tableName = tableName;
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + File.separator + "database.db");
            createTableIfNotExists();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // метод для создания таблицы, если её не существует
    private void createTableIfNotExists() throws SQLException {
        Statement stmt = conn.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + " key VARCHAR(255) NOT NULL,"
                + " value INT NOT NULL,"
                + " PRIMARY KEY (key))";
        stmt.executeUpdate(sql);
    }

    // метод для получения значения по ключу
    public int getValue(String key) throws SQLException {
        int value = 0;
        String sql = "SELECT value FROM " + tableName + " WHERE key=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, key);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            value = rs.getInt("value");
        }
        rs.close();
        stmt.close();
        return value;
    }
    public void sumNumInKey(String key, int value) throws SQLException {
        int orig_value = getValue(key);
        setValue(key, orig_value + value);
    }

    // метод для добавления/обновления значения по ключу
    public void setValue(String key, int value) throws SQLException {
        String sql = "INSERT OR REPLACE INTO " + tableName + " (key, value) VALUES (?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, key);
        stmt.setInt(2, value);
        stmt.executeUpdate();
        stmt.close();
    }

    // метод для удаления значения по ключу
    public void deleteValue(String key) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE key=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, key);
        stmt.executeUpdate();
        stmt.close();
    }
}
