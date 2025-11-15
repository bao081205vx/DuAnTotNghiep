package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.poly.bagistore.Service.PhieuGiamGiaAppService;
import vn.poly.bagistore.model.PhieuGiamGia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/discounts")
@CrossOrigin(origins = "*")
public class PhieuGiamGiaRestController {

    @Autowired
    private PhieuGiamGiaAppService service;

    @GetMapping
    public List<Object> getAll() {
        List<PhieuGiamGia> list = service.findAll();
        return list.stream().map(d -> {
            return new java.util.HashMap<String, Object>() {{
                put("id", d.getId());
                put("maPhieu", d.getMaPhieu());
                put("tenPhieu", d.getTenPhieu());
                put("giaTriGiamGia", d.getGiaTriGiamGia());
                put("soLuong", d.getSoLuong());
                put("loaiPhieu", d.getLoaiPhieu());
                put("trangThai", d.getTrangThai());
                put("ngayBatDau", d.getNgayBatDau() != null ? d.getNgayBatDau().toString() : null);
                put("ngayKetThu", d.getNgayKetThu() != null ? d.getNgayKetThu().toString() : null);
            }};
        }).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return service.findById(id).map(d -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", d.getId());
            m.put("maPhieu", d.getMaPhieu());
            m.put("tenPhieu", d.getTenPhieu());
            m.put("giaTriGiamGia", d.getGiaTriGiamGia());
            m.put("soTienToiDa", d.getSoTienToiDa());
            m.put("hoaDonToiThieu", d.getHoaDonToiThieu());
            m.put("soLuong", d.getSoLuong());
            m.put("loaiPhieu", d.getLoaiPhieu());
            m.put("trangThai", d.getTrangThai());
            m.put("ngayBatDau", d.getNgayBatDau() != null ? d.getNgayBatDau().toString() : null);
            m.put("ngayKetThu", d.getNgayKetThu() != null ? d.getNgayKetThu().toString() : null);
            m.put("moTa", d.getMoTa());
            m.put("ngayTao", d.getNgayTao() != null ? d.getNgayTao().toString() : null);
            m.put("ngaySua", d.getNgaySua() != null ? d.getNgaySua().toString() : null);
            return ResponseEntity.ok(m);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/best")
    public ResponseEntity<?> computeBestForCart(@RequestBody java.util.Map<String, Object> payload) {
        try {
            double subtotal = 0.0;
            if (payload.containsKey("subtotal") && payload.get("subtotal") != null) {
                try { subtotal = Double.parseDouble(String.valueOf(payload.get("subtotal"))); } catch(Exception ex) { subtotal = 0.0; }
            }

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            List<PhieuGiamGia> list = service.findAll();

            double bestValue = 0.0;
            PhieuGiamGia best = null;

            for (PhieuGiamGia d : list) {
                if (d == null) continue;
                // only consider active/ongoing coupons
                String status = d.getTrangThai();
                if (status == null || !status.toLowerCase().contains("đang")) continue;
                if (d.getNgayBatDau() != null && now.isBefore(d.getNgayBatDau())) continue;
                if (d.getNgayKetThu() != null && now.isAfter(d.getNgayKetThu())) continue;
                // check minimum order value
                if (d.getHoaDonToiThieu() != null && subtotal < d.getHoaDonToiThieu()) continue;

                double value = 0.0;
                String loai = d.getLoaiPhieu() == null ? "" : d.getLoaiPhieu().toLowerCase();
                if (loai.contains("phần") || loai.contains("percent") || loai.contains("%")) {
                    // percentage coupon
                    value = subtotal * (d.getGiaTriGiamGia() == null ? 0.0 : d.getGiaTriGiamGia() / 100.0);
                    if (d.getSoTienToiDa() != null) value = Math.min(value, d.getSoTienToiDa());
                } else {
                    // fixed amount coupon
                    value = d.getGiaTriGiamGia() == null ? 0.0 : d.getGiaTriGiamGia();
                    if (d.getSoTienToiDa() != null) value = Math.min(value, d.getSoTienToiDa());
                }

                if (value > bestValue) {
                    bestValue = value;
                    best = d;
                }
            }

            if (best == null) {
                return ResponseEntity.ok(java.util.Collections.singletonMap("found", false));
            }

            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("found", true);
            m.put("id", best.getId());
            m.put("maPhieu", best.getMaPhieu());
            m.put("tenPhieu", best.getTenPhieu());
            m.put("loaiPhieu", best.getLoaiPhieu());
            m.put("giaTriGiamGia", best.getGiaTriGiamGia());
            m.put("soTienToiDa", best.getSoTienToiDa());
            m.put("hoaDonToiThieu", best.getHoaDonToiThieu());
            m.put("discountAmount", Math.round(bestValue));
            if (best.getLoaiPhieu() != null && best.getLoaiPhieu().toLowerCase().contains("phần")) m.put("percent", best.getGiaTriGiamGia());
            else m.put("amount", best.getGiaTriGiamGia());

            return ResponseEntity.ok(m);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Collections.singletonMap("error", ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody java.util.Map<String, Object> payload) {
        try {
            PhieuGiamGia d = new PhieuGiamGia();

            // maPhieu: accept client-provided code if present. Ensure the DB column is writable (not a computed column).
            if (payload.containsKey("maPhieu") && payload.get("maPhieu") != null && !String.valueOf(payload.get("maPhieu")).isEmpty()) {
                d.setMaPhieu(String.valueOf(payload.get("maPhieu")));
            }

            // tenPhieu (required)
            Object ten = payload.get("tenPhieu") != null ? payload.get("tenPhieu") : payload.get("tenPhieu") ;
            if (ten == null) return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", "tenPhieu is required"));
            d.setTenPhieu(String.valueOf(ten));

            // loaiPhieu (optional) - normalize values from frontend ('percentage'/'fixed') to Vietnamese labels
            if (payload.containsKey("loaiPhieu")) {
                String rawType = String.valueOf(payload.get("loaiPhieu")).toLowerCase();
                if (rawType.contains("percent") || rawType.contains("phan")) d.setLoaiPhieu("Phần trăm");
                else if (rawType.contains("fixed") || rawType.contains("tien") || rawType.contains("tiền") || rawType.contains("vnd")) d.setLoaiPhieu("Tiền mặt");
                else d.setLoaiPhieu(String.valueOf(payload.get("loaiPhieu")));
            }

            // giaTriGiamGia (required, numeric)
            Double gia = null;
            try {
                Object g = payload.get("giaTriGiamGia");
                if (g != null && !String.valueOf(g).isEmpty()) gia = Double.valueOf(String.valueOf(g));
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", "giaTriGiamGia must be a number"));
            }
            if (gia == null || gia <= 0.0) return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", "giaTriGiamGia must be > 0"));
            d.setGiaTriGiamGia(gia);

            // soTienToiDa (optional numeric)
            try {
                Object smax = payload.get("soTienToiDa");
                if (smax != null && !String.valueOf(smax).isEmpty()) d.setSoTienToiDa(Double.valueOf(String.valueOf(smax)));
            } catch (Exception ex) { /* ignore parse errors */ }

            // hoaDonToiThieu
            try {
                Object min = payload.get("hoaDonToiThieu");
                if (min != null && !String.valueOf(min).isEmpty()) d.setHoaDonToiThieu(Double.valueOf(String.valueOf(min)));
            } catch (Exception ex) { /* ignore */ }

            // soLuong
            try {
                Object sl = payload.get("soLuong");
                if (sl != null && !String.valueOf(sl).isEmpty()) d.setSoLuong(Integer.valueOf(String.valueOf(sl)));
            } catch (Exception ex) { /* ignore */ }

            // ngayBatDau / ngayKetThu parse as LocalDateTime if present. Accept both "yyyy-MM-dd'T'HH:mm" and full ISO
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            try {
                Object nb = payload.get("ngayBatDau");
                if (nb != null && !String.valueOf(nb).isEmpty()) {
                    String s = String.valueOf(nb);
                    if (s.length() == 16 && s.charAt(10) == 'T') s = s + ":00"; // add seconds if missing
                    d.setNgayBatDau(LocalDateTime.parse(s, fmt));
                }
            } catch (Exception ex) { /* ignore parse */ }
            try {
                Object nk = payload.get("ngayKetThu");
                if (nk != null && !String.valueOf(nk).isEmpty()) {
                    String s = String.valueOf(nk);
                    if (s.length() == 16 && s.charAt(10) == 'T') s = s + ":00";
                    d.setNgayKetThu(LocalDateTime.parse(s, fmt));
                }
            } catch (Exception ex) { /* ignore parse */ }

            // moTa
            if (payload.containsKey("moTa")) d.setMoTa(String.valueOf(payload.get("moTa")));

            // Compute trangThai (status) based on ngayBatDau and ngayKetThu relative to current time
            // Rules:
            // - now < start => "Sắp diễn ra"
            // - start <= now <= end => "Đang diễn ra"
            // - now > end => "Kết thúc"
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime start = d.getNgayBatDau();
            java.time.LocalDateTime end = d.getNgayKetThu();
            String computedStatus;
            if (start != null && end != null) {
                if (now.isBefore(start)) {
                    computedStatus = "Sắp diễn ra";
                } else if (!now.isAfter(end)) {
                    computedStatus = "Đang diễn ra";
                } else {
                    computedStatus = "Kết thúc";
                }
            } else if (start != null) {
                // only start provided
                if (now.isBefore(start)) computedStatus = "Sắp diễn ra"; else computedStatus = "Đang diễn ra";
            } else if (end != null) {
                // only end provided
                if (now.isAfter(end)) computedStatus = "Kết thúc"; else computedStatus = "Đang diễn ra";
            } else {
                // fallback to provided payload or default
                computedStatus = payload.containsKey("trangThai") ? String.valueOf(payload.get("trangThai")) : "Sắp diễn ra";
            }
            // If quantity is exhausted, mark as ended regardless of date-based computation
            if (d.getSoLuong() != null && d.getSoLuong() <= 0) {
                d.setTrangThai("Kết thúc");
            } else {
                d.setTrangThai(computedStatus);
            }

            // ngayTao
            d.setNgayTao(LocalDateTime.now());

            // If loaiPhieu == fixed and soTienToiDa is null, set it to giaTriGiamGia
            if (d.getLoaiPhieu() != null && d.getLoaiPhieu().toLowerCase().contains("fixed") || (d.getLoaiPhieu() != null && d.getLoaiPhieu().toLowerCase().contains("tiền"))) {
                if (d.getSoTienToiDa() == null) d.setSoTienToiDa(d.getGiaTriGiamGia());
            }

            PhieuGiamGia saved = service.save(d);
            // return a sanitized map to avoid lazy-loading / circular references when serializing
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", saved.getId());
            m.put("maPhieu", saved.getMaPhieu());
            m.put("tenPhieu", saved.getTenPhieu());
            m.put("giaTriGiamGia", saved.getGiaTriGiamGia());
            m.put("soTienToiDa", saved.getSoTienToiDa());
            m.put("hoaDonToiThieu", saved.getHoaDonToiThieu());
            m.put("soLuong", saved.getSoLuong());
            m.put("loaiPhieu", saved.getLoaiPhieu());
            m.put("trangThai", saved.getTrangThai());
            m.put("ngayBatDau", saved.getNgayBatDau() != null ? saved.getNgayBatDau().toString() : null);
            m.put("ngayKetThu", saved.getNgayKetThu() != null ? saved.getNgayKetThu().toString() : null);
            m.put("moTa", saved.getMoTa());
            m.put("ngayTao", saved.getNgayTao() != null ? saved.getNgayTao().toString() : null);
            m.put("ngaySua", saved.getNgaySua() != null ? saved.getNgaySua().toString() : null);
            return ResponseEntity.ok(m);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Collections.singletonMap("error", "Server error: " + ex.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id) {
        return service.findById(id).map(d -> {
            String cur = d.getTrangThai();
            if (cur == null) cur = "Ngừng hoạt động";
            if (cur.equalsIgnoreCase("Đang diễn ra") || cur.equalsIgnoreCase("active") || cur.equalsIgnoreCase("hoạt động")) {
                d.setTrangThai("Ngừng hoạt động");
            } else {
                d.setTrangThai("Đang diễn ra");
            }
            d.setNgaySua(LocalDateTime.now());
            service.save(d);
            return ResponseEntity.ok().body(java.util.Collections.singletonMap("trangThai", d.getTrangThai()));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody java.util.Map<String, Object> payload) {
        try {
            return service.findById(id).map(d -> {
                // Allow updating maPhieu if provided
                if (payload.containsKey("maPhieu") && payload.get("maPhieu") != null && !String.valueOf(payload.get("maPhieu")).isEmpty()) {
                    d.setMaPhieu(String.valueOf(payload.get("maPhieu")));
                }

                if (payload.containsKey("tenPhieu") && payload.get("tenPhieu") != null) d.setTenPhieu(String.valueOf(payload.get("tenPhieu")));

                if (payload.containsKey("loaiPhieu") && payload.get("loaiPhieu") != null) {
                    String rawType = String.valueOf(payload.get("loaiPhieu")).toLowerCase();
                    if (rawType.contains("percent") || rawType.contains("phan")) d.setLoaiPhieu("Phần trăm");
                    else if (rawType.contains("fixed") || rawType.contains("tien") || rawType.contains("tiền") || rawType.contains("vnd")) d.setLoaiPhieu("Tiền mặt");
                    else d.setLoaiPhieu(String.valueOf(payload.get("loaiPhieu")));
                }

                try { if (payload.containsKey("giaTriGiamGia") && payload.get("giaTriGiamGia") != null && !String.valueOf(payload.get("giaTriGiamGia")).isEmpty()) d.setGiaTriGiamGia(Double.valueOf(String.valueOf(payload.get("giaTriGiamGia")))); } catch(Exception ex) {}
                try { if (payload.containsKey("soTienToiDa") && payload.get("soTienToiDa") != null && !String.valueOf(payload.get("soTienToiDa")).isEmpty()) d.setSoTienToiDa(Double.valueOf(String.valueOf(payload.get("soTienToiDa")))); } catch(Exception ex) {}
                try { if (payload.containsKey("hoaDonToiThieu") && payload.get("hoaDonToiThieu") != null && !String.valueOf(payload.get("hoaDonToiThieu")).isEmpty()) d.setHoaDonToiThieu(Double.valueOf(String.valueOf(payload.get("hoaDonToiThieu")))); } catch(Exception ex) {}
                try { if (payload.containsKey("soLuong") && payload.get("soLuong") != null && !String.valueOf(payload.get("soLuong")).isEmpty()) d.setSoLuong(Integer.valueOf(String.valueOf(payload.get("soLuong")))); } catch(Exception ex) {}

                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                try {
                    Object nb = payload.get("ngayBatDau");
                    if (nb != null && !String.valueOf(nb).isEmpty()) {
                        String s = String.valueOf(nb);
                        if (s.length() == 16 && s.charAt(10) == 'T') s = s + ":00";
                        d.setNgayBatDau(java.time.LocalDateTime.parse(s, fmt));
                    }
                } catch (Exception ex) { /* ignore */ }
                try {
                    Object nk = payload.get("ngayKetThu");
                    if (nk != null && !String.valueOf(nk).isEmpty()) {
                        String s = String.valueOf(nk);
                        if (s.length() == 16 && s.charAt(10) == 'T') s = s + ":00";
                        d.setNgayKetThu(java.time.LocalDateTime.parse(s, fmt));
                    }
                } catch (Exception ex) { /* ignore */ }

                if (payload.containsKey("moTa")) d.setMoTa(payload.get("moTa") == null ? null : String.valueOf(payload.get("moTa")));

                // recompute status based on dates
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.LocalDateTime start = d.getNgayBatDau();
                java.time.LocalDateTime end = d.getNgayKetThu();
                String computedStatus;
                if (start != null && end != null) {
                    if (now.isBefore(start)) computedStatus = "Sắp diễn ra";
                    else if (!now.isAfter(end)) computedStatus = "Đang diễn ra";
                    else computedStatus = "Kết thúc";
                } else if (start != null) {
                    computedStatus = now.isBefore(start) ? "Sắp diễn ra" : "Đang diễn ra";
                } else if (end != null) {
                    computedStatus = now.isAfter(end) ? "Kết thúc" : "Đang diễn ra";
                } else {
                    computedStatus = payload.containsKey("trangThai") ? String.valueOf(payload.get("trangThai")) : d.getTrangThai();
                }
                // If quantity is exhausted, mark as ended regardless of date-based computation
                if (d.getSoLuong() != null && d.getSoLuong() <= 0) {
                    d.setTrangThai("Kết thúc");
                } else {
                    d.setTrangThai(computedStatus);
                }

                d.setNgaySua(java.time.LocalDateTime.now());
                PhieuGiamGia saved = service.save(d);
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", saved.getId());
                m.put("maPhieu", saved.getMaPhieu());
                m.put("tenPhieu", saved.getTenPhieu());
                m.put("giaTriGiamGia", saved.getGiaTriGiamGia());
                m.put("soTienToiDa", saved.getSoTienToiDa());
                m.put("hoaDonToiThieu", saved.getHoaDonToiThieu());
                m.put("soLuong", saved.getSoLuong());
                m.put("loaiPhieu", saved.getLoaiPhieu());
                m.put("trangThai", saved.getTrangThai());
                m.put("ngayBatDau", saved.getNgayBatDau() != null ? saved.getNgayBatDau().toString() : null);
                m.put("ngayKetThu", saved.getNgayKetThu() != null ? saved.getNgayKetThu().toString() : null);
                m.put("moTa", saved.getMoTa());
                m.put("ngayTao", saved.getNgayTao() != null ? saved.getNgayTao().toString() : null);
                m.put("ngaySua", saved.getNgaySua() != null ? saved.getNgaySua().toString() : null);
                return ResponseEntity.ok(m);
            }).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Collections.singletonMap("error", "Server error: " + ex.getMessage()));
        }
    }
}
