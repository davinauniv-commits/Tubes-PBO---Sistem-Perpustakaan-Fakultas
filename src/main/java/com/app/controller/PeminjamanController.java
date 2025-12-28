package com.app.controller;

import com.app.config.Database;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;
import javafx.scene.layout.VBox;

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
        dpTanggalPinjam.setValue(LocalDate.now());

        txtNama.setPromptText("Nama Lengkap");
        txtNama.setEditable(false);

        activateOnClick(txtNpm, "NPM", false);
        activateOnClick(txtJudulBuku, "Judul Buku", false);

        txtNpm.setOnAction(e -> autoIsiNamaDariNpm());
        txtNpm.focusedProperty().addListener((o, oldF, newF) -> {
            if (!newF) autoIsiNamaDariNpm();
        });
    }

    private void activateOnClick(TextField tf, String prompt, boolean editableOnStart) {
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
        String npm = txtNpm.getText() == null ? "" : txtNpm.getText().trim();
        if (npm.isEmpty()) return;

        try (Connection conn = Database.getConnection()) {
            Anggota a = getAnggotaByNpm(conn, npm);
            if (a != null) txtNama.setText(a.nama);
        } catch (Exception ignored) {}
    }

    @FXML
    private void handleAjukanPeminjaman() {

        if (txtNpm.getText().isEmpty() ||
            txtJudulBuku.getText().isEmpty() ||
            dpTanggalPinjam.getValue() == null) {

            showAlert(
                Alert.AlertType.ERROR,
                "Form Tidak Lengkap",
                "NPM, Judul Buku, dan Tanggal Peminjaman wajib diisi!"
            );
            return;
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            Anggota anggota = getAnggotaByNpm(conn, txtNpm.getText());
            if (anggota == null) {
                showAlert(Alert.AlertType.ERROR, "Ditolak", "NPM tidak terdaftar!");
                conn.rollback();
                return;
            }
            txtNama.setText(anggota.nama);

            Buku buku = getBukuByJudul(conn, txtJudulBuku.getText());
            if (buku == null) {
                showAlert(Alert.AlertType.ERROR, "Ditolak", "Judul buku tidak ditemukan!");
                conn.rollback();
                return;
            }
            txtJudulBuku.setText(buku.judul);

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

            LocalDate tglPinjam = dpTanggalPinjam.getValue();
            LocalDate tglKembali = tglPinjam.plusDays(7);

            String insert = """
                INSERT INTO peminjaman
                (id_admin, id_anggota, id_buku, tgl_pinjam, tgl_kembali, status)
                VALUES (?, ?, ?, ?, ?, 'Dipinjam')
            """;

            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setInt(1, CURRENT_ADMIN_ID);
                ps.setInt(2, anggota.id);
                ps.setInt(3, buku.id);
                ps.setDate(4, Date.valueOf(tglPinjam));
                ps.setDate(5, Date.valueOf(tglKembali));
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE buku SET status='Dipinjam' WHERE id_buku=?")) {
                ps.setInt(1, buku.id);
                ps.executeUpdate();
            }

            conn.commit();
            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Peminjaman berhasil diajukan");
            clearForm();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Database", e.getMessage());
        }
    }

    // ===================== QUERY METHOD =====================

    private Anggota getAnggotaByNpm(Connection c, String npm) throws SQLException {
        String sql = "SELECT id_anggota, nama FROM anggota WHERE npm=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, npm.trim());
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return new Anggota(rs.getInt(1), rs.getString(2));
        }
        return null;
    }

    private Buku getBukuByJudul(Connection c, String judul) throws SQLException {
        String sql = "SELECT id_buku, judul FROM buku WHERE judul LIKE ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + judul.trim() + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return new Buku(rs.getInt("id_buku"), rs.getString("judul"));
        }
        return null;
    }

    private boolean masihPinjam(Connection c, int idAnggota) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM peminjaman
            WHERE id_anggota=? AND status='Dipinjam'
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idAnggota);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private boolean punyaDenda(Connection c, int idAnggota) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM denda
            WHERE id_anggota=? AND status='Belum Lunas'
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idAnggota);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    // ===================== UTIL =====================

    private void clearForm() {
        txtNama.clear();
        txtNpm.clear();
        txtJudulBuku.clear();
        dpTanggalPinjam.setValue(LocalDate.now());

        txtNpm.setEditable(false);
        txtJudulBuku.setEditable(false);
    }

    private record Anggota(int id, String nama) {}
    private record Buku(int id, String judul) {}
}
