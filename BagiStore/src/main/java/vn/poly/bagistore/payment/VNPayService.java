package vn.poly.bagistore.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class VNPayService {
    private final Logger logger = LoggerFactory.getLogger(VNPayService.class);
    private final VNPayConfig config;

    public VNPayService(VNPayConfig config) {
        this.config = config;
        logger.info("VNPayService initialized with TMN: {}", config.getTmnCode());
    }

    public String buildPaymentUrl(String amount, String txnRef, String orderInfo, String returnUrl, String clientIp) {
        try {
            // Validate config first
            if (config.getTmnCode() == null || config.getTmnCode().trim().isEmpty()) {
                logger.error("VNPay TMN Code is not configured!");
                throw new RuntimeException("VNPay TMN Code not configured");
            }
            if (config.getHashSecret() == null || config.getHashSecret().trim().isEmpty()) {
                logger.error("VNPay Hash Secret is not configured!");
                throw new RuntimeException("VNPay Hash Secret not configured");
            }

            Map<String, String> vnpParams = new TreeMap<>(); // Use TreeMap for auto-sorting

            // Required parameters
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", config.getTmnCode());

            // Amount processing - FIXED to handle VND properly
            // VNPay expects amount in cents (VND * 100)
            String vnpAmount;
            try {
                // Parse as double first to handle decimal values
                double amountDouble = Double.parseDouble(amount);
                // Multiply by 100 to get cents
                long amountCents = Math.round(amountDouble * 100);
                vnpAmount = String.valueOf(amountCents);
                logger.debug("Amount converted: {} VND -> {} (cents)", amount, vnpAmount);
            } catch(Exception ex) {
                logger.error("Invalid amount format: {}", amount, ex);
                throw new RuntimeException("Invalid amount: " + amount, ex);
            }
            vnpParams.put("vnp_Amount", vnpAmount);

            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_OrderType", "other");

            // Order info - clean but keep meaningful text
            String cleanOrderInfo = orderInfo != null ? orderInfo.trim() : "Payment";
            // VNPay allows most characters, just ensure valid encoding
            if (cleanOrderInfo.isEmpty()) {
                cleanOrderInfo = "Payment";
            }
            if (cleanOrderInfo.length() > 255) {
                cleanOrderInfo = cleanOrderInfo.substring(0, 255);
            }
            vnpParams.put("vnp_OrderInfo", cleanOrderInfo);

            // Transaction reference
            vnpParams.put("vnp_TxnRef", txnRef != null ? txnRef : String.valueOf(System.currentTimeMillis()));

            // Return URL
            String useReturnUrl = (returnUrl != null && !returnUrl.isEmpty()) ?
                    returnUrl : config.getReturnUrl();
            vnpParams.put("vnp_ReturnUrl", useReturnUrl);

            // Create date
            String createDate = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            vnpParams.put("vnp_CreateDate", createDate);

            // IP Address
            if (clientIp != null && !clientIp.isEmpty()) {
                vnpParams.put("vnp_IpAddr", clientIp);
            }

            // Build query string
            StringBuilder query = new StringBuilder();
            StringBuilder hashData = new StringBuilder();
            boolean first = true;

            for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null || value.isEmpty()) continue;

                String encodedValue = urlEncode(value);
                if (!first) {
                    query.append('&');
                    hashData.append('&');
                }
                query.append(key).append('=').append(encodedValue);
                hashData.append(key).append('=').append(encodedValue);
                first = false;
            }

            // Generate secure hash
            String secureHash = hmacSHA512(config.getHashSecret(), hashData.toString());

            // Debug information
            logger.info("=== VNPay Payment URL Debug ===");
            logger.info("Hash Data: {}", hashData.toString());
            logger.info("Secure Hash: {}", secureHash);
            logger.info("TMN Code: {}", config.getTmnCode());
            logger.info("Amount (original): {}", amount);
            logger.info("Amount (converted): {}", vnpAmount);
            logger.info("===============================");

            String finalUrl = config.getPayUrl() + "?" + query.toString() + "&vnp_SecureHash=" + secureHash;
            logger.info("Final VNPay URL: {}", finalUrl);

            return finalUrl;
        } catch (Exception e) {
            logger.error("Failed to build VNPay URL", e);
            throw new RuntimeException("Failed to build VNPay payment URL", e);
        }
    }

    public Map<String, String> buildPaymentParams(String amount, String txnRef, String orderInfo, String returnUrl, String clientIp) {
        Map<String, String> params = new LinkedHashMap<>();
        try {
            // Validate config
            if (config.getTmnCode() == null || config.getTmnCode().trim().isEmpty()) {
                throw new RuntimeException("VNPay TMN Code not configured");
            }
            if (config.getHashSecret() == null || config.getHashSecret().trim().isEmpty()) {
                throw new RuntimeException("VNPay Hash Secret not configured");
            }

            Map<String, String> vnpParams = new TreeMap<>();

            // Required parameters
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", config.getTmnCode());

            // Amount in cents
            String vnpAmount;
            try {
                double amountDouble = Double.parseDouble(amount);
                long amountCents = Math.round(amountDouble * 100);
                vnpAmount = String.valueOf(amountCents);
            } catch(Exception ex) {
                throw new RuntimeException("Invalid amount: " + amount, ex);
            }
            vnpParams.put("vnp_Amount", vnpAmount);

            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_OrderType", "other");

            String cleanOrderInfo = orderInfo != null ? orderInfo.trim() : "Payment";
            if (cleanOrderInfo.isEmpty()) cleanOrderInfo = "Payment";
            if (cleanOrderInfo.length() > 255) cleanOrderInfo = cleanOrderInfo.substring(0, 255);
            vnpParams.put("vnp_OrderInfo", cleanOrderInfo);

            vnpParams.put("vnp_TxnRef", txnRef != null ? txnRef : String.valueOf(System.currentTimeMillis()));
            vnpParams.put("vnp_ReturnUrl", (returnUrl != null && !returnUrl.isEmpty()) ? returnUrl : config.getReturnUrl());

            String createDate = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            vnpParams.put("vnp_CreateDate", createDate);

            if (clientIp != null && !clientIp.isEmpty()) {
                vnpParams.put("vnp_IpAddr", clientIp);
            }

            // Build hash data and compute signature
            StringBuilder hashData = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null || value.isEmpty()) continue;

                if (!first) hashData.append('&');
                hashData.append(key).append('=').append(urlEncode(value));
                first = false;
            }

            String secureHash = hmacSHA512(config.getHashSecret(), hashData.toString());

            // Build params map for return
            for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
                String value = entry.getValue();
                if (value != null && !value.isEmpty()) {
                    params.put(entry.getKey(), value);
                }
            }
            params.put("vnp_SecureHash", secureHash);

        } catch (Exception e) {
            logger.error("Error building payment parameters", e);
            throw new RuntimeException("Failed to build payment parameters", e);
        }
        return params;
    }

    /**
     * Debug helper: builds the VNPay parameter set and computes hashes using
     * different canonicalization methods to help diagnose signature mismatches.
     * Returns a map containing raw params, encoded variants, hashData strings,
     * computed secure hashes and final URLs for each encoding style.
     */
    public Map<String, Object> buildPaymentDebug(String amount, String txnRef, String orderInfo, String returnUrl, String clientIp) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            Map<String, String> vnpParams = new TreeMap<>(); // sorted

            vnpParams.put("vnp_Version", config.getVersion());
            vnpParams.put("vnp_Command", config.getCommand());
            vnpParams.put("vnp_TmnCode", config.getTmnCode());

            // Amount: in cents (VND * 100)
            String vnpAmount;
            try {
                double amountDouble = Double.parseDouble(amount);
                long amountCents = Math.round(amountDouble * 100);
                vnpAmount = String.valueOf(amountCents);
            } catch (Exception ex) {
                logger.error("Invalid amount in debug: {}", amount, ex);
                vnpAmount = "0";
            }
            vnpParams.put("vnp_Amount", vnpAmount);
            vnpParams.put("vnp_CurrCode", config.getCurrCode());
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_OrderType", "other");

            String cleanOrderInfo = orderInfo != null ? orderInfo.trim() : "Payment";
            if (cleanOrderInfo.isEmpty()) cleanOrderInfo = "Payment";
            if (cleanOrderInfo.length() > 255) cleanOrderInfo = cleanOrderInfo.substring(0, 255);
            vnpParams.put("vnp_OrderInfo", cleanOrderInfo);

            vnpParams.put("vnp_TxnRef", txnRef != null ? txnRef : String.valueOf(System.currentTimeMillis()));
            vnpParams.put("vnp_ReturnUrl", (returnUrl != null && !returnUrl.isEmpty()) ? returnUrl : config.getReturnUrl());
            String createDate = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            vnpParams.put("vnp_CreateDate", createDate);
            if (clientIp != null && !clientIp.isEmpty()) vnpParams.put("vnp_IpAddr", clientIp);
            vnpParams.put("vnp_SecureHashType", "HMACSHA512");

            // Build three variants of hashData: urlEncoded (URLEncoder), rfc3986 (with %20), raw (no encode)
            StringBuilder hashUrlEncoded = new StringBuilder();
            StringBuilder hashRfc3986 = new StringBuilder();
            StringBuilder hashRaw = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                if (v == null || v.isEmpty()) continue;
                String enc1 = urlEncode(v);
                String enc2 = rfc3986Encode(v);
                String enc3 = v; // raw
                if (!first) {
                    hashUrlEncoded.append('&'); hashRfc3986.append('&'); hashRaw.append('&');
                }
                hashUrlEncoded.append(k).append('=').append(enc1);
                hashRfc3986.append(k).append('=').append(enc2);
                hashRaw.append(k).append('=').append(enc3);
                first = false;
            }

            String hashDataUrl = hashUrlEncoded.toString();
            String hashDataRfc = hashRfc3986.toString();
            String hashDataRaw = hashRaw.toString();

            String secureUrl = hmacSHA512(config.getHashSecret(), hashDataUrl);
            String secureRfc = hmacSHA512(config.getHashSecret(), hashDataRfc);
            String secureRaw = hmacSHA512(config.getHashSecret(), hashDataRaw);

            // Build final URLs for each variant (append vnp_SecureHash)
            String queryUrl = hashDataUrl + "&vnp_SecureHash=" + secureUrl;
            String queryRfc = hashDataRfc + "&vnp_SecureHash=" + secureRfc;
            String queryRaw = hashDataRaw + "&vnp_SecureHash=" + secureRaw;

            String finalUrlUrlEncoded = config.getPayUrl() + "?" + queryUrl;
            String finalUrlRfc = config.getPayUrl() + "?" + queryRfc;
            String finalUrlRaw = config.getPayUrl() + "?" + queryRaw;

            out.put("paramsRaw", vnpParams);
            out.put("hashDataUrlEncoded", hashDataUrl);
            out.put("secureHashUrlEncoded", secureUrl);
            out.put("finalUrlUrlEncoded", finalUrlUrlEncoded);
            out.put("hashDataRfc3986", hashDataRfc);
            out.put("secureHashRfc3986", secureRfc);
            out.put("finalUrlRfc3986", finalUrlRfc);
            out.put("hashDataRaw", hashDataRaw);
            out.put("secureHashRaw", secureRaw);
            out.put("finalUrlRaw", finalUrlRaw);

        } catch (Exception e) {
            logger.error("Error building payment debug data", e);
            out.put("error", e.getMessage());
        }
        return out;
    }

    // RFC3986 style percent-encoding: encode UTF-8 then replace '+' with %20 and keep ~
    private static String rfc3986Encode(String input) {
        if (input == null) return "";
        try {
            String encoded = URLEncoder.encode(input, StandardCharsets.UTF_8.toString());
            // URLEncoder turns space into +; VNPay canonical sometimes expects %20
            encoded = encoded.replace("+", "%20");
            // Keep tilde unencoded
            encoded = encoded.replace("%7E", "~");
            return encoded;
        } catch (UnsupportedEncodingException e) {
            return input;
        }
    }

    public boolean validateResponse(Map<String, String> params) {
        try {
            logger.info("=== VNPay Validation Start ===");
            logger.info("Received params: {}", params);

            if (params == null || params.isEmpty()) {
                logger.warn("Validation failed: empty parameters");
                return false;
            }

            // Make a copy and remove secure hash
            Map<String, String> signParams = new TreeMap<>(params);
            String providedHash = signParams.remove("vnp_SecureHash");

            if (providedHash == null) {
                logger.warn("Validation failed: no vnp_SecureHash parameter");
                return false;
            }

            // Build hash data
            StringBuilder hashData = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : signParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null || value.isEmpty()) continue;

                // Skip vnp_SecureHashType if present
                if ("vnp_SecureHashType".equals(key)) continue;

                String encodedValue = urlEncode(value);
                if (!first) hashData.append('&');
                hashData.append(key).append('=').append(encodedValue);
                first = false;
            }

            String hashDataStr = hashData.toString();
            String computedHash = hmacSHA512(config.getHashSecret(), hashDataStr);

            logger.info("Hash Data for validation: {}", hashDataStr);
            logger.info("Computed Hash: {}", computedHash);
            logger.info("Provided Hash: {}", providedHash);

            boolean isValid = computedHash != null && computedHash.equalsIgnoreCase(providedHash);
            logger.info("Validation Result: {}", isValid ? "SUCCESS" : "FAILED");
            logger.info("=== VNPay Validation End ===");

            return isValid;
        } catch (Exception e) {
            logger.error("VNPay validation error", e);
            return false;
        }
    }

    // Các phương thức helper giữ nguyên
    public String buildAutoSubmitFormHtml(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html><head><meta charset=\"utf-8\"><title>Redirecting to VNPay</title></head>");
        sb.append("<body onload=\"document.forms['vnpayform'].submit()\">");
        sb.append("<form name=\"vnpayform\" method=\"GET\" action=\"").append(config.getPayUrl()).append("\">");

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (v != null) {
                sb.append("<input type=\"hidden\" name=\"").append(k)
                        .append("\" value=\"").append(htmlEscape(v)).append("\"/>");
            }
        }

        sb.append("<noscript>");
        sb.append("<p>Please click the button below to proceed with payment.</p>");
        sb.append("<button type=\"submit\">Continue to VNPay</button>");
        sb.append("</noscript>");
        sb.append("</form>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private static String urlEncode(String input) {
        try {
            return URLEncoder.encode(input, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return input;
        }
    }

    private static String hmacSHA512(String key, String data) {
        try {
            if (key == null) key = "";
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hashBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC SHA512", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}