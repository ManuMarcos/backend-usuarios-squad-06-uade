package com.reparaya.users;

import org.junit.jupiter.api.Test;

class UsersApplicationMainTest {

    @Test
    void main_startsWithoutErrors() {
        System.setProperty("server.port", "0");
        UsersApplication.main(new String[] { "--spring.profiles.active=test" });
    }
}
