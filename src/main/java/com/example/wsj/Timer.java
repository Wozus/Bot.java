package com.example.wsj;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import static com.example.wsj.Client.TILE_SIZE;
import static com.example.wsj.Client.WIDTH;

public class Timer extends StackPane {

    private final StringProperty timeString = new SimpleStringProperty("Timer: 0s.");


    public Timer(){
        Rectangle rectangle = new Rectangle();
        double width = TILE_SIZE;
        rectangle.setWidth(width);
        double height = TILE_SIZE * 0.4;
        rectangle.setHeight(height);
        rectangle.setFill(Color.LIGHTGRAY);
        rectangle.setArcWidth(10.0);
        rectangle.setArcHeight(10.0);
        rectangle.setStroke(Color.BLACK);

        Label timeLabel = new Label();
        timeLabel.textProperty().bind(timeString);
        timeLabel.setFont(new Font(height *0.5));
        timeLabel.setTextAlignment(TextAlignment.CENTER);
        timeLabel.setTextFill(Color.web("#4f4f4f"));

        getChildren().addAll(rectangle,timeLabel);

        relocate((TILE_SIZE * WIDTH - width)/2,0);
    }
    public void setTimeString(String string){
        timeString.set(string);
    }
}
