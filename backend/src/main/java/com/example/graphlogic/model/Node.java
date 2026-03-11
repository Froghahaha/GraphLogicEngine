package com.example.graphlogic.model;

import lombok.Data;
import java.util.Map;

@Data
public class Node {
    private String id;
    private String type; // START, END, ACTION
    private String label;
    private Map<String, Object> data; // Custom data
    private Position position;

    @Data
    public static class Position {
        private double x;
        private double y;
    }
}
