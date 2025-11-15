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

    public StatsDTO getStatsLast7Days() {
        StatsDTO stats = new StatsDTO();

        // orders last 7 days
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(6);
        LocalDateTime from = LocalDateTime.of(fromDate, LocalTime.MIN);
        LocalDateTime to = LocalDateTime.of(today, LocalTime.MAX);

        List<HoaDon> invoices = hoaDonRepository.findByNgayTaoBetween(from, to);

        // status counts
        Map<String, Long> statusMap = invoices.stream()
                .collect(Collectors.groupingBy(h -> h.getTrangThai() != null ? h.getTrangThai().toString() : "unknown", Collectors.counting()));
        List<OrderStatusCountDTO> statusCounts = statusMap.entrySet().stream()
                .map(e -> new OrderStatusCountDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // revenue per day
        Map<LocalDate, Double> revenueByDay = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = fromDate.plusDays(i);
            revenueByDay.put(d, 0.0);
        }
        for (HoaDon h : invoices) {
            if (h.getNgayTao() == null) continue;
            LocalDate d = h.getNgayTao().toLocalDate();
            Double prev = revenueByDay.getOrDefault(d, 0.0);
            Double add = h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : (h.getTongTien() != null ? h.getTongTien() : 0.0);
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
        Double totalRevenue = invoices.stream().mapToDouble(h -> (h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : (h.getTongTien() != null ? h.getTongTien() : 0.0))).sum();
        Long totalCustomers = khachHangRepository.count();

        stats.setStatusCounts(statusCounts);
        stats.setRevenue(revenuePoints);
        stats.setTopProducts(topProducts);
        stats.setTotalOrders(totalOrders);
        stats.setTotalRevenue(totalRevenue);
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
            double rev = invs.stream().mapToDouble(h -> (h.getTongTienSauGiam() != null ? h.getTongTienSauGiam() : (h.getTongTien() != null ? h.getTongTien() : 0.0))).sum();
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
}
