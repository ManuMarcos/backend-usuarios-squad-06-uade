package com.reparaya.users.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PresignUploadReqTest {

    @Test
    void gettersAndSettersWork() {
        PresignUploadReq req = new PresignUploadReq();
        req.setKey("users/1/profile.jpg");
        req.setContentType("image/jpeg");
        req.setExpiresIn(15);

        assertThat(req.getKey()).isEqualTo("users/1/profile.jpg");
        assertThat(req.getContentType()).isEqualTo("image/jpeg");
        assertThat(req.getExpiresIn()).isEqualTo(15);
    }
}
