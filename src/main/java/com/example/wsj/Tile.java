package com.example.wsj;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Tile extends Rectangle {

    private Piece piece;

    public boolean hasPiece() {
        return piece != null;
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public Tile(boolean light, int x, int y){
        setWidth(Client.TILE_SIZE);
        setHeight(Client.TILE_SIZE);


        relocate(x*Client.TILE_SIZE,y*Client.TILE_SIZE);

        setFill(light ? Color.valueOf("#feb") : Color.SADDLEBROWN);
    }
}
