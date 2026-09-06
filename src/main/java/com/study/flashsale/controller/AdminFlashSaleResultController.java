package com.study.flashsale.controller;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.common.Result;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.service.FlashSaleResultService;
import com.study.flashsale.vo.FlashSaleResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-秒杀结果")
@RestController
@RequestMapping("/api/admin/flash-sale-results")
@RequiredArgsConstructor
public class AdminFlashSaleResultController {

    private final FlashSaleResultService flashSaleResultService;

    @Operation(summary = "分页查询秒杀结果")
    @LogOperation("管理员查询秒杀结果列表")
    @GetMapping
    public Result<PageResponse<FlashSaleResultVO>> listResults(@Valid PageRequest request) {
        return Result.success(flashSaleResultService.listAllResults(request));
    }
}
