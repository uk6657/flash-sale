package com.study.flashsale.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "flash-sale")
public class FlashSaleProperties {

    @Valid
    private Order order = new Order();

    @Valid
    private Seckill seckill = new Seckill();

    @Valid
    private Cache cache = new Cache();

    @Valid
    private Task task = new Task();

    @Valid
    private ThreadPool threadPool = new ThreadPool();

    @Valid
    private Admin admin = new Admin();

    @Valid
    private Mq mq = new Mq();

    @Data
    public static class Order {
        @NotNull(message = "订单支付超时时间不能为空")
        @Min(value = 1, message = "订单支付超时时间最少为1分钟")
        private Integer payTimeoutMinutes = 15;
    }

    @Data
    public static class Seckill {
        @NotNull(message = "秒杀限流窗口不能为空")
        @Min(value = 1, message = "秒杀限流窗口最少为1秒")
        private Integer rateLimitSeconds = 1;

        @NotNull(message = "秒杀限流次数不能为空")
        @Min(value = 1, message = "秒杀限流次数最少为1")
        private Integer rateLimitCount = 3;

        @NotNull(message = "用户秒杀标记额外保留时间不能为空")
        @Min(value = 0, message = "用户秒杀标记额外保留时间不能小于0")
        private Integer userMarkerExtraMinutes = 10;
    }

    @Data
    public static class Cache {
        @NotNull(message = "商品详情缓存时间不能为空")
        @Min(value = 1, message = "商品详情缓存时间最少为1分钟")
        private Integer productDetailMinutes = 30;

        @NotNull(message = "活动详情缓存时间不能为空")
        @Min(value = 1, message = "活动详情缓存时间最少为1分钟")
        private Integer activityDetailMinutes = 30;

        @NotNull(message = "空值缓存时间不能为空")
        @Min(value = 1, message = "空值缓存时间最少为1分钟")
        private Integer emptyValueMinutes = 2;

        @NotNull(message = "缓存重建锁时间不能为空")
        @Min(value = 1, message = "缓存重建锁时间最少为1秒")
        private Integer rebuildLockSeconds = 10;

        @NotNull(message = "缓存重建重试次数不能为空")
        @Min(value = 0, message = "缓存重建重试次数不能小于0")
        private Integer rebuildRetryTimes = 3;

        @NotNull(message = "缓存重建重试间隔不能为空")
        @Min(value = 1, message = "缓存重建重试间隔最少为1毫秒")
        private Long rebuildRetryIntervalMs = 50L;

        @NotNull(message = "数据库兜底最大并发数不能为空")
        @Min(value = 1, message = "数据库兜底最大并发数最少为1")
        private Integer dbFallbackMaxConcurrency = 20;
    }

    @Data
    public static class Task {
        @NotNull(message = "订单超时任务间隔不能为空")
        @Min(value = 1000, message = "订单超时任务间隔最少为1000毫秒")
        private Long orderTimeoutFixedDelayMs = 5000L;

        @NotNull(message = "订单超时任务批量数不能为空")
        @Min(value = 1, message = "订单超时任务批量数最少为1")
        @Max(value = 1000, message = "订单超时任务批量数最大为1000")
        private Integer orderTimeoutBatchSize = 100;

        @NotNull(message = "库存补偿任务初始延迟不能为空")
        @Min(value = 1000, message = "库存补偿任务初始延迟最少为1000毫秒")
        private Long stockCompensationInitialDelayMs = 60000L;

        @NotNull(message = "库存补偿任务间隔不能为空")
        @Min(value = 1000, message = "库存补偿任务间隔最少为1000毫秒")
        private Long stockCompensationFixedDelayMs = 60000L;

        @NotNull(message = "定时任务分布式锁过期时间不能为空")
        @Min(value = 1000, message = "定时任务分布式锁过期时间最少为1000毫秒")
        private Long scheduledLockTtlMs = 30000L;
    }

    @Data
    public static class ThreadPool {
        @NotNull(message = "订单线程池核心线程数不能为空")
        @Min(value = 1, message = "订单线程池核心线程数最少为1")
        private Integer orderCoreSize = 4;

        @NotNull(message = "订单线程池最大线程数不能为空")
        @Min(value = 1, message = "订单线程池最大线程数最少为1")
        private Integer orderMaxSize = 8;

        @NotNull(message = "订单线程池队列容量不能为空")
        @Min(value = 1, message = "订单线程池队列容量最少为1")
        private Integer orderQueueCapacity = 100;

        @NotNull(message = "订单线程池空闲回收时间不能为空")
        @Min(value = 1, message = "订单线程池空闲回收时间最少为1秒")
        private Integer orderKeepAliveSeconds = 60;

        @NotBlank(message = "订单线程池名称前缀不能为空")
        private String orderThreadNamePrefix = "order-task-";

        @AssertTrue(message = "订单线程池最大线程数不能小于核心线程数")
        public boolean isOrderMaxSizeValid() {
            return orderCoreSize == null || orderMaxSize == null || orderMaxSize >= orderCoreSize;
        }
    }

    @Data
    public static class Admin {
        @NotNull(message = "管理员初始化开关不能为空")
        private Boolean initEnabled = true;

        @NotBlank(message = "管理员用户名不能为空")
        private String username = "admin";

        @NotBlank(message = "管理员密码不能为空")
        private String password;

        @NotBlank(message = "管理员昵称不能为空")
        private String nickname = "系统管理员";
    }

    @Data
    public static class Mq {
        @NotNull(message = "订单消费最大重试次数不能为空")
        @Min(value = 0, message = "订单消费最大重试次数不能小于0")
        private Integer orderMaxRetryCount = 3;

        @NotNull(message = "订单消费重试延迟不能为空")
        @Min(value = 1000, message = "订单消费重试延迟最少为1000毫秒")
        private Integer orderRetryDelayMs = 5000;

        @NotNull(message = "消息发送最大重试次数不能为空")
        @Min(value = 0, message = "消息发送最大重试次数不能小于0")
        private Integer sendMaxRetryCount = 3;

        @NotNull(message = "消息发送重试任务间隔不能为空")
        @Min(value = 1000, message = "消息发送重试任务间隔最少为1000毫秒")
        private Long sendRetryFixedDelayMs = 5000L;

        @NotNull(message = "消息发送重试批量数不能为空")
        @Min(value = 1, message = "消息发送重试批量数最少为1")
        @Max(value = 1000, message = "消息发送重试批量数最大为1000")
        private Integer sendRetryBatchSize = 100;
    }
}
