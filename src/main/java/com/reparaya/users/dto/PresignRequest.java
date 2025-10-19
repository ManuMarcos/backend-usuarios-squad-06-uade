package com.reparaya.users.dto;

import lombok.Data;
@Data
public class PresignRequest {
    private String fileName;
    private String contentType;
}
