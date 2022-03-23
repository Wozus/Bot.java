package com.example.wsj;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client extends Application {

    public static final int TILE_SIZE = 100;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    private final Group tileGroup = new Group();
    private final Group pieceGroup = new Group();

    private final Tile[][] board = new Tile[WIDTH][HEIGHT];
    private String player;

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private boolean isYourTurn;

    private double time = 0;
    private final Timer timer = new Timer();

    private int whitePieces = 0;
    private int blackPieces = 0;

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
    private Parent createContent(){
        Pane root = new Pane();
        root.setPrefSize(WIDTH*TILE_SIZE,HEIGHT*TILE_SIZE);
        root.getChildren().addAll(tileGroup,pieceGroup,timer);
        for (int i=0;i<HEIGHT;i++){
            for(int j=0;j<WIDTH;j++){
                Tile tile = new Tile((i+j)%2 ==0, i,j);
                board[i][j] = tile;

                tileGroup.getChildren().add(tile);

                Piece piece = null;

                if( j <= 2 && (i+j)%2 == 1){
                    piece = makePiece(PieceType.BLACK,i,j);
                    blackPieces++;
                }
                if( j >= 5 && (i+j)%2 == 1){
                    piece = makePiece(PieceType.WHITE,i,j);
                    whitePieces++;
                }
                if(piece != null) {
                    tile.setPiece(piece);
                    pieceGroup.getChildren().add(piece);
                }
            }
        }
        return root;
    }
    public void countTime(){
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(()->{
            if(blackPieces*whitePieces == 0) {
                String winner;
                if(blackPieces == 0){
                    winner = "WHITE";
                }
                else{
                    winner = "BLACK";
                }
                Platform.runLater(() -> timer.setTimeString(Objects.equals(winner, player) ? "You won!":"You lost!"));
                executorService.shutdown();
                return;
            }

            if(isYourTurn){
                time+=0.1;
                Platform.runLater(()->timer.setTimeString("Timer: " + (int)time+ "s."));
            }
        },0,100, TimeUnit.MILLISECONDS);
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

        listenNotification();

        Scene scene = new Scene(createContent());
        countTime();
        stage.setTitle("Warcaby: " + player);
        stage.setScene(scene);
        stage.show();

    }
    public int toBoard(double pixel){
        return (int)(pixel+TILE_SIZE/2)/TILE_SIZE;
    }
    private Piece makePiece(PieceType pieceType,int x,int y){
        Piece piece = new Piece(pieceType,x,y);

        piece.setOnMouseReleased(e ->{
            if(!isYourTurn || !checkColor(piece)){
                piece.abortMove();
                return;
            }
            int newX = toBoard(piece.getLayoutX());
            int newY = toBoard(piece.getLayoutY());

            int x0 = toBoard(piece.getOldX());
            int y0 = toBoard(piece.getOldY());
            if(newX > 8 || newX < 0 || newY > 8 || newY < 0){
                piece.abortMove();
                return;
            }

            StringBuilder playerIntention = new StringBuilder();
            playerIntention.append(x0).append(" ");
            playerIntention.append(y0).append(" ");
            playerIntention.append(newX).append(" ");
            playerIntention.append(newY);
            try {
                bufferedWriter.write(String.valueOf(playerIntention));
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException ex) {
                System.out.println("Problem with connection to the server");
                System.exit(0);
            }
        });
        return piece;
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
                    System.out.println(notificationFromServer);
                    String[] data = notificationFromServer.split(" ");
                    int add = 0;
                    if (Objects.equals(data[0], "YOUR") || Objects.equals(data[0], "NO"))
                        add = 1;

                    int firstX = Integer.parseInt(data[add]);
                    int firstY = Integer.parseInt(data[1 + add]);
                    int lastX = Integer.parseInt(data[2 + add]);
                    int lastY = Integer.parseInt(data[3 + add]);

                    Piece piece = board[firstX][firstY].getPiece();
                    if (Objects.equals(data[0], "NO")) {
                        piece.abortMove();
                    }
                    else {
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
                                Platform.runLater(() -> pieceGroup.getChildren().remove(pieceToKill));
                                if (piece.getPieceType() == PieceType.BLACK || piece.getPieceType() == PieceType.PROMOTED_BLACK) {
                                    whitePieces--;
                                } else {
                                    blackPieces--;
                                }
                            }
                        }
                        if (moveResult.getType() != MoveType.NONE) {
                            if (piece.getPieceType() == PieceType.BLACK && lastY == 7) {
                                Platform.runLater(piece::getPromoted);
                            } else if (piece.getPieceType() == PieceType.WHITE && lastY == 0) {
                                Platform.runLater(piece::getPromoted);
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
}
