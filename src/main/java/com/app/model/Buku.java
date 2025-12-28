package com.app.model;

import javafx.beans.property.*;

public class Buku {
    private final IntegerProperty idBuku = new SimpleIntegerProperty();
    private final StringProperty judul = new SimpleStringProperty();
    private final StringProperty pengarang = new SimpleStringProperty();
    private final StringProperty penerbit = new SimpleStringProperty();
    private final StringProperty kategori = new SimpleStringProperty();
    private final IntegerProperty tahunTerbit = new SimpleIntegerProperty();
    private final IntegerProperty jumlahTersedia = new SimpleIntegerProperty();

    // constructor sesuai error kamu: (int, String, String, String, String, int, int)
    public Buku(int idBuku, String judul, String pengarang, String penerbit, String kategori, int tahunTerbit, int jumlahTersedia) {
        this.idBuku.set(idBuku);
        this.judul.set(judul);
        this.pengarang.set(pengarang);
        this.penerbit.set(penerbit);
        this.kategori.set(kategori);
        this.tahunTerbit.set(tahunTerbit);
        this.jumlahTersedia.set(jumlahTersedia);
    }

    // ===== Getter standar (buat PropertyValueFactory) =====
    public int getIdBuku() { return idBuku.get(); }
    public String getJudul() { return judul.get(); }
    public String getPengarang() { return pengarang.get(); }
    public String getPenerbit() { return penerbit.get(); }
    public String getKategori() { return kategori.get(); }
    public int getTahunTerbit() { return tahunTerbit.get(); }
    public int getJumlahTersedia() { return jumlahTersedia.get(); }

    // ===== Alias biar kode lama kamu gak error =====
    public int getId() { return getIdBuku(); }
    public int getJumlah() { return getJumlahTersedia(); }

    // ===== Property buat TableColumn lambda =====
    public IntegerProperty idBukuProperty() { return idBuku; }
    public StringProperty judulProperty() { return judul; }
    public StringProperty pengarangProperty() { return pengarang; }
    public StringProperty penerbitProperty() { return penerbit; }
    public StringProperty kategoriProperty() { return kategori; }
    public IntegerProperty tahunTerbitProperty() { return tahunTerbit; }
    public IntegerProperty jumlahTersediaProperty() { return jumlahTersedia; }
}
