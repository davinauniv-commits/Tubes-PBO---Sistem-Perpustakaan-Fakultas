package com.app.controller;

import com.app.config.Database;
import com.app.controller.DashboardController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML private TextField txtUsername;

    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private Label lblTogglePassword;

    @FXML private Button btnLogin;

    private boolean passwordVisible = false;

    @FXML
    private void initialize() {
        txtPassword.setVisible(true);
        txtPassword.setManaged(true);

        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);

        lblTogglePassword.setText("Show");
        lblTogglePassword.setOnMouseClicked(e -> togglePasswordVisibility());

        txtPassword.setOnAction(e -> handleLogin());
        txtPasswordVisible.setOnAction(e -> handleLogin());
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            txtPasswordVisible.setText(txtPassword.getText());
            txtPasswordVisible.setVisible(true);
            txtPasswordVisible.setManaged(true);

            txtPassword.setVisible(false);
            txtPassword.setManaged(false);

            lblTogglePassword.setText("Hide");
        } else {
            txtPassword.setText(txtPasswordVisible.getText());
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);

            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);

            lblTogglePassword.setText("Show");
        }
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        String password = passwordVisible
                ? (txtPasswordVisible.getText() == null ? "" : txtPasswordVisible.getText())
                : (txtPassword.getText() == null ? "" : txtPassword.getText());

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Username dan password wajib diisi.");
            return;
        }

        String sql = "SELECT id_admin FROM admin_perpus WHERE username=? AND password=? LIMIT 1";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    showAlert(Alert.AlertType.ERROR, "Login Gagal", "Username atau password salah.");
                    return;
                }

                int idAdmin = rs.getInt("id_admin");

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/app/dashboard.fxml"));
                Scene scene = new Scene(loader.load());

                DashboardController ctrl = loader.getController();
                ctrl.setCurrentAdminId(idAdmin);

                Stage stage = (Stage) btnLogin.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Dashboard - Sistem Perpustakaan");

                stage.show();

                com.app.util.WindowUtil.forceFull(stage);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Gagal konek/akses database.\nPastikan MySQL jalan.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}