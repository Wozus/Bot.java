package com.example.wsj;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.Random;

public class Bot extends Application {
    public static final int TILE_SIZE = 100;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;


    private final Tile[][] board = new Tile[WIDTH][HEIGHT];
    private String player;

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private boolean isYourTurn;

    public void close(Socket socket,BufferedReader bufferedReader,BufferedWriter bufferedWriter){
        try {
            if(socket != null)
                socket.close();
            if(bufferedReader != null)
                bufferedReader.close();
            if(bufferedWriter != null)
                bufferedWriter.close();
        }
        catch (IOException e){
            System.out.println("Problem with close");
        }
    }
    private void createContent(){
        for (int i=0;i<HEIGHT;i++){
            for(int j=0;j<WIDTH;j++){
                Tile tile = new Tile((i+j)%2 ==0, i,j);
                board[i][j] = tile;

                Piece piece = null;

                if( j <= 2 && (i+j)%2 == 1){
                    piece = makePiece(PieceType.BLACK,i,j);
                }
                if( j >= 5 && (i+j)%2 == 1){
                    piece = makePiece(PieceType.WHITE,i,j);
                }
                if(piece != null) {
                    tile.setPiece(piece);
                }
            }
        }
    }
    @Override
    public void start(Stage stage) throws Exception {
        Socket socket = new Socket("localhost",6666);
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter( socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (IOException e){
            close(socket,bufferedReader,bufferedWriter);
            System.out.println("Problem with connection to the server");
            System.exit(0);
        }
        player = bufferedReader.readLine();
        if(Objects.equals(player,"1")){
            player = "WHITE";
            isYourTurn = true;
        }
        else {
            player = "BLACK";
            isYourTurn = false;
        }
        createContent();
        listenNotification();
        sendBotMove();
    }
    public int toBoard(double pixel){
        return (int)(pixel+TILE_SIZE/2)/TILE_SIZE;
    }
    private Piece makePiece(PieceType pieceType,int x,int y){
        return new Piece(pieceType,x,y);
    }
    public MoveResult tryMove(Piece piece,int newX,int newY){
        if(board[newX][newY].hasPiece() || (newX+newY)%2 == 0){
            return new MoveResult(MoveType.NONE);
        }
        int x0 = toBoard(piece.getOldX());
        int y0 = toBoard(piece.getOldY());

        if(Math.abs(newX-x0)==1 && newY - y0 == piece.getPieceType().moveDir ||
                ((piece.getPieceType() == PieceType.PROMOTED_BLACK || piece.getPieceType() == PieceType.PROMOTED_WHITE) && Math.abs(newX-x0)==1 && Math.abs(newY-y0)==1)){
            return new MoveResult(MoveType.NORMAL);
        }
        else if((Math.abs(newX-x0) == 2 && newY - y0 == piece.getPieceType().moveDir * 2) ||
                ((piece.getPieceType() == PieceType.PROMOTED_BLACK || piece.getPieceType() == PieceType.PROMOTED_WHITE))){

            int x1 = x0 + (newX-x0)/2;
            int y1 = y0 + (newY-y0)/2;

            if(board[x1][y1].hasPiece() && board[x1][y1].getPiece().getPieceType() != piece.getPieceType()){
                return new MoveResult(MoveType.KILL,board[x1][y1].getPiece());
            }
        }
        return new MoveResult(MoveType.NONE);
    }
    public void listenNotification(){
        new Thread(() -> {
            String notificationFromServer=null;
            while(socket.isConnected()) {
                try {
                    notificationFromServer = bufferedReader.readLine();
                }
                catch (IOException e){
                    close(socket,bufferedReader,bufferedWriter);
                    System.out.println("Problem with connection to the server");
                    System.exit(0);
                }

                if(notificationFromServer != null) {
                    String[] data = notificationFromServer.split(" ");
                    int add = 0;
                    if (Objects.equals(data[0], "YOUR") || Objects.equals(data[0], "NO"))
                        add = 1;

                    int firstX = Integer.parseInt(data[add]);
                    int firstY = Integer.parseInt(data[1 + add]);
                    int lastX = Integer.parseInt(data[2 + add]);
                    int lastY = Integer.parseInt(data[3 + add]);

                    Piece piece = board[firstX][firstY].getPiece();
                    if (!Objects.equals(data[0], "NO")){
                        MoveResult moveResult = tryMove(board[firstX][firstY].getPiece(), lastX, lastY);
                        switch (moveResult.getType()) {
                            case NORMAL -> {
                                piece.move(lastX, lastY);
                                board[firstX][firstY].setPiece(null);
                                board[lastX][lastY].setPiece(piece);
                            }
                            case KILL -> {
                                piece.move(lastX, lastY);
                                board[firstX][firstY].setPiece(null);
                                board[lastX][lastY].setPiece(piece);

                                Piece pieceToKill = moveResult.getPiece();
                                board[toBoard(pieceToKill.getOldX())][toBoard(pieceToKill.getOldY())].setPiece(null);
                            }
                        }
                        if (moveResult.getType() != MoveType.NONE) {
                            if (piece.getPieceType() == PieceType.BLACK && lastY == 7) {
                                piece.getPromoted();
                            } else if (piece.getPieceType() == PieceType.WHITE && lastY == 0) {
                                piece.getPromoted();
                            }
                        }
                        isYourTurn = add != 1;
                        notificationFromServer = null;
                    }
                }
            }
        }).start();
    }
    public boolean checkColor(Piece piece){
        if(piece.getPieceType() == PieceType.BLACK || piece.getPieceType() == PieceType.PROMOTED_BLACK){
            return Objects.equals(player, "BLACK");
        }
        else if(piece.getPieceType() == PieceType.WHITE || piece.getPieceType() == PieceType.PROMOTED_WHITE){
            return Objects.equals(player, "WHITE");
        }
        return false;
    }
    public void sendBotMove(){
        new Thread(()->{
            int firstX,firstY,lastX,lastY;
            StringBuilder stringBuilder = new StringBuilder();
            Random random = new Random();
            while(socket.isConnected()) {
                firstX = random.nextInt(0, 8);
                firstY = random.nextInt(0, 8);
                lastX = random.nextInt(0, 8);
                lastY = random.nextInt(0, 8);

                if (isYourTurn && board[firstX][firstY].hasPiece() && checkColor(board[firstX][firstY].getPiece())) {

                    MoveResult moveResult = tryMove(board[firstX][firstY].getPiece(),lastX,lastY);
                    if(moveResult.getType() != MoveType.NONE) {
                        stringBuilder.append(firstX).append(" ");
                        stringBuilder.append(firstY).append(" ");
                        stringBuilder.append(lastX).append(" ");
                        stringBuilder.append(lastY);

                        try {
                            bufferedWriter.write(stringBuilder.toString());
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        stringBuilder.setLength(0);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }).start();
    }
}
