package com.study.flashsale.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class OrderNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public String generate() {
        String timePart = LocalDateTime.now().format(FORMATTER);
        int randomPart = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "FS" + timePart + randomPart;
    }
}