package org.jngcoding.chat.app.Controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import org.jngcoding.chat.app.ChatServer;
import org.jngcoding.chat.app.Pages;
import org.jngcoding.chat.app.Utility;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.Observable;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GroupchatController {
    @FXML
    public ListView<String> AvailableGroupChats;

    @FXML
    public VBox MessageSPVBox;

    @FXML
    public ScrollPane MessageScrollPane;

    @FXML
    public Button MessageSendButton;

    @FXML
    public TextField MessageTypeField;

    private String CurrentOpenedUserChat = null;
    private String CurrentOpenedTableName = null;
    private Timeline chatShower;
    private Timeline MessageboxShower;
    private volatile boolean RunningTimelines = false;
    private String[] ChatBuffer = {""};
    private final List<String> CurrentMessages = new ArrayList<>();

    private void openChatWindow(String tablename) {
        CurrentOpenedUserChat = tablename;
        CurrentOpenedTableName = tablename;
    }

    @FXML
    public void initialize() {
        chatShower = new Timeline(new KeyFrame(
            Duration.millis(500), event -> {
                try {
                    ChatServer.GroupChats = ChatServer.GenerateSQLOperation(String.format("SELECT CHAT, TABLE_NAME FROM GROUPCHATS WHERE JSON_CONTAINS(CHAT, '[\"%s\"]')==QUERY\n", ChatServer.Client_AccountName));
                    if (!Arrays.equals(ChatBuffer, ChatServer.GroupChats)) {
                        ChatBuffer = ChatServer.GroupChats;
                    } else {
                        return;
                    }

                    ObservableList<String> chats = FXCollections.observableArrayList(ChatBuffer);
                    AvailableGroupChats.setItems(chats);

                    AvailableGroupChats.setCellFactory(listCell -> new ListCell<>() {
                        private final Button openButton = new Button();
                        private final HBox layout = new HBox(10);

                        {
                            layout.setAlignment(Pos.CENTER);
                            layout.getChildren().add(openButton);
                        }

                        @Override
                        public void updateItem(String item, boolean empty) {
                            if (empty || item == null) {
                                setText(null);
                                setGraphic(null);
                                return;
                            } else {
                                setText("");
                                setGraphic(layout);
                            }

                            super.updateItem(item, empty);

                            String table_name = Utility.getValue(item, "TABLE_NAME");
                            openButton.setText(table_name);
                            openButton.setOnAction(eh -> {
                                openChatWindow(table_name);
                            });
                        }
                    });
                } catch (Exception ignored) {}
            }
        ));

        MessageboxShower = new Timeline(new KeyFrame(
            Duration.millis(100), event -> {
                MessageTypeField.setVisible(CurrentOpenedUserChat != null);
                MessageSendButton.setVisible(CurrentOpenedUserChat != null);

                // TODO: COMPLETE THE MESSAGE EXTRACTION FKIN PROTOCOL TOMORROW.
                // TODO: FUCKIN CREATE THE PROJECT REPORT OF THIS FUCKIN APPLICATION.
                if (CurrentOpenedTableName != null) {
                    CurrentMessages.clear();
                    String[] messages = ChatServer.GenerateSQLOperation(String.format("SELECT USER, MESSAGE FROM %s ORDER BY ID ASC LIMIT 50;==QUERY\n", CurrentOpenedTableName));
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
                            MessageLabel.setText(account_name + ": " + message_content);
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
                if (ChatServer.onGroupChatPage && !RunningTimelines) {
                    RunningTimelines = true;

                    chatShower.play();
                    MessageboxShower.play();
                } else if (!ChatServer.onGroupChatPage && RunningTimelines) {
                    RunningTimelines = false;

                    chatShower.stop();
                    MessageboxShower.stop();
                }

                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
            }
        });

        rThread.setDaemon(true);
        rThread.start();
    }

    public void CreateGroupChat(ActionEvent event) {
        final AtomicBoolean flag = new AtomicBoolean(true);

        TextInputDialog gc_name_input = new TextInputDialog();
        gc_name_input.setTitle("GroupChat Creation");
        gc_name_input.setHeaderText("Input GroupChat Name");
        gc_name_input.setContentText("Group Chat");

        Optional<String> gc_name = gc_name_input.showAndWait();
        gc_name.ifPresent(action -> {
            Runnable showInvalidNameError = () -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("ERROR");
                alert.setHeaderText("Failed to create gc");
                alert.setContentText("Invalid name given for gc.");
                alert.showAndWait();
                flag.set(false);
            };

            if (action.trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("ERROR");
                alert.setHeaderText("Failed to create gc");
                alert.setContentText("No Input was given for GC Name.");
                alert.showAndWait();
                flag.set(false);
            }

            switch (action.toLowerCase().trim()) {
                case "groupchats": {
                    showInvalidNameError.run();
                    break;
                }

                case "chats": {
                    showInvalidNameError.run();
                    break;
                }

                case "users": {
                    showInvalidNameError.run();
                    break;
                }
            }

            if (action.trim().contains(" ")) {
                showInvalidNameError.run();
            }
        });

        if (!flag.get() || !gc_name.isPresent()) {
            return;
        }
        
        TextInputDialog accounts_input = new TextInputDialog();
        accounts_input.setTitle("GroupChat Creation");
        accounts_input.setHeaderText("Input AccountNames seperated by ','");
        accounts_input.setContentText("Account Names");

        Optional<String> accounts_name = accounts_input.showAndWait();
        accounts_name.ifPresent(action -> {
            if (action.equals("")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("ERROR");
                alert.setHeaderText("Failed to create gcs");
                alert.setContentText("No Input was given for Accounts.");
                alert.showAndWait();
            } else {
                String[] accounts = action.split(",");
                for (int i = 0; i < accounts.length; i++) {
                    accounts[i] = accounts[i].trim();
                    String SQLOperation = "SELECT * FROM USER WHERE ACCOUNTNAME = \"" + accounts[i] + "\";==QUERY\n";
                    String[] ids = ChatServer.GenerateSQLOperation(SQLOperation);
                    if (ids.length <= 0) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("ERROR");
                        alert.setHeaderText("Failed to create gcs");
                        alert.setContentText("Account " + accounts[i] + " not found.");
                        alert.showAndWait();
                        return;
                    }
                }

                Set<String> accountSet = new HashSet<>(Arrays.asList(accounts));
                accountSet.add(ChatServer.Client_AccountName);
                String accounts_string = "[";
                int index = 0;
                for (String account : accountSet) {
                    accounts_string += "\"" + account.trim() + "\"";
                    if (index == accountSet.size() - 1) {
                        accounts_string += "]";
                    } else {
                        accounts_string += ",";
                    }
                    index++;
                }

                ChatServer.RunSQLOperation(String.format("INSERT INTO GROUPCHATS(CHAT, TABLE_NAME) VALUES('%s', \"%s\");==UPDATE\n", accounts_string, gc_name.get()));

                // Making Group Chat table
                ChatServer.RunSQLOperation(String.format("CREATE TABLE %s(ID INT AUTO_INCREMENT PRIMARY KEY, USER VARCHAR(50) NOT NULL, MESSAGE VARCHAR(250) NOT NULL, AT_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP);==UPDATE\n", gc_name.get()));
            }
        });
    }

    public void UploadMessage(ActionEvent event) {
        String message = MessageTypeField.getText();
        if (!message.isEmpty()) {
            MessageTypeField.setText("");
            ChatServer.RunSQLOperation(String.format("INSERT INTO %s(USER, MESSAGE) VALUES(\"%s\", \"%s\");==UPDATE\n", CurrentOpenedTableName, ChatServer.Client_AccountName, message));
        }
    }

    public void ChangeToPersonalChatPage(ActionEvent event) {
        ChatServer.onGroupChatPage = false;
        ChatServer.onChatPage = true;
        
        Pages all_pages = new Pages();
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(all_pages.Chat);
        stage.setScene(scene);
        stage.show();
    }
}
