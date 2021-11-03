package com.muc;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ChatClient {

    private final String serverName;
    private final int serverPort;
    private Socket socket;
    private OutputStream serverOut;
    private InputStream serverIn;
    private BufferedReader bufferedIn;

    private ArrayList<UserStatusListener> userStatusListeners = new ArrayList<>();
    private ArrayList<MessageListener> messageListeners = new ArrayList<>();


    public ChatClient(String serverName, int serverPort) {
        this.serverName = serverName;
        this.serverPort = serverPort;

    }

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient("192.168.0.40",9090);
        client.addUserStatusListener(new UserStatusListener() {
            @Override
            public void online(String login) {
                System.out.println("ONLINE "+ login);
            }

            @Override
            public void offline(String login) {
                System.out.println("OFFLINE "+login);
            }
        });

        client.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(String fromLogin, String msgBody) {
                System.out.println("You got a message from "+fromLogin + " ==> "+msgBody);
            }
        });
        //client.msg("stuff".getBytes());
        if(!client.connect()) {
            System.err.println("Connection Failed");
        } else {
            System.out.println("Connection Success");

            if(client.login("guest","guest")) {
                System.out.println("Login Successful");
                client.msg("john", "Hello my friend");
            } else {
                System.err.println("Login Failed");
            }
        }

        //client.logoff();
    }

    public void msg(String sendTo, String message) throws IOException {
        String cmd = "msg "+sendTo+" "+message+"\n";
        serverOut.write(cmd.getBytes());
    }

    public void logoff() throws IOException {
        String cmd = "logout\n";
        serverOut.write(cmd.getBytes());
    }

    public boolean login(String username, String password) throws IOException {
        String cmd = "login " + username + " " + password+"\n";
        serverOut.write(cmd.getBytes());
        
        String response = bufferedIn.readLine();

        System.out.println("Server Response "+response);
        if("ok login".equalsIgnoreCase(response)) {
            System.out.println("Starting Message Reader");
            startMessageReader();
            return true;
        } else {
            return false;
        }
    }

    private void startMessageReader() {
        Thread t = new Thread() {
            @Override
            public void run() {
                readMessageLoop();
            }
        };
        t.start();
    }

    private void readMessageLoop() {
        String line;
        try {
            while ((line = bufferedIn.readLine()) != null) {
                System.out.println("Read Message Loop. Line: "+line);
                String[] tokens = StringUtils.split(line);
                if(tokens != null && tokens.length > 0) {
                    String cmd = tokens[0];
                    if("online".equalsIgnoreCase(cmd)) {
                        handleOnline(tokens);
                    } else if ("offline".equalsIgnoreCase(cmd)) {
                        handleOffline(tokens);
                    } else if("msg".equalsIgnoreCase(cmd)) {
                        String[] tokensMsg = StringUtils.split(line,null,3);
                        handleMessage(tokensMsg);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleMessage(String[] tokens) {
        String login = tokens[1];
        String msgBody = tokens[2];

        for(MessageListener listener : messageListeners) {
            listener.onMessage(login,msgBody);
        }
    }

    private void handleOnline(String[] tokens) {
        String login = tokens[1];
        for(UserStatusListener listener : userStatusListeners) {
            listener.online(login);
        }
    }

    private void handleOffline(String[] tokens) {
        String login = tokens[1];
        for(UserStatusListener listener : userStatusListeners) {
            listener.offline(login);
        }
    }

    public boolean connect() {
        try {
            this.socket = new Socket(serverName, serverPort);
            System.out.println("Client Port is "+socket.getLocalPort());
            this.serverOut = socket.getOutputStream();
            this.serverIn = socket.getInputStream();
            this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void addUserStatusListener(UserStatusListener listener) {
        userStatusListeners.add(listener);
    }
    public void removeUserStatusListener(UserStatusListener listener) {
        userStatusListeners.remove(listener);
    }

    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }
    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }
}
