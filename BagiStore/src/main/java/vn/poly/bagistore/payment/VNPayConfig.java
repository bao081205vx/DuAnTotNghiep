package vn.poly.bagistore.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class VNPayConfig {
    private final Logger logger = LoggerFactory.getLogger(VNPayConfig.class);

    @Value("${vnpay.tmnCode:ILYVBCTM}")
    private String tmnCode;

    @Value("${vnpay.hashSecret:FRE825MCUY2PQCO15DV8MT2VF7MN70OA}")
    private String hashSecret;

    @Value("${vnpay.payUrl:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    @Value("${vnpay.returnUrl:http://localhost:8080/api/vnpay/return}")
    private String returnUrl;

    @Value("${vnpay.version:2.1.0}")
    private String version;

    @Value("${vnpay.command:pay}")
    private String command;

    @Value("${vnpay.currCode:VND}")
    private String currCode;

    // Constructor để log config
    public VNPayConfig() {
        logger.info("VNPay Config Initialized - TMN: {}, URL: {}",
                tmnCode != null ? tmnCode.substring(0, 3) + "..." : "null",
                payUrl);
    }

    public String getTmnCode() {
        if (tmnCode == null || tmnCode.trim().isEmpty()) {
            logger.error("VNPay TMN Code is null or empty!");
        }
        return tmnCode;
    }

    public String getHashSecret() {
        if (hashSecret == null || hashSecret.trim().isEmpty()) {
            logger.error("VNPay Hash Secret is null or empty!");
        }
        return hashSecret;
    }

    public String getPayUrl() { return payUrl; }
    public String getReturnUrl() { return returnUrl; }
    public String getVersion() { return version; }
    public String getCommand() { return command; }
    public String getCurrCode() { return currCode; }
}