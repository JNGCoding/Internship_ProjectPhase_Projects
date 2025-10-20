module org.jngcoding.chat.app {
    requires javafx.controls;
    requires transitive javafx.graphics;

    requires javafx.fxml;

    requires java.sql;
    requires transitive java.sql.rowset;

    //& SUPPRESS WARNING: Automatic module name "jmdns"
    requires jmdns;

    opens org.jngcoding.chat.app to javafx.fxml;
    opens org.jngcoding.chat.app.Controllers to javafx.fxml;
    
    exports org.jngcoding.chat.app;
    exports org.jngcoding.chat.app.Controllers;
}
