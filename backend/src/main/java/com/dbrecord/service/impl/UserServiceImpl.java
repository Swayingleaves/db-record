package com.dbrecord.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dbrecord.entity.domain.User;
import com.dbrecord.service.UserService;
import com.dbrecord.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author edy
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2025-07-13 20:28:21
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public User findByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }
}




