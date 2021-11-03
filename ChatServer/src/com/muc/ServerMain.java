package com.muc;



public class ServerMain {

    public static void main(String[] args) {
        int port = 9090;
        Server server = new Server(port);

        server.start();

    }


}
