package com.example.wsj;

public enum PieceType {
    BLACK(1),WHITE(-1),PROMOTED_BLACK(2),PROMOTED_WHITE(-2);

    final int moveDir;

    PieceType(int moveDir){
        this.moveDir = moveDir;
    }
}
