package org.jngcoding.chat.server;

import java.io.*;
import java.net.*;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.jngcoding.chat.server.DBController.QueryType;

@SuppressWarnings("CallToPrintStackTrace")
public class Server {
    public final String MDNS_TYPE = "_chat._tcp.local.";
    public final String MDNS_NAME = "chatserver";
    public final int PORT = 8080;

    private ServerSocket ChatServer = null;
    private final List<ServerClient> Clients = Collections.synchronizedList(new ArrayList<>());
    private final Thread ConnectorThread, ControlThread;
    public boolean RunFlag = true;
    private final DBController database;

    public static class ServerClient {
        Socket ChatClient;
        BufferedReader Reader;
        OutputStream ClientOutput;

        public ServerClient(Socket sc) {
            ChatClient = sc;
            try {
                Reader = new BufferedReader(new InputStreamReader(ChatClient.getInputStream()));
                ClientOutput = ChatClient.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public boolean isDisconnected() {
            return ChatClient.isClosed() || !ChatClient.isConnected();
        }

        public void Close() {
            try {
                ChatClient.close();
                Reader.close();
                ClientOutput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Server(DBController dbController) {
        this.database = dbController;

        try {
            ChatServer = new ServerSocket(PORT);
            JmDNS jmdns = JmDNS.create(ChatServer.getInetAddress());
            ServiceInfo serviceInfo = ServiceInfo.create(MDNS_TYPE, MDNS_NAME, PORT, "CHAT SERVER");
            jmdns.registerService(serviceInfo);

            System.out.println("Started at MDNS: " + MDNS_NAME);
            System.out.println("Started at PORT: " + PORT);
        } catch (IOException exception) {
            System.out.println("Failed to start Server: " + exception.getMessage());
        }

        ConnectorThread = new Thread(() -> {
            System.out.println("Started Connector Thread.");
            while (RunFlag) {
                try {
                    Socket ChatClient = ChatServer.accept();
                    ServerClient client = new ServerClient(ChatClient);
                    Clients.add(client);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Clean up disconnected clients
                Clients.removeIf(client -> {
                    if (client.isDisconnected()) {
                        client.Close();
                        return true;
                    }
                    return false;
                });
            }
        });

        ControlThread = new Thread(() -> {
            System.out.println("Started Control Thread.");
            while (RunFlag) {
                synchronized (Clients) {
                    for (ServerClient client : Clients) {
                        try {
                            if (client.Reader.ready()) {
                                String instruction = client.Reader.readLine();
                                if (instruction != null && !instruction.isEmpty()) {
                                    instruction = MessageEncryptor.decrypt(instruction, 3);

                                    String[] parts = instruction.split("==");
                                    if (parts.length < 2) continue;

                                    System.out.println("==================================");
                                    System.out.println("INSTRUCTION: " + parts[0]);
                                    System.out.println("QUERYTYPE: " + parts[1]);

                                    DBController.QueryType querytype;
                                    switch (parts[1].trim().toUpperCase()) {
                                        case "UPDATE": querytype = DBController.QueryType.UPDATE; break;
                                        case "QUERY": querytype = DBController.QueryType.QUERY; break;
                                        case "NORMAL":
                                        default: querytype = DBController.QueryType.NORMAL; break;
                                    }

                                    DBController.DBResult result = database.ExecuteQuery(parts[0].trim(), querytype);
                                    if (querytype == QueryType.QUERY && result != null && result.rlst != null) {
                                        try {
                                            ResultSetMetaData rsltmtdt = result.rlst.getMetaData();
                                            int column_count = rsltmtdt.getColumnCount();

                                            while (result.rlst.next()) {
                                                StringBuilder tuple = new StringBuilder();
                                                for (int i = 1; i <= column_count; i++) {
                                                    String column_name = rsltmtdt.getColumnName(i);
                                                    Object Value = result.rlst.getObject(i);
                                                    tuple.append(column_name);
                                                    tuple.append(":");
                                                    tuple.append(Value.toString());

                                                    if (i < column_count) {
                                                        tuple.append("--,");
                                                    }
                                                }

                                                tuple = new StringBuilder(MessageEncryptor.encrypt(tuple.toString(), 3));

                                                client.ClientOutput.write(tuple.toString().getBytes());
                                                client.ClientOutput.write('\n');

                                                System.out.println(tuple.toString() + "\n\r");
                                            }
                                            client.ClientOutput.write('\r');
                                        } catch (SQLException exception) {
                                            exception.printStackTrace();
                                        }
                                    } else {
                                        //! NOTHING ?
                                    }
                                    database.closeDBResult(result);
                                    System.out.println("==================================");
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50)); // Prevent CPU Hogging
            }
        });

        ConnectorThread.setDaemon(true);
        ControlThread.setDaemon(true);
    }

    public void start_threads() {
        ConnectorThread.start();
        ControlThread.start();
    }
}