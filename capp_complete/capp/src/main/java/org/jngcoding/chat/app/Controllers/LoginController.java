package org.jngcoding.chat.app.Controllers;

import org.jngcoding.chat.app.ChatServer;
import org.jngcoding.chat.app.Pages;
import org.jngcoding.chat.app.Utility;
import javafx.util.Duration;
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

public class LoginController {
    @FXML
    private TextField AccountnameInput;

    @FXML
    private TextField LoginInput;

    @FXML
    private Label ErrorLabel;

    @FXML
    private Rectangle ErrorBox;

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
    
    public void login(ActionEvent e) {
        String Accountname = AccountnameInput.getText();
        String Password = LoginInput.getText();

        if (Accountname.isEmpty()) {
            ShowError("ERROR: No Input in Accountname.");
            return;
        }

        if (Password.isEmpty()) {
            ShowError("ERROR: No Input in Password.");
            return;
        }

        AccountnameInput.setText("");
        LoginInput.setText("");

        String SQLOperation = "SELECT * FROM USER WHERE ACCOUNTNAME = \"" + Accountname + "\" AND PASSWORD = \"" + Password + "\";==QUERY\n";
        String[] ids = ChatServer.GenerateSQLOperation(SQLOperation);

        if (ids.length > 0) {
            ChatServer.Client_AccountName = Utility.getValue(ids[0], "ACCOUNTNAME");
            ChatServer.Client_UserName = Utility.getValue(ids[0], "USERNAME");

            ChangeToChatPage(e);
            ChatServer.onChatPage = true;
        } else {
            ShowError("ERROR: Account not found.");
        }
    }

    public void ChangeToSignUpPage(ActionEvent event) {
        Pages all_pages = new Pages();
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(all_pages.SignUp);
        stage.setScene(scene);
        stage.show();
    }

    public void ChangeToChatPage(ActionEvent event) {
        Pages all_pages = new Pages();
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(all_pages.Chat);
        stage.setScene(scene);
        stage.show();
    }
}
