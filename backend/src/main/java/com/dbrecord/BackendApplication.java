package com.dbrecord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import com.dbrecord.service.UserService;
import com.dbrecord.entity.domain.User;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@MapperScan("com.dbrecord.mapper")
public class BackendApplication implements CommandLineRunner {

	@Autowired
	private UserService userService;
	@Autowired
	private PasswordEncoder passwordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (userService.findByUsername("admin") == null) {
			User user = new User();
			user.setUsername("admin");
			user.setPassword(passwordEncoder.encode("zd@123"));
			user.setStatus(1);
			user.setRole("ADMIN");
			userService.save(user);
			System.out.println("已自动添加admin用户，密码: zd@123");
		} else {
			System.out.println("admin用户已存在");
		}
	}

}
