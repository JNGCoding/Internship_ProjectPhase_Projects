package org.jngcoding.chat.app;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class Pages {
    public final Parent Login;
    public final Parent SignUp;
    public final Parent Chat;
    public final Parent GroupChat;

    public Pages() {
        Parent loginTemp = null;
        Parent signupTemp = null;
        Parent chatTemp = null;
        Parent groupChatTemp = null;
        try {
            loginTemp = FXMLLoader.load(getClass().getResource("Login.fxml"));
            signupTemp = FXMLLoader.load(getClass().getResource("SignUp.fxml"));      
            chatTemp = FXMLLoader.load(getClass().getResource("Chat.fxml"));
            groupChatTemp = FXMLLoader.load(getClass().getResource("Groupchat.fxml"));
        } catch (IOException e) {
            System.out.println("Failed to load Pages.");
        }
        Login = loginTemp;
        SignUp = signupTemp;
        Chat = chatTemp;
        GroupChat = groupChatTemp;
    }
}
