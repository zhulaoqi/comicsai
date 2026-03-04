package com.comicsai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.config.XunhupayProperties;
import com.comicsai.mapper.PaymentOrderMapper;
import com.comicsai.mapper.RechargeRecordMapper;
import com.comicsai.mapper.UserMapper;
import com.comicsai.model.entity.PaymentOrder;
import com.comicsai.model.entity.RechargeRecord;
import com.comicsai.model.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentOrderMapper paymentOrderMapper;
    private final UserMapper userMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final XunhupayService xunhupayService;
    private final XunhupayProperties xunhupayProperties;

    public PaymentService(PaymentOrderMapper paymentOrderMapper,
                          UserMapper userMapper,
                          RechargeRecordMapper rechargeRecordMapper,
                          XunhupayService xunhupayService,
                          XunhupayProperties xunhupayProperties) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.userMapper = userMapper;
        this.rechargeRecordMapper = rechargeRecordMapper;
        this.xunhupayService = xunhupayService;
        this.xunhupayProperties = xunhupayProperties;
    }

    public record CreateOrderVO(String orderNo, String qrcodeUrl, String payUrl, BigDecimal amount) {}

    /**
     * 创建支付订单并调用虎皮椒下单。
     */
    public CreateOrderVO createPaymentOrder(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "充值金额必须大于0");
        }
        if (amount.compareTo(new BigDecimal("9999")) > 0) {
            throw new BusinessException(400, "单次充值金额不能超过9999元");
        }

        String orderNo = generateOrderNo();
        LocalDateTime now = LocalDateTime.now();

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setAmount(amount);
        order.setStatus(PaymentOrder.STATUS_PENDING);
        order.setChannel("xunhupay");
        order.setCreatedAt(now);
        order.setExpiredAt(now.plusMinutes(xunhupayProperties.getOrderExpireMinutes()));

        String title = "ComicsAI充值 ¥" + amount.toPlainString();
        XunhupayService.CreateOrderResult result = xunhupayService.createOrder(orderNo, amount, title);

        if (!result.success()) {
            order.setStatus(PaymentOrder.STATUS_FAILED);
            paymentOrderMapper.insert(order);
            throw new BusinessException(500, "创建支付订单失败：" + result.errMsg());
        }

        order.setQrcodeUrl(result.qrcodeUrl());
        order.setPayUrl(result.payUrl());
        paymentOrderMapper.insert(order);

        return new CreateOrderVO(orderNo, result.qrcodeUrl(), result.payUrl(), amount);
    }

    /**
     * 查询订单状态（前端轮询用）。
     */
    public PaymentOrder getOrderStatus(String orderNo, Long userId) {
        LambdaQueryWrapper<PaymentOrder> query = new LambdaQueryWrapper<>();
        query.eq(PaymentOrder::getOrderNo, orderNo);
        query.eq(PaymentOrder::getUserId, userId);
        PaymentOrder order = paymentOrderMapper.selectOne(query);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        if (PaymentOrder.STATUS_PENDING.equals(order.getStatus())
                && order.getExpiredAt() != null
                && LocalDateTime.now().isAfter(order.getExpiredAt())) {
            order.setStatus(PaymentOrder.STATUS_EXPIRED);
            paymentOrderMapper.updateById(order);
        }

        return order;
    }

    /**
     * 处理虎皮椒异步回调：验签 → 幂等判断 → 加余额 → 写充值记录。
     */
    @Transactional
    public boolean handlePaymentCallback(Map<String, String> params) {
        if (!xunhupayService.verifyCallbackHash(params)) {
            log.warn("支付回调签名验证失败: {}", params);
            return false;
        }

        String tradeOrderId = params.get("trade_order_id");
        String status = params.get("status");
        String transactionId = params.get("transaction_id");
        String totalFeeStr = params.get("total_fee");

        if (!"OD".equals(status)) {
            log.info("支付回调非成功状态: orderNo={}, status={}", tradeOrderId, status);
            return true;
        }

        LambdaQueryWrapper<PaymentOrder> query = new LambdaQueryWrapper<>();
        query.eq(PaymentOrder::getOrderNo, tradeOrderId);
        PaymentOrder order = paymentOrderMapper.selectOne(query);
        if (order == null) {
            log.error("支付回调找不到订单: orderNo={}", tradeOrderId);
            return false;
        }

        // 幂等：已支付则直接返回成功
        if (PaymentOrder.STATUS_PAID.equals(order.getStatus())) {
            log.info("订单已处理过, 跳过: orderNo={}", tradeOrderId);
            return true;
        }

        // 金额校验
        BigDecimal callbackAmount = new BigDecimal(totalFeeStr);
        if (callbackAmount.compareTo(order.getAmount()) != 0) {
            log.error("支付回调金额不匹配: orderNo={}, 期望={}, 实际={}", tradeOrderId, order.getAmount(), callbackAmount);
            return false;
        }

        // 更新订单状态
        order.setStatus(PaymentOrder.STATUS_PAID);
        order.setTransactionId(transactionId);
        order.setPaidAt(LocalDateTime.now());
        paymentOrderMapper.updateById(order);

        // 加余额
        User user = userMapper.selectById(order.getUserId());
        if (user == null) {
            log.error("支付回调用户不存在: userId={}", order.getUserId());
            return false;
        }

        BigDecimal newBalance = user.getBalance().add(order.getAmount());
        user.setBalance(newBalance);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 写充值记录
        RechargeRecord record = new RechargeRecord();
        record.setUserId(order.getUserId());
        record.setAmount(order.getAmount());
        record.setBalanceAfter(newBalance);
        record.setCreatedAt(LocalDateTime.now());
        rechargeRecordMapper.insert(record);

        log.info("支付成功处理完毕: orderNo={}, userId={}, amount={}", tradeOrderId, order.getUserId(), order.getAmount());
        return true;
    }

    private String generateOrderNo() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
