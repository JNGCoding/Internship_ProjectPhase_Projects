package org.jngcoding.chat.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

@SuppressWarnings("CallToPrintStackTrace")
public class ChatServer {
    public static Socket Server = null;
    public static InputStream ServerInput = null;
    public static OutputStream ServerOutput = null;

    public static String Client_AccountName = null;
    public static String Client_UserName = null;
    public static String[] Chats = null;
    public static String[] GroupChats = null;

    public static volatile boolean onChatPage = false;
    public static volatile boolean onGroupChatPage = false;

    public enum ResponseType {
        EMPTY_SUCCESS_RESPONSE,
        EMPTY_FAIL_RESPONSE,
        RESULT_SUCCESS_RESPONSE,
        RESULT_FAIL_RESPONSE,
        ERROR_RESPONSE
    }

    public static boolean ConnectToServer() throws IOException {
        JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
        ServiceInfo info = jmdns.getServiceInfo("_chat._tcp.local", "chatserver");
        if (info != null) {
            String host = info.getHostAddresses()[0];
            Server = new Socket(host, 8080);
            ServerInput = Server.getInputStream();
            ServerOutput = Server.getOutputStream();
        } else {
            throw new IOException("MDNS Name not found.");
        }

        return Server.isConnected();
    }

    public static void RunSQLOperation(String SQL) {
        if (ServerOutput != null) {
            try {
                SQL = MessageEncryptor.encrypt(SQL, 3);
                ServerOutput.write(SQL.getBytes());
                ServerOutput.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String[] GenerateSQLOperation(String SQL) {
        RunSQLOperation(SQL);

        List<String> result = new ArrayList<>();
        long millis = System.currentTimeMillis();
        StringBuilder dummy = new StringBuilder();

        OUTER:
        while (true) {
            if (System.currentTimeMillis() - millis > 1500) {
                break;
            }
            try {
                if (ServerInput.available() > 0) {
                    char c = (char) ServerInput.read();
                    switch (c) {
                        case '\n':
                            result.add(dummy.toString());
                            dummy.setLength(0);  // Reset Buffer
                            break;
                        case '\r':
                            break OUTER;
                        default:
                            dummy.append(c);
                            break;
                    }
                    millis = System.currentTimeMillis();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        for (int i = 0; i < result.size(); i++) {
            result.set(i, MessageEncryptor.decrypt(result.get(i), 3));
        }

        return result.toArray(String[]::new);
    }
}
