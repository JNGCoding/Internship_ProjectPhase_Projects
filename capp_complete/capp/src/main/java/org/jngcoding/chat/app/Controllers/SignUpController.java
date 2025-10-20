package org.jngcoding.chat.app.Controllers;

import org.jngcoding.chat.app.ChatServer;
import org.jngcoding.chat.app.Pages;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SignUpController {
    @FXML
    private TextField UsernameInput;

    @FXML
    private TextField PasswordInput;

    @FXML
    private Label ErrorLabel;

    @FXML
    private Rectangle ErrorBox;

    @FXML
    private TextField AccountnameInput;

    private boolean ErrorAnimationRunning = false;

    private void ShowError(String error) {
        if (ErrorAnimationRunning) {
            return;
        }

        ErrorAnimationRunning = true;

        ErrorLabel.setText(error);
        ErrorLabel.toFront();
        ErrorBox.toBack();

        // Fade-in transitions
        FadeTransition fadeInLabel = new FadeTransition(Duration.millis(500), ErrorLabel);
        fadeInLabel.setFromValue(0);
        fadeInLabel.setToValue(1);

        FadeTransition fadeInBox = new FadeTransition(Duration.millis(500), ErrorBox);
        fadeInBox.setFromValue(0);
        fadeInBox.setToValue(0.7);

        // Fade-out transitions (created after delay)
        PauseTransition delay = new PauseTransition(Duration.millis(5000));
        delay.setOnFinished(e -> {
            FadeTransition fadeOutLabel = new FadeTransition(Duration.millis(500), ErrorLabel);
            fadeOutLabel.setFromValue(1);
            fadeOutLabel.setToValue(0);

            FadeTransition fadeOutBox = new FadeTransition(Duration.millis(500), ErrorBox);
            fadeOutBox.setFromValue(0.7);
            fadeOutBox.setToValue(0);

            fadeOutBox.setOnFinished(ev -> ErrorAnimationRunning = false);

            fadeOutLabel.play();
            fadeOutBox.play();
        });

        fadeInLabel.play();
        fadeInBox.play();
        delay.play();
    }

    public void CreateAccount(ActionEvent event) {
        String Accountname = AccountnameInput.getText();
        String Username = UsernameInput.getText();
        String Password = PasswordInput.getText();

        if (Accountname.isEmpty()) {
            ShowError("ERROR: No Input in Accountname.");
            return;
        }

        if (Username.isEmpty()) {
            ShowError("ERROR: No Input in Username.");
            return;
        }

        if (Password.isEmpty()) {
            ShowError("ERROR: No Input in Password.");
            return;
        }

        AccountnameInput.setText("");
        UsernameInput.setText("");
        PasswordInput.setText("");

        String SQLOperation = "INSERT INTO USER(ACCOUNTNAME, USERNAME, PASSWORD) VALUES(\"" + Accountname + "\", \"" + Username + "\", \"" + Password + "\");==UPDATE\n";
        ChatServer.RunSQLOperation(SQLOperation);
    }

    public void ChangeToLoginPage(ActionEvent event) {
        Pages all_pages = new Pages();
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(all_pages.Login);
        stage.setScene(scene);
        stage.show();
    }
}
