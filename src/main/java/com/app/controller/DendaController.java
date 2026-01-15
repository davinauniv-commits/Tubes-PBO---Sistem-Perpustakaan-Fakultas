package com.app.controller;

import com.app.config.Database;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Date;
import java.time.LocalDate;

public class DendaController extends BaseController {

    @FXML private VBox sidebarContainer;

    @FXML private TextField txtNama;
    @FXML private TextField txtNpm;

    @FXML private DatePicker dpTglPinjam;

    // OUTPUT: status + tabel
    @FXML private Label lblStatus;

    @FXML private TableView<RowHasil> tblHasil;
    @FXML private TableColumn<RowHasil, String> colKey;
    @FXML private TableColumn<RowHasil, String> colValue;

    @FXML private Button btnTandaiBayar;

    private final ObservableList<RowHasil> hasilItems = FXCollections.observableArrayList();
    private Integer currentIdDenda = null;

    @FXML
    public void initialize() {
        attachSidebar(sidebarContainer, "denda");

        if (btnTandaiBayar != null) btnTandaiBayar.setDisable(true);

        // INPUT fields (boleh diisi)
        if (txtNama != null) txtNama.setEditable(true);
        if (txtNpm != null) txtNpm.setEditable(true);
        if (dpTglPinjam != null) dpTglPinjam.setDisable(false);

        // Setup tabel hasil
        if (colKey != null) {
            colKey.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey()));
        }
        if (colValue != null) {
            colValue.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
        }
        if (tblHasil != null) {
            tblHasil.setItems(hasilItems);
            tblHasil.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }

        if (lblStatus != null) lblStatus.setText("");
        hasilItems.clear();
    }

    @FXML
    private void handleCekDenda() {
        String npmInput = safe(txtNpm.getText());
        String namaInput = safe(txtNama.getText());
        LocalDate tglInput = (dpTglPinjam != null) ? dpTglPinjam.getValue() : null;

        if (npmInput.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Minimal isi NPM untuk cek denda.");
            return;
        }

        if (tglInput == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Tanggal peminjaman wajib dipilih.");
            return;
        }

        String sql =
                "SELECT d.id_denda, d.jumlah_denda, d.status_denda, " +
                "       p.tgl_pinjam, " +
                "       a.nama, a.npm " +
                "FROM denda d " +
                "JOIN peminjaman p ON d.id_peminjaman = p.id_peminjaman " +
                "JOIN anggota a ON p.id_anggota = a.id_anggota " +
                "WHERE a.npm = ? " +
                "  AND p.tgl_pinjam = ? " +
                "  AND d.status_denda = 'Belum Dibayar' " +
                "ORDER BY p.tgl_pinjam DESC " +
                "LIMIT 1";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, npmInput);
            ps.setDate(2, Date.valueOf(tglInput));

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    clearResult("Tidak ada denda BELUM dibayar untuk NPM & tanggal tersebut.");
                    return;
                }

                String namaDB = rs.getString("nama");
                String npmDB  = rs.getString("npm");

                // user mengisi nama, validasi harus sama dengan yang ada di DB 
                if (!namaInput.isBlank() && (namaDB == null || !namaDB.equalsIgnoreCase(namaInput))) {
                    clearResult("Nama tidak cocok dengan NPM di database.");
                    showAlert(Alert.AlertType.WARNING, "Validasi",
                            "Nama tidak cocok dengan NPM di database.\n" +
                            "Nama DB: " + (namaDB != null ? namaDB : "-"));
                    return;
                }

                currentIdDenda = rs.getInt("id_denda");

                Date tglSql   = rs.getDate("tgl_pinjam");
                long jumlah   = rs.getLong("jumlah_denda");
                String status = rs.getString("status_denda");

                // isi ulang input 
                if (txtNama != null) txtNama.setText(namaDB != null ? namaDB : "");
                if (txtNpm != null) txtNpm.setText(npmDB != null ? npmDB : npmInput);
                if (dpTglPinjam != null) dpTglPinjam.setValue(tglSql != null ? tglSql.toLocalDate() : tglInput);

                // isi tabel hasil
                hasilItems.clear();
                hasilItems.add(new RowHasil("Nama", namaDB != null ? namaDB : "-"));
                hasilItems.add(new RowHasil("NPM", npmDB != null ? npmDB : "-"));
                hasilItems.add(new RowHasil("Tgl Pinjam", tglSql != null ? tglSql.toString() : tglInput.toString()));
                hasilItems.add(new RowHasil("Denda", rupiah(jumlah)));
                hasilItems.add(new RowHasil("Status", status != null ? status : "-"));

                if (lblStatus != null) lblStatus.setText("✅ Data ditemukan");
                if (btnTandaiBayar != null) btnTandaiBayar.setDisable(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Gagal cek denda.\nPastikan tabel/kolom sesuai.");
        }
    }

    @FXML
    private void handleTandaiSudahBayar() {
        if (currentIdDenda == null) {
            showAlert(Alert.AlertType.WARNING, "Info", "Silakan cek denda dulu.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Tandai denda ini sebagai SUDAH dibayar?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Konfirmasi");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        String sql = "UPDATE denda SET status_denda='Sudah Dibayar', jumlah_denda=0 WHERE id_denda=?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, currentIdDenda);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Berhasil", "✅ Denda ditandai SUDAH dibayar.");

                currentIdDenda = null;
                if (btnTandaiBayar != null) btnTandaiBayar.setDisable(true);

                // update output
                if (lblStatus != null) lblStatus.setText("✅ Status denda sudah diupdate menjadi: Tidak Ada");
                hasilItems.clear();

            } else {
                showAlert(Alert.AlertType.WARNING, "Info", "Tidak ada data terupdate.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Gagal update status denda.");
        }
    }

    private void clearResult(String message) {
        currentIdDenda = null;
        if (lblStatus != null) lblStatus.setText("⚠ " + message);
        if (btnTandaiBayar != null) btnTandaiBayar.setDisable(true);
        if (dpTglPinjam != null) dpTglPinjam.setValue(null);
        hasilItems.clear();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // Model baris untuk tabel hasil
    public static class RowHasil {
        private final SimpleStringProperty key;
        private final SimpleStringProperty value;

        public RowHasil(String key, String value) {
            this.key = new SimpleStringProperty(key);
            this.value = new SimpleStringProperty(value);
        }

        public String getKey() { return key.get(); }
        public String getValue() { return value.get(); }
    }
}
