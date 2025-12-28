package com.app.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.text.NumberFormat;
import java.util.Locale;

public abstract class BaseController {

    // session admin ID
    protected static int currentAdminId = -1;

    public void setCurrentAdminId(int id) {
        currentAdminId = id;
    }

    public int getCurrentAdminId() {
        return currentAdminId;
    }

    // ===== SIDEBAR SETUP =====
    protected void attachSidebar(VBox sidebarContainer, String activeKey) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/app/sidebar.fxml"));
            Parent sidebar = loader.load();

            VBox.setVgrow(sidebar, Priority.ALWAYS);
            ((VBox) sidebar).setFillWidth(true);

            SidebarController sc = loader.getController();
            sc.setOwner(this);
            sc.setActive(activeKey);

            sidebarContainer.getChildren().setAll(sidebar);
            sidebarContainer.setStyle("-fx-background-color:white;");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== NAVIGATION =====
        protected void switchScene(ActionEvent event, String fxmlPath) {
            try {
                var url = getClass().getResource(fxmlPath);
                if (url == null) {
                    showAlert(Alert.AlertType.ERROR, "Navigasi", "FXML tidak ditemukan:\n" + fxmlPath);
                    return;
                }

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

                FXMLLoader loader = new FXMLLoader(url);
                Parent root = loader.load();

                Object ctrl = loader.getController();
                if (ctrl instanceof BaseController bc) {
                    bc.setCurrentAdminId(getCurrentAdminId());
                }

                stage.setScene(new Scene(root));
                stage.show();

                // FULL LAYAR SETIAP PINDAH HALAMAN
                com.app.util.WindowUtil.forceFull(stage);

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Navigasi",
                        "Gagal buka halaman: " + fxmlPath + "\n" + e.getMessage());
            }
        }


    protected void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    protected void safeSet(Label lbl, String text) {
        if (lbl != null) lbl.setText(text == null ? "" : text);
    }

    protected int queryInt(Connection conn, String sql) throws Exception {
        try (var st = conn.createStatement();
             var rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    protected long queryLong(Connection conn, String sql) throws Exception {
        try (var st = conn.createStatement();
             var rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    protected String rupiah(long v) {
        return NumberFormat.getCurrencyInstance(new Locale("id", "ID")).format(v);
    }

    // ===== NAV BUTTON TARGETS =====
    public void goDashboard(ActionEvent e)    { switchScene(e, "/com/app/dashboard.fxml"); }
    public void goAnggota(ActionEvent e)      { switchScene(e, "/com/app/anggota.fxml"); }
    public void goBuku(ActionEvent e)         { switchScene(e, "/com/app/buku.fxml"); }
    public void goPeminjaman(ActionEvent e)   { switchScene(e, "/com/app/peminjaman.fxml"); }
    public void goPengembalian(ActionEvent e) { switchScene(e, "/com/app/pengembalian.fxml"); }
    public void goDenda(ActionEvent e)        { switchScene(e, "/com/app/denda.fxml"); }
}
