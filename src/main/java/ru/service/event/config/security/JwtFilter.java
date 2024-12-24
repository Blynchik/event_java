package ru.service.event.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.service.event.config.common.ServerProperties;
import ru.service.event.dto.exception.ExceptionInfo;
import ru.service.event.dto.exception.ExceptionResponse;
import ru.service.event.service.JwtService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ServerProperties serverProperties;

    @Autowired
    public JwtFilter(JwtService jwtService,
                     ServerProperties serverProperties) {
        this.jwtService = jwtService;
        this.serverProperties = serverProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            log.error("JWT validation failed: {}", "Empty token");
            sendErrorResponse(response, "", "Empty token");
            return;
        }
        String token = authHeader.substring(7);
        Long userId = null;
        String login = null;
        List<String> authorities = new ArrayList<>();
        try {
            Claims claims = jwtService.extractAllClaims(token);
            userId = claims.get("userId", Long.class);
            login = claims.getSubject();
            authorities = claims.get("authorities", List.class);
            jwtService.validateToken(token);
            if (userId != null && !authorities.isEmpty()) {
                List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                JwtUser jwtUser = new JwtUser(userId, login, authorities);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        jwtUser, null, grantedAuthorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            sendErrorResponse(response, e.getClass().getSimpleName(), e.getMessage());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String exception, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        List<ExceptionInfo> exceptions = List.of(new ExceptionInfo(exception, "", message));
        ExceptionResponse exceptionResponse = new ExceptionResponse(exceptions, serverProperties.getName(), serverProperties.getPort());
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(exceptionResponse);
        response.setContentLength(json.getBytes(UTF_8).length);
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}