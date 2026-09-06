package com.study.flashsale.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.study.flashsale.dto.request.LoginRequest;
import com.study.flashsale.dto.request.RegisterRequest;
import com.study.flashsale.dto.response.LoginResponse;
import com.study.flashsale.entity.User;
import com.study.flashsale.vo.UserVO;

public interface UserService extends IService<User> {

    void register(RegisterRequest request);

    UserVO createAdmin(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserVO getCurrentUser();
}
