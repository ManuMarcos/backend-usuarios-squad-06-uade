package com.reparaya.users;

import org.junit.jupiter.api.Test;

class UsersApplicationMainTest {

    @Test
    void main_startsWithoutErrors() {
        UsersApplication.main(new String[] { "--spring.profiles.active=test" });
    }
}
