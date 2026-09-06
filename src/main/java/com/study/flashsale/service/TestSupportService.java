package com.study.flashsale.service;

import com.study.flashsale.dto.request.TestCreateUsersRequest;
import com.study.flashsale.dto.request.TestResetSeckillRequest;
import com.study.flashsale.dto.response.TestResetSeckillResponse;
import com.study.flashsale.dto.response.TestUserResponse;

import java.util.List;

public interface TestSupportService {

    TestResetSeckillResponse resetSeckill(Long activityId, TestResetSeckillRequest request);

    List<TestUserResponse> createUsers(TestCreateUsersRequest request);
}
