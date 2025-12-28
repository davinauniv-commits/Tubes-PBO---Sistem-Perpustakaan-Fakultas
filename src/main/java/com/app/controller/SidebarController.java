package com.app.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class SidebarController {

    private BaseController owner;

    @FXML private Button btnDashboard;
    @FXML private Button btnAnggota;
    @FXML private Button btnBuku;
    @FXML private Button btnPeminjaman;
    @FXML private Button btnPengembalian;
    @FXML private Button btnDenda;

    public void setOwner(BaseController owner) {
        this.owner = owner;
    }

    public void setActive(String key) {
        clearActive();
        Button b = switch (key) {
            case "dashboard" -> btnDashboard;
            case "anggota" -> btnAnggota;
            case "buku" -> btnBuku;
            case "peminjaman" -> btnPeminjaman;
            case "pengembalian" -> btnPengembalian;
            case "denda" -> btnDenda;
            default -> null;
        };
        if (b != null) {
            b.setStyle("-fx-background-color:#d9f0ff; -fx-border-color:#2aa8ff; " +
                       "-fx-border-radius:6; -fx-background-radius:6;");
        }
    }

    private void clearActive() {
        if (btnDashboard != null) btnDashboard.setStyle("");
        if (btnAnggota != null) btnAnggota.setStyle("");
        if (btnBuku != null) btnBuku.setStyle("");
        if (btnPeminjaman != null) btnPeminjaman.setStyle("");
        if (btnPengembalian != null) btnPengembalian.setStyle("");
        if (btnDenda != null) btnDenda.setStyle("");
    }

    @FXML private void goDashboard(ActionEvent e)    { if (owner != null) owner.goDashboard(e); }
    @FXML private void goAnggota(ActionEvent e)      { if (owner != null) owner.goAnggota(e); }
    @FXML private void goBuku(ActionEvent e)         { if (owner != null) owner.goBuku(e); }
    @FXML private void goPeminjaman(ActionEvent e)   { if (owner != null) owner.goPeminjaman(e); }
    @FXML private void goPengembalian(ActionEvent e) { if (owner != null) owner.goPengembalian(e); }
    @FXML private void goDenda(ActionEvent e)        { if (owner != null) owner.goDenda(e); }
}