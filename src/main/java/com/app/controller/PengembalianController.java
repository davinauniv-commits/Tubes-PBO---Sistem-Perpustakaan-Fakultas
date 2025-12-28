package com.app.controller;

import com.app.config.Database;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PengembalianController extends BaseController {
    @FXML private VBox sidebarContainer;

    @FXML private TextField txtNama;
    @FXML private TextField txtNpm;
    @FXML private TextField txtJudulBuku;
    @FXML private DatePicker dpTanggalKembali;

    @FXML
    private void initialize() {
        attachSidebar(sidebarContainer, "pengembalian");
        dpTanggalKembali.setValue(LocalDate.now());
    }

    private void loadSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/app/sidebar.fxml"));
            javafx.scene.Parent sidebar = loader.load();
            SidebarController sc = loader.getController();
            sc.setOwner(this);
            sidebarContainer.getChildren().setAll(sidebar);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleKonfirmasiPengembalian() {

        if (txtNama.getText().trim().isEmpty() ||
            txtNpm.getText().trim().isEmpty() ||
            txtJudulBuku.getText().trim().isEmpty() ||
            dpTanggalKembali.getValue() == null) {

            showAlert(Alert.AlertType.ERROR, "Form Tidak Lengkap",
                    "Nama, NPM, Judul Buku, dan Tanggal Pengembalian wajib diisi!");
            return;
        }

        String npm = txtNpm.getText().trim();
        LocalDate tglKembali = dpTanggalKembali.getValue();

        long hariTerlambat = 0;
        int totalDenda = 0;

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            Integer idAnggota = getIdAnggotaByNpm(conn, npm);
            if (idAnggota == null) {
                showAlert(Alert.AlertType.ERROR, "Ditolak", "NPM tidak terdaftar.");
                conn.rollback();
                return;
            }

            Integer idPeminjaman = getIdPeminjamanAktif(conn, idAnggota);
            if (idPeminjaman == null) {
                showAlert(Alert.AlertType.ERROR, "Gagal",
                        "Tidak ditemukan peminjaman aktif (status='Dipinjam') untuk NPM tersebut.");
                conn.rollback();
                return;
            }

            // ===== HITUNG DENDA jika telat > 7 hari =====
            LocalDate tglPinjam = getTglPinjamByIdPeminjaman(conn, idPeminjaman);
            LocalDate jatuhTempo = tglPinjam.plusDays(7);

            if (tglKembali.isAfter(jatuhTempo)) {
                hariTerlambat = ChronoUnit.DAYS.between(jatuhTempo, tglKembali);
            }

            final int DENDA_PER_HARI = 1000; // ubah sesuai aturan
            totalDenda = (int) (hariTerlambat * DENDA_PER_HARI);

            // MENYIMPAN DENDA KE DB HANYA JIKA ADA DENDA
            // (Kalau kamu ingin tetap simpan histori 0, hapus IF ini dan langsung panggil upsertDenda)
            if (totalDenda > 0) {
                upsertDenda(conn, idPeminjaman, totalDenda);
            }

            // ===== UPDATE PEMINJAMAN =====
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE peminjaman SET status='Kembali', tgl_kembali=? WHERE id_peminjaman=?")) {
                ps.setDate(1, Date.valueOf(tglKembali));
                ps.setInt(2, idPeminjaman);
                ps.executeUpdate();
            }

            conn.commit();

            // ===== SHOW ALERT =====
            if (totalDenda > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Berhasil",
                        "Pengembalian berhasil.\nDenda: Rp " + totalDenda +
                        " (terlambat " + hariTerlambat + " hari)");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Berhasil",
                        "Pengembalian berhasil. Tidak ada denda.");
            }

            clearForm();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private Integer getIdAnggotaByNpm(Connection conn, String npm) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_anggota FROM anggota WHERE npm=?")) {
            ps.setString(1, npm);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id_anggota");
                return null;
            }
        }
    }

    private Integer getIdPeminjamanAktif(Connection conn, int idAnggota) throws SQLException {
        String sql = """
            SELECT id_peminjaman
            FROM peminjaman
            WHERE id_anggota=? AND status='Dipinjam'
            ORDER BY tgl_pinjam DESC
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAnggota);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id_peminjaman");
                return null;
            }
        }
    }

    private LocalDate getTglPinjamByIdPeminjaman(Connection conn, int idPeminjaman) throws SQLException {
        String sql = "SELECT tgl_pinjam FROM peminjaman WHERE id_peminjaman=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPeminjaman);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDate("tgl_pinjam").toLocalDate();
            }
        }
        throw new SQLException("tgl_pinjam tidak ditemukan untuk id_peminjaman=" + idPeminjaman);
    }

    /**
     * UPSERT DENDA (ANTI DOBEL) - MySQL/MariaDB
     * Wajib: set UNIQUE pada kolom denda.id_peminjaman
     *
     * Jalankan sekali di DB:
     * ALTER TABLE denda ADD UNIQUE (id_peminjaman);
     */
    private void upsertDenda(Connection conn, int idPeminjaman, int totalDenda) throws SQLException {
        String status = "Terlambat, Dikenakan Denda";

        String sql = """
            INSERT INTO denda (id_peminjaman, jumlah_denda, status_denda)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                jumlah_denda = VALUES(jumlah_denda),
                status_denda = VALUES(status_denda)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPeminjaman);
            ps.setInt(2, totalDenda);
            ps.setString(3, status);
            ps.executeUpdate();
        }
    }

    private void clearForm() {
        txtNama.clear();
        txtNpm.clear();
        txtJudulBuku.clear();
        dpTanggalKembali.setValue(LocalDate.now());
    }
}