package com.example.wsj;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private final ServerSocket serverSocket;
    private Socket socket1;
    private Socket socket2;
    public Server(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    public void startServer() throws IOException {

        try{
            while (!serverSocket.isClosed()){
                socket1 = serverSocket.accept();
                System.out.println("First player has connected");
                socket2 = serverSocket.accept();
                System.out.println("Second player has connected");
                ClientHandler clientHandler = new ClientHandler(socket1,socket2);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        }
        catch (IOException e){
            socket1.close();
            socket2.close();
            closeServerSocket();
        }
    }
    public void closeServerSocket(){
        try{
            if(serverSocket != null){
                serverSocket.close();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(6666);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}

