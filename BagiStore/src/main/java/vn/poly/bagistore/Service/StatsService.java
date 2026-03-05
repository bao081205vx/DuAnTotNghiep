package vn.poly.bagistore.Service;

import org.springframework.stereotype.Service;
import vn.poly.bagistore.dto.*;
import vn.poly.bagistore.model.HoaDon;
import vn.poly.bagistore.repository.HoaDonRepository;
import vn.poly.bagistore.repository.ChiTietHoaDonRepository;
import vn.poly.bagistore.repository.KhachHangRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final KhachHangRepository khachHangRepository;

    public StatsService(HoaDonRepository hoaDonRepository, ChiTietHoaDonRepository chiTietHoaDonRepository, KhachHangRepository khachHangRepository) {
        this.hoaDonRepository = hoaDonRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
        this.khachHangRepository = khachHangRepository;
    }

    public Double calculateShippingFee(HoaDon invoice) {
        if (invoice == null || invoice.getKhachHang() == null) return 0.0;

        // Try to get address from customer's address list
        String address = null;
        if (invoice.getKhachHang().getDiaChiKhachHangs() != null && !invoice.getKhachHang().getDiaChiKhachHangs().isEmpty()) {
            // Prefer the default address (macDinh=true) then fallback to first
            vn.poly.bagistore.model.DiaChiKhachHang dck = null;
            for (vn.poly.bagistore.model.DiaChiKhachHang a : invoice.getKhachHang().getDiaChiKhachHangs()) {
                try { if (a.getMacDinh() != null && a.getMacDinh()) { dck = a; break; } } catch(Exception ex) { /* ignore */ }
            }
            if (dck == null) dck = invoice.getKhachHang().getDiaChiKhachHangs().get(0);
            if (dck != null) {
                // Combine address parts: diaChiCuThe, xaPhuong, quanHuyen, thanhPhoTinh
                address = (dck.getDiaChiCuThe() != null ? dck.getDiaChiCuThe() : "") + " " +
                        (dck.getXaPhuong() != null ? dck.getXaPhuong() : "") + " " +
                        (dck.getQuanHuyen() != null ? dck.getQuanHuyen() : "") + " " +
                        (dck.getThanhPhoTinh() != null ? dck.getThanhPhoTinh() : "");
                address = address.trim();
            }
        }

        if (address == null || address.isEmpty()) return 0.0;

        // Map of provinces and their distance from Vinh Phuc
        // Mirror the distances used in the frontend (legacy_index.html) so server/print logic matches UI
        java.util.Map<String, Integer> distancesFromVinhPhuc = new java.util.HashMap<>();
        distancesFromVinhPhuc.put("Vĩnh Phúc", 0);
        distancesFromVinhPhuc.put("Vinh Phuc", 0);
        distancesFromVinhPhuc.put("Hà Nội", 50);
        distancesFromVinhPhuc.put("Ha Noi", 50);
        distancesFromVinhPhuc.put("Bắc Ninh", 70);
        distancesFromVinhPhuc.put("Bac Ninh", 70);
        distancesFromVinhPhuc.put("Bắc Giang", 100);
        distancesFromVinhPhuc.put("Bac Giang", 100);
        distancesFromVinhPhuc.put("Thái Nguyên", 120);
        distancesFromVinhPhuc.put("Thai Nguyen", 120);
        distancesFromVinhPhuc.put("Phú Thọ", 110);
        distancesFromVinhPhuc.put("Phu Tho", 110);
        distancesFromVinhPhuc.put("Hòa Bình", 130);
        distancesFromVinhPhuc.put("Hoa Binh", 130);
        distancesFromVinhPhuc.put("Tuyên Quang", 150);
        distancesFromVinhPhuc.put("Tuyen Quang", 150);
        distancesFromVinhPhuc.put("Yên Bái", 230);
        distancesFromVinhPhuc.put("Yen Bai", 230);
        distancesFromVinhPhuc.put("Lào Cai", 330);
        distancesFromVinhPhuc.put("Lao Cai", 330);
        distancesFromVinhPhuc.put("Hà Giang", 300);
        distancesFromVinhPhuc.put("Ha Giang", 300);
        distancesFromVinhPhuc.put("Lạng Sơn", 260);
        distancesFromVinhPhuc.put("Lang Son", 260);
        distancesFromVinhPhuc.put("Cao Bằng", 300);
        distancesFromVinhPhuc.put("Cao Bang", 300);
        distancesFromVinhPhuc.put("Bắc Kạn", 210);
        distancesFromVinhPhuc.put("Bac Kan", 210);
        distancesFromVinhPhuc.put("Thái Bình", 160);
        distancesFromVinhPhuc.put("Thai Binh", 160);
        distancesFromVinhPhuc.put("Hải Dương", 120);
        distancesFromVinhPhuc.put("Hai Duong", 120);
        distancesFromVinhPhuc.put("Hải Phòng", 160);
        distancesFromVinhPhuc.put("Hai Phong", 160);
        distancesFromVinhPhuc.put("Quảng Ninh", 200);
        distancesFromVinhPhuc.put("Quang Ninh", 200);
        distancesFromVinhPhuc.put("Nam Định", 140);
        distancesFromVinhPhuc.put("Nam Dinh", 140);
        distancesFromVinhPhuc.put("Ninh Bình", 160);
        distancesFromVinhPhuc.put("Ninh Binh", 160);
        distancesFromVinhPhuc.put("Hà Nam", 120);
        distancesFromVinhPhuc.put("Ha Nam", 120);
        distancesFromVinhPhuc.put("Nghệ An", 350);
        distancesFromVinhPhuc.put("Nghe An", 350);
        distancesFromVinhPhuc.put("Thanh Hóa", 220);
        distancesFromVinhPhuc.put("Thanh Hoa", 220);
        distancesFromVinhPhuc.put("Hà Tĩnh", 430);
        distancesFromVinhPhuc.put("Ha Tinh", 430);
        distancesFromVinhPhuc.put("Quảng Bình", 420);
        distancesFromVinhPhuc.put("Quang Binh", 420);
        distancesFromVinhPhuc.put("Quảng Trị", 570);
        distancesFromVinhPhuc.put("Quang Tri", 570);
        distancesFromVinhPhuc.put("Thừa Thiên Huế", 620);
        distancesFromVinhPhuc.put("Thua Thien Hue", 620);
        distancesFromVinhPhuc.put("Đà Nẵng", 780);
        distancesFromVinhPhuc.put("Da Nang", 780);

        double SHIPPING_BASE_FEE = 30000;
        double SHIPPING_RATE_PER_KM = 200; // or similar

        // Try to find province in address
        for (String province : distancesFromVinhPhuc.keySet()) {
            if (address.toLowerCase().contains(province.toLowerCase())) {
                int km = distancesFromVinhPhuc.get(province);
                if (km == 0) return SHIPPING_BASE_FEE;
                return Math.max(SHIPPING_BASE_FEE, SHIPPING_BASE_FEE + (km * SHIPPING_RATE_PER_KM));
            }
        }

        // Default to base fee if province not found
        return SHIPPING_BASE_FEE;
    }

    private Double calculateRevenueWithoutShipping(HoaDon invoice) {
        if (invoice == null) return 0.0;
        // Start from stored total after discount (may include shipping depending on where it was set)
        Double totalAfterDiscount = invoice.getTongTienSauGiam() != null ? invoice.getTongTienSauGiam() :
                (invoice.getTongTien() != null ? invoice.getTongTien() : 0.0);

        double result = Math.max(0.0, totalAfterDiscount);

        // If invoice is of type 'online' (case-insensitive contains), subtract computed shipping fee
        try {
            if (invoice.getLoaiHoaDon() != null && invoice.getLoaiHoaDon().toLowerCase().contains("online")) {
                Double shipping = 0.0;
                try { shipping = this.calculateShippingFee(invoice); } catch (Exception ex) { shipping = 0.0; }
                result = Math.max(0.0, result - (shipping != null ? shipping : 0.0));
            }
        } catch (Exception ex) {
            // swallow and return the base result
        }

        return result;
    }

    public StatsDTO getStatsLast7Days() {
        StatsDTO stats = new StatsDTO();

        // orders last 7 days
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(6);
        LocalDateTime from = LocalDateTime.of(fromDate, LocalTime.MIN);
        LocalDateTime to = LocalDateTime.of(today, LocalTime.MAX);

        List<HoaDon> invoices = hoaDonRepository.findByNgayTaoBetween(from, to);

        // status counts for entire year (not just 7 days)
        LocalDate yearStartDate = LocalDate.now().withDayOfYear(1);
        LocalDateTime yearFrom = LocalDateTime.of(yearStartDate, LocalTime.MIN);
        LocalDateTime yearTo = LocalDateTime.of(today, LocalTime.MAX);
        List<HoaDon> yearInvoices = hoaDonRepository.findByNgayTaoBetween(yearFrom, yearTo);

        Map<String, Long> statusMap = yearInvoices.stream()
                .collect(Collectors.groupingBy(h -> h.getTrangThai() != null ? h.getTrangThai().toString() : "unknown", Collectors.counting()));
        List<OrderStatusCountDTO> statusCounts = statusMap.entrySet().stream()
                .map(e -> new OrderStatusCountDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // revenue per day (including shipping fees)
        Map<LocalDate, Double> revenueByDay = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = fromDate.plusDays(i);
            revenueByDay.put(d, 0.0);
        }
        for (HoaDon h : invoices) {
            if (h.getNgayTao() == null) continue;
            LocalDate d = h.getNgayTao().toLocalDate();
            Double prev = revenueByDay.getOrDefault(d, 0.0);
            Double add = calculateRevenueWithoutShipping(h); // Use revenue (now includes shipping)
            revenueByDay.put(d, prev + add);
        }
        List<RevenuePointDTO> revenuePoints = revenueByDay.entrySet().stream()
                .map(e -> new RevenuePointDTO(e.getKey().getMonthValue() + "/" + e.getKey().getDayOfMonth(), e.getValue()))
                .collect(Collectors.toList());

        // top products: aggregate quantities and revenue but only for the same range (last 7 days)
        List<vn.poly.bagistore.model.ChiTietHoaDon> lines = chiTietHoaDonRepository.findAll();
        Map<Integer, long[]> prodMap = new HashMap<>(); // id -> [qty(total), revenue VND]
        Map<Integer, String> prodName = new HashMap<>();
        Map<Integer, String> prodImage = new HashMap<>();
        Map<Integer, Double> prodPrice = new HashMap<>();
        for (vn.poly.bagistore.model.ChiTietHoaDon ct : lines) {
            if (ct.getSanPhamChiTiet() == null || ct.getSanPhamChiTiet().getSanPham() == null) continue;
            if (ct.getHoaDon() == null || ct.getHoaDon().getNgayTao() == null) continue;
            // only include lines whose invoice date is within the last-7-days window
            LocalDate ld = ct.getHoaDon().getNgayTao().toLocalDate();
            if (ld.isBefore(fromDate) || ld.isAfter(today)) continue;
            Integer pid = ct.getSanPhamChiTiet().getSanPham().getId();
            String name = ct.getSanPhamChiTiet().getSanPham().getTenSanPham();
            long qty = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
            double lineRevenue = (ct.getDonGia() != null ? ct.getDonGia() : 0.0) * qty;
            long[] arr = prodMap.getOrDefault(pid, new long[]{0L, 0L});
            arr[0] = arr[0] + qty;
            // store revenue as long of VND (rounded)
            arr[1] = arr[1] + Math.round(lineRevenue);
            prodMap.put(pid, arr);
            prodName.put(pid, name);
            // try to capture an image path and unit price for this product variant
            try{
                if (ct.getSanPhamChiTiet().getAnhSanPham() != null && ct.getSanPhamChiTiet().getAnhSanPham().getDuongDan() != null) {
                    prodImage.putIfAbsent(pid, ct.getSanPhamChiTiet().getAnhSanPham().getDuongDan());
                }
            }catch(Exception ex){ /* ignore */ }
            try{
                if (ct.getDonGia() != null) prodPrice.putIfAbsent(pid, ct.getDonGia());
                else if (ct.getSanPhamChiTiet().getGia() != null) prodPrice.putIfAbsent(pid, ct.getSanPhamChiTiet().getGia());
            }catch(Exception ex){ }
        }

        List<TopProductDTO> topProducts = prodMap.entrySet().stream()
                .map(e -> {
                    Integer id = e.getKey();
                    Long qty = e.getValue()[0];
                    Double rev = Double.valueOf(e.getValue()[1]);
                    Double price = prodPrice.getOrDefault(id, 0.0);
                    String img = prodImage.get(id);
                    return new TopProductDTO(id, prodName.get(id), qty, price, rev, img);
                })
                .sorted((a,b) -> Long.compare(b.getQtySold()!=null?b.getQtySold():0L, a.getQtySold()!=null?a.getQtySold():0L))
                .limit(10)
                .collect(Collectors.toList());

        // totals
        Long totalOrders = Long.valueOf(invoices.size());
        Double totalRevenue = invoices.stream().mapToDouble(this::calculateRevenueWithoutShipping).sum(); // Include shipping fees

        // actual revenue: only from completed orders (Hoàn thành)
        Double actualRevenue = invoices.stream()
                .filter(h -> h.getTrangThai() != null &&
                        (h.getTrangThai().toLowerCase().contains("hoàn") ||
                                h.getTrangThai().toLowerCase().contains("hoan") ||
                                h.getTrangThai().toLowerCase().contains("completed")))
                .mapToDouble(this::calculateRevenueWithoutShipping)
                .sum();

        Long totalCustomers = khachHangRepository.count();

        stats.setStatusCounts(statusCounts);
        stats.setRevenue(revenuePoints);
        stats.setTopProducts(topProducts);
        stats.setTotalOrders(totalOrders);
        stats.setTotalRevenue(totalRevenue);
        stats.setActualRevenue(actualRevenue);
        stats.setTotalCustomers(totalCustomers);

        // build summary/detail rows for the UI (today / week / month / year)
        List<StatsDetailRowDTO> rows = new ArrayList<>();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime weekEnd = LocalDate.now().atTime(LocalTime.MAX);

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().atTime(LocalTime.MAX);

        LocalDateTime yearStart = LocalDate.now().withDayOfYear(1).atStartOfDay();
        LocalDateTime yearEnd = LocalDate.now().atTime(LocalTime.MAX);

        // helper to compute revenue/orders/products for a given range
        java.util.function.BiFunction<LocalDateTime, LocalDateTime, StatsDetailRowDTO> computeForRange = (f,t) -> {
            List<HoaDon> invs = hoaDonRepository.findByNgayTaoBetween(f, t);
            double rev = invs.stream().mapToDouble(this::calculateRevenueWithoutShipping).sum(); // Include shipping fees
            long ordersCount = invs.size();
            // products sold: sum quantities of lines whose parent invoice date in range
            long products = 0L;
            for (vn.poly.bagistore.model.ChiTietHoaDon ct : lines) {
                if (ct.getHoaDon() == null || ct.getHoaDon().getNgayTao() == null) continue;
                LocalDateTime invDate = ct.getHoaDon().getNgayTao();
                if (!invDate.isBefore(f) && !invDate.isAfter(t)) {
                    products += ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                }
            }
            double avg = ordersCount > 0 ? rev / ordersCount : 0.0;
            return new StatsDetailRowDTO("", rev, ordersCount, products, avg, "+0%");
        };

        StatsDetailRowDTO todayRow = computeForRange.apply(todayStart, todayEnd);
        todayRow.setLabel("Hôm nay");
        rows.add(todayRow);

        StatsDetailRowDTO weekRow = computeForRange.apply(weekStart, weekEnd);
        weekRow.setLabel("Tuần này");
        rows.add(weekRow);

        StatsDetailRowDTO monthRow = computeForRange.apply(monthStart, monthEnd);
        monthRow.setLabel("Tháng này");
        rows.add(monthRow);

        StatsDetailRowDTO yearRow = computeForRange.apply(yearStart, yearEnd);
        yearRow.setLabel("Năm nay");
        rows.add(yearRow);

        stats.setDetailRows(rows);

        return stats;
    }

    public StatsDTO getStatsByRange(String range) {
        // normalize
        String r = (range == null) ? "" : range.trim().toLowerCase();
        LocalDate today = LocalDate.now();
        LocalDateTime from;
        LocalDateTime to = LocalDateTime.of(today, LocalTime.MAX);

        switch (r) {
            case "day":
            case "today":
                from = LocalDateTime.of(today, LocalTime.MIN);
                break;
            case "week":
            case "this_week":
                java.time.DayOfWeek dow = today.getDayOfWeek();
                LocalDate weekStartDate = today.minusDays(dow.getValue() - 1);
                from = LocalDateTime.of(weekStartDate, LocalTime.MIN);
                break;
            case "month":
            case "this_month":
                LocalDate monthStart = today.withDayOfMonth(1);
                from = LocalDateTime.of(monthStart, LocalTime.MIN);
                break;
            case "year":
            case "this_year":
                LocalDate yearStartDate = today.withDayOfYear(1);
                from = LocalDateTime.of(yearStartDate, LocalTime.MIN);
                break;
            default:
                // fallback: last 7 days
                LocalDate fromDate = today.minusDays(6);
                from = LocalDateTime.of(fromDate, LocalTime.MIN);
                break;
        }

        StatsDTO stats = new StatsDTO();

        List<HoaDon> invoices = hoaDonRepository.findByNgayTaoBetween(from, to);

        // status counts for requested range
        Map<String, Long> statusMap = invoices.stream()
                .collect(Collectors.groupingBy(h -> h.getTrangThai() != null ? h.getTrangThai().toString() : "unknown", Collectors.counting()));
        List<OrderStatusCountDTO> statusCounts = statusMap.entrySet().stream()
                .map(e -> new OrderStatusCountDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // revenue per day (simple daily buckets)
        LocalDate startDate = from.toLocalDate();
        LocalDate endDate = to.toLocalDate();
        Map<LocalDate, Double> revenueByDay = new LinkedHashMap<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            revenueByDay.put(d, 0.0);
        }
        for (HoaDon h : invoices) {
            if (h.getNgayTao() == null) continue;
            LocalDate d = h.getNgayTao().toLocalDate();
            Double prev = revenueByDay.getOrDefault(d, 0.0);
            Double add = calculateRevenueWithoutShipping(h);
            revenueByDay.put(d, prev + add);
        }
        List<RevenuePointDTO> revenuePoints = revenueByDay.entrySet().stream()
                .map(e -> new RevenuePointDTO(e.getKey().getMonthValue() + "/" + e.getKey().getDayOfMonth(), e.getValue()))
                .collect(Collectors.toList());

        // top products in range
        List<vn.poly.bagistore.model.ChiTietHoaDon> lines = chiTietHoaDonRepository.findAll();
        Map<Integer, long[]> prodMap = new HashMap<>();
        Map<Integer, String> prodName = new HashMap<>();
        Map<Integer, String> prodImage = new HashMap<>();
        Map<Integer, Double> prodPrice = new HashMap<>();
        for (vn.poly.bagistore.model.ChiTietHoaDon ct : lines) {
            if (ct.getSanPhamChiTiet() == null || ct.getSanPhamChiTiet().getSanPham() == null) continue;
            if (ct.getHoaDon() == null || ct.getHoaDon().getNgayTao() == null) continue;
            LocalDate ld = ct.getHoaDon().getNgayTao().toLocalDate();
            if (ld.isBefore(startDate) || ld.isAfter(endDate)) continue;
            Integer pid = ct.getSanPhamChiTiet().getSanPham().getId();
            String name = ct.getSanPhamChiTiet().getSanPham().getTenSanPham();
            long qty = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
            double lineRevenue = (ct.getDonGia() != null ? ct.getDonGia() : 0.0) * qty;
            long[] arr = prodMap.getOrDefault(pid, new long[]{0L, 0L});
            arr[0] = arr[0] + qty;
            arr[1] = arr[1] + Math.round(lineRevenue);
            prodMap.put(pid, arr);
            prodName.put(pid, name);
            try{
                if (ct.getSanPhamChiTiet().getAnhSanPham() != null && ct.getSanPhamChiTiet().getAnhSanPham().getDuongDan() != null) {
                    prodImage.putIfAbsent(pid, ct.getSanPhamChiTiet().getAnhSanPham().getDuongDan());
                }
            }catch(Exception ex){ }
            try{
                if (ct.getDonGia() != null) prodPrice.putIfAbsent(pid, ct.getDonGia());
                else if (ct.getSanPhamChiTiet().getGia() != null) prodPrice.putIfAbsent(pid, ct.getSanPhamChiTiet().getGia());
            }catch(Exception ex){ }
        }

        List<TopProductDTO> topProducts = prodMap.entrySet().stream()
                .map(e -> {
                    Integer id = e.getKey();
                    Long qty = e.getValue()[0];
                    Double rev = Double.valueOf(e.getValue()[1]);
                    Double price = prodPrice.getOrDefault(id, 0.0);
                    String img = prodImage.get(id);
                    return new TopProductDTO(id, prodName.get(id), qty, price, rev, img);
                })
                .sorted((a,b) -> Long.compare(b.getQtySold()!=null?b.getQtySold():0L, a.getQtySold()!=null?a.getQtySold():0L))
                .limit(10)
                .collect(Collectors.toList());

        Long totalOrders = Long.valueOf(invoices.size());
        Double totalRevenue = invoices.stream().mapToDouble(this::calculateRevenueWithoutShipping).sum();
        Double actualRevenue = invoices.stream()
                .filter(h -> h.getTrangThai() != null &&
                        (h.getTrangThai().toLowerCase().contains("hoàn") ||
                                h.getTrangThai().toLowerCase().contains("hoan") ||
                                h.getTrangThai().toLowerCase().contains("completed")))
                .mapToDouble(this::calculateRevenueWithoutShipping)
                .sum();

        Long totalCustomers = khachHangRepository.count();

        stats.setStatusCounts(statusCounts);
        stats.setRevenue(revenuePoints);
        stats.setTopProducts(topProducts);
        stats.setTotalOrders(totalOrders);
        stats.setTotalRevenue(totalRevenue);
        stats.setActualRevenue(actualRevenue);
        stats.setTotalCustomers(totalCustomers);

        // keep detail rows (today/week/month/year) as existing summary for convenience
        List<StatsDetailRowDTO> rows = new ArrayList<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime weekEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime yearStart = LocalDate.now().withDayOfYear(1).atStartOfDay();
        LocalDateTime yearEnd = LocalDate.now().atTime(LocalTime.MAX);

        java.util.function.BiFunction<LocalDateTime, LocalDateTime, StatsDetailRowDTO> computeForRange = (f,t) -> {
            List<HoaDon> invs = hoaDonRepository.findByNgayTaoBetween(f, t);
            double rev = invs.stream().mapToDouble(this::calculateRevenueWithoutShipping).sum();
            long ordersCount = invs.size();
            long products = 0L;
            for (vn.poly.bagistore.model.ChiTietHoaDon ct : lines) {
                if (ct.getHoaDon() == null || ct.getHoaDon().getNgayTao() == null) continue;
                LocalDateTime invDate = ct.getHoaDon().getNgayTao();
                if (!invDate.isBefore(f) && !invDate.isAfter(t)) {
                    products += ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                }
            }
            double avg = ordersCount > 0 ? rev / ordersCount : 0.0;
            return new StatsDetailRowDTO("", rev, ordersCount, products, avg, "+0%");
        };

        StatsDetailRowDTO todayRow = computeForRange.apply(todayStart, todayEnd);
        todayRow.setLabel("Hôm nay");
        rows.add(todayRow);
        StatsDetailRowDTO weekRow = computeForRange.apply(weekStart, weekEnd);
        weekRow.setLabel("Tuần này");
        rows.add(weekRow);
        StatsDetailRowDTO monthRow = computeForRange.apply(monthStart, monthEnd);
        monthRow.setLabel("Tháng này");
        rows.add(monthRow);
        StatsDetailRowDTO yearRow = computeForRange.apply(yearStart, yearEnd);
        yearRow.setLabel("Năm nay");
        rows.add(yearRow);

        stats.setDetailRows(rows);
        return stats;
    }
}
