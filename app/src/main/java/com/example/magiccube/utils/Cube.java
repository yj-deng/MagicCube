package com.example.magiccube.utils;

import java.util.Random;

public class Cube {
    public static String generateScramble(int steps,int level) {
        String[] axis = {"x","y","z"};
        String[] angleType = {"1", "-1", "2"};

        int range=level/2;
        String[] value =new String[level];
        for (int i = -range; i <= range; i++) {
            value[i + range] = Integer.toString(i);
        }

        StringBuilder scramble = new StringBuilder();
        Random rand = new Random();

        for (int i = 0; i < steps; i++) {
            String rotateAxis = axis[rand.nextInt(axis.length)];
            String rotateValue = value[rand.nextInt(value.length)];
            String rotateAngleType = angleType[rand.nextInt(angleType.length)];
            scramble.append(rotateAxis).append(rotateValue).append(rotateAngleType);
        }
        return scramble.toString();
    }
}