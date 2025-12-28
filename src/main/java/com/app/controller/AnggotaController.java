package com.app.controller;

import com.app.config.Database;
import com.app.model.Anggota;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class AnggotaController extends BaseController {

    @FXML private VBox sidebarContainer;
    @FXML private TextField txtSearch;

    @FXML private TableView<Anggota> tblAnggota;
    @FXML private TableColumn<Anggota, Integer> colId;
    @FXML private TableColumn<Anggota, String> colNama;
    @FXML private TableColumn<Anggota, String> colJurusan;
    @FXML private TableColumn<Anggota, String> colNpm;
    @FXML private TableColumn<Anggota, String> colNoHp;
    @FXML private TableColumn<Anggota, String> colEmail;

    private final ObservableList<Anggota> data = FXCollections.observableArrayList();
    private boolean searchActivated = false;

    @FXML
    public void initialize() {
        attachSidebar(sidebarContainer, "anggota");

        if (tblAnggota != null) {
            tblAnggota.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tblAnggota.setItems(data);
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("idAnggota"));
        colNama.setCellValueFactory(new PropertyValueFactory<>("nama"));
        colJurusan.setCellValueFactory(new PropertyValueFactory<>("jurusan"));
        colNpm.setCellValueFactory(new PropertyValueFactory<>("npm"));
        colNoHp.setCellValueFactory(new PropertyValueFactory<>("noHp"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        loadAnggota("");

        if (txtSearch != null) {
            txtSearch.setPromptText("Search");
            txtSearch.clear();

            txtSearch.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused && !searchActivated) {
                    searchActivated = true;
                    txtSearch.textProperty().addListener((o, oldText, newText) -> loadAnggota(newText));
                }
            });
        }
    }

    @FXML
    private void handleSearch() {
        loadAnggota(txtSearch.getText());
    }

    private void loadAnggota(String keyword) {
        data.clear();
        String key = keyword == null ? "" : keyword.trim();

        String sql =
                "SELECT id_anggota, nama, jurusan, npm, no_hp, email " +
                "FROM anggota " +
                "WHERE (? = '' " +
                "   OR nama LIKE ? " +
                "   OR jurusan LIKE ? " +
                "   OR npm LIKE ? " +
                "   OR no_hp LIKE ? " +
                "   OR email LIKE ?) " +
                "ORDER BY id_anggota ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, key);
            ps.setString(2, "%" + key + "%");
            ps.setString(3, "%" + key + "%");
            ps.setString(4, "%" + key + "%");
            ps.setString(5, "%" + key + "%");
            ps.setString(6, "%" + key + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Anggota a = new Anggota(
                            rs.getInt("id_anggota"),
                            rs.getString("nama"),
                            rs.getString("jurusan"),
                            rs.getString("npm"),
                            rs.getString("no_hp"),
                            rs.getString("email")
                    );
                    data.add(a);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Gagal load data anggota dari database.").showAndWait();
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
}
