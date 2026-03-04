package com.comicsai.controller.reader;

import com.comicsai.common.ApiResponse;
import com.comicsai.common.annotation.RequireAuth;
import com.comicsai.config.JwtInterceptor;
import com.comicsai.model.dto.RechargeDTO;
import com.comicsai.model.entity.PaymentOrder;
import com.comicsai.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * 创建支付订单（需登录）
     */
    @PostMapping("/reader/payment/create")
    @RequireAuth
    public ApiResponse<PaymentService.CreateOrderVO> createOrder(
            @Valid @RequestBody RechargeDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        PaymentService.CreateOrderVO result = paymentService.createPaymentOrder(userId, dto.getAmount());
        return ApiResponse.success(result);
    }

    /**
     * 查询订单支付状态（前端轮询用，需登录）
     */
    @GetMapping("/reader/payment/status/{orderNo}")
    @RequireAuth
    public ApiResponse<Map<String, Object>> getOrderStatus(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        PaymentOrder order = paymentService.getOrderStatus(orderNo, userId);

        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("status", order.getStatus());
        data.put("amount", order.getAmount());
        data.put("paidAt", order.getPaidAt());
        return ApiResponse.success(data);
    }

    /**
     * 虎皮椒异步回调（无需登录，虎皮椒服务器直接调用）
     */
    @PostMapping(value = "/payment/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public String paymentNotify(@RequestParam Map<String, String> params) {
        log.info("收到支付回调: {}", params);
        boolean success = paymentService.handlePaymentCallback(params);
        return success ? "success" : "fail";
    }
}
