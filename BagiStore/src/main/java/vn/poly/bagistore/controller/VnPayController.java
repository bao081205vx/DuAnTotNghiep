package vn.poly.bagistore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.poly.bagistore.payment.VNPayService;
import vn.poly.bagistore.model.HoaDon;
import vn.poly.bagistore.model.LichSuThanhToan;
import vn.poly.bagistore.model.PhuongThucThanhToan;
import vn.poly.bagistore.repository.HoaDonRepository;
import vn.poly.bagistore.repository.LichSuThanhToanRepository;
import vn.poly.bagistore.repository.PhuongThucThanhToanRepository;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vnpay")
public class VnPayController {

    private final Logger logger = LoggerFactory.getLogger(VnPayController.class);

    private final HoaDonRepository hoaDonRepository;
    private final LichSuThanhToanRepository lichSuThanhToanRepository;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    private final VNPayService vnPayService;

    public VnPayController(HoaDonRepository hoaDonRepository,
                           LichSuThanhToanRepository lichSuThanhToanRepository,
                           PhuongThucThanhToanRepository phuongThucThanhToanRepository,
                           VNPayService vnPayService) {
        this.hoaDonRepository = hoaDonRepository;
        this.lichSuThanhToanRepository = lichSuThanhToanRepository;
        this.phuongThucThanhToanRepository = phuongThucThanhToanRepository;
        this.vnPayService = vnPayService;
    }

    @GetMapping("/create")
    public ResponseEntity<?> createPaymentUrl(@RequestParam("invoiceId") Integer invoiceId, HttpServletRequest request) {
        try {
            HoaDon h = hoaDonRepository.findById(invoiceId).orElse(null);
            if (h == null) return ResponseEntity.badRequest().body(Map.of("error","Invoice not found"));
            // amount in VND (no decimals)
            double amt = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien() != null ? h.getTongTien() : 0.0;
            String amount = String.valueOf(Math.round(amt));
            String txnRef = String.valueOf(h.getId());
            // Tạo orderInfo đơn giản, tránh ký tự đặc biệt có thể gây lỗi
            String orderInfo = "Thanh toan hoa don " + (h.getMaHoaDon() != null ? h.getMaHoaDon() : String.valueOf(h.getId()));

            // Allow overriding the VNPay return URL via environment variable. If not set,
            // default to scheme://host/api/vnpay/return so the controller can handle the redirect.
            String envReturn = System.getenv("VNP_RETURN_URL");
            String returnUrl;
            if (envReturn != null && !envReturn.isEmpty()) {
                returnUrl = envReturn;
            } else {
                String scheme = request.getScheme();
                String host = request.getHeader("Host");
                returnUrl = scheme + "://" + host + "/api/vnpay/return"; // VNPay will redirect here
            }
            // Get client IP, check X-Forwarded-For header if behind proxy
            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
                clientIp = request.getHeader("X-Real-IP");
            }
            if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
                clientIp = request.getRemoteAddr();
            }
            // If X-Forwarded-For contains multiple IPs, take the first one
            if (clientIp != null && clientIp.contains(",")) {
                clientIp = clientIp.split(",")[0].trim();
            }
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = "127.0.0.1";
            }

            String url = vnPayService.buildPaymentUrl(amount, txnRef, orderInfo, returnUrl, clientIp);
            Map<String,Object> resp = new HashMap<>();
            resp.put("vnpUrl", url);
            resp.put("invoiceId", invoiceId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * Alternative endpoint: return an auto-submitting HTML form that posts the VNPay parameters to the payUrl.
     * This ensures a top-level navigation and can avoid any embedding issues in the client.
     */
    @GetMapping("/create/form")
    public ResponseEntity<String> createPaymentForm(@RequestParam("invoiceId") Integer invoiceId, HttpServletRequest request) {
        try {
            HoaDon h = hoaDonRepository.findById(invoiceId).orElse(null);
            if (h == null) return ResponseEntity.badRequest().body("Invoice not found");
            double amt = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien() != null ? h.getTongTien() : 0.0;
            String amount = String.valueOf(Math.round(amt));
            String txnRef = String.valueOf(h.getId());
            String orderInfo = "Thanh toan hoa don " + (h.getMaHoaDon() != null ? h.getMaHoaDon() : String.valueOf(h.getId()));

            String envReturn = System.getenv("VNP_RETURN_URL");
            String returnUrl;
            if (envReturn != null && !envReturn.isEmpty()) {
                returnUrl = envReturn;
            } else {
                String scheme = request.getScheme();
                String host = request.getHeader("Host");
                returnUrl = scheme + "://" + host + "/api/vnpay/return";
            }
            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
                clientIp = request.getHeader("X-Real-IP");
            }
            if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
                clientIp = request.getRemoteAddr();
            }
            if (clientIp != null && clientIp.contains(",")) clientIp = clientIp.split(",")[0].trim();

            Map<String,String> params = vnPayService.buildPaymentParams(amount, txnRef, orderInfo, returnUrl, clientIp);
            String html = vnPayService.buildAutoSubmitFormHtml(params);
            return ResponseEntity.ok().header("Content-Type", "text/html; charset=UTF-8").body(html);
        } catch (Exception e) {
            logger.error("createPaymentForm error", e);
            return ResponseEntity.status(500).body("Error building payment form: " + e.getMessage());
        }
    }

    // Dev helper: expose server-built params and multiple hash variants for debugging signature mismatches
    @GetMapping("/debug/params")
    public ResponseEntity<?> debugParams(@RequestParam("invoiceId") Integer invoiceId, HttpServletRequest request) {
        try {
            HoaDon h = hoaDonRepository.findById(invoiceId).orElse(null);
            if (h == null) return ResponseEntity.badRequest().body(Map.of("error","Invoice not found"));
            double amt = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien() != null ? h.getTongTien() : 0.0;
            String amount = String.valueOf(Math.round(amt));
            String txnRef = String.valueOf(h.getId());
            String orderInfo = "Thanh toan hoa don " + (h.getMaHoaDon() != null ? h.getMaHoaDon() : String.valueOf(h.getId()));

            String envReturn = System.getenv("VNP_RETURN_URL");
            String returnUrl;
            if (envReturn != null && !envReturn.isEmpty()) {
                returnUrl = envReturn;
            } else {
                String scheme = request.getScheme();
                String host = request.getHeader("Host");
                returnUrl = scheme + "://" + host + "/api/vnpay/return";
            }

            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
                clientIp = request.getHeader("X-Real-IP");
            }
            if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
                clientIp = request.getRemoteAddr();
            }
            if (clientIp != null && clientIp.contains(",")) clientIp = clientIp.split(",")[0].trim();
            if (clientIp == null || clientIp.isEmpty()) clientIp = "127.0.0.1";

            Map<String, Object> debug = vnPayService.buildPaymentDebug(amount, txnRef, orderInfo, returnUrl, clientIp);
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            logger.error("debugParams error", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/return")
    public ResponseEntity<String> vnpayReturn(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((k,v) -> { if (v != null && v.length>0) params.put(k, v[0]); });
            boolean valid = vnPayService.validateResponse(params);
            if (!valid) {
                logger.warn("VNPay return validation failed for params: {}", params);
            }
            String rspCode = params.getOrDefault("vnp_ResponseCode", "");
            String txnRef = params.getOrDefault("vnp_TxnRef", "");
            if (!valid) {
                return ResponseEntity.ok("Invalid checksum");
            }
            if (rspCode.equals("00")) {
                // success -> create payment history (do NOT update invoice status, keep it as "chờ xác nhận")
                Integer invoiceId = Integer.valueOf(txnRef);
                HoaDon h = hoaDonRepository.findById(invoiceId).orElse(null);
                if (h != null) {
                    PhuongThucThanhToan pt = phuongThucThanhToanRepository.findById(2).orElse(null);
                    LichSuThanhToan ls = new LichSuThanhToan();
                    ls.setHoaDon(h);
                    ls.setPhuongThucThanhToan(pt);
                    ls.setSoTienThanhToan(h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien());
                    ls.setNgayThanhToan(java.time.LocalDateTime.now());
                    ls.setTrangThai(Boolean.TRUE);
                    lichSuThanhToanRepository.save(ls);
                    // Note: Do NOT update invoice status - it remains in "chờ xác nhận" state
                    // Admin will manually confirm and update status after verification
                    logger.info("Payment recorded for invoice {} - payment history saved, status remains: {}", invoiceId, h.getTrangThai());
                } else {
                    logger.warn("Invoice not found for payment return: {}", invoiceId);
                }

                // Build frontend client success URL
                // Preference: env FRONTEND_CLIENT_SUCCESS_URL -> default to Vite dev frontend
                String frontendSuccess = System.getenv("FRONTEND_CLIENT_SUCCESS_URL");
                if (frontendSuccess == null || frontendSuccess.trim().isEmpty()) {
                    // Default to Vite dev server on localhost:5173
                    frontendSuccess = "http://localhost:5173/client/";
                }

                // Build query parameters for success page
                String amountStr = "";
                String orderCode = "";
                try {
                    Integer invId = invoiceId;
                    HoaDon hh = hoaDonRepository.findById(invId).orElse(null);
                    if (hh != null) {
                        double amt2 = hh.getTongTienSauGiam() != null ? hh.getTongTienSauGiam() : hh.getTongTien() != null ? hh.getTongTien() : 0.0;
                        amountStr = String.valueOf(Math.round(amt2));
                        orderCode = hh.getMaHoaDon() != null ? hh.getMaHoaDon() : String.valueOf(hh.getId());
                    } else {
                        amountStr = params.getOrDefault("vnp_Amount", "");
                        orderCode = txnRef;
                    }
                } catch (Exception ex) {
                    logger.warn("Failed to get invoice details", ex);
                    amountStr = params.getOrDefault("vnp_Amount", "");
                    orderCode = txnRef;
                }

                try {
                    String encodedOrder = java.net.URLEncoder.encode(orderCode, java.nio.charset.StandardCharsets.UTF_8.toString());
                    String encodedAmount = java.net.URLEncoder.encode(amountStr, java.nio.charset.StandardCharsets.UTF_8.toString());

                    // Build final redirect URL with parameters
                    String redirectUrl = frontendSuccess;
                    if (!redirectUrl.contains("?")) {
                        redirectUrl += "?";
                    } else if (!redirectUrl.endsWith("&")) {
                        redirectUrl += "&";
                    }
                    redirectUrl += "invoiceId=" + java.net.URLEncoder.encode(txnRef, "UTF-8")
                            + "&amount=" + encodedAmount + "&orderCode=" + encodedOrder;

                    logger.info("VNPay payment success - redirecting to: {}", redirectUrl);
                    // Issue an HTTP redirect to the frontend success page
                    return ResponseEntity.status(302).header("Location", redirectUrl).build();
                } catch (Exception ex) {
                    logger.error("Error building redirect URL", ex);
                    String redirectUrl = frontendSuccess + (frontendSuccess.contains("?") ? "&" : "?") + "invoiceId=" + txnRef;
                    logger.warn("VNPay redirect fallback to: {}", redirectUrl);
                    String html = "<html><head><meta http-equiv=\"refresh\" content=\"0;url=" + redirectUrl + "\"/></head><body>Redirecting...</body></html>";
                    return ResponseEntity.ok().header("Content-Type","text/html; charset=UTF-8").body(html);
                }
            } else {
                logger.warn("VNPay return with non-success code: {}", rspCode);
                return ResponseEntity.ok("Payment failed or canceled");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing VNPay return: " + e.getMessage());
        }
    }

    @PostMapping("/ipn")
    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((k,v) -> { if (v != null && v.length>0) params.put(k, v[0]); });
            boolean valid = vnPayService.validateResponse(params);
            if (!valid) {
                logger.warn("VNPay IPN validation failed for params: {}", params);
                return ResponseEntity.ok("Invalid checksum");
            }
            String rspCode = params.getOrDefault("vnp_ResponseCode", "");
            String txnRef = params.getOrDefault("vnp_TxnRef", "");
            if (rspCode.equals("00")) {
                Integer invoiceId = Integer.valueOf(txnRef);
                HoaDon h = hoaDonRepository.findById(invoiceId).orElse(null);
                if (h != null) {
                    // if payment record exists, ignore duplicate
                    java.util.List<vn.poly.bagistore.model.LichSuThanhToan> pays = hoaDonRepository.findPaymentsByHoaDonId(h.getId());
                    boolean exists = false;
                    if (pays != null) for (vn.poly.bagistore.model.LichSuThanhToan p : pays) {
                        if (p.getPhuongThucThanhToan() != null && p.getPhuongThucThanhToan().getId() != null && p.getPhuongThucThanhToan().getId() == 2) { exists = true; break; }
                    }
                    if (!exists) {
                        PhuongThucThanhToan pt = phuongThucThanhToanRepository.findById(2).orElse(null);
                        LichSuThanhToan ls = new LichSuThanhToan();
                        ls.setHoaDon(h);
                        ls.setPhuongThucThanhToan(pt);
                        ls.setSoTienThanhToan(h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien());
                        ls.setNgayThanhToan(java.time.LocalDateTime.now());
                        ls.setTrangThai(Boolean.TRUE);
                        lichSuThanhToanRepository.save(ls);
                        // Note: Do NOT update invoice status - it remains in "chờ xác nhận" state
                        // Admin will manually confirm and update status after verification
                        logger.info("IPN Payment recorded for invoice {} - payment history saved, status remains: {}", invoiceId, h.getTrangThai());
                    }
                }
                return ResponseEntity.ok("OK");
            } else {
                return ResponseEntity.ok("NOT_OK");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("ERROR");
        }
    }

    // Dev helper: dumps incoming request details (method, headers, query, params, raw body)
    @RequestMapping(value = "/debug/dump", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> debugDump(HttpServletRequest request) {
        try {
            Map<String, Object> out = new HashMap<>();
            out.put("method", request.getMethod());
            out.put("requestURL", request.getRequestURL().toString());
            out.put("queryString", request.getQueryString());
            Map<String, String> headers = new HashMap<>();
            java.util.Enumeration<String> names = request.getHeaderNames();
            while (names.hasMoreElements()) {
                String n = names.nextElement(); headers.put(n, request.getHeader(n));
            }
            out.put("headers", headers);

            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((k,v) -> { if (v != null && v.length>0) params.put(k, v[0]); });
            out.put("parameterMap", params);

            StringBuilder body = new StringBuilder();
            try {
                java.io.BufferedReader reader = request.getReader();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            } catch (Exception ex) {
                // ignore
            }
            out.put("rawBody", body.toString());

            logger.info("VNPay debug dump called: method={} query={} paramsPresent={}", request.getMethod(), request.getQueryString(), !params.isEmpty());
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    // Thêm method này vào VnPayController
    @GetMapping("/test-signature")
    public ResponseEntity<?> testSignature(@RequestParam("amount") String amount,
                                           @RequestParam("txnRef") String txnRef,
                                           HttpServletRequest request) {
        try {
            String orderInfo = "Test payment";
            String returnUrl = "http://localhost:8080/api/vnpay/return";
            String clientIp = "127.0.0.1";

            Map<String, String> params = vnPayService.buildPaymentParams(amount, txnRef, orderInfo, returnUrl, clientIp);

            // Build test URL
            String testUrl = vnPayService.buildPaymentUrl(amount, txnRef, orderInfo, returnUrl, clientIp);

            Map<String, Object> result = new HashMap<>();
            result.put("parameters", params);
            result.put("paymentUrl", testUrl);
            result.put("hashData", "Check logs for hash data details");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
