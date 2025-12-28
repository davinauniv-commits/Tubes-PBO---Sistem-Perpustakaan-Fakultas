package com.app.util;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class WindowUtil {
    private WindowUtil() {}

    //=== WINDOW PENUH ===//
    public static void forceFull(Stage stage) {
        if (stage == null) return;

        stage.setResizable(true);

        Platform.runLater(() -> {
            Rectangle2D vb = Screen.getPrimary().getVisualBounds();
            stage.setX(vb.getMinX());
            stage.setY(vb.getMinY());
            stage.setWidth(vb.getWidth());
            stage.setHeight(vb.getHeight());
        });
    }
}
