package com.reparaya.users.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignResponse {
    private String uploadUrl;
    private String objectUrl;
    private String objectKey;
    private int expiresInMinutes;
}