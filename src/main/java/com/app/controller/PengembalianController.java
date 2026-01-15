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
        String judul = txtJudulBuku.getText().trim();
        LocalDate tglKembali = dpTanggalKembali.getValue();

        Connection conn = null;

        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            Integer idAnggota = getIdAnggotaByNpm(conn, npm);
            if (idAnggota == null) {
                showAlert(Alert.AlertType.ERROR, "Ditolak", "NPM tidak terdaftar.");
                conn.rollback();
                return;
            }

            // Cari peminjaman aktif yang memang memuat buku dengan judul itu
            Integer idPeminjaman = getIdPeminjamanAktifByJudul(conn, idAnggota, judul);
            if (idPeminjaman == null) {
                showAlert(Alert.AlertType.ERROR, "Gagal",
                        "Tidak ditemukan peminjaman aktif untuk judul tersebut (status='Dipinjam').");
                conn.rollback();
                return;
            }

            // Update stok dari detailpinjam
            boolean adaDetail = updateStokDariDetailPinjam(conn, idPeminjaman);
            if (!adaDetail) {
                showAlert(Alert.AlertType.ERROR, "Gagal",
                        "Detail peminjaman tidak ditemukan di detailpinjam.");
                conn.rollback();
                return;
            }

            // Hitung denda (pinjam max 7 hari)
            long hariTerlambat = 0;
            int totalDenda = 0;

            LocalDate tglPinjam = getTglPinjamByIdPeminjaman(conn, idPeminjaman);
            LocalDate jatuhTempo = tglPinjam.plusDays(7);

            if (tglKembali.isAfter(jatuhTempo)) {
                hariTerlambat = ChronoUnit.DAYS.between(jatuhTempo, tglKembali);
            }

            final int DENDA_PER_HARI = 1000;
            totalDenda = (int) (hariTerlambat * DENDA_PER_HARI);

            if (totalDenda > 0) {
                upsertDenda(conn, idPeminjaman, totalDenda);
            }

            // Update peminjaman menjadi Kembali (cuma kalau masih Dipinjam)
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE peminjaman SET status='Kembali', tgl_kembali=? WHERE id_peminjaman=? AND status='Dipinjam'")) {
                ps.setDate(1, Date.valueOf(tglKembali));
                ps.setInt(2, idPeminjaman);

                int updated = ps.executeUpdate();
                if (updated == 0) {
                    showAlert(Alert.AlertType.WARNING, "Tidak Diproses",
                            "Peminjaman ini sudah tidak berstatus 'Dipinjam'.");
                    conn.rollback();
                    return;
                }
            }

            conn.commit();

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
            try { if (conn != null) conn.rollback(); } catch (SQLException ignored) {}
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
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

    private Integer getIdPeminjamanAktifByJudul(Connection conn, int idAnggota, String judul) throws SQLException {
        String sql = """
            SELECT p.id_peminjaman
            FROM peminjaman p
            JOIN detailpinjam d ON d.id_peminjaman = p.id_peminjaman
            JOIN buku b ON b.id_buku = d.id_buku
            WHERE p.id_anggota=? AND p.status='Dipinjam' AND b.judul LIKE ?
            ORDER BY p.tgl_pinjam DESC
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAnggota);
            ps.setString(2, "%" + judul + "%");
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

    private boolean updateStokDariDetailPinjam(Connection conn, int idPeminjaman) throws SQLException {
        String sqlDetail = """
            SELECT id_buku, jumlah
            FROM detailpinjam
            WHERE id_peminjaman = ?
        """;

        boolean adaDetail = false;

        try (PreparedStatement ps = conn.prepareStatement(sqlDetail)) {
            ps.setInt(1, idPeminjaman);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    adaDetail = true;

                    int idBuku = rs.getInt("id_buku");
                    int jumlah = rs.getInt("jumlah");

                    try (PreparedStatement psUpdate = conn.prepareStatement("""
                        UPDATE buku
                        SET jumlah_tersedia = jumlah_tersedia + ?,
                            status_buku = IF(jumlah_tersedia + ? > 0, 'Tersedia', 'Tidak Tersedia')
                        WHERE id_buku = ?
                    """)) {
                        psUpdate.setInt(1, jumlah);
                        psUpdate.setInt(2, jumlah);
                        psUpdate.setInt(3, idBuku);

                        int updated = psUpdate.executeUpdate();
                        if (updated == 0) {
                            throw new SQLException("Buku tidak ditemukan untuk id_buku=" + idBuku);
                        }
                    }
                }
            }
        }

        return adaDetail;
    }

    private void upsertDenda(Connection conn, int idPeminjaman, int totalDenda) throws SQLException {
        // STANDAR: Belum Lunas
        String status = (totalDenda > 0) ? "Belum Lunas" : "Tidak Ada Denda";

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