package com.study.flashsale.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.flashsale.entity.User;
import com.study.flashsale.enums.CommonStatus;
import com.study.flashsale.enums.UserRole;
import com.study.flashsale.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.jspecify.annotations.NonNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserService userService;
    private final FlashSaleProperties flashSaleProperties;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        FlashSaleProperties.Admin admin = flashSaleProperties.getAdmin();
        if (!Boolean.TRUE.equals(admin.getInitEnabled())) {
            log.info("默认管理员初始化已关闭");
            return;
        }

        LambdaQueryWrapper<User> adminQueryWrapper = new LambdaQueryWrapper<>();
        adminQueryWrapper.eq(User::getRole, UserRole.ADMIN.name());
        if (userService.count(adminQueryWrapper) > 0) {
            log.info("已存在管理员账号，跳过默认管理员初始化");
            return;
        }

        if (!StringUtils.hasText(admin.getUsername()) || !StringUtils.hasText(admin.getPassword())) {
            log.warn("默认管理员配置不完整，跳过初始化");
            return;
        }

        LambdaQueryWrapper<User> usernameQueryWrapper = new LambdaQueryWrapper<>();
        usernameQueryWrapper.eq(User::getUsername, admin.getUsername());
        User existUser = userService.getOne(usernameQueryWrapper);
        if (existUser != null) {
            existUser.setRole(UserRole.ADMIN.name());
            existUser.setStatus(CommonStatus.ENABLED.getCode());
            userService.updateById(existUser);
            log.info("默认管理员用户名已存在，已提升为管理员，userId={}, username={}",
                    existUser.getId(), existUser.getUsername());
            return;
        }

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        User user = new User();
        user.setUsername(admin.getUsername());
        user.setPassword(passwordEncoder.encode(admin.getPassword()));
        user.setNickname(StringUtils.hasText(admin.getNickname()) ? admin.getNickname() : admin.getUsername());
        user.setRole(UserRole.ADMIN.name());
        user.setStatus(CommonStatus.ENABLED.getCode());
        userService.save(user);

        log.info("默认管理员初始化成功，userId={}, username={}", user.getId(), user.getUsername());
    }
}
