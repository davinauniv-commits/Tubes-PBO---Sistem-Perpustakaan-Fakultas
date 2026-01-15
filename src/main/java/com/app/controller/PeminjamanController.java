package com.app.controller;

import com.app.config.Database;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.time.LocalDate;

public class PeminjamanController extends BaseController {

    @FXML private VBox sidebarContainer;

    @FXML private TextField txtNama;
    @FXML private TextField txtNpm;
    @FXML private TextField txtJudulBuku;
    @FXML private DatePicker dpTanggalPinjam;

    private final int CURRENT_ADMIN_ID = 1;

    @FXML
    private void initialize() {
        attachSidebar(sidebarContainer, "peminjaman");
        if (dpTanggalPinjam != null) dpTanggalPinjam.setValue(LocalDate.now());

        if (txtNama != null) {
            txtNama.setPromptText("Nama Lengkap");
            txtNama.setEditable(true);
        }

        activateOnClick(txtNpm, "NPM", false);
        activateOnClick(txtJudulBuku, "Judul Buku", false);

        if (txtNpm != null) {
            txtNpm.setOnAction(e -> autoIsiNamaDariNpm());
            txtNpm.focusedProperty().addListener((o, oldF, newF) -> {
                if (!newF) autoIsiNamaDariNpm();
            });
        }
    }

    private void activateOnClick(TextField tf, String prompt, boolean editableOnStart) {
        if (tf == null) return;
        tf.setPromptText(prompt);
        tf.setEditable(editableOnStart);

        final boolean[] activated = {false};
        tf.focusedProperty().addListener((obs, was, is) -> {
            if (is && !activated[0]) {
                activated[0] = true;
                tf.setEditable(true);
            }
        });
    }

    private void autoIsiNamaDariNpm() {
        String npm = (txtNpm == null || txtNpm.getText() == null) ? "" : txtNpm.getText().trim();
        if (npm.isEmpty()) return;

        try (Connection conn = Database.getConnection()) {
            Anggota a = getAnggotaByNpm(conn, npm);
            if (a != null && txtNama != null && (txtNama.getText() == null || txtNama.getText().isEmpty())) {
                txtNama.setText(a.nama);
            }
        } catch (Exception ignored) {}
    }

    @FXML
    private void handleAjukanPeminjaman() {

        String npm = txtNpm.getText().trim();
        String judulInput = txtJudulBuku.getText().trim();
        LocalDate tglPinjam = dpTanggalPinjam.getValue();

        if (npm.isEmpty() || judulInput.isEmpty() || tglPinjam == null) {
            showAlert(Alert.AlertType.ERROR,
                    "Form Tidak Lengkap",
                    "NPM, Judul Buku, dan Tanggal Peminjaman wajib diisi!");
            return;
        }

        Connection conn = null;

        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            // 1) Ambil anggota
            Anggota anggota = getAnggotaByNpm(conn, npm);
            if (anggota == null) {
                showAlert(Alert.AlertType.ERROR, "Ditolak", "NPM tidak terdaftar!");
                conn.rollback();
                return;
            }
            if (txtNama != null) txtNama.setText(anggota.nama);

            // 2) Ambil buku (LIKE)
            Buku buku = getBukuByJudul(conn, judulInput);
            if (buku == null) {
                showAlert(Alert.AlertType.ERROR, "Ditolak", "Judul buku tidak ditemukan!");
                conn.rollback();
                return;
            }

            // 3) Cek masih pinjam & denda
            if (masihPinjam(conn, anggota.id)) {
                showAlert(Alert.AlertType.ERROR, "Ditolak",
                        "Masih ada buku yang belum dikembalikan!");
                conn.rollback();
                return;
            }

            if (punyaDenda(conn, anggota.id)) {
                showAlert(Alert.AlertType.ERROR, "Ditolak",
                        "Masih memiliki denda yang belum dibayar!");
                conn.rollback();
                return;
            }

            // 4) Kurangi stok buku + update status_buku (ketersediaan)
            try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE buku
                SET jumlah_tersedia = jumlah_tersedia - 1,
                    status_buku = IF(jumlah_tersedia - 1 > 0, 'Tersedia', 'Tidak Tersedia')
                WHERE id_buku=? AND jumlah_tersedia > 0
            """)) {
                ps.setInt(1, buku.id);
                if (ps.executeUpdate() == 0) {
                    showAlert(Alert.AlertType.ERROR, "Ditolak", "Stok buku habis!");
                    conn.rollback();
                    return;
                }
            }

            // 5) Insert ke peminjaman (HEADER)
            LocalDate jatuhTempo = tglPinjam.plusDays(7);
            int idPeminjaman;

            String insertPinjam = """
                INSERT INTO peminjaman
                (id_admin, id_anggota, tgl_pinjam, tgl_kembali, status)
                VALUES (?, ?, ?, ?, 'Dipinjam')
            """;

            try (PreparedStatement ps = conn.prepareStatement(insertPinjam, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, CURRENT_ADMIN_ID);
                ps.setInt(2, anggota.id);
                ps.setDate(3, Date.valueOf(tglPinjam));
                ps.setDate(4, Date.valueOf(jatuhTempo));
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("Gagal mengambil id_peminjaman");
                    idPeminjaman = rs.getInt(1);
                }
            }

            // 6) Insert ke detailpinjam (DETAIL)
            String insertDetail = """
                INSERT INTO detailpinjam
                (id_peminjaman, id_buku, jumlah, tgl_pinjam, tgl_kembali)
                VALUES (?, ?, ?, ?, ?)
            """;

            try (PreparedStatement ps = conn.prepareStatement(insertDetail)) {
                ps.setInt(1, idPeminjaman);
                ps.setInt(2, buku.id);
                ps.setInt(3, 1);
                ps.setDate(4, Date.valueOf(tglPinjam));
                ps.setDate(5, Date.valueOf(jatuhTempo));
                ps.executeUpdate();
            }

            conn.commit();

            showAlert(Alert.AlertType.INFORMATION,
                    "Berhasil",
                    "Peminjaman berhasil diajukan.\nBuku: " + buku.judul);

            clearForm();

        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (Exception ignored) {}
            showAlert(Alert.AlertType.ERROR, "Error Database", e.getMessage());
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }

    // ===================== QUERY METHOD =====================

    private Anggota getAnggotaByNpm(Connection c, String npm) throws SQLException {
        String sql = "SELECT id_anggota, nama FROM anggota WHERE npm=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, npm.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Anggota(rs.getInt("id_anggota"), rs.getString("nama"));
            }
        }
        return null;
    }

    private Buku getBukuByJudul(Connection c, String judul) throws SQLException {
        String sql = "SELECT id_buku, judul FROM buku WHERE judul LIKE ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + judul.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Buku(rs.getInt("id_buku"), rs.getString("judul"));
            }
        }
        return null;
    }

    private boolean masihPinjam(Connection c, int idAnggota) throws SQLException {
        String sql = "SELECT COUNT(*) FROM peminjaman WHERE id_anggota=? AND status='Dipinjam'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idAnggota);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    // SAMAKAN dengan PengembalianController: status denda = 'Belum Lunas'
    private boolean punyaDenda(Connection c, int idAnggota) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM denda d
            JOIN peminjaman p ON d.id_peminjaman = p.id_peminjaman
            WHERE p.id_anggota = ? AND d.status_denda = 'Belum Lunas'
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idAnggota);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private void clearForm() {
        if (txtNama != null) txtNama.clear();
        if (txtNpm != null) { txtNpm.clear(); txtNpm.setEditable(false); }
        if (txtJudulBuku != null) { txtJudulBuku.clear(); txtJudulBuku.setEditable(false); }
        if (dpTanggalPinjam != null) dpTanggalPinjam.setValue(LocalDate.now());
    }

    private record Anggota(int id, String nama) {}
    private record Buku(int id, String judul) {}
}