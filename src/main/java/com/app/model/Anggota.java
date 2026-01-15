package com.app.model;

import javafx.beans.property.*;

public class Anggota {

    private final IntegerProperty idAnggota = new SimpleIntegerProperty();
    private final StringProperty nama = new SimpleStringProperty();
    private final StringProperty jurusan = new SimpleStringProperty();
    private final StringProperty npm = new SimpleStringProperty();
    private final StringProperty noHp = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();

    public Anggota(int idAnggota, String nama, String jurusan, String npm, String noHp, String email) {
        this.idAnggota.set(idAnggota);
        this.nama.set(nama);
        this.jurusan.set(jurusan);
        this.npm.set(npm);
        this.noHp.set(noHp);
        this.email.set(email);
    }

    // ===== Getter standar (untuk PropertyValueFactory) =====
    public int getIdAnggota() { return idAnggota.get(); }
    public String getNama() { return nama.get(); }
    public String getJurusan() { return jurusan.get(); }
    public String getNpm() { return npm.get(); }
    public String getNoHp() { return noHp.get(); }
    public String getEmail() { return email.get(); }

    // ===== Alias (opsional, kalau kepakai di kode lama) =====
    public int getId() { return getIdAnggota(); }

    // ===== Property (kalau pakai lambda di TableColumn) =====
    public IntegerProperty idAnggotaProperty() { return idAnggota; }
    public StringProperty namaProperty() { return nama; }
    public StringProperty jurusanProperty() { return jurusan; }
    public StringProperty npmProperty() { return npm; }
    public StringProperty noHpProperty() { return noHp; }
    public StringProperty emailProperty() { return email; }
}
