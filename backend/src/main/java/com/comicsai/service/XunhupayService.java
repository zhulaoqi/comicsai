package com.comicsai.service;

import com.comicsai.config.XunhupayProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class XunhupayService {

    private static final Logger log = LoggerFactory.getLogger(XunhupayService.class);
    private static final String API_VERSION = "1.1";

    private final XunhupayProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public XunhupayService(XunhupayProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public record CreateOrderResult(boolean success, String qrcodeUrl, String payUrl, String errMsg) {}

    /**
     * 调用虎皮椒下单接口，返回支付二维码和跳转链接。
     */
    public CreateOrderResult createOrder(String orderNo, BigDecimal amount, String title) {
        try {
            Map<String, String> params = new TreeMap<>();
            params.put("version", API_VERSION);
            params.put("appid", props.getAppid());
            params.put("trade_order_id", orderNo);
            params.put("total_fee", amount.toPlainString());
            params.put("title", title);
            params.put("time", String.valueOf(System.currentTimeMillis() / 1000));
            params.put("notify_url", props.getNotifyUrl());
            params.put("return_url", props.getReturnUrl());
            params.put("nonce_str", UUID.randomUUID().toString().replace("-", "").substring(0, 32));

            String hash = generateHash(params, props.getAppsecret());
            params.put("hash", hash);

            String formBody = params.entrySet().stream()
                    .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                            + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getGatewayUrl()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            int errcode = json.has("errcode") ? json.get("errcode").asInt() : -1;
            if (errcode == 0) {
                String qrcodeUrl = json.has("url_qrcode") ? json.get("url_qrcode").asText() : "";
                String payUrl = json.has("url") ? json.get("url").asText() : "";
                return new CreateOrderResult(true, qrcodeUrl, payUrl, null);
            } else {
                String errmsg = json.has("errmsg") ? json.get("errmsg").asText() : "未知错误";
                log.error("虎皮椒下单失败: errcode={}, errmsg={}", errcode, errmsg);
                return new CreateOrderResult(false, null, null, errmsg);
            }
        } catch (Exception e) {
            log.error("虎皮椒下单异常", e);
            return new CreateOrderResult(false, null, null, "支付服务暂不可用");
        }
    }

    /**
     * 验证回调签名是否合法。
     */
    public boolean verifyCallbackHash(Map<String, String> params) {
        String receivedHash = params.get("hash");
        if (receivedHash == null || receivedHash.isEmpty()) return false;

        String computed = generateHash(params, props.getAppsecret());
        return receivedHash.equalsIgnoreCase(computed);
    }

    /**
     * 虎皮椒签名算法：参数按 ASCII 排序拼接后追加 appsecret，取 MD5。
     */
    public static String generateHash(Map<String, String> params, String appsecret) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        String stringA = sorted.entrySet().stream()
                .filter(e -> !"hash".equals(e.getKey()) && e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        return md5(stringA + appsecret);
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 计算失败", e);
        }
    }
}
