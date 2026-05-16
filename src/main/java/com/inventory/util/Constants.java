package com.inventory.util;

public final class Constants {

    private Constants() {}

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_NUMBER = 0;

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String USER_ID_MDC_KEY = "userId";
}
