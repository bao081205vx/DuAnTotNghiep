package vn.poly.bagistore.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.poly.bagistore.dto.InvoiceSummaryDTO;
import vn.poly.bagistore.model.HoaDon;
import vn.poly.bagistore.repository.HoaDonRepository;

import java.util.List;
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

    public InvoiceController(HoaDonRepository hoaDonRepository,
                             LichSuThanhToanRepository lichSuThanhToanRepository,
                             PhuongThucThanhToanRepository phuongThucThanhToanRepository,
                             ChiTietHoaDonRepository chiTietHoaDonRepository,
                             SanPhamChiTietRepository sanPhamChiTietRepository,
                             KhachHangRepository khachHangRepository,
                             NhanVienRepository nhanVienRepository,
                             PhieuGiamGiaRepository phieuGiamGiaRepository) {
        this.hoaDonRepository = hoaDonRepository;
        this.lichSuThanhToanRepository = lichSuThanhToanRepository;
        this.phuongThucThanhToanRepository = phuongThucThanhToanRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
        this.khachHangRepository = khachHangRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.phieuGiamGiaRepository = phieuGiamGiaRepository;
    }

    @PutMapping("/invoices/{id}")
    public ResponseEntity<?> updateInvoice(@PathVariable Integer id, @RequestBody InvoiceUpdateDTO payload) {
        // load invoice with relations so we have access to chiTietHoaDons for stock adjustments
        HoaDon h = hoaDonRepository.findByIdWithRelations(id);
        if (h == null) return ResponseEntity.notFound().build();
        boolean changed = false;
        // capture previous status to detect transition into 'Hủy' (canceled)
        String previousStatus = h.getTrangThai() != null ? h.getTrangThai().toString() : null;
        String newStatusCandidate = null;
        if (payload.getTrangThai() != null) { newStatusCandidate = payload.getTrangThai(); h.setTrangThai(payload.getTrangThai()); changed = true; }
        if (payload.getLoaiHoaDon() != null) { h.setLoaiHoaDon(payload.getLoaiHoaDon()); changed = true; }
        if (payload.getGhiChu() != null) { h.setGhiChu(payload.getGhiChu()); changed = true; }
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
                            }
                            break;
                        }
                    }
                }
            }
        }
        if (changed) hoaDonRepository.save(h);

        // If invoice transitioned into canceled state, restore stock quantities for its items
        try {
            String prev = previousStatus != null ? previousStatus.toLowerCase() : null;
            String now = newStatusCandidate != null ? newStatusCandidate.toLowerCase() : (h.getTrangThai() != null ? h.getTrangThai().toString().toLowerCase() : null);
            if (now != null && now.contains("hủy") || (now != null && now.equals("huy"))) {
                // Only restore if previous status was not already canceled
                if (!(prev != null && (prev.contains("hủy") || prev.equals("huy")))) {
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
                }
            }
        } catch (Exception e) {
            // swallow so we don't fail the update if stock restore has issues
            System.err.println("Error while attempting to restore stock on cancel: " + e.getMessage());
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
        dto.setTongTien(h.getTongTien());
        dto.setTongTienSauGiam(h.getTongTienSauGiam());
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
            dto.setTongTien(h.getTongTien());
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
            // try to pick the first customer address if available
            if (h.getKhachHang().getDiaChiKhachHangs() != null && !h.getKhachHang().getDiaChiKhachHangs().isEmpty()) {
                DiaChiKhachHang addr = h.getKhachHang().getDiaChiKhachHangs().get(0);
                String full = (addr.getDiaChiCuThe() != null ? addr.getDiaChiCuThe() + ", " : "")
                        + (addr.getXaPhuong() != null ? addr.getXaPhuong() + ", " : "")
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
                return p;
            }).collect(Collectors.toList());
            dto.setPayments(pays);
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
                h.setTrangThai("hoàn thành");
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
     * Create a new invoice (confirm order) with items, decrement stock, assign current employee,
     * and optionally create an initial payment record when a payment method is provided.
     */
    @PostMapping("/invoices")
    @Transactional
    public ResponseEntity<InvoiceDetailDTO> createInvoice(@RequestBody InvoiceCreateDTO payload, HttpServletRequest request) {
        try {

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
                customer.setSoDienThoai(payload.getCustomerPhone());
                customer.setTrangThai(Boolean.TRUE);
                customer = khachHangRepository.save(customer);
            }

            // prepare invoice
            HoaDon h = new HoaDon();
            h.setKhachHang(customer);
            // if this is a pickup (tại cửa hàng), force the invoice type to that value
            if (payload.getIsPickup() != null && payload.getIsPickup()) {
                h.setLoaiHoaDon("tại cửa hàng");
            } else {
                h.setLoaiHoaDon(payload.getLoaiHoaDon());
            }
            h.setGhiChu(payload.getGhiChu());
            h.setTongTien(payload.getTongTien() != null ? payload.getTongTien() : 0.0);
            h.setTongTienSauGiam(payload.getTongTienSauGiam() != null ? payload.getTongTienSauGiam() : h.getTongTien());
            h.setNgayTao(java.time.LocalDateTime.now());
            // set status: if pickup (tại cửa hàng) mark completed
            if (payload.getIsPickup() != null && payload.getIsPickup()) {
                h.setTrangThai("hoàn thành");
                h.setNgayThanhToan(java.time.LocalDateTime.now());
            } else if (payload.getLoaiHoaDon() != null && payload.getLoaiHoaDon().equalsIgnoreCase("Giao hàng")) {
                // if delivery order (Giao hàng), skip directly to "Đang vận chuyển" status
                h.setTrangThai("Đang vận chuyển");
            } else {
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
                    // decrement stock
                    sp.setSoLuong(curStock - qty);
                    sanPhamChiTietRepository.save(sp);
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
            if (payload.getPhuongThucId() != null) {
                PhuongThucThanhToan pt = phuongThucThanhToanRepository.findById(payload.getPhuongThucId()).orElse(null);
                LichSuThanhToan ls = new LichSuThanhToan();
                ls.setHoaDon(h);
                ls.setPhuongThucThanhToan(pt);
                ls.setSoTienThanhToan(h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : h.getTongTien());
                ls.setNgayThanhToan(java.time.LocalDateTime.now());
                ls.setTrangThai(Boolean.TRUE);
                lichSuThanhToanRepository.save(ls);
            } else if (payload.getIsPickup() != null && payload.getIsPickup()) {
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
            return getInvoiceById(h.getId());
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create invoice", ex);
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
        html.append("<div class=\"store\"><img src=\"/uploads/images/logo2.png\" alt=\"logo\" class=\"logo\"><div class=\"small\">ĐT: 0123456789<br/>Email: fshoesweb@gmail.com</div></div>\n");
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

        // totals
        double tongTien = h.getTongTien() != null ? h.getTongTien() : subtotal;
        double tongSauGiam = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : tongTien;
        double discount = Math.max(0, tongTien - tongSauGiam);
        double shipping = Math.max(0, tongSauGiam - (subtotal - discount));

        html.append("<div class=\"totals\">" +
                "<table><tr><td>Tổng tiền hàng</td><td class=\"right\">" + formatVnd(subtotal) + "</td></tr>" +
                "<tr><td>Giảm giá</td><td class=\"right\">" + (discount > 0 ? ("-" + formatVnd(discount)) : formatVnd(Double.valueOf(0))) + "</td></tr>" +
                "<tr><td>Phí giao hàng</td><td class=\"right\">" + formatVnd(shipping) + "</td></tr>" +
                "<tr><th>Tổng cần thanh toán</th><th class=\"right\">" + formatVnd(tongSauGiam) + "</th></tr></table>" +
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
        // simple formatting using integer VND grouping
        java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi","VN"));
        return nf.format(amt);
    }

    private static String getCustomerAddress(HoaDon h) {
        try {
            if (h.getKhachHang() != null && h.getKhachHang().getDiaChiKhachHangs() != null && !h.getKhachHang().getDiaChiKhachHangs().isEmpty()) {
                vn.poly.bagistore.model.DiaChiKhachHang addr = h.getKhachHang().getDiaChiKhachHangs().get(0);
                String full = (addr.getDiaChiCuThe() != null ? addr.getDiaChiCuThe() + ", " : "") + (addr.getXaPhuong() != null ? addr.getXaPhuong() + ", " : "") + (addr.getThanhPhoTinh() != null ? addr.getThanhPhoTinh() : "");
                return full;
            }
        } catch (Exception e) { /* ignore */ }
        return "";
    }

    // Note: creation of invoice items is handled in a dedicated controller to avoid duplicate mappings.
    // If you intentionally want this controller to expose item creation, move or rename the endpoint to
    // a non-conflicting path. Left intentionally blank to avoid ambiguous mapping on application startup.
}
