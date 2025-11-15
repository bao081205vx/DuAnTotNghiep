package vn.poly.bagistore.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.poly.bagistore.dto.InvoiceItemDTO;
import vn.poly.bagistore.model.ChiTietHoaDon;
import vn.poly.bagistore.model.HoaDon;
import vn.poly.bagistore.model.SanPhamChiTiet;
import vn.poly.bagistore.repository.ChiTietHoaDonRepository;
import vn.poly.bagistore.repository.HoaDonRepository;
import vn.poly.bagistore.repository.SanPhamChiTietRepository;

@RestController
@RequestMapping("/api")
public class InvoiceItemController {

    private final HoaDonRepository hoaDonRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;

    public InvoiceItemController(HoaDonRepository hoaDonRepository,
                                 SanPhamChiTietRepository sanPhamChiTietRepository,
                                 ChiTietHoaDonRepository chiTietHoaDonRepository) {
        this.hoaDonRepository = hoaDonRepository;
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
    }

    @PostMapping("/invoices/{id}/items")
    public ResponseEntity<?> addItemToInvoice(@PathVariable Integer id, @RequestBody InvoiceItemDTO payload) {
        // find invoice
        HoaDon h = hoaDonRepository.findById(id).orElse(null);
        if (h == null) return ResponseEntity.notFound().build();

        SanPhamChiTiet spct = null;
        if (payload.getVariantId() != null) {
            spct = sanPhamChiTietRepository.findById(payload.getVariantId()).orElse(null);
        }

        // create new ChiTietHoaDon
        ChiTietHoaDon ct = new ChiTietHoaDon();
        ct.setHoaDon(h);
        if (spct != null) ct.setSanPhamChiTiet(spct);
        Integer qty = payload.getSoLuong() != null ? payload.getSoLuong() : 1;
        ct.setSoLuong(qty);
        Double price = payload.getDonGia() != null ? payload.getDonGia() : (spct != null ? spct.getGia() : 0.0);
        ct.setDonGia(price);

        chiTietHoaDonRepository.save(ct);

        // update invoice totals (best-effort)
        double added = price * qty;
        if (h.getTongTien() == null) h.setTongTien(0.0);
        h.setTongTien(h.getTongTien() + added);
        if (h.getTongTienSauGiam() == null) h.setTongTienSauGiam(h.getTongTien());
        else h.setTongTienSauGiam(h.getTongTienSauGiam() + added);
        hoaDonRepository.save(h);

        return ResponseEntity.ok().build();
    }
}

