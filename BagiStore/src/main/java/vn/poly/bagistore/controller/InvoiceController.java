package vn.poly.bagistore.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.poly.bagistore.dto.InvoiceSummaryDTO;
import vn.poly.bagistore.model.HoaDon;
import vn.poly.bagistore.repository.HoaDonRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import vn.poly.bagistore.dto.*;
import vn.poly.bagistore.dto.InvoiceItemUpdateDTO;
import vn.poly.bagistore.model.ChiTietHoaDon;


import vn.poly.bagistore.model.LichSuThanhToan;
import vn.poly.bagistore.model.DiaChiKhachHang;
import vn.poly.bagistore.model.PhuongThucThanhToan;
import vn.poly.bagistore.dto.PaymentCreateDTO;
import vn.poly.bagistore.repository.LichSuThanhToanRepository;
import vn.poly.bagistore.repository.PhuongThucThanhToanRepository;
import vn.poly.bagistore.repository.ChiTietHoaDonRepository;
import vn.poly.bagistore.repository.SanPhamChiTietRepository;
import vn.poly.bagistore.repository.KhachHangRepository;
import vn.poly.bagistore.repository.NhanVienRepository;
import vn.poly.bagistore.repository.PhieuGiamGiaRepository;
import jakarta.servlet.http.HttpServletRequest;
import vn.poly.bagistore.payment.VNPayService;
import vn.poly.bagistore.Service.StatsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import io.jsonwebtoken.Claims;
import vn.poly.bagistore.security.JwtUtil;
import vn.poly.bagistore.model.KhachHang;
import vn.poly.bagistore.model.SanPhamChiTiet;
import vn.poly.bagistore.model.NhanVien;
import vn.poly.bagistore.model.PhieuGiamGia;

@RestController
@RequestMapping("/api")
public class InvoiceController {

    private final HoaDonRepository hoaDonRepository;
    private final LichSuThanhToanRepository lichSuThanhToanRepository;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final VNPayService vnPayService;
    private final StatsService statsService;

    public InvoiceController(HoaDonRepository hoaDonRepository,
                             LichSuThanhToanRepository lichSuThanhToanRepository,
                             PhuongThucThanhToanRepository phuongThucThanhToanRepository,
                             ChiTietHoaDonRepository chiTietHoaDonRepository,
                             SanPhamChiTietRepository sanPhamChiTietRepository,
                             KhachHangRepository khachHangRepository,
                             NhanVienRepository nhanVienRepository,
                             PhieuGiamGiaRepository phieuGiamGiaRepository,
                             VNPayService vnPayService,
                             StatsService statsService) {
        this.hoaDonRepository = hoaDonRepository;
        this.lichSuThanhToanRepository = lichSuThanhToanRepository;
        this.phuongThucThanhToanRepository = phuongThucThanhToanRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
        this.khachHangRepository = khachHangRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.phieuGiamGiaRepository = phieuGiamGiaRepository;
        this.vnPayService = vnPayService;
        this.statsService = statsService;
    }

    @PutMapping("/invoices/{id}")
    public ResponseEntity<?> updateInvoice(@PathVariable Integer id, @RequestBody InvoiceUpdateDTO payload) {
        // load invoice with relations so we have access to chiTietHoaDons for stock adjustments
        HoaDon h = hoaDonRepository.findByIdWithRelations(id);
        if (h == null) return ResponseEntity.notFound().build();
        boolean changed = false;
        // snapshot original quantities per invoice line so restore uses original ordered amounts
        java.util.Map<Integer, Integer> originalQtyByChiTietId = new java.util.HashMap<>();
        if (h.getChiTietHoaDons() != null) {
            for (ChiTietHoaDon ct : h.getChiTietHoaDons()) {
                try {
                    Integer cid = ct.getId();
                    Integer q = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                    if (cid != null) originalQtyByChiTietId.put(cid, q);
                } catch (Exception e) { /* ignore per-line errors */ }
            }
        }
        // capture previous status to detect transition into 'Hủy' (canceled)
        String previousStatus = h.getTrangThai() != null ? h.getTrangThai().toString() : null;
        String newStatusCandidate = null;
        if (payload.getTrangThai() != null) { newStatusCandidate = payload.getTrangThai(); h.setTrangThai(payload.getTrangThai()); changed = true; }

        // KIỂM TRA THỨ TỰ CHUYỂN TRẠNG THÁI HỢP LỆ (VALIDATION)
        // Hóa đơn online phải tuân thủ quy trình từng bước:
        // Chờ xác nhận → Chờ giao hàng → Đang vận chuyển → Đã giao hàng → Đã thanh toán → Hoàn thành
        // (Ngoại lệ: Giao hàng thất bại có thể xảy ra từ Đang vận chuyển)
        // (Ngoại lệ: Draft invoices "Chờ thanh toán" can transition to any status)
        if (newStatusCandidate != null && previousStatus != null) {
            // Allow any transition FROM "Chờ thanh toán" (draft) status
            boolean isDraftTransition = previousStatus.contains("Chờ thanh toán") || previousStatus.contains("cho thanh toan");

            String isOnline = h.getLoaiHoaDon() != null && h.getLoaiHoaDon().equalsIgnoreCase("online") ? "yes" : "no";
            String prevNorm = normalizeStatus(previousStatus);
            String newNorm = normalizeStatus(newStatusCandidate);

            // Kiểm tra nếu hóa đơn online, chỉ cho phép chuyển tiếp theo tuần tự (không nhảy bước)
            // UNLESS transitioning FROM draft status
            if (!isDraftTransition && "yes".equals(isOnline) && !prevNorm.equals(newNorm)) {
                int prevIdx = getStatusIndex(prevNorm);
                int newIdx = getStatusIndex(newNorm);

                // ✅ KIỂM TRA THANH TOÁN TRƯỚC KHI CHUYỂN SANG "ĐÃ GIAO HÀNG"
                // Logic: Chỉ được chuyển sang "Đã giao hàng" nếu CHƯA thanh toán hoặc đã thanh toán
                // Nhưng lưu ý: Nếu đã thanh toán → sau "Đã giao hàng" phải chuyển sang "Đã thanh toán"
                if (newIdx == 4 && !newNorm.contains("giao_that_bai")) {
                    // Đang chuyển sang "Đã giao hàng" (không phải giao hàng thất bại)
                    java.util.List<LichSuThanhToan> payments = hoaDonRepository.findPaymentsByHoaDonId(h.getId());
                    double totalPaid = 0.0;
                    if (payments != null) {
                        for (LichSuThanhToan p : payments) {
                            totalPaid += p.getSoTienThanhToan() != null ? p.getSoTienThanhToan() : 0.0;
                        }
                    }
                    double invoiceTotal = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : (h.getTongTien() != null ? h.getTongTien() : 0.0);

                    // Cho phép chuyển sang "Đã giao hàng" dù chưa thanh toán hay đã thanh toán
                    // (hóa đơn offline hoặc online đều có thể giao rồi mới thanh toán sau)
                    System.out.println("Invoice " + id + " transitioning to 'Đã giao hàng' - Payment status: " + totalPaid + "/" + invoiceTotal);
                }

                boolean isValidTransition = false;
                if (newIdx == 4 && prevIdx == 3 && newNorm.contains("giao_that_bai")) {
                    // Exception: Giao hàng thất bại từ Đang vận chuyển
                    isValidTransition = true;
                } else if (newIdx == prevIdx + 1 || newIdx == prevIdx - 1) {
                    // Chuyển sang bước tiếp theo hoặc quay lại
                    isValidTransition = true;
                } else if (prevIdx == -1 || newIdx == -1) {
                    // Nếu không nhận diện được status cũ/mới, cho phép (fallback)
                    isValidTransition = true;
                }

                if (!isValidTransition) {
                    System.out.println("⚠️ INVALID STATUS TRANSITION for online invoice " + id + ": " + prevNorm + " → " + newNorm + " (prevIdx=" + prevIdx + ", newIdx=" + newIdx + ")");
                    return ResponseEntity.status(400).body(Map.of(
                            "error", "Chuyển trạng thái không hợp lệ",
                            "message", "Hóa đơn online phải tuân theo thứ tự: Chờ xác nhận → Chờ giao hàng → Đang vận chuyển → Đã giao hàng → Đã thanh toán → Hoàn thành",
                            "currentStatus", prevNorm,
                            "attemptedStatus", newNorm
                    ));
                }
            }
        }
        if (payload.getLoaiHoaDon() != null) { h.setLoaiHoaDon(payload.getLoaiHoaDon()); changed = true; }
        if (payload.getGhiChu() != null) { h.setGhiChu(payload.getGhiChu()); changed = true; }
        // persist selected customer if provided
        if (payload.getCustomerId() != null) {
            try {
                KhachHang kh = khachHangRepository.findById(payload.getCustomerId()).orElse(null);
                if (kh != null) { h.setKhachHang(kh); changed = true; }
            } catch (Exception e) { /* ignore lookup errors */ }
        }
        // persist voucher association if provided (do not decrement quantity here)
        // Only mark for recalculation when voucher actually changes
        boolean recalcNeeded = false;
        if (payload.getPhieuGiamGiaId() != null) {
            try {
                PhieuGiamGia pg = phieuGiamGiaRepository.findById(payload.getPhieuGiamGiaId()).orElse(null);
                if (pg != null) {
                    // only consider changed if different from current association
                    if (h.getPhieuGiamGia() == null || !pg.getId().equals(h.getPhieuGiamGia().getId())) {
                        h.setPhieuGiamGia(pg);
                        changed = true;
                        recalcNeeded = true;
                    }
                }
            } catch (Exception e) { /* ignore */ }
        }
        // process item quantity updates if provided
        if (payload.getItems() != null && !payload.getItems().isEmpty()) {
            // ensure we have loaded chiTietHoaDons
            if (h.getChiTietHoaDons() != null) {
                for (InvoiceItemUpdateDTO iu : payload.getItems()) {
                    if (iu == null || iu.getId() == null) continue;
                    for (ChiTietHoaDon ct : h.getChiTietHoaDons()) {
                        if (ct.getId() != null && ct.getId().equals(iu.getId())) {
                            if (iu.getSoLuong() != null && !iu.getSoLuong().equals(ct.getSoLuong())) {
                                ct.setSoLuong(iu.getSoLuong());
                                changed = true;
                                recalcNeeded = true; // quantities changed -> totals must be recalculated
                            }
                            break;
                        }
                    }
                }
            }
        }
        if (changed) hoaDonRepository.save(h);

        // Recalculate totals only when items or voucher actually changed so we don't override
        // stored totals (and shipping fee) when the caller only updates metadata like status.
        if (recalcNeeded) {
            try {
                if (h.getChiTietHoaDons() != null) {
                    double subtotal = 0.0;
                    for (ChiTietHoaDon ct : h.getChiTietHoaDons()) {
                        if (ct == null) continue;
                        int q = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                        double price = ct.getDonGia() != null ? ct.getDonGia() : 0.0;
                        subtotal += q * price;
                    }
                    double discount = 0.0;
                    PhieuGiamGia pg = h.getPhieuGiamGia();
                    if (pg != null) {
                        String loai = pg.getLoaiPhieu() != null ? pg.getLoaiPhieu().toLowerCase() : "";
                        if (loai.contains("phần") || loai.contains("percent") || loai.contains("%")) {
                            double pct = pg.getGiaTriGiamGia() != null ? pg.getGiaTriGiamGia() : 0.0;
                            discount = Math.round(subtotal * (pct / 100.0));
                            if (pg.getSoTienToiDa() != null) discount = Math.min(discount, pg.getSoTienToiDa());
                        } else {
                            discount = pg.getGiaTriGiamGia() != null ? pg.getGiaTriGiamGia() : 0.0;
                            if (pg.getSoTienToiDa() != null) discount = Math.min(discount, pg.getSoTienToiDa());
                        }
                    }
                    h.setTongTien(subtotal);
                    h.setTongTienSauGiam(Math.max(0.0, subtotal - discount));
                    hoaDonRepository.save(h);
                }
            } catch (Exception e) { /* ignore recalculation errors */ }
        }

        // If invoice transitioned into canceled state, restore stock quantities for its items
        try {
            String prev = previousStatus != null ? previousStatus.toLowerCase() : null;
            String now = newStatusCandidate != null ? newStatusCandidate.toLowerCase() : (h.getTrangThai() != null ? h.getTrangThai().toString().toLowerCase() : null);

            // detect canceled and delivery-failed states (CHỈ khi status CHÍNH XÁC là hủy hoặc giao hàng thất bại)
            // Bỏ các trạng thái bình thường như "Đang vận chuyển", "Đã giao hàng", etc.
            boolean nowIsCancelled = now != null && (now.contains("hủy") || now.contains("huy") || now.contains("cancel"))
                    && !now.contains("giao") && !now.contains("vận chuyển") && !now.contains("đã");
            boolean prevIsCancelled = prev != null && (prev.contains("hủy") || prev.contains("huy") || prev.contains("cancel"))
                    && !prev.contains("giao") && !prev.contains("vận chuyển") && !prev.contains("đã");

            boolean nowIsDeliveryFailed = false;
            boolean prevIsDeliveryFailed = false;
            if (now != null) {
                // CHỈ detect "giao hàng thất bại" chính xác, không phải trạng thái khác có chứa "thất bại"
                nowIsDeliveryFailed = now.contains("giao") && (now.contains("thất bại") || now.contains("that bai") || now.contains("failed"))
                        && !now.contains("vận chuyển") && !now.contains("đang");
            }
            if (prev != null) {
                prevIsDeliveryFailed = prev.contains("giao") && (prev.contains("thất bại") || prev.contains("that bai") || prev.contains("failed"))
                        && !prev.contains("vận chuyển") && !prev.contains("đang");
            }

            System.out.println("Invoice " + id + " status transition: prev='" + prev + "' -> now='" + now + "' | isCancelled=" + nowIsCancelled + ", isDeliveryFailed=" + nowIsDeliveryFailed);

            // If invoice transitioned into canceled or delivery-failed state, restore stock quantities for its items
            if ((nowIsCancelled || nowIsDeliveryFailed) && !(prevIsCancelled || prevIsDeliveryFailed)) {
                if (h.getChiTietHoaDons() != null) {
                    for (ChiTietHoaDon ct : h.getChiTietHoaDons()) {
                        try {
                            if (ct == null) continue;
                            SanPhamChiTiet sp = ct.getSanPhamChiTiet();
                            if (sp == null) continue;
                            Integer current = sp.getSoLuong() != null ? sp.getSoLuong() : 0;
                            Integer add = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                            sp.setSoLuong(current + add);
                            sanPhamChiTietRepository.save(sp);
                        } catch (Exception e) {
                            // continue restoring other lines even if one fails
                            System.err.println("Failed to restore stock for invoice " + id + ": " + e.getMessage());
                        }
                    }
                }
                try {
                    java.util.List<LichSuThanhToan> payments = hoaDonRepository.findPaymentsByHoaDonId(h.getId());
                    double totalPaid = 0.0;
                    if (payments != null) {
                        for (LichSuThanhToan p : payments) {
                            totalPaid += p.getSoTienThanhToan() != null ? p.getSoTienThanhToan() : 0.0;
                        }
                    }
                    if (totalPaid > 0.001) {
                        PhuongThucThanhToan refundMethod = phuongThucThanhToanRepository.findById(3).orElse(null);
                        if (refundMethod != null) {
                            LichSuThanhToan refund = new LichSuThanhToan();
                            refund.setHoaDon(h);
                            refund.setPhuongThucThanhToan(refundMethod);
                            refund.setSoTienThanhToan(totalPaid);
                            refund.setNgayThanhToan(java.time.LocalDateTime.now());
                            refund.setTrangThai(Boolean.TRUE);
                            lichSuThanhToanRepository.save(refund);
                            System.out.println("Invoice " + id + " REFUND recorded: " + totalPaid + " VNĐ (reason: " + (nowIsCancelled ? "CANCELLED" : "DELIVERY_FAILED") + ")");
                        } else {
                            System.err.println("Refund method with id=3 not found; skipping refund history creation for invoice " + h.getId());
                        }
                    } else {
                        // Không có tiền thanh toán → không cần hoàn tiền
                        System.out.println("Invoice " + id + " - No refund needed (totalPaid=" + totalPaid + "), reason: " + (nowIsCancelled ? "CANCELLED" : "DELIVERY_FAILED"));
                    }
                } catch (Exception e) {
                    System.err.println("Error creating refund history for invoice " + id + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // swallow so we don't fail the update if stock restore has issues
            System.err.println("Error while attempting to restore stock on cancel/delivery-failed: " + e.getMessage());
        }

        // Check if status changed to "đã giao hàng" and if payment is complete, auto-transition to "hoàn thành"
        try {
            String prev = previousStatus != null ? previousStatus.toLowerCase() : null;
            String now = newStatusCandidate != null ? newStatusCandidate.toLowerCase() : (h.getTrangThai() != null ? h.getTrangThai().toString().toLowerCase() : null);

            // For online orders: deduct stock when transitioning FROM "chờ xác nhận" TO any other status
            try {
                boolean isOnlineOrder = h.getLoaiHoaDon() != null && h.getLoaiHoaDon().equalsIgnoreCase("online");
                boolean prevWasWaiting = prev != null && (prev.contains("chờ xác nhận") || prev.contains("cho xac nhan"));
                boolean nowIsNotWaiting = now != null && !now.contains("chờ xác nhận") && !now.contains("cho xac nhan");

                if (isOnlineOrder && prevWasWaiting && nowIsNotWaiting) {
                    // Deduct stock for all items in this online order
                    if (h.getChiTietHoaDons() != null) {
                        for (ChiTietHoaDon ct : h.getChiTietHoaDons()) {
                            try {
                                if (ct == null || ct.getSanPhamChiTiet() == null) continue;
                                SanPhamChiTiet sp = ct.getSanPhamChiTiet();
                                int qty = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                                Integer curStock = sp.getSoLuong() != null ? sp.getSoLuong() : 0;
                                sp.setSoLuong(curStock - qty);
                                sanPhamChiTietRepository.save(sp);
                                System.out.println("Online order " + h.getId() + ": deducted " + qty + " units for variant " + sp.getId());
                            } catch (Exception e) {
                                System.err.println("Failed to deduct stock for online order: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error during online order stock deduction: " + e.getMessage());
            }

            // ✅ SMART AUTO-TRANSITION LOGIC (CHỈ CHUYỂN KHI ĐỦ ĐIỀU KIỆN)
            // Logic:
            // 1. Khi chuyển sang "Đã giao hàng" + CHƯA thanh toán → CHỈ LƯU "Đã giao hàng", KHÔNG tự động chuyển tiếp
            // 2. Khi chuyển sang "Đã giao hàng" + ĐÃ thanh toán → TỰ ĐỘNG chuyển "Đã thanh toán" rồi "Hoàn thành"
            // 3. Khi update sang "Đã thanh toán" + ĐÃ giao → TỰ ĐỘNG chuyển "Hoàn thành"

            try {
                // Get payment status
                java.util.List<LichSuThanhToan> payments = hoaDonRepository.findPaymentsByHoaDonId(h.getId());
                double totalPaid = 0.0;
                if (payments != null) {
                    for (LichSuThanhToan p : payments) {
                        totalPaid += p.getSoTienThanhToan() != null ? p.getSoTienThanhToan() : 0.0;
                    }
                }
                double invoiceTotal = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : (h.getTongTien() != null ? h.getTongTien() : 0.0);
                boolean isFullyPaid = invoiceTotal >= 0 && totalPaid + 0.001 >= invoiceTotal;

                // Check current status (after potential update)
                String currentStatus = h.getTrangThai() != null ? h.getTrangThai().toString().toLowerCase() : "";
                boolean isDelivered = currentStatus.contains("đã giao") || currentStatus.contains("da giao");
                boolean isPaid = currentStatus.contains("đã thanh toán") || currentStatus.contains("da thanh toan") || currentStatus.contains("paid");
                boolean isCompleted = currentStatus.contains("hoàn thành") || currentStatus.contains("hoan thanh") || currentStatus.contains("completed");

                // RULE 1: Nếu chuyển sang "Đã giao hàng" + ĐÃ thanh toán → tự động chuyển tiếp
                if (isDelivered && !isPaid && isFullyPaid) {
                    System.out.println("💰 AUTO-TRANSITION (Đã giao hàng + Đã thanh toán): Invoice " + id + " transitioning to 'Đã thanh toán'");
                    h.setTrangThai("đã thanh toán");
                    h.setNgayThanhToan(java.time.LocalDateTime.now());

                    // Tiếp theo: chuyển "Hoàn thành"
                    System.out.println("🎉 AUTO-TRANSITION (Hoàn thành): Invoice " + id + " transitioning to 'Hoàn Thành'");
                    h.setTrangThai("Hoàn Thành");
                    hoaDonRepository.save(h);
                    System.out.println("✅ Invoice " + id + " auto-completed (delivered + fully paid)");
                }
                // RULE 2: Nếu chuyển sang "Đã thanh toán" + ĐÃ giao hàng → tự động "Hoàn thành"
                else if (isPaid && isDelivered && !isCompleted && isFullyPaid) {
                    System.out.println("🎉 AUTO-TRANSITION (Hoàn thành từ Đã thanh toán): Invoice " + id + " transitioning to 'Hoàn Thành'");
                    h.setTrangThai("Hoàn Thành");
                    hoaDonRepository.save(h);
                    System.out.println("✅ Invoice " + id + " auto-completed (delivered + paid)");
                }
                // RULE 3: Nếu chuyển sang "Đã giao hàng" nhưng CHƯA thanh toán
                else if (isDelivered && !isPaid && !isFullyPaid) {
                    System.out.println("⏳ Invoice " + id + " is delivered but NOT fully paid. Payment status: " + totalPaid + "/" + invoiceTotal + " VNĐ. Waiting for payment...");
                    hoaDonRepository.save(h);
                }
                else {
                    System.out.println("ℹ️ Invoice " + id + " status: " + currentStatus + " (Paid: " + isFullyPaid + ", Delivered: " + isDelivered + ")");
                }
            } catch (Exception e) {
                System.err.println("Error in smart auto-transition logic: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error during status change processing: " + e.getMessage());
        }

        // return updated invoice detail dto
        InvoiceDetailDTO dto = new InvoiceDetailDTO();
        dto.setId(h.getId());
        dto.setMaHoaDon(h.getMaHoaDon());
        dto.setNgayTao(h.getNgayTao());
        dto.setNgayThanhToan(h.getNgayThanhToan());
        dto.setTrangThai(h.getTrangThai());
        dto.setLoaiHoaDon(h.getLoaiHoaDon());
        dto.setGhiChu(h.getGhiChu());

        // Compute presentation totals for delivery invoices without persisting changes.
        try {
            double subtotal = 0.0;
            if (h.getChiTietHoaDons() != null) {
                for (ChiTietHoaDon ct : h.getChiTietHoaDons()) {
                    if (ct == null) continue;
                    int q = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                    double price = ct.getDonGia() != null ? ct.getDonGia() : 0.0;
                    subtotal += q * price;
                }
            }
            double storedTotal = h.getTongTien() != null ? h.getTongTien() : subtotal;
            double storedAfterDiscount = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : storedTotal;
            double discount = Math.max(0.0, storedTotal - storedAfterDiscount);
            double shipping = Math.max(0.0, storedAfterDiscount - (subtotal - discount));

            String loai = h.getLoaiHoaDon() != null ? h.getLoaiHoaDon().toLowerCase() : "";
            boolean isDelivery = loai.contains("giao") || loai.contains("delivery") || loai.contains("giao hàng") || loai.contains("cho giao");
            if (isDelivery) {
                // For delivery invoices: show subtotal and final = subtotal - discount + shipping
                dto.setTongTien(subtotal);
                dto.setTongTienSauGiam(Math.max(0.0, subtotal - discount + shipping));
            } else {
                // Keep original stored values for other invoice types
                dto.setTongTien(h.getTongTien());
                dto.setTongTienSauGiam(h.getTongTienSauGiam());
            }
        } catch (Exception ex) {
            dto.setTongTien(h.getTongTien());
            dto.setTongTienSauGiam(h.getTongTienSauGiam());
        }

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/invoices")
    public List<InvoiceSummaryDTO> getInvoices() {
        List<HoaDon> list = hoaDonRepository.findAllWithRelations();
        return list.stream().map(h -> {
            InvoiceSummaryDTO dto = new InvoiceSummaryDTO();
            dto.setId(h.getId());
            dto.setMaHoaDon(h.getMaHoaDon());
            dto.setTenKhachHang(h.getKhachHang() != null ? h.getKhachHang().getHoTenKhachHang() : null);
            dto.setTenNhanVien(h.getNhanVien() != null ? h.getNhanVien().getHoTenNhanVien() : null);
            dto.setNgayTao(h.getNgayTao());

            // compute subtotal, discount and shipping for presentation (so list matches sales UI)
            try {
                double subtotal = 0.0;
                if (h.getChiTietHoaDons() != null) {
                    for (ChiTietHoaDon ct : h.getChiTietHoaDons()) {
                        if (ct == null) continue;
                        int q = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                        double price = ct.getDonGia() != null ? ct.getDonGia() : 0.0;
                        subtotal += q * price;
                    }
                }
                double storedTotal = h.getTongTien() != null ? h.getTongTien() : subtotal;
                double storedAfterDiscount = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : storedTotal;
                double discount = Math.max(0.0, storedTotal - storedAfterDiscount);

                double shipping = 0.0;
                try {
                    if (this.statsService != null) {
                        Double calc = this.statsService.calculateShippingFee(h);
                        if (calc != null) shipping = Math.max(0.0, calc);
                    }
                } catch (Exception e) {
                    shipping = Math.max(0.0, storedAfterDiscount - (subtotal - discount));
                }

                String loai = h.getLoaiHoaDon() != null ? h.getLoaiHoaDon().toLowerCase() : "";
                boolean isDelivery = loai.contains("giao") || loai.contains("delivery") || loai.contains("giao hàng") || loai.contains("cho giao");
                if (isDelivery) {
                    dto.setTongTien(subtotal);
                    dto.setTongTienSauGiam(Math.max(0.0, subtotal - discount + shipping));
                } else {
                    dto.setTongTien(h.getTongTien());
                    dto.setTongTienSauGiam(h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien());
                }
            } catch (Exception ex) {
                dto.setTongTien(h.getTongTien());
                dto.setTongTienSauGiam(h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien());
            }

            dto.setLoaiHoaDon(h.getLoaiHoaDon());
            dto.setTrangThai(h.getTrangThai());
            return dto;
        }).collect(Collectors.toList());
    }

    @GetMapping("/customers/{customerId}/invoices")
    public List<InvoiceSummaryDTO> getInvoicesByCustomer(@PathVariable Integer customerId) {
        List<HoaDon> list = hoaDonRepository.findByKhachHangIdWithRelations(customerId);
        return list.stream().map(h -> {
            InvoiceSummaryDTO dto = new InvoiceSummaryDTO();
            dto.setId(h.getId());
            dto.setMaHoaDon(h.getMaHoaDon());
            dto.setTenKhachHang(h.getKhachHang() != null ? h.getKhachHang().getHoTenKhachHang() : null);
            dto.setTenNhanVien(h.getNhanVien() != null ? h.getNhanVien().getHoTenNhanVien() : null);
            dto.setNgayTao(h.getNgayTao());

            try {
                double subtotal = 0.0;
                if (h.getChiTietHoaDons() != null) {
                    for (ChiTietHoaDon ct : h.getChiTietHoaDons()) {
                        if (ct == null) continue;
                        int q = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                        double price = ct.getDonGia() != null ? ct.getDonGia() : 0.0;
                        subtotal += q * price;
                    }
                }
                double storedTotal = h.getTongTien() != null ? h.getTongTien() : subtotal;
                double storedAfterDiscount = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : storedTotal;
                double discount = Math.max(0.0, storedTotal - storedAfterDiscount);

                double shipping = 0.0;
                try {
                    if (this.statsService != null) {
                        Double calc = this.statsService.calculateShippingFee(h);
                        if (calc != null) shipping = Math.max(0.0, calc);
                    }
                } catch (Exception e) {
                    shipping = Math.max(0.0, storedAfterDiscount - (subtotal - discount));
                }

                String loai = h.getLoaiHoaDon() != null ? h.getLoaiHoaDon().toLowerCase() : "";
                boolean isDelivery = loai.contains("giao") || loai.contains("delivery") || loai.contains("giao hàng") || loai.contains("cho giao");
                if (isDelivery) {
                    dto.setTongTien(subtotal);
                    dto.setTongTienSauGiam(Math.max(0.0, subtotal - discount + shipping));
                } else {
                    dto.setTongTien(h.getTongTien());
                    dto.setTongTienSauGiam(h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien());
                }
            } catch (Exception ex) {
                dto.setTongTien(h.getTongTien());
                dto.setTongTienSauGiam(h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien());
            }

            dto.setLoaiHoaDon(h.getLoaiHoaDon());
            dto.setTrangThai(h.getTrangThai());
            return dto;
        }).collect(Collectors.toList());
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<InvoiceDetailDTO> getInvoiceById(@PathVariable Integer id) {
        HoaDon h = hoaDonRepository.findByIdWithRelations(id);
        if (h == null) return ResponseEntity.notFound().build();

        InvoiceDetailDTO dto = new InvoiceDetailDTO();
        dto.setId(h.getId());
        dto.setMaHoaDon(h.getMaHoaDon());
        dto.setNgayTao(h.getNgayTao());
        dto.setNgayThanhToan(h.getNgayThanhToan());
        dto.setTrangThai(h.getTrangThai());
        dto.setLoaiHoaDon(h.getLoaiHoaDon());
        dto.setGhiChu(h.getGhiChu());
        dto.setTongTien(h.getTongTien());
        dto.setTongTienSauGiam(h.getTongTienSauGiam());

        if (h.getKhachHang() != null) {
            CustomerDTO c = new CustomerDTO();
            c.setId(h.getKhachHang().getId());
            c.setHoTen(h.getKhachHang().getHoTenKhachHang());
            c.setSoDienThoai(h.getKhachHang().getSoDienThoai());
            c.setEmail(h.getKhachHang().getEmail());
            // prefer the customer's default address (macDinh=true); fallback to first address
            if (h.getKhachHang().getDiaChiKhachHangs() != null && !h.getKhachHang().getDiaChiKhachHangs().isEmpty()) {
                DiaChiKhachHang addr = null;
                for (DiaChiKhachHang a : h.getKhachHang().getDiaChiKhachHangs()) {
                    try { if (a.getMacDinh() != null && a.getMacDinh()) { addr = a; break; } } catch(Exception ex) { /* ignore */ }
                }
                if (addr == null) addr = h.getKhachHang().getDiaChiKhachHangs().get(0);
                String full = (addr.getDiaChiCuThe() != null ? addr.getDiaChiCuThe() + ", " : "")
                        + (addr.getXaPhuong() != null ? addr.getXaPhuong() + ", " : "")
                        + (addr.getQuanHuyen() != null ? addr.getQuanHuyen() + ", " : "")
                        + (addr.getThanhPhoTinh() != null ? addr.getThanhPhoTinh() : "");
                c.setDiaChi(full);
            }
            dto.setKhachHang(c);
        }

        if (h.getNhanVien() != null) {
            EmployeeDTO e = new EmployeeDTO();
            e.setId(h.getNhanVien().getId());
            e.setHoTen(h.getNhanVien().getHoTenNhanVien());
            dto.setNhanVien(e);
        }

        // items
        List<InvoiceItemDTO> items = null;
        if (h.getChiTietHoaDons() != null) {
            items = h.getChiTietHoaDons().stream().map(ct -> {
                InvoiceItemDTO it = new InvoiceItemDTO();
                it.setId(ct.getId());
                if (ct.getSanPhamChiTiet() != null) {
                    it.setVariantId(ct.getSanPhamChiTiet().getId());
                    it.setTenSanPham(ct.getSanPhamChiTiet().getSanPham() != null ? ct.getSanPhamChiTiet().getSanPham().getTenSanPham() : null);
                    it.setKichThuoc(ct.getSanPhamChiTiet().getKichThuoc() != null ? ct.getSanPhamChiTiet().getKichThuoc().getTenKichThuoc() : null);
                    it.setMauSac(ct.getSanPhamChiTiet().getMauSac() != null ? ct.getSanPhamChiTiet().getMauSac().getTenMauSac() : null);
                    // expose available stock of the variant so frontend can cap quantity changes
                    it.setSoLuongTon(ct.getSanPhamChiTiet().getSoLuong());
                    // add image URL
                    if (ct.getSanPhamChiTiet().getAnhSanPham() != null) {
                        it.setAnhUrl(ct.getSanPhamChiTiet().getAnhSanPham().getDuongDan());
                    }
                }
                // defensive: avoid NullPointerException when donGia or soLuong are null
                Integer qty = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                Double price = ct.getDonGia() != null ? ct.getDonGia() : 0.0;
                it.setSoLuong(qty);
                it.setDonGia(price);
                it.setThanhTien(price * qty);
                return it;
            }).collect(Collectors.toList());
            dto.setItems(items);
        }

        // payments history - load in separate query to avoid MultipleBagFetchException
        java.util.List<LichSuThanhToan> paymentsEntities = hoaDonRepository.findPaymentsByHoaDonId(h.getId());
        if (paymentsEntities != null && !paymentsEntities.isEmpty()) {
            List<PaymentHistoryDTO> pays = paymentsEntities.stream().map(ls -> {
                PaymentHistoryDTO p = new PaymentHistoryDTO();
                p.setId(ls.getId());
                p.setSoTien(ls.getSoTienThanhToan());
                p.setNgayThanhToan(ls.getNgayThanhToan());
                p.setHinhThuc(ls.getPhuongThucThanhToan() != null ? ls.getPhuongThucThanhToan().getTenPhuongThucThanhToan() : null);
                // Đánh dấu nếu đây là hoàn tiền (phương thức ID=3)
                boolean isRefund = ls.getPhuongThucThanhToan() != null && ls.getPhuongThucThanhToan().getId() != null && ls.getPhuongThucThanhToan().getId() == 3;
                p.setIsRefund(isRefund);
                return p;
            }).collect(Collectors.toList());
            dto.setPayments(pays);
        }

        // Adjust presentation totals for delivery invoices: Total = products - discount + shipping
        try {
            double subtotal = 0.0;
            if (h.getChiTietHoaDons() != null) {
                for (ChiTietHoaDon ct : h.getChiTietHoaDons()) {
                    if (ct == null) continue;
                    int q = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                    double price = ct.getDonGia() != null ? ct.getDonGia() : 0.0;
                    subtotal += q * price;
                }
            }
            double storedTotal = h.getTongTien() != null ? h.getTongTien() : subtotal;
            double storedAfterDiscount = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : storedTotal;
            double discount = Math.max(0.0, storedTotal - storedAfterDiscount);
            double shipping = Math.max(0.0, storedAfterDiscount - (subtotal - discount));

            String loai = h.getLoaiHoaDon() != null ? h.getLoaiHoaDon().toLowerCase() : "";
            boolean isDelivery = loai.contains("giao") || loai.contains("delivery") || loai.contains("giao hàng") || loai.contains("cho giao");
            if (isDelivery) {
                dto.setTongTien(subtotal);
                dto.setTongTienSauGiam(Math.max(0.0, subtotal - discount + shipping));
            } else {
                dto.setTongTien(h.getTongTien());
                dto.setTongTienSauGiam(h.getTongTienSauGiam());
            }
        } catch (Exception e) {
            dto.setTongTien(h.getTongTien());
            dto.setTongTienSauGiam(h.getTongTienSauGiam());
        }

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/invoices/{id}/payments")
    public ResponseEntity<PaymentHistoryDTO> addPayment(@PathVariable Integer id, @RequestBody PaymentCreateDTO payload) {
        HoaDon h = hoaDonRepository.findById(id).orElse(null);
        if (h == null) return ResponseEntity.notFound().build();

        PhuongThucThanhToan pt = null;
        if (payload.getPhuongThucId() != null) {
            pt = phuongThucThanhToanRepository.findById(payload.getPhuongThucId()).orElse(null);
        }
        if (pt == null) {
            // fallback: use first available payment method if none provided
            pt = phuongThucThanhToanRepository.findAll().stream().findFirst().orElse(null);
        }

        LichSuThanhToan ls = new LichSuThanhToan();
        ls.setHoaDon(h);
        ls.setPhuongThucThanhToan(pt);
        ls.setSoTienThanhToan(payload.getSoTien() != null ? payload.getSoTien() : 0.0);
        ls.setNgayThanhToan(java.time.LocalDateTime.now());
        ls.setTrangThai(Boolean.TRUE);
        lichSuThanhToanRepository.save(ls);

        // After saving the payment, re-calc total paid and update invoice status when appropriate.
        try {
            java.util.List<LichSuThanhToan> payments = hoaDonRepository.findPaymentsByHoaDonId(h.getId());
            double totalPaid = 0.0;
            if (payments != null) {
                for (LichSuThanhToan p : payments) {
                    totalPaid += p.getSoTienThanhToan() != null ? p.getSoTienThanhToan() : 0.0;
                }
            }
            double invoiceTotal = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : (h.getTongTien() != null ? h.getTongTien() : 0.0);
            // detect delivered state heuristically (many installations use "đã giao", "đã giao hàng" or similar)
            String status = h.getTrangThai() != null ? h.getTrangThai().toString().toLowerCase() : null;
            boolean isDelivered = status != null && (status.contains("đã giao") || status.contains("giao hàng") || status.contains("giao"));
            // consider paid if totalPaid >= invoiceTotal (allow tiny rounding tolerance)
            if (invoiceTotal >= 0 && totalPaid + 0.001 >= invoiceTotal && isDelivered) {
                h.setTrangThai("Hoàn Thành");
                h.setNgayThanhToan(java.time.LocalDateTime.now());
                try { hoaDonRepository.save(h); } catch (Exception ex) { System.err.println("Failed to update invoice status after payment: " + ex.getMessage()); }
            }
        } catch (Exception e) {
            // don't fail payment creation if status update has problems
            System.err.println("Error while checking/updating invoice paid status: " + e.getMessage());
        }

        PaymentHistoryDTO dto = new PaymentHistoryDTO();
        dto.setId(ls.getId());
        dto.setSoTien(ls.getSoTienThanhToan());
        dto.setNgayThanhToan(ls.getNgayThanhToan());
        dto.setHinhThuc(pt != null ? pt.getTenPhuongThucThanhToan() : null);
        return ResponseEntity.ok(dto);
    }

    /**
     * Cancel an invoice when it is in 'chờ xác nhận' state.
     * This will set the invoice status to 'Đã hủy' and restore stock quantities for items.
     */
    @PostMapping("/invoices/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelInvoice(@PathVariable Integer id) {
        HoaDon h = hoaDonRepository.findByIdWithRelations(id);
        if (h == null) return ResponseEntity.notFound().build();

        String status = h.getTrangThai() != null ? h.getTrangThai().toString().toLowerCase() : "";
        boolean isWaiting = status.contains("chờ xác nhận") || status.contains("cho xac nhan") || status.contains("cho xac") || status.contains("cho xac nhan");
        if (!isWaiting) {
            // reject cancelling from other states
            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("message", "Chỉ có thể hủy đơn khi trạng thái là 'chờ xác nhận'.");
            resp.put("currentStatus", h.getTrangThai());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        }

        String previous = h.getTrangThai() != null ? h.getTrangThai().toString() : null;
        h.setTrangThai("Đã hủy");
        hoaDonRepository.save(h);

        // restore stock for each invoice line
        try {
            if (h.getChiTietHoaDons() != null) {
                for (ChiTietHoaDon ct : h.getChiTietHoaDons()) {
                    try {
                        if (ct == null) continue;
                        SanPhamChiTiet sp = ct.getSanPhamChiTiet();
                        if (sp == null) continue;
                        Integer current = sp.getSoLuong() != null ? sp.getSoLuong() : 0;
                        Integer add = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                        sp.setSoLuong(current + add);
                        sanPhamChiTietRepository.save(sp);
                    } catch (Exception e) {
                        System.err.println("Failed to restore stock for invoice " + id + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error while restoring stock on cancel: " + e.getMessage());
        }

        // If invoice had payments, create a refund history entry with payment method id = 3
        try {
            java.util.List<LichSuThanhToan> payments = hoaDonRepository.findPaymentsByHoaDonId(h.getId());
            double totalPaid = 0.0;
            if (payments != null) {
                for (LichSuThanhToan p : payments) {
                    totalPaid += p.getSoTienThanhToan() != null ? p.getSoTienThanhToan() : 0.0;
                }
            }
            if (totalPaid > 0.001) {
                PhuongThucThanhToan refundMethod = phuongThucThanhToanRepository.findById(3).orElse(null);
                if (refundMethod != null) {
                    LichSuThanhToan refund = new LichSuThanhToan();
                    refund.setHoaDon(h);
                    refund.setPhuongThucThanhToan(refundMethod);
                    refund.setSoTienThanhToan(totalPaid);
                    refund.setNgayThanhToan(java.time.LocalDateTime.now());
                    refund.setTrangThai(Boolean.TRUE);
                    lichSuThanhToanRepository.save(refund);
                } else {
                    System.err.println("Refund method with id=3 not found; skipping refund history creation for invoice " + h.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating refund history on cancel for invoice " + id + ": " + e.getMessage());
        }

        // return updated invoice DTO
        return getInvoiceById(h.getId());
    }

    /**
     * Confirm an existing draft/temporary invoice: decrement voucher quantity if applied,
     * set invoice status according to type (pickup -> paid, delivery -> chờ xác nhận),
     * and persist changes transactionally.
     */
    @PostMapping("/invoices/{id}/confirm")
    @Transactional
    public ResponseEntity<?> confirmInvoice(@PathVariable Integer id) {
        HoaDon h = hoaDonRepository.findByIdWithRelations(id);
        if (h == null) return ResponseEntity.notFound().build();

        try {
            // If invoice has a voucher attached, ensure availability and decrement its quantity
            if (h.getPhieuGiamGia() != null) {
                try {
                    PhieuGiamGia pg = phieuGiamGiaRepository.findById(h.getPhieuGiamGia().getId()).orElse(null);
                    if (pg == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Phiếu giảm giá không tồn tại"));
                    }
                    Integer remain = pg.getSoLuong() != null ? pg.getSoLuong() : 0;
                    if (remain <= 0) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Phiếu giảm giá đã hết lượt sử dụng"));
                    }
                    pg.setSoLuong(remain - 1);
                    phieuGiamGiaRepository.save(pg);
                    h.setPhieuGiamGia(pg); // reassign updated entity
                } catch (Exception ex) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Không thể cập nhật phiếu giảm giá", "details", ex.getMessage()));
                }
            }

            // Recalculate totals (subtotal and total after discount) before finalizing
            try {
                double subtotal = 0.0;
                if (h.getChiTietHoaDons() != null) {
                    for (ChiTietHoaDon ct : h.getChiTietHoaDons()) {
                        if (ct == null) continue;
                        int q = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                        double price = ct.getDonGia() != null ? ct.getDonGia() : 0.0;
                        subtotal += q * price;
                    }
                }
                double discount = 0.0;
                PhieuGiamGia pg2 = h.getPhieuGiamGia();
                if (pg2 != null) {
                    String loai2 = pg2.getLoaiPhieu() != null ? pg2.getLoaiPhieu().toLowerCase() : "";
                    if (loai2.contains("phần") || loai2.contains("percent") || loai2.contains("%")) {
                        double pct = pg2.getGiaTriGiamGia() != null ? pg2.getGiaTriGiamGia() : 0.0;
                        discount = Math.round(subtotal * (pct / 100.0));
                        if (pg2.getSoTienToiDa() != null) discount = Math.min(discount, pg2.getSoTienToiDa());
                    } else {
                        discount = pg2.getGiaTriGiamGia() != null ? pg2.getGiaTriGiamGia() : 0.0;
                        if (pg2.getSoTienToiDa() != null) discount = Math.min(discount, pg2.getSoTienToiDa());
                    }
                }
                h.setTongTien(subtotal);
                h.setTongTienSauGiam(Math.max(0.0, subtotal - discount));
            } catch (Exception e) {
                // ignore calc errors but continue
                System.err.println("Failed to recalc totals on confirm: " + e.getMessage());
            }

            // Determine final status based on invoice type
            String loai = h.getLoaiHoaDon() != null ? h.getLoaiHoaDon().toLowerCase() : "";
            if (loai.contains("tại") || loai.contains("tai") || loai.contains("pickup")) {
                h.setTrangThai("Hoàn Thành");
                h.setNgayThanhToan(java.time.LocalDateTime.now());
            } else {
                // delivery / online: move to fulfillment queue so courier can pick up
                // Use the canonical display status used in the UI: "Chờ giao hàng"
                h.setTrangThai("Chờ giao hàng");
            }

            hoaDonRepository.save(h);
            return getInvoiceById(h.getId());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to confirm invoice", "details", e.getMessage()));
        }
    }

    /**
     * Create a new invoice (confirm order) with items, decrement stock, assign current employee,
     * and optionally create an initial payment record when a payment method is provided.
     */
    @PostMapping("/invoices")
    @Transactional
    public ResponseEntity<?> createInvoice(@RequestBody InvoiceCreateDTO payload, HttpServletRequest request) {
        try {
            System.err.println("===== POST /api/invoices START =====");
            System.err.println("Items in payload: " + (payload.getItems() != null ? payload.getItems().size() : 0));

            // resolve or create customer
            vn.poly.bagistore.model.KhachHang customer = null;
            if (payload.getCustomerId() != null) {
                customer = khachHangRepository.findById(payload.getCustomerId()).orElse(null);
            }
            if (customer == null) {
                customer = new vn.poly.bagistore.model.KhachHang();
                customer.setHoTenKhachHang(payload.getCustomerName() != null ? payload.getCustomerName() : "Khách lẻ");
                // ensure email not null (DB requires it). Generate guest email when not provided.
                String email = payload.getCustomerEmail();
                if (email == null || email.trim().isEmpty()) {
                    email = "guest+" + System.currentTimeMillis() + "@local";
                }
                customer.setEmail(email);
                // only set phone if provided and not empty (validation requires digits-only)
                String phone = payload.getCustomerPhone();
                if (phone != null && !phone.trim().isEmpty()) {
                    customer.setSoDienThoai(phone);
                }
                customer.setTrangThai(Boolean.TRUE);
                customer = khachHangRepository.save(customer);
            }

            // prepare invoice
            HoaDon h = new HoaDon();
            h.setKhachHang(customer);
            // if this is a pickup (tại cửa hàng), force the invoice type to that value
            if (payload.getIsPickup() != null && payload.getIsPickup()) {
                h.setLoaiHoaDon("tại cửa hàng");
            } else if (payload.getLoaiHoaDon() != null && !payload.getLoaiHoaDon().isEmpty()) {
                h.setLoaiHoaDon(payload.getLoaiHoaDon());
            } else {
                h.setLoaiHoaDon("tại cửa hàng"); // default to in-store
            }
            h.setGhiChu(payload.getGhiChu() != null ? payload.getGhiChu() : "");
            h.setTongTien(payload.getTongTien() != null ? payload.getTongTien() : 0.0);
            h.setTongTienSauGiam(payload.getTongTienSauGiam() != null ? payload.getTongTienSauGiam() : h.getTongTien());
            h.setNgayTao(java.time.LocalDateTime.now());

            // set status: Determine initial status based on invoice type and items
            // If items list is empty or null, set status to "Chờ thanh toán" (draft)
            // For pickup / in-store orders (either `isPickup` flag or loaiHoaDon containing "tại"), mark as completed
            // For delivery orders and other normal sales, start at "chờ xác nhận" so the order appears in lists/details
            boolean hasItems = payload.getItems() != null && payload.getItems().size() > 0;

            boolean isPickupFlag = (payload.getIsPickup() != null && payload.getIsPickup())
                    || (h.getLoaiHoaDon() != null && h.getLoaiHoaDon().toLowerCase().contains("tại"));

            if (!hasItems) {
                // No items yet - this is a draft invoice being created for future completion
                h.setTrangThai("Chờ thanh toán");
            } else if (isPickupFlag) {
                // Pickup / in-store with items - mark completed and set payment time
                h.setTrangThai("Hoàn Thành");
                h.setNgayThanhToan(java.time.LocalDateTime.now());
            } else {
                // Delivery or normal sales with items: start in confirmation queue so frontend shows full data
                h.setTrangThai("chờ xác nhận");
            }

            // assign current employee from JWT if available
            try {
                String auth = request.getHeader("Authorization");
                if (auth != null) {
                    if (auth.startsWith("Bearer ")) auth = auth.substring(7).trim();
                    String secret = System.getenv().getOrDefault("BAGISTORE_JWT_SECRET", "ChangeThisSecretToAStrongOneAtLeast32Chars!");
                    Claims claims = JwtUtil.parseClaims(auth, secret);
                    if (claims != null && claims.getId() != null) {
                        Integer eid = Integer.valueOf(claims.getId());
                        var opt = nhanVienRepository.findById(eid);
                        if (opt.isPresent()) h.setNhanVien(opt.get());
                    }
                }
            } catch (Exception ex) { /* ignore and continue */ }

            // persist invoice to obtain id
            h = hoaDonRepository.save(h);

            // items and stock adjustments
            if (payload.getItems() != null) {
                for (InvoiceItemCreateDTO it : payload.getItems()) {
                    if (it == null || it.getVariantId() == null || it.getSoLuong() == null) continue;
                    var optV = sanPhamChiTietRepository.findById(it.getVariantId());
                    if (!optV.isPresent()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Variant not found: " + it.getVariantId());
                    }
                    SanPhamChiTiet sp = optV.get();
                    int qty = it.getSoLuong();
                    Integer curStock = sp.getSoLuong() != null ? sp.getSoLuong() : 0;
                    if (qty > curStock) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for variant " + it.getVariantId());
                    }
                    // create line
                    ChiTietHoaDon ct = new ChiTietHoaDon();
                    ct.setHoaDon(h);
                    ct.setSanPhamChiTiet(sp);
                    ct.setSoLuong(qty);
                    ct.setDonGia(it.getDonGia() != null ? it.getDonGia() : 0.0);
                    if (it.getMoTa() != null) ct.setMoTa(it.getMoTa());
                    chiTietHoaDonRepository.save(ct);

                    // For online orders, defer stock deduction until status changes from "chờ xác nhận"
                    // For pickup/in-store orders, decrement stock immediately
                    boolean isOnlineOrder = h.getLoaiHoaDon() != null && h.getLoaiHoaDon().equalsIgnoreCase("online");
                    if (!isOnlineOrder) {
                        sp.setSoLuong(curStock - qty);
                        sanPhamChiTietRepository.save(sp);
                    }
                }
            }

            // attach coupon if provided and decrement its remaining quantity
            if (payload.getPhieuGiamGiaId() != null) {
                var opt = phieuGiamGiaRepository.findById(payload.getPhieuGiamGiaId());
                if (opt.isPresent()) {
                    PhieuGiamGia pg = opt.get();
                    Integer avail = pg.getSoLuong() != null ? pg.getSoLuong() : 0;
                    if (avail <= 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiếu giảm giá đã hết số lượng");
                    }
                    // decrement by 1 for this use and persist the coupon
                    pg.setSoLuong(avail - 1);
                    phieuGiamGiaRepository.save(pg);
                    // attach the (updated) coupon to the invoice and persist the invoice
                    pg.setSoLuong(avail - 1);
                    // if exhausted, also mark the coupon as ended
                    if (pg.getSoLuong() != null && pg.getSoLuong() <= 0) {
                        pg.setTrangThai("Kết thúc");
                    }
                    h.setPhieuGiamGia(pg);
                    h = hoaDonRepository.save(h);
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiếu giảm giá không hợp lệ");
                }
            }

            // initial payment record when payment method provided
            // Skip payment creation for draft invoices (Chờ thanh toán status with 0 amount and no items)
            double totalAmount = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : (h.getTongTien() != null ? h.getTongTien() : 0.0);
            boolean isDraftInvoice = "Chờ thanh toán".equals(h.getTrangThai()) && totalAmount == 0.0;

            if (!isDraftInvoice && payload.getPhuongThucId() != null) {
                // If VNPay (assumed id = 2) is selected, do not create payment record now.
                if (payload.getPhuongThucId().intValue() == 2) {
                    try {
                        double amt = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien() != null ? h.getTongTien() : 0.0;
                        String amount = String.valueOf(Math.round(amt));
                        String txnRef = String.valueOf(h.getId());
                        // Tạo orderInfo đơn giản, tránh ký tự đặc biệt có thể gây lỗi
                        String orderInfo = "Thanh toan hoa don " + (h.getMaHoaDon() != null ? h.getMaHoaDon() : String.valueOf(h.getId()));
                        // Allow overriding the VNPay return URL via environment variable for
                        // deployments where the backend is behind a proxy or frontend runs on a
                        // different host. If not set, fall back to scheme://host/api/vnpay/return.
                        String envReturn = System.getenv("VNP_RETURN_URL");
                        String returnUrl;
                        if (envReturn != null && !envReturn.isEmpty()) {
                            returnUrl = envReturn;
                        } else {
                            String scheme = request.getScheme();
                            String host = request.getHeader("Host");
                            returnUrl = scheme + "://" + host + "/api/vnpay/return";
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
                        java.util.Map<String, Object> resp = new java.util.HashMap<>();
                        resp.put("vnpUrl", url);
                        resp.put("invoiceId", h.getId());
                        return ResponseEntity.ok(resp);
                    } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create VNPay URL", e);
                    }
                } else {
                    PhuongThucThanhToan pt = phuongThucThanhToanRepository.findById(payload.getPhuongThucId()).orElse(null);
                    LichSuThanhToan ls = new LichSuThanhToan();
                    ls.setHoaDon(h);
                    ls.setPhuongThucThanhToan(pt);
                    ls.setSoTienThanhToan(h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien());
                    ls.setNgayThanhToan(java.time.LocalDateTime.now());
                    ls.setTrangThai(Boolean.TRUE);
                    lichSuThanhToanRepository.save(ls);
                }
            } else if (!isDraftInvoice && payload.getIsPickup() != null && payload.getIsPickup()) {
                // For pickup orders, mark as paid and create a payment history record (default to first available method)
                PhuongThucThanhToan pt = phuongThucThanhToanRepository.findAll().stream().findFirst().orElse(null);
                LichSuThanhToan ls = new LichSuThanhToan();
                ls.setHoaDon(h);
                ls.setPhuongThucThanhToan(pt);
                ls.setSoTienThanhToan(h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien());
                ls.setNgayThanhToan(java.time.LocalDateTime.now());
                ls.setTrangThai(Boolean.TRUE);
                lichSuThanhToanRepository.save(ls);
            }

            // return full invoice DTO (reuse existing getter mapping)
            System.err.println("Calling getInvoiceById(" + h.getId() + ")...");
            ResponseEntity<?> result = getInvoiceById(h.getId());
            System.err.println("===== POST /api/invoices SUCCESS =====");
            return result;
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            System.err.println("CREATE INVOICE ERROR: " + ex.getMessage());
            ex.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create invoice: " + ex.getMessage(), ex);
        }
    }

    @GetMapping(value = "/invoices/{id}/print", produces = "text/html; charset=UTF-8")
    public ResponseEntity<String> getInvoicePrintable(@PathVariable Integer id) {
        HoaDon h = hoaDonRepository.findByIdWithRelations(id);
        if (h == null) return ResponseEntity.notFound().build();

        // Build a small printable HTML. Keep styles inline to ensure consistent print rendering.
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"vi\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
        html.append("<title>Hóa đơn " + escapeHtml(h.getMaHoaDon()) + "</title>");
        html.append("<style>body{font-family:Arial,Helvetica,sans-serif;color:#111} .container{width:800px;margin:0 auto;padding:20px} .header{display:flex;align-items:center;justify-content:space-between} .logo{height:80px} h1{text-align:center;margin:8px 0} table{width:100%;border-collapse:collapse;margin-top:12px} th,td{border:1px solid #222;padding:6px;font-size:13px} th{text-align:left;background:#f3f3f3} .right{text-align:right} .totals{margin-top:12px;width:320px;float:right} .small{font-size:12px;color:#444} @media print{body{margin:0} .container{box-shadow:none}}</style>");
        html.append("</head><body>");

        html.append("<div class=\"container\">\n");
        // header area
        html.append("<div class=\"header\">\n");
        // Use logo from uploads/images so the image can be managed via backend uploads folder
        html.append("<div class=\"store\"><img src=\"/uploads/images/logo1.png\" alt=\"logo\" class=\"logo\"><div class=\"small\">Hotline: 0981808416<br/>Email: ducbao2005123@gmail.com</div></div>\n");
        html.append("<div class=\"meta right\"><strong>HÓA ĐƠN BÁN HÀNG</strong><br/>Mã hóa đơn: " + escapeHtml(h.getMaHoaDon()) + "<br/>Ngày: " + (h.getNgayTao() != null ? h.getNgayTao().toString() : "") + "</div>\n");
        html.append("</div>");

        html.append("<h1>HÓA ĐƠN BÁN HÀNG</h1>");

        // customer / shipping block
        html.append("<div class=\"small\">Người mua: " + escapeHtml(h.getKhachHang() != null ? h.getKhachHang().getHoTenKhachHang() : "") + "<br/>Địa chỉ: " + escapeHtml(getCustomerAddress(h)) + "</div>");

        // items
        html.append("<table><thead><tr><th style=\"width:40px\">STT</th><th>Tên sản phẩm</th><th style=\"width:80px\">Kích thước</th><th style=\"width:80px\">Màu</th><th style=\"width:80px\">Số lượng</th><th style=\"width:120px\" class=\"right\">Đơn giá</th><th style=\"width:140px\" class=\"right\">Thành tiền</th></tr></thead><tbody>");
        int idx = 1;
        double subtotal = 0.0;
        if (h.getChiTietHoaDons() != null) {
            for (ChiTietHoaDon ct : h.getChiTietHoaDons()) {
                final Integer qty = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                final Double price = ct.getDonGia() != null ? ct.getDonGia() : 0.0;
                final double line = price * qty;
                subtotal += line;
                String name = ct.getSanPhamChiTiet() != null && ct.getSanPhamChiTiet().getSanPham() != null ? ct.getSanPhamChiTiet().getSanPham().getTenSanPham() : "";
                String size = ct.getSanPhamChiTiet() != null && ct.getSanPhamChiTiet().getKichThuoc() != null ? ct.getSanPhamChiTiet().getKichThuoc().getTenKichThuoc() : "";
                String color = ct.getSanPhamChiTiet() != null && ct.getSanPhamChiTiet().getMauSac() != null ? ct.getSanPhamChiTiet().getMauSac().getTenMauSac() : "";
                html.append("<tr>");
                html.append("<td>" + idx + "</td>");
                html.append("<td>" + escapeHtml(name) + "</td>");
                html.append("<td>" + escapeHtml(size) + "</td>");
                html.append("<td>" + escapeHtml(color) + "</td>");
                html.append("<td class=\"right\">" + qty + "</td>");
                html.append("<td class=\"right\">" + formatVnd(price) + "</td>");
                html.append("<td class=\"right\">" + formatVnd(line) + "</td>");
                html.append("</tr>");
                idx++;
            }
        }
        html.append("</tbody></table>");

        // totals - compute presentation totals so printed invoice matches detail view
        double tongTien = h.getTongTien() != null ? h.getTongTien() : subtotal;
        double tongSauGiam = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : tongTien;
        double discount = Math.max(0, tongTien - tongSauGiam);

        // Prefer computing shipping from address using StatsService (matches frontend computeShippingFeeForDto)
        double shipping = 0.0;
        try {
            if (this.statsService != null) {
                Double calc = this.statsService.calculateShippingFee(h);
                if (calc != null) shipping = Math.max(0.0, calc);
            }
        } catch (Exception e) {
            // fallback to inferring shipping from recorded totals
            shipping = Math.max(0, tongSauGiam - (subtotal - discount));
        }
        // If invoice type indicates pickup, zero shipping
        try {
            String loai = h.getLoaiHoaDon() != null ? h.getLoaiHoaDon().toLowerCase() : "";
            if (loai.contains("tại") || loai.contains("tai") || loai.contains("pickup")) shipping = 0.0;
        } catch (Exception e) { /* ignore */ }

        double displayTotal = Math.max(0.0, subtotal - discount + shipping);

        html.append("<div class=\"totals\">" +
                "<table><tr><td>Tổng tiền hàng</td><td class=\"right\">" + formatVnd(subtotal) + "</td></tr>" +
                "<tr><td>Giảm giá</td><td class=\"right\">" + (discount > 0 ? ("-" + formatVnd(discount)) : formatVnd(Double.valueOf(0))) + "</td></tr>" +
                "<tr><td>Phí giao hàng</td><td class=\"right\">" + formatVnd(shipping) + "</td></tr>" +
                "<tr><th>Tổng cần thanh toán</th><th class=\"right\">" + formatVnd(displayTotal) + "</th></tr></table>" +
                "</div>");

        html.append("<div style=\"clear:both;margin-top:140px;text-align:center;\"><div class=\"small\">Cám ơn quý khách. Hẹn gặp lại!</div></div>");

        // auto print
        html.append("<script>window.onload=function(){setTimeout(function(){window.print();},200);}</script>");
        html.append("</div></body></html>");

        return ResponseEntity.ok().header("Content-Type", "text/html; charset=UTF-8").body(html.toString());
    }

    // helper for server-side HTML building
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#039;");
    }

    private static String formatVnd(Double amt) {
        if (amt == null) return "0 ₫";
        java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi","VN"));
        return nf.format(amt);
    }

    private static String getCustomerAddress(HoaDon h) {
        try {
            if (h.getKhachHang() != null && h.getKhachHang().getDiaChiKhachHangs() != null && !h.getKhachHang().getDiaChiKhachHangs().isEmpty()) {
                vn.poly.bagistore.model.DiaChiKhachHang addr = null;
                for (vn.poly.bagistore.model.DiaChiKhachHang a : h.getKhachHang().getDiaChiKhachHangs()) {
                    try { if (a.getMacDinh() != null && a.getMacDinh()) { addr = a; break; } } catch(Exception ex) { /* ignore */ }
                }
                if (addr == null) addr = h.getKhachHang().getDiaChiKhachHangs().get(0);
                String full = (addr.getDiaChiCuThe() != null ? addr.getDiaChiCuThe() + ", " : "")
                        + (addr.getXaPhuong() != null ? addr.getXaPhuong() + ", " : "")
                        + (addr.getQuanHuyen() != null ? addr.getQuanHuyen() + ", " : "")
                        + (addr.getThanhPhoTinh() != null ? addr.getThanhPhoTinh() : "");
                return full;
            }
        } catch (Exception e) { /* ignore */ }
        return "";
    }

    private String normalizeStatus(String status) {
        if (status == null) return "";
        String s = status.toLowerCase().trim();
        // Map various forms to canonical keys
        if (s.contains("chờ xác nhận") || s.contains("cho xac nhan") || s.contains("pending")) return "cho_xac_nhan";
        if (s.contains("chờ giao") || s.contains("cho giao")) return "cho_giao_hang";
        if (s.contains("đang vận chuyển") || s.contains("dang van chuyen") || s.contains("shipping")) return "dang_van_chuyen";
        if (s.contains("đã giao") || s.contains("da giao") || s.contains("delivered")) return "da_giao_hang";
        if (s.contains("thất bại") || s.contains("that bai") || s.contains("giao_that_bai")) return "giao_that_bai";
        if (s.contains("đã thanh toán") || s.contains("da thanh toan") || s.contains("paid")) return "da_thanh_toan";
        if (s.contains("hoàn thành") || s.contains("hoan thanh") || s.contains("completed")) return "hoan_thanh";
        if (s.contains("hủy") || s.contains("huy") || s.contains("cancel")) return "huy";
        return s;
    }


    private int getStatusIndex(String normalizedStatus) {
        if (normalizedStatus == null) return -1;
        switch(normalizedStatus) {
            case "cho_xac_nhan": return 1;
            case "cho_giao_hang": return 2;
            case "dang_van_chuyen": return 3;
            case "da_giao_hang":
            case "giao_that_bai": return 4;
            case "da_thanh_toan": return 5;
            case "hoan_thanh": return 6;
            default: return -1;
        }
    }

    // DELETE endpoint to delete an invoice by ID
    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<?> deleteInvoice(@PathVariable Integer id) {
        try {
            HoaDon h = hoaDonRepository.findById(id).orElse(null);
            if (h == null) {
                return ResponseEntity.notFound().build();
            }

            // Delete associated line items first (due to foreign key constraints)
            List<ChiTietHoaDon> items = h.getChiTietHoaDons();
            if (items != null && !items.isEmpty()) {
                chiTietHoaDonRepository.deleteAll(items);
            }

            // Delete the invoice itself
            hoaDonRepository.delete(h);

            System.err.println("Deleted invoice ID: " + id);
            return ResponseEntity.ok(Map.of("message", "Invoice deleted successfully"));
        } catch (Exception e) {
            System.err.println("Delete invoice error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete invoice: " + e.getMessage()));
        }
    }

    // POST endpoint to add item to invoice and reduce stock
    @PostMapping("/invoices/{id}/items")
    public ResponseEntity<?> addItemToInvoice(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        try {
            Integer variantId = payload.get("variantId") != null ? Integer.parseInt(String.valueOf(payload.get("variantId"))) : null;
            Integer quantity = payload.get("quantity") != null ? Integer.parseInt(String.valueOf(payload.get("quantity"))) : 1;
            Double price = payload.get("price") != null ? Double.parseDouble(String.valueOf(payload.get("price"))) : 0.0;

            if (variantId == null || quantity <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Variant ID and quantity are required"));
            }

            // Get invoice
            HoaDon h = hoaDonRepository.findById(id).orElse(null);
            if (h == null) {
                return ResponseEntity.notFound().build();
            }

            // Get variant and check stock
            SanPhamChiTiet spct = sanPhamChiTietRepository.findById(variantId).orElse(null);
            if (spct == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Variant not found"));
            }

            Integer currentStock = spct.getSoLuong() != null ? spct.getSoLuong() : 0;
            if (quantity > currentStock) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Insufficient stock. Available: " + currentStock));
            }

            // Create invoice item
            ChiTietHoaDon item = new ChiTietHoaDon();
            item.setHoaDon(h);
            item.setSanPhamChiTiet(spct);
            item.setSoLuong(quantity);
            item.setDonGia(price);

            // Save item to database
            ChiTietHoaDon savedItem = chiTietHoaDonRepository.save(item);

            // Reduce stock
            spct.setSoLuong(currentStock - quantity);
            sanPhamChiTietRepository.save(spct);

            System.err.println("Added item to invoice: variant=" + variantId + " qty=" + quantity + " price=" + price);

            return ResponseEntity.ok(Map.of(
                    "id", savedItem.getId(),
                    "message", "Item added successfully",
                    "newStock", currentStock - quantity
            ));
        } catch (Exception e) {
            System.err.println("Add item error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add item: " + e.getMessage()));
        }
    }

    // Update quantity of an invoice item and adjust stock accordingly +/-
    @PutMapping("/invoices/{invoiceId}/items/{itemId}")
    public ResponseEntity<?> updateItemQuantity(@PathVariable Integer invoiceId, @PathVariable Integer itemId, @RequestBody Map<String, Object> payload) {
        try {
            // Get the invoice item
            ChiTietHoaDon item = chiTietHoaDonRepository.findById(itemId).orElse(null);
            if (item == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Invoice item not found"));
            }

            // Verify item belongs to the invoice
            if (item.getHoaDon() == null || !item.getHoaDon().getId().equals(invoiceId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Item does not belong to this invoice"));
            }

            // Get new quantity from request
            Integer newQuantity = null;
            if (payload.get("soLuong") instanceof Number) {
                newQuantity = ((Number) payload.get("soLuong")).intValue();
            }

            if (newQuantity == null || newQuantity < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid quantity. Must be >= 0"));
            }

            // Get old quantity
            Integer oldQuantity = item.getSoLuong() != null ? item.getSoLuong() : 0;

            // Get product variant for stock adjustment
            SanPhamChiTiet spct = item.getSanPhamChiTiet();
            if (spct == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Product variant not found"));
            }

            // Calculate stock difference
            Integer quantityDifference = newQuantity - oldQuantity;
            Integer currentStock = spct.getSoLuong() != null ? spct.getSoLuong() : 0;
            Integer newStock = currentStock - quantityDifference;

            // Check if stock is sufficient for increase
            if (quantityDifference > 0 && newStock < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "error", "Insufficient stock",
                                "currentStock", currentStock,
                                "requested", newQuantity,
                                "shortage", -newStock
                        ));
            }

            // Update item quantity
            item.setSoLuong(newQuantity);
            chiTietHoaDonRepository.save(item);

            // Adjust stock in product variant
            spct.setSoLuong(newStock);
            sanPhamChiTietRepository.save(spct);

            // Update invoice total amounts
            HoaDon invoice = item.getHoaDon();
            if (invoice != null) {
                Double unitPrice = item.getDonGia() != null ? item.getDonGia() : 0.0;
                Double priceChange = unitPrice * quantityDifference;

                if (invoice.getTongTien() != null) {
                    invoice.setTongTien(invoice.getTongTien() + priceChange);
                }
                if (invoice.getTongTienSauGiam() != null) {
                    invoice.setTongTienSauGiam(invoice.getTongTienSauGiam() + priceChange);
                }
                hoaDonRepository.save(invoice);
            }

            System.err.println("Updated item " + itemId + ": quantity " + oldQuantity + " → " + newQuantity + ", stock adjusted by " + quantityDifference);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Item quantity updated",
                    "oldQuantity", oldQuantity,
                    "newQuantity", newQuantity,
                    "stockAdjusted", quantityDifference,
                    "newStock", newStock
            ));
        } catch (Exception e) {
            System.err.println("Update item quantity error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update item: " + e.getMessage()));
        }
    }

    // DELETE an invoice item and restore stock
    @DeleteMapping("/invoices/{invoiceId}/items/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteInvoiceItem(@PathVariable Integer invoiceId, @PathVariable Integer itemId) {
        try {
            // Get the invoice item
            ChiTietHoaDon item = chiTietHoaDonRepository.findById(itemId).orElse(null);
            if (item == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Invoice item not found"));
            }

            // Verify item belongs to the invoice
            if (item.getHoaDon() == null || !item.getHoaDon().getId().equals(invoiceId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Item does not belong to this invoice"));
            }

            // Get variant and restore stock
            SanPhamChiTiet variant = item.getSanPhamChiTiet();
            if (variant != null && item.getSoLuong() != null) {
                Integer currentStock = variant.getSoLuong() != null ? variant.getSoLuong() : 0;
                variant.setSoLuong(currentStock + item.getSoLuong());
                sanPhamChiTietRepository.save(variant);
            }

            // Delete the item
            chiTietHoaDonRepository.deleteById(itemId);

            // Update invoice totals
            HoaDon invoice = hoaDonRepository.findByIdWithRelations(invoiceId);
            if (invoice != null) {
                Double tongTien = 0.0;
                if (invoice.getChiTietHoaDons() != null) {
                    for (ChiTietHoaDon ct : invoice.getChiTietHoaDons()) {
                        Double price = ct.getDonGia() != null ? ct.getDonGia() : 0.0;
                        Integer qty = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                        tongTien += price * qty;
                    }
                }
                invoice.setTongTien(tongTien);
                // preserve any existing discount
                if (invoice.getTongTienSauGiam() == null) {
                    invoice.setTongTienSauGiam(tongTien);
                }
                hoaDonRepository.save(invoice);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Item deleted successfully",
                    "updatedTotal", invoice != null ? invoice.getTongTien() : 0.0,
                    "newStock", variant != null ? variant.getSoLuong() : 0
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete item: " + e.getMessage()));
        }
    }

}
