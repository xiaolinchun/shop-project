package com.shop.online_shop.util;

import org.apache.commons.lang3.RandomStringUtils;

public class TokenUtil {
    public static String generateToken(){
        return RandomStringUtils.randomAlphanumeric(8);
    }
}