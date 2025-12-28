package com.app.controller;

import com.app.config.Database;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardController extends BaseController {
    @FXML private VBox sidebarContainer;

    @FXML private Label lblTotalDenda;
    @FXML private Label lblAnggota;
    @FXML private Label lblDipinjam;

    @FXML private Label lblNamaAdmin;
    @FXML private Label lblKategoriAdmin;

    @FXML private Label lblInfoNama;
    @FXML private Label lblInfoUsername;
    @FXML private Label lblInfoEmail;
    @FXML private Label lblInfoTelp;

    @FXML private Label lblStatus;

    @FXML
    public void initialize() {
        attachSidebar(sidebarContainer, "dashboard");

        safeSet(lblTotalDenda, "...");
        safeSet(lblAnggota, "...");
        safeSet(lblDipinjam, "...");
        safeSet(lblNamaAdmin, "NAMA ADMIN");
        safeSet(lblKategoriAdmin, "-");
        safeSet(lblInfoNama, "-");
        safeSet(lblInfoUsername, "-");
        safeSet(lblInfoEmail, "-");
        safeSet(lblInfoTelp, "-");
        safeSet(lblStatus, "");

        // kalau admin sudah login sebelumnya, load lagi datanya saat dashboard dibuka
        if (currentAdminId > 0) {
            loadCards();
            loadAdminInfo();
        } else {
            safeSet(lblStatus, "Admin ID belum dikirim dari login.");
        }
    }

    private void loadSidebar() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/com/app/sidebar.fxml"));

            Parent sidebar = loader.load();

            // Sidebar styling (untuk full tinggi)
            VBox.setVgrow(sidebar, javafx.scene.layout.Priority.ALWAYS);
            ((VBox) sidebar).setFillWidth(true);

            SidebarController sc = loader.getController();
            sc.setOwner(this);

            sidebarContainer.getChildren().setAll(sidebar);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setCurrentAdminId(int id) {
        super.setCurrentAdminId(id); 
        loadCards();
        loadAdminInfo();
    }

    private void loadCards() {
        try (Connection conn = Database.getConnection()) {

            long totalNominal = queryLong(conn,
                    "SELECT COALESCE(SUM(jumlah_denda),0) " +
                    "FROM denda WHERE status_denda='Belum Dibayar'");

            int totalKasus = queryInt(conn,
                    "SELECT COUNT(*) FROM denda WHERE status_denda='Belum Dibayar'");

            safeSet(lblTotalDenda, rupiah(totalNominal) + " (" + totalKasus + ")");
            safeSet(lblAnggota, String.valueOf(queryInt(conn, "SELECT COUNT(*) FROM anggota")));
            safeSet(lblDipinjam, String.valueOf(queryInt(conn,
                    "SELECT COUNT(*) FROM peminjaman WHERE status='Dipinjam'")));

            safeSet(lblStatus, "");

        } catch (Exception e) {
            e.printStackTrace();
            safeSet(lblTotalDenda, "Rp 0 (0)");
            safeSet(lblAnggota, "0");
            safeSet(lblDipinjam, "0");
            safeSet(lblStatus, "Gagal memuat dashboard.");
        }
    }

    private void loadAdminInfo() {
        if (currentAdminId <= 0) {
            safeSet(lblStatus, "Admin ID belum dikirim dari login.");
            return;
        }

        String sql = """
            SELECT nama_admin, username, email, no_telp, katagori_admin
            FROM admin_perpus WHERE id_admin=? LIMIT 1
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, currentAdminId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    safeSet(lblNamaAdmin, rs.getString("nama_admin"));
                    safeSet(lblKategoriAdmin, rs.getString("katagori_admin"));
                    safeSet(lblInfoNama, rs.getString("nama_admin"));
                    safeSet(lblInfoUsername, rs.getString("username"));
                    safeSet(lblInfoEmail, rs.getString("email"));
                    safeSet(lblInfoTelp, rs.getString("no_telp"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            safeSet(lblStatus, "Gagal memuat info admin.");
        }
    }

    // CARD CLICK
    @FXML
    private void openDendaBelumBayar(ActionEvent e) {
        switchScene(e, "/com/app/denda_belum_bayar.fxml");
    }
}

