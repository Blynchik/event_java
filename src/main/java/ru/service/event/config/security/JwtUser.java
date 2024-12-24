package ru.service.event.config.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class JwtUser {
    private final Long userId;
    private final String login;
    private final List<String> authorities;
}
