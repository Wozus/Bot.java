package com.example.wsj;

public class MoveResult {

    private MoveType moveType;
    private Piece piece;

    public MoveType getType() {
        return moveType;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public Piece getPiece() {
        return piece;
    }

    public void setType(MoveType type) {
        this.moveType = type;
    }
    public MoveResult(MoveType moveType){
        this(moveType,null);
    }

    public MoveResult(MoveType moveType,Piece piece){
        this.moveType = moveType;
        this.piece = piece;
    }

}
