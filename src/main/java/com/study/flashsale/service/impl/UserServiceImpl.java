package com.study.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.study.flashsale.common.ErrorCode;
import com.study.flashsale.context.UserContext;
import com.study.flashsale.dto.request.LoginRequest;
import com.study.flashsale.dto.request.RegisterRequest;
import com.study.flashsale.dto.response.LoginResponse;
import com.study.flashsale.entity.User;
import com.study.flashsale.enums.CommonStatus;
import com.study.flashsale.enums.UserRole;
import com.study.flashsale.exception.BusinessException;
import com.study.flashsale.mapper.UserMapper;
import com.study.flashsale.service.UserService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.util.JwtUtil;
import com.study.flashsale.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequest request) {
        createUser(request, UserRole.USER.name());
    }

    @Override
    public UserVO createAdmin(RegisterRequest request) {
        User user = createUser(request, UserRole.ADMIN.name());
        return toUserVO(user);
    }

    private User createUser(RegisterRequest request, String role) {
        String username = request.getUsername();
        String password = request.getPassword();
        String nickname = request.getNickname();

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User existUser = getOne(queryWrapper);

        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        user.setRole(role);
        user.setStatus(CommonStatus.ENABLED.getCode());

        save(user);
        return user;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = getOne(queryWrapper);

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean matches = passwordEncoder.matches(password, user.getPassword());
        if (!matches) {
            throw new BusinessException("用户名或密码错误");
        }

        if (CommonStatus.DISABLED.getCode().equals(user.getStatus())) {
            throw new BusinessException("用户已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getRole());
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        return toUserVO(user);
    }

    private UserVO toUserVO(User user) {
        return new UserVO(IdUtil.toString(user.getId()), user.getUsername(), user.getNickname(), user.getRole());
    }
}
