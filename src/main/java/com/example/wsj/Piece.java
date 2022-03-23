package com.example.wsj;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;

import static com.example.wsj.Client.TILE_SIZE;

public class Piece extends StackPane {

    private PieceType pieceType;
    private double oldX,oldY;
    private double mouseX,mouseY;

    public PieceType getPieceType() {
        return pieceType;
    }

    public void setPieceType(PieceType pieceType) {
        this.pieceType = pieceType;
    }

    public double getOldX() {
        return oldX;
    }

    public double getOldY() {
        return oldY;
    }

    public Piece(PieceType pieceType, int x, int y){
        this.pieceType = pieceType;

        move(x,y);

        Ellipse bg = new Ellipse(TILE_SIZE * 0.3125,TILE_SIZE * 0.26);
        bg.setFill(Color.valueOf("#2a2a2a"));

        bg.setStroke(Color.valueOf("#2a2a2a"));
        bg.setStrokeWidth(TILE_SIZE * 0.03);

        bg.setTranslateX((TILE_SIZE-TILE_SIZE * 0.3125 * 2)/2);
        bg.setTranslateY((TILE_SIZE-TILE_SIZE * 0.26 * 2)/2 + TILE_SIZE*0.07);

        Ellipse ellipse = new Ellipse(TILE_SIZE * 0.3125,TILE_SIZE * 0.26);
        ellipse.setFill(pieceType == PieceType.BLACK? Color.BLACK:Color.WHITE);

        ellipse.setStroke(Color.valueOf("#2a2a2a"));
        ellipse.setStrokeWidth(TILE_SIZE * 0.03);

        ellipse.setTranslateX((TILE_SIZE-TILE_SIZE * 0.3125 * 2)/2);
        ellipse.setTranslateY((TILE_SIZE-TILE_SIZE * 0.26 * 2)/2);


        getChildren().addAll(bg,ellipse);
        setOnMousePressed(e ->{
            mouseX = e.getSceneX();
            mouseY = e.getSceneY();
        });
        setOnMouseDragged(e ->{
            relocate(e.getSceneX() - mouseX + oldX,e.getSceneY() - mouseY + oldY);
        });
    }
    public void move (int x,int y){
        oldX = x * TILE_SIZE;
        oldY = y * TILE_SIZE;
        relocate(oldX,oldY);
    }
    public void abortMove(){
        relocate(oldX,oldY);
    }
    public void getPromoted(){
        if(pieceType == PieceType.WHITE)
            pieceType = PieceType.PROMOTED_WHITE;
        else
            pieceType = PieceType.PROMOTED_BLACK;


        Ellipse ellipse = new Ellipse(TILE_SIZE * 0.3125/2,TILE_SIZE * 0.26/2);
        ellipse.setFill(Color.RED);

        ellipse.setStroke(Color.valueOf("#2a2a2a"));
        ellipse.setStrokeWidth(TILE_SIZE * 0.03);

        ellipse.setTranslateX((TILE_SIZE-TILE_SIZE * 0.3125 * 2)/2);
        ellipse.setTranslateY((TILE_SIZE-TILE_SIZE * 0.26 * 2)/2);

        getChildren().addAll(ellipse);
    }
}
