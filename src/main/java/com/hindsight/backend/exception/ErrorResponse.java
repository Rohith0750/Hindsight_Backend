package com.hindsight.backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private boolean success;
    private int statuscode;
    private RequestDetails request;
    private String message;
    private Object data;
    private Map<String, String> trace;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestDetails {
        private String ip;
        private String method;
        private String url;
    }
}
