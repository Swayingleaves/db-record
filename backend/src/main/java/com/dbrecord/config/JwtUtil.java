package com.dbrecord.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    // 建议放到配置文件
    private static final String SECRET_KEY = "dbrecord-secret-key-dbrecord-secret-key";
    private static final long EXPIRATION = 1000 * 60 * 60 * 24; // 24小时
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // 生成token
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 解析token
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 校验token
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 获取用户名
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    // 获取角色
    public String getRole(String token) {
        return (String) getClaims(token).get("role");
    }
} 