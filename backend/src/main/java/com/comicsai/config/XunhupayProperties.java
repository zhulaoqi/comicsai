package com.comicsai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "xunhupay")
public class XunhupayProperties {

    private String appid = "";
    private String appsecret = "";
    private String gatewayUrl = "https://api.xunhupay.com/payment/do.html";
    private String notifyUrl = "";
    private String returnUrl = "";
    private int orderExpireMinutes = 5;

    public String getAppid() { return appid; }
    public void setAppid(String appid) { this.appid = appid; }

    public String getAppsecret() { return appsecret; }
    public void setAppsecret(String appsecret) { this.appsecret = appsecret; }

    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }

    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }

    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }

    public int getOrderExpireMinutes() { return orderExpireMinutes; }
    public void setOrderExpireMinutes(int orderExpireMinutes) { this.orderExpireMinutes = orderExpireMinutes; }
}
