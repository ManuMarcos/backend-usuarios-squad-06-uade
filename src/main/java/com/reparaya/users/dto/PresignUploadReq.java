package com.reparaya.users.dto;

import lombok.Getter;

@Getter
public class PresignUploadReq {
    private String key;          // ej: users/2/profile.jpg
    private String contentType;  // ej: image/jpeg
    private Integer expiresIn;
}
