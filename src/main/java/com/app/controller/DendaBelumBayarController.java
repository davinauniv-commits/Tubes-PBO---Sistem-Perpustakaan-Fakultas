package com.app.controller;

import com.app.config.Database;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DendaBelumBayarController extends BaseController {
    @FXML private VBox sidebarContainer;

    @FXML private Label lblInfo;

    @FXML private TableView<Row> tblDenda;
    @FXML private TableColumn<Row, Integer> colNo;
    @FXML private TableColumn<Row, String> colNama;
    @FXML private TableColumn<Row, String> colNpm;
    @FXML private TableColumn<Row, Integer> colDenda;
    @FXML private TableColumn<Row, String> colTotal;

    private final ObservableList<Row> data = FXCollections.observableArrayList();
    private int currentAdminId = 0;

    private long grandTotalBelumBayar = 0;

    @FXML
    public void initialize() {
        attachSidebar(sidebarContainer, "denda_belum_bayar");

        if (tblDenda != null) {
            tblDenda.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tblDenda.setItems(data);
        }

        if (colNo != null) colNo.setCellValueFactory(new PropertyValueFactory<>("no"));
        if (colNama != null) colNama.setCellValueFactory(new PropertyValueFactory<>("nama"));
        if (colNpm != null) colNpm.setCellValueFactory(new PropertyValueFactory<>("npm"));
        if (colDenda != null) colDenda.setCellValueFactory(new PropertyValueFactory<>("jumlahDenda"));
        if (colTotal != null) colTotal.setCellValueFactory(new PropertyValueFactory<>("totalDenda"));

        safeSet(lblInfo, "");
        loadData();
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

    public void setCurrentAdminId(int id) {
        this.currentAdminId = id;
    }

    public long getGrandTotalBelumBayar() {
        return grandTotalBelumBayar;
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void loadData() {
        data.clear();
        grandTotalBelumBayar = 0;

        // belum dibayar + jumlah_denda > 0
        String sql =
                "SELECT a.id_anggota, a.nama, a.npm, " +
                "       COUNT(d.id_denda) AS jumlah_denda, " +
                "       COALESCE(SUM(d.jumlah_denda),0) AS total_denda " +
                "FROM denda d " +
                "JOIN peminjaman p ON d.id_peminjaman = p.id_peminjaman " +
                "JOIN anggota a ON p.id_anggota = a.id_anggota " +
                "WHERE d.status_denda = 'Belum Dibayar' " +
                "  AND d.jumlah_denda > 0 " +
                "GROUP BY a.id_anggota, a.nama, a.npm " +
                "ORDER BY total_denda DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int no = 1;
            while (rs.next()) {
                String nama = rs.getString("nama");
                String npm = rs.getString("npm");
                int jumlahKasus = rs.getInt("jumlah_denda");
                long total = rs.getLong("total_denda");

                grandTotalBelumBayar += total;
                data.add(new Row(no++, nama, npm, jumlahKasus, rupiah(total)));
            }

            if (data.isEmpty()) {
                safeSet(lblInfo, "Tidak ada denda yang berstatus 'Belum Dibayar'.");
            } else {
                safeSet(lblInfo,
                        "Total anggota memiliki denda: " + data.size() +
                        " | Total denda belum bayar: " + rupiah(grandTotalBelumBayar));
            }

        } catch (Exception e) {
            e.printStackTrace();
            safeSet(lblInfo, "Gagal load data denda. Cek relasi tabel denda → peminjaman → anggota.");
        }
    }

    // ===== Row model =====
    public static class Row {
        private final SimpleIntegerProperty no;
        private final SimpleStringProperty nama;
        private final SimpleStringProperty npm;
        private final SimpleIntegerProperty jumlahDenda;
        private final SimpleStringProperty totalDenda;

        public Row(int no, String nama, String npm, int jumlahDenda, String totalDenda) {
            this.no = new SimpleIntegerProperty(no);
            this.nama = new SimpleStringProperty(nama);
            this.npm = new SimpleStringProperty(npm);
            this.jumlahDenda = new SimpleIntegerProperty(jumlahDenda);
            this.totalDenda = new SimpleStringProperty(totalDenda);
        }

        public int getNo() { return no.get(); }
        public String getNama() { return nama.get(); }
        public String getNpm() { return npm.get(); }
        public int getJumlahDenda() { return jumlahDenda.get(); }
        public String getTotalDenda() { return totalDenda.get(); }
    }
}