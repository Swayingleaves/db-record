package com.dbrecord.service;

import com.dbrecord.entity.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author edy
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2025-07-13 20:28:21
*/
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);
}
