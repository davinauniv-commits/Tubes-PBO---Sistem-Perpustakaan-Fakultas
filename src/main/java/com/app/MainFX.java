package com.app;

import com.app.util.WindowUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainFX.class.getResource("/com/app/login.fxml"));
        stage.setScene(new Scene(loader.load()));
        stage.setTitle("Login - Sistem Perpustakaan");

        stage.show();
        WindowUtil.forceFull(stage); // ✅ full layar sejak awal
    }

    public static void main(String[] args) {
        launch(args);
    }
}
