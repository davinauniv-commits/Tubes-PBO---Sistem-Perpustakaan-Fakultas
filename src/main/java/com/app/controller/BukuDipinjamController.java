package com.app.controller;

import com.app.config.Database;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.*;

public class BukuDipinjamController extends BaseController {

    @FXML private VBox sidebarContainer;
    @FXML private Label lblInfo;

    @FXML private TableView<Row> tblDipinjam;
    @FXML private TableColumn<Row, Integer> colNo;
    @FXML private TableColumn<Row, String> colNama;
    @FXML private TableColumn<Row, String> colNpm;
    @FXML private TableColumn<Row, String> colJudul;
    @FXML private TableColumn<Row, Integer> colJumlah;
    @FXML private TableColumn<Row, String> colTglPinjam;

    private final ObservableList<Row> data = FXCollections.observableArrayList();

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    public void initialize() {
        attachSidebar(sidebarContainer, "buku_dipinjam");

        tblDipinjam.setItems(data);
        tblDipinjam.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colNo.setCellValueFactory(new PropertyValueFactory<>("no"));
        colNama.setCellValueFactory(new PropertyValueFactory<>("nama"));
        colNpm.setCellValueFactory(new PropertyValueFactory<>("npm"));
        colJudul.setCellValueFactory(new PropertyValueFactory<>("judul"));
        colJumlah.setCellValueFactory(new PropertyValueFactory<>("jumlah"));
        colTglPinjam.setCellValueFactory(new PropertyValueFactory<>("tglPinjam"));

        loadData();
    }

    private void loadData() {
        data.clear();

        String sql =
            "SELECT a.nama, a.npm, b.judul, dp.jumlah, dp.tgl_pinjam " +
            "FROM peminjaman p " +
            "JOIN anggota a ON a.id_anggota = p.id_anggota " +
            "JOIN detailpinjam dp ON dp.id_peminjaman = p.id_peminjaman " +
            "JOIN buku b ON b.id_buku = dp.id_buku " +
            "WHERE p.status = 'Dipinjam' " +
            "ORDER BY dp.tgl_pinjam DESC";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int no = 1;
            while (rs.next()) {
                data.add(new Row(
                    no++,
                    rs.getString("nama"),
                    rs.getString("npm"),
                    rs.getString("judul"),
                    rs.getInt("jumlah"),
                    rs.getString("tgl_pinjam")
                ));
            }

            lblInfo.setText(
                data.isEmpty()
                    ? "Tidak ada buku yang sedang dipinjam."
                    : "Total buku sedang dipinjam: " + data.size()
            );

        } catch (Exception e) {
            e.printStackTrace();
            lblInfo.setText("Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

    }

    // ===== Row Model =====
    public static class Row {
        private final IntegerProperty no;
        private final StringProperty nama;
        private final StringProperty npm;
        private final StringProperty judul;
        private final IntegerProperty jumlah;
        private final StringProperty tglPinjam;

        public Row(int no, String nama, String npm, String judul, int jumlah, String tglPinjam) {
            this.no = new SimpleIntegerProperty(no);
            this.nama = new SimpleStringProperty(nama);
            this.npm = new SimpleStringProperty(npm);
            this.judul = new SimpleStringProperty(judul);
            this.jumlah = new SimpleIntegerProperty(jumlah);
            this.tglPinjam = new SimpleStringProperty(tglPinjam);
        }

        public int getNo() { return no.get(); }
        public String getNama() { return nama.get(); }
        public String getNpm() { return npm.get(); }
        public String getJudul() { return judul.get(); }
        public int getJumlah() { return jumlah.get(); }
        public String getTglPinjam() { return tglPinjam.get(); }
    }
}