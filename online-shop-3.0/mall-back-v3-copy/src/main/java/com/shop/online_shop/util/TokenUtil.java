package com.shop.online_shop.util;

import org.apache.commons.lang3.RandomStringUtils;

public final class TokenUtil {

    private static final int TOKEN_LENGTH = 48;

    private TokenUtil() {
    }

    public static String generateToken() {
        return RandomStringUtils.randomAlphanumeric(TOKEN_LENGTH);
    }
}
