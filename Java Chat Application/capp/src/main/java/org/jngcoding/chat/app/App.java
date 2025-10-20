package org.jngcoding.chat.app;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@SuppressWarnings("CallToPrintStackTrace")
public class App extends Application implements AppConstants {
    private final Pages AllPages = new Pages();
    private Thread GCThread;

    private void configurePrimaryStage(Stage ps) {
        ps.setResizable(APPLICATION_RESIZABLE_CONDITION);
        ps.setTitle(APPLICATION_NAME);
        ps.getIcons().add(new Image(getClass().getResourceAsStream(APPLICATION_LOGO_NAME)));
        ps.setFullScreen(APPLICATION_FULLSCREEN_CONDITION);
    }

    private void ShowConnectionSplashScreen(Stage afterStage) {
        ProgressIndicator Spinner = new ProgressIndicator();
        Label StatusLabel = new Label("Connecting to server...");
        StatusLabel.setFont(Font.font(18));

        ImageView ErrorIcon = new ImageView(new Image(getClass().getResourceAsStream("Error.png")));
        ErrorIcon.setFitWidth(64);
        ErrorIcon.setPreserveRatio(true);
        ErrorIcon.setVisible(false);

        VBox CenterPane = new VBox(10, Spinner, StatusLabel);
        CenterPane.setAlignment(Pos.CENTER);
        CenterPane.setPadding(new Insets(20, 20, 40, 20));

        Button CloseButton = new Button("✕");
        CloseButton.setVisible(false);
        CloseButton.setOnAction(e -> Platform.exit());

        HBox TopBar = new HBox(CloseButton);
        TopBar.setAlignment(Pos.TOP_RIGHT);
        TopBar.setPadding(new Insets(10, 10, 0, 0)); // top, right, bottom, left

        BorderPane RootPane = new BorderPane();
        RootPane.setCenter(CenterPane);
        BorderPane.setAlignment(CloseButton, Pos.TOP_RIGHT);
        RootPane.setTop(TopBar);
        RootPane.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: lightgray; -fx-border-width: 1;");

        Platform.runLater(() -> {
            Stage loadingStage = new Stage();
            Scene scene = new Scene(RootPane, 320, 240);
            scene.setFill(Color.TRANSPARENT);
            loadingStage.setScene(scene);
            loadingStage.initStyle(StageStyle.TRANSPARENT);
            loadingStage.show();

            CompletableFuture<Boolean> FutureConnection = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(3000);
                    return ChatServer.ConnectToServer();
                } catch (IOException | InterruptedException exception) {
                    exception.printStackTrace();
                    return false;
                } finally {
                    System.out.println("CONNECTION TO SERVER FINISHED.");
                }
            });

            FutureConnection.thenAccept(result -> Platform.runLater(() -> {
                if (result) {
                    loadingStage.close();
                    afterStage.show();
                } else {
                    CenterPane.getChildren().remove(Spinner);
                    CenterPane.getChildren().add(0, ErrorIcon);

                    StatusLabel.setText("Connection to server failed...");
                    ErrorIcon.setVisible(true);
                    CloseButton.setVisible(true);
                }
            }));
        });
    }

    @Override
    public void init() {
        GCThread = new Thread(() -> {
            long time = System.currentTimeMillis();
            while (true) {
                if (System.currentTimeMillis() - time > 60000) {
                    System.gc();
                    time = System.currentTimeMillis();
                }

                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000));
            }
        });

        GCThread.setDaemon(true);
        GCThread.start();
    }

    @Override
    public void start(Stage primaryStage) {
        Scene firstScene = new Scene(AllPages.Login);
        configurePrimaryStage(primaryStage);
        primaryStage.setScene(firstScene);
        ShowConnectionSplashScreen(primaryStage);
    }

    @Override
    public void stop() {
        // TODO: AS THE APPLICATION GOES COMPLEX, CHANGE THIS METHOD ACCORDINGLY
        if (ChatServer.Server != null) {
            try {
                ChatServer.Server.close();
                ChatServer.ServerInput.close();
                ChatServer.ServerOutput.close();
            } catch (IOException ignored) {}
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}