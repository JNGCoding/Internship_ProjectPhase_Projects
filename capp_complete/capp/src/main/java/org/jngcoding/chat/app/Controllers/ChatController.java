package org.jngcoding.chat.app.Controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.jngcoding.chat.app.ChatServer;
import org.jngcoding.chat.app.Pages;
import org.jngcoding.chat.app.Utility;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

@SuppressWarnings({"unused"})
public class ChatController {
    @FXML
    private ListView<String> AvailableContacts;

    @FXML
    private HBox TopHBox;

    @FXML
    private Button AddContactButton;

    @FXML
    private ScrollPane MessageScrollPane;

    @FXML
    private VBox MessageSPVBox;

    @FXML
    private HBox MessageSendHBox;

    @FXML
    private TextField MessageTypeField;

    @FXML
    private Button MessageSendButton;

    private Timeline chatShower;
    private Timeline MessageboxShower;
    private final List<Tooltip> hover_texts = new ArrayList<>();
    private String[] ChatBuffer = {""};

    private String CurrentOpenedUserChat = null;
    private String CurrentOpenedTableName = null;
    private final List<String> CurrentMessages = new ArrayList<>();

    private boolean RunningTimeLines = false;

    private void openChatWindow(String name, String account_name) {
        CurrentOpenedUserChat = account_name;
        String[] ids = ChatServer.GenerateSQLOperation(String.format("SELECT * FROM CHATS WHERE (CHAT->\"$.USER1\" = \"%s\" AND CHAT->\"$.USER2\" = \"%s\") OR (CHAT->\"$.USER2\" = \"%s\" AND CHAT->\"$.USER1\" = \"%s\");==QUERY\n", ChatServer.Client_AccountName, account_name, ChatServer.Client_AccountName, account_name));
        CurrentOpenedTableName = Utility.getValue(ids[0], "TABLE_NAME");
    }

    private void CreateMessageTable(String Acc1, String Acc2) {
        ChatServer.RunSQLOperation(String.format("CREATE TABLE chat_%s(ID INT AUTO_INCREMENT PRIMARY KEY, USER VARCHAR(50) NOT NULL, MESSAGE VARCHAR(250) NOT NULL, AT_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP);==UPDATE\n", Acc1 + Acc2));
    }

    public void UploadMessage(ActionEvent event) {
        String message = MessageTypeField.getText();
        if (!message.equals("")) {
            ChatServer.RunSQLOperation(String.format("INSERT INTO chat_%s(USER, MESSAGE) VALUES(\"%s\", \"%s\");==UPDATE\n", CurrentOpenedTableName, ChatServer.Client_AccountName, message));
            MessageTypeField.setText("");
        }
    }

    @FXML
    public void initialize() {
        chatShower = new Timeline(
            new KeyFrame(Duration.millis(500), event -> {
                MessageTypeField.setVisible(CurrentOpenedUserChat != null);
                MessageSendButton.setVisible(CurrentOpenedUserChat != null);

                try {
                    ChatServer.Chats = ChatServer.GenerateSQLOperation(String.format("SELECT * FROM CHATS WHERE JSON_EXTRACT(CHAT, \"$.USER1\") = \"%s\" OR JSON_EXTRACT(CHAT, \"$.USER2\") = \"%s\";==QUERY\n", ChatServer.Client_AccountName, ChatServer.Client_AccountName));
                    if (!Arrays.equals(ChatBuffer, ChatServer.Chats)) {
                        ChatBuffer = ChatServer.Chats;
                        hover_texts.clear();
                    } else {
                        return;
                    }

                    ObservableList<String> chats = FXCollections.observableArrayList(ChatServer.Chats);
                    AvailableContacts.setItems(chats);

                    AvailableContacts.setCellFactory(listView -> new ListCell<>() {
                        private final Button openButton = new Button();
                        private final HBox layout = new HBox(10);

                        {
                            layout.setAlignment(Pos.CENTER);
                            layout.getChildren().add(openButton);
                        }

                        @Override
                        protected void updateItem(String item, boolean empty) {
                            if (empty || item == null) {
                                setText(null);
                                setGraphic(null);
                                return;
                            } else {
                                setText(""); // shows username
                                setGraphic(layout); // shows button next to it
                            }

                            super.updateItem(item, empty);

                            int start = item.indexOf("CHAT:{") + 5;
                            int end = item.lastIndexOf("}");
                            String jsonPart = item.substring(start, end + 1);

                            String[] pairs = jsonPart.split(",");
                            String user1 = pairs[0].split(":")[1].replace("\"", "").trim();
                            String user2 = pairs[1].split(":")[1].replace("\"", "").trim();
                            user2 = user2.substring(0, user2.length() - 1);

                            String account_name = !user1.equals(ChatServer.Client_AccountName) ? user1 : user2;

                            Tooltip tt = new Tooltip(account_name);
                            Tooltip.install(openButton, tt);
                            hover_texts.add(tt);

                            String match = ChatServer.GenerateSQLOperation(String.format("SELECT * FROM USER WHERE ACCOUNTNAME = \"%s\";==QUERY\n", account_name))[0];
                            String name = Utility.getValue(match, "USERNAME");

                            openButton.setText(name);
                            openButton.setOnAction(eh -> {
                                openChatWindow(name, account_name);
                            });
                        }
                    });
                } catch (Exception ignored) {}
            })
        );

        MessageboxShower = new Timeline(new KeyFrame(
            Duration.millis(100), event -> {
                if (CurrentOpenedTableName != null) {
                    CurrentMessages.clear();
                    String[] messages = ChatServer.GenerateSQLOperation(String.format("SELECT USER, MESSAGE FROM chat_%s ORDER BY ID ASC LIMIT 50;==QUERY\n", CurrentOpenedTableName));
                    CurrentMessages.addAll(Arrays.asList(messages));
                }
                
                if (!CurrentMessages.isEmpty()) {
                    MessageSPVBox.getChildren().clear();

                    for (String message : CurrentMessages) {
                        String account_name = Utility.getValue(message, "USER");
                        String message_content = Utility.getValue(message, "MESSAGE");

                        HBox MessageHBox = new HBox();
                        Label MessageLabel = new Label(message_content);

                        MessageLabel.setWrapText(true);
                        MessageLabel.setMaxWidth(150);

                        if (account_name.equals(ChatServer.Client_AccountName)) {
                            MessageHBox.setAlignment(Pos.CENTER_RIGHT);
                        } else {
                            MessageHBox.setAlignment(Pos.CENTER_LEFT);
                        }

                        MessageLabel.setStyle("-fx-background-color: #cfe9ff; -fx-padding: 10; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 14px;");
                        MessageHBox.getChildren().add(MessageLabel);

                        MessageSPVBox.getChildren().add(MessageHBox);
                    }
                } else {
                    MessageSPVBox.getChildren().clear();
                }
            }
        ));

        chatShower.setCycleCount(Animation.INDEFINITE);
        MessageboxShower.setCycleCount(Animation.INDEFINITE);

        Thread rThread = new Thread(() -> {
            while (true) {
                if (ChatServer.onChatPage && !RunningTimeLines) {
                    chatShower.play();
                    MessageboxShower.play();
                    RunningTimeLines = true;
                } else if (!ChatServer.onChatPage && RunningTimeLines) {
                    chatShower.stop();
                    MessageboxShower.stop();
                    RunningTimeLines = false;
                }

                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
            }
        });

        rThread.setDaemon(true);
        rThread.start();
    }

    public void AddContact(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Contact Details");
        dialog.setHeaderText("Enter the Account Name");
        dialog.setContentText("Account Name");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(action -> {
            if (action.equals("")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("ERROR");
                alert.setHeaderText("Failed to add contact!");
                alert.setContentText("No Input was given for Contact Account Name.");
                alert.showAndWait();
            } else {
                String SQLOperation = "SELECT * FROM USER WHERE ACCOUNTNAME = \"" + action + "\";==QUERY\n";
                String[] ids = ChatServer.GenerateSQLOperation(SQLOperation);
                if (ids.length > 0) {
                    SQLOperation = String.format("INSERT INTO CHATS(CHAT, TABLE_NAME) VALUES('{\"USER1\": \"%s\", \"USER2\": \"%s\"}', \"%s\");==UPDATE\n", ChatServer.Client_AccountName, action, ChatServer.Client_AccountName + action);
                    ChatServer.RunSQLOperation(SQLOperation);

                    //~ CREATING TABLE
                    CreateMessageTable(ChatServer.Client_AccountName, action);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("ERROR");
                    alert.setHeaderText("Failed to add contact!");
                    alert.setContentText("Contact was not found.");
                    alert.showAndWait();
                }
            }
        });
    }

    public void ChangeToGroupChatPage(ActionEvent event) {
        ChatServer.onChatPage = false;
        ChatServer.onGroupChatPage = true;
        
        Pages all_pages = new Pages();
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(all_pages.GroupChat);
        stage.setScene(scene);
        stage.show();
    }
}
