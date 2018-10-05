package com.swiper.utils;

import java.util.Random;

public class Utils {
    public static int getRandomNumber(int min, int max) {
        return (new Random()).nextInt((max - min) + 1) + min;
    }
}
