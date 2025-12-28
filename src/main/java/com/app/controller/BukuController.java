package com.app.controller;

import com.app.config.Database;
import com.app.model.Buku;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BukuController extends BaseController {

    @FXML private VBox sidebarContainer;


    @FXML private TextField txtSearch;

    @FXML private TableView<Buku> tblBuku;
    @FXML private TableColumn<Buku, Integer> colId;
    @FXML private TableColumn<Buku, String> colJudul;
    @FXML private TableColumn<Buku, String> colPengarang;
    @FXML private TableColumn<Buku, String> colKategori;
    @FXML private TableColumn<Buku, Integer> colTahun;
    @FXML private TableColumn<Buku, Integer> colJumlah;

    private final ObservableList<Buku> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        attachSidebar(sidebarContainer, "buku");  
        if (tblBuku != null) {
            tblBuku.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tblBuku.setItems(data);
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("idBuku"));
        colJudul.setCellValueFactory(new PropertyValueFactory<>("judul"));
        colPengarang.setCellValueFactory(new PropertyValueFactory<>("pengarang"));
        colKategori.setCellValueFactory(new PropertyValueFactory<>("kategori"));
        colTahun.setCellValueFactory(new PropertyValueFactory<>("tahunTerbit"));
        colJumlah.setCellValueFactory(new PropertyValueFactory<>("jumlahTersedia"));

        loadBuku("");

        if (txtSearch != null) {
                    // Placeholder
                    txtSearch.setPromptText("Search");
                    txtSearch.clear();

                    // Realtime search 
                    txtSearch.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                        if (isFocused && !searchActivated) {
                            searchActivated = true;

                            txtSearch.textProperty().addListener((o, oldText, newText) -> {
                                loadBuku(newText);
                            });
                        }
                    });
        }
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
    private void handleSearch() {
        loadBuku(txtSearch.getText());
    }

    private void loadBuku(String keyword) {
        data.clear();
        String key = keyword == null ? "" : keyword.trim();

        String sql =
                "SELECT id_buku, judul, pengarang, penerbit, kategori, tahun_terbit, jumlah_tersedia " +
                "FROM buku " +
                "WHERE (? = '' OR judul LIKE ? OR pengarang LIKE ? OR kategori LIKE ?) " +
                "ORDER BY id_buku ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, key);
            ps.setString(2, "%" + key + "%");
            ps.setString(3, "%" + key + "%");
            ps.setString(4, "%" + key + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.add(new Buku(
                            rs.getInt("id_buku"),
                            rs.getString("judul"),
                            rs.getString("pengarang"),
                            rs.getString("penerbit"),
                            rs.getString("kategori"),
                            rs.getInt("tahun_terbit"),
                            rs.getInt("jumlah_tersedia")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,
                    "Database",
                    "Gagal load data buku dari database.");
        }
    }
}