package org.jngcoding.chat.server;

@SuppressWarnings("CallToPrintStackTrace")
public class Main {
    public static void main(String[] args) {
        DBController databaseManager = new DBController(new DBController.DBMetaData(DBController.DEFAULT_DBUSERNAME, "root@root123", DBController.DEFAULT_DBURL, "CHAT_APPLICATION"));
        if (databaseManager.LatestException != null) {
            System.out.println(databaseManager.LatestException.toString());
            System.exit(0);
        }

        Server server = new Server(databaseManager);
        server.start_threads();
    }
}