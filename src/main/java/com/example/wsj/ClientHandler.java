package com.example.wsj;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable{

    public static final int TILE_SIZE = 100;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    private Socket socket1;
    private BufferedReader bufferedReader1;
    private BufferedWriter bufferedWriter1;

    private Socket socket2;
    private BufferedReader bufferedReader2;
    private BufferedWriter bufferedWriter2;

    private int turn=0;

    private final Tile[][] board = new Tile[WIDTH][HEIGHT];

    public ClientHandler(Socket socket1, Socket socket2) {
        try {
            this.socket1 = socket1;
            this.socket2 = socket2;
            this.bufferedWriter1 = new BufferedWriter(new OutputStreamWriter(socket1.getOutputStream()));
            this.bufferedReader1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
            this.bufferedWriter2 = new BufferedWriter(new OutputStreamWriter(socket2.getOutputStream()));
            this.bufferedReader2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
        } catch (IOException e) {
            close(socket1, bufferedReader1, bufferedWriter1);
            close(socket2, bufferedReader2, bufferedWriter2);
        }
    }

    public void close(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (socket != null)
                socket.close();
            if (bufferedReader != null)
                bufferedReader.close();
            if (bufferedWriter != null)
                bufferedWriter.close();
        } catch (IOException e) {
            System.out.println("Problem with close");
        }
    }
    public int toBoard(double pixel){
        return (int)(pixel+TILE_SIZE/2)/TILE_SIZE;
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
    public void createServerBoard(){
        for (int i=0;i<HEIGHT;i++) {
            for (int j = 0; j < WIDTH; j++) {
                Tile tile = new Tile((i + j) % 2 == 0, i, j);
                board[i][j] = tile;

                Piece piece = null;

                if( j <= 2 && (i+j)%2 == 1){
                    piece = new Piece(PieceType.BLACK,i,j);
                }
                if(j >= 5 && (i+j)%2 == 1){
                    piece = new Piece(PieceType.WHITE,i,j);
                }
                if(piece!= null )
                    tile.setPiece(piece);
            }
        }
    }
    @Override
    public void run() {
        if(socket1.isConnected()) {
            try {
                bufferedWriter1.write("1");
                bufferedWriter1.newLine();
                bufferedWriter1.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(socket2.isConnected()) {
            try {
                bufferedWriter2.write("2");
                bufferedWriter2.newLine();
                bufferedWriter2.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        createServerBoard();

        String clientIntention = null;
        while(socket1.isConnected() && socket2.isConnected()){
            if(turn%2 == 0){
                //WHITE TURN
                try {
                    clientIntention = bufferedReader1.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(clientIntention != null) {
                    System.out.println("CLIENT: "+clientIntention);
                    String[] splitIntention = clientIntention.split(" ");
                    if(checkClientIntention(splitIntention)){

                        try {
                            bufferedWriter1.write("YOUR "+clientIntention);
                            bufferedWriter1.newLine();
                            bufferedWriter1.flush();

                            bufferedWriter2.write(clientIntention);
                            bufferedWriter2.newLine();
                            bufferedWriter2.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        turn++;
                    }
                    else {
                        try {
                            bufferedWriter1.write("NO "+clientIntention);
                            bufferedWriter1.newLine();
                            bufferedWriter1.flush();
                        }catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
            else{
                //BLACK TURN
                try {
                    clientIntention = bufferedReader2.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(clientIntention != null) {
                    System.out.println("BOT: "+clientIntention);
                    String[] splitIntention = clientIntention.split(" ");
                    if(checkClientIntention(splitIntention)){

                        try {
                            bufferedWriter2.write("YOUR "+clientIntention);
                            bufferedWriter2.newLine();
                            bufferedWriter2.flush();

                            bufferedWriter1.write(clientIntention);
                            bufferedWriter1.newLine();
                            bufferedWriter1.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        turn++;
                    }
                    else {
                        try {
                            bufferedWriter2.write("NO "+clientIntention);
                            bufferedWriter2.newLine();
                            bufferedWriter2.flush();
                        }catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    public void updateServerBoard(Piece piece,MoveResult moveResult,int firstX,int firstY,int lastX,int lastY){
        switch (moveResult.getType()){
            case NORMAL -> {
                piece.move(lastX,lastY);
                board[firstX][firstY].setPiece(null);
                board[lastX][lastY].setPiece(piece);
            }
            case KILL -> {
                piece.move(lastX,lastY);
                board[firstX][firstY].setPiece(null);
                board[lastX][lastY].setPiece(piece);

                Piece pieceToKill = moveResult.getPiece();
                board[toBoard(pieceToKill.getOldX())][toBoard(pieceToKill.getOldY())].setPiece(null);

            }
        }
        if(moveResult.getType() != MoveType.NONE) {
            if (piece.getPieceType() == PieceType.BLACK && lastY == 7) {
                piece.getPromoted();
            } else if (piece.getPieceType() == PieceType.WHITE && lastY == 0) {
                piece.getPromoted();
            }
        }
    }
    public boolean checkClientIntention(String[] clientIntention){
        if(clientIntention == null)
            return false;
        if(clientIntention.length != 4)
            return false;
        int firstX = Integer.parseInt(clientIntention[0]);
        int firstY = Integer.parseInt(clientIntention[1]);
        int lastX = Integer.parseInt(clientIntention[2]);
        int lastY = Integer.parseInt(clientIntention[3]);
        if(!board[firstX][firstY].hasPiece())
            return false;
        Piece chosenPiece = board[firstX][firstY].getPiece();

        MoveResult checkMove = tryMove(chosenPiece,lastX,lastY);

        if(checkMove.getType() == MoveType.NONE) {
            chosenPiece.abortMove();
            return false;
        }
        updateServerBoard(chosenPiece,checkMove,firstX,firstY,lastX,lastY);

        return true;

    }
}
