package vn.poly.bagistore.dto;

import lombok.Data;
import vn.poly.bagistore.model.PhieuGiamGia;

import java.time.LocalDateTime;

@Data
public class DiscountDTO {
    private Integer id;
    private String maPhieu;
    private String tenPhieu;
    private Double giaTriGiamGia;
    private Integer soLuong;
    private String loaiPhieu;
    private String trangThai;
    private String ngayBatDau;
    private String ngayKetThu;

    public static DiscountDTO fromEntity(PhieuGiamGia e) {
        if (e == null) return null;
        DiscountDTO dto = new DiscountDTO();
        dto.setId(e.getId());
        dto.setMaPhieu(e.getMaPhieu());
        dto.setTenPhieu(e.getTenPhieu());
        dto.setGiaTriGiamGia(e.getGiaTriGiamGia());
        dto.setSoLuong(e.getSoLuong());
        dto.setLoaiPhieu(e.getLoaiPhieu());
        dto.setTrangThai(e.getTrangThai());
        LocalDateTime s = e.getNgayBatDau();
        LocalDateTime t = e.getNgayKetThu();
        dto.setNgayBatDau(s != null ? s.toString() : null);
        dto.setNgayKetThu(t != null ? t.toString() : null);
        return dto;
    }
}
