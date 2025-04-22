package com.example.magiccube.utils;

import java.util.Random;

public class Cube {
    public static String generateScramble(int steps) {
        String[] moves = {"R", "L", "U", "D", "F", "B"};
        String[] suffixes = {"", "`", "2"};

        StringBuilder scramble = new StringBuilder();
        Random rand = new Random();

        for (int i = 0; i < steps; i++) {
            String move = moves[rand.nextInt(moves.length)];
            String suffix = suffixes[rand.nextInt(suffixes.length)];
            scramble.append(move).append(suffix);
        }
        return scramble.toString();
    }
}