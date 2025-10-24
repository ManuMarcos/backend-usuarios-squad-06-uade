package com.reparaya.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionService {
    public Map<String, List<String>> getPermissionsForUser(Long userId) {
        return Collections.emptyMap();
    }
}
