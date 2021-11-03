package com.muc;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.nio.Buffer;
import java.util.HashSet;
import java.util.List;

public class ServerWorker extends Thread {

    private final Socket clientSocket;
    private final Server server;
    private String login;
    private OutputStream outputStream;
    private HashSet<String> topicSet = new HashSet<>();

    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSocket() throws IOException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        //outputStream.write("Login To Begin:\n\n".getBytes());

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while( (line = reader.readLine()) != null) {
            System.out.println("Incoming From Client \""+line+"\"");
            String[] tokens = StringUtils.split(line);
            if(tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if (("quit".equalsIgnoreCase(cmd)) || ("logout".equalsIgnoreCase(cmd))) {
                    handleLogout();
                    break;
                } else if ("login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream, tokens);
                } else if ("msg".equalsIgnoreCase(cmd)) {
                    String[] tokensMsg = StringUtils.split(line, null, 3);
                    handleMessage(tokensMsg);
                } else if ("join".equalsIgnoreCase(cmd)) {
                    handleJoin(tokens);
                } else if ("leave".equalsIgnoreCase(cmd)) {
                    handleLeave(tokens);
                } else {
                    String msg = "SVR: Unknown " + cmd + " Command\n";
                    outputStream.write(msg.getBytes());
                }
            }
           // String msg = "You typed " + line + "";
           // outputStream.write(msg.getBytes());
        }

        //outputStream.write("End of handle client socket()".getBytes());
        System.out.println("SVR: User "+login+" Has Logged Out");
        clientSocket.close();
    }

    private void handleLeave(String[] tokens) {
        if(tokens.length > 1) {
            String topic = tokens[1];
            topicSet.remove(topic);
        }
    }

    public boolean isMemberOfTopic(String topic) {
        return topicSet.contains(topic);
    }

    private void handleJoin(String[] tokens) {
        if(tokens.length > 1) {
            String topic = tokens[1];
            topicSet.add(topic);
        }
    }

    private void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String message = tokens[2];

        boolean isTopic = sendTo.charAt(0) == '#';

        List<ServerWorker> workerList = server.getWorkerList();
        //Send the current user to all online users
        for(ServerWorker worker : workerList) {
            if(isTopic) {
                if(worker.isMemberOfTopic(sendTo)) {
                    String outMsg = "msg " + login + " " + message + "\n";
                    worker.send(outMsg);
                }
            } else {
                if(sendTo.equalsIgnoreCase(worker.getLogin())) {
                    String outMsg = "msg " + login + " " + message + "\n";
                    worker.send(outMsg);
                }
            }
        }
    }

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if(tokens.length == 3) {
            String login = tokens[1];
            String password = tokens[2];

            if((login.equals("guest") && password.equals("guest"))
                    || (login.equals("john") && password.equals("john"))
            ){
                String msg = "ok login\n";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("SVR: User "+login+" Logged in Successfully");


                List<ServerWorker> workerList = server.getWorkerList();
                //Send the current user to all online users
                for(ServerWorker worker : workerList) {
                    if(this != worker) { //If this object is the same as the interation obj
                    //if(!login.equals(worker.getLogin())
                        if(worker.getLogin() != null) {
                            String onlineUsers = "online " + worker.getLogin() + "\n";
                            send(onlineUsers);
                        }
                    }
                }

                //Send all users the new online user
                String onlineMsg = "online "+login+"\n";
                for(ServerWorker worker : workerList) {
                    if(!login.equals(worker.getLogin())) {
                        worker.send(onlineMsg);
                    }
                }

            } else {
                String msg = "login failed\n";
                outputStream.write(msg.getBytes());
                System.out.println("SVR: User Log In Failed");
            }
        }
    }

    private void send(String msg) throws IOException {
        if(login != null)
            outputStream.write(msg.getBytes());
    }

    private void handleLogout() throws IOException {

        //server.getWorkerList().remove(this);
        server.removeWorker(this);

        //Send all users the new online user
        String onlineMsg = "offline "+login+"\n";
        List<ServerWorker> workerList = server.getWorkerList();
        for(ServerWorker worker : workerList) {
                worker.send(onlineMsg);
        }
        clientSocket.close();
    }

    public String getLogin()
    {
        return login;
    }

}
