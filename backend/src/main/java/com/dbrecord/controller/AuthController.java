package com.dbrecord.controller;

import com.dbrecord.entity.domain.User;
import com.dbrecord.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.dbrecord.config.JwtUtil;

@RestController
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        Map<String, Object> result = new HashMap<>();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            // 登录成功，生成JWT
            User user = (User) authentication.getPrincipal();
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
            result.put("success", true);
            result.put("token", token);
            result.put("message", "登录成功");
        } catch (AuthenticationException e) {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }
        return result;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> registerRequest) {
        String username = registerRequest.get("username");
        String password = registerRequest.get("password");
        Map<String, Object> result = new HashMap<>();
        if (userService.findByUsername(username) != null) {
            result.put("success", false);
            result.put("message", "用户名已存在");
            return result;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus(1);
        user.setRole("USER");
        userService.save(user);
        result.put("success", true);
        result.put("message", "注册成功");
        return result;
    }
} 