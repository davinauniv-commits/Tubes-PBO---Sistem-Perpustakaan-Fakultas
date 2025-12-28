package com.app.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL =
        "jdbc:mysql://127.0.0.1:3306/perpusatakaan_fakultas?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver tidak ditemukan.", e);
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}