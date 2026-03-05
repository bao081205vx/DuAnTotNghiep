package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "phieu_giam_gia")
@Data
public class PhieuGiamGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_phieu")
    private String maPhieu;

    @Column(name = "ten_phieu", nullable = false)
    private String tenPhieu;

    @Column(name = "gia_tri_giam_gia", nullable = false)
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private Double giaTriGiamGia;

    @Column(name = "so_tien_toi_da")
    private Double soTienToiDa;

    @Column(name = "hoa_don_toi_thieu")
    private Double hoaDonToiThieu;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "ngay_bat_dau")
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thu")
    private LocalDateTime ngayKetThu;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "trang_thai")
    private String trangThai; // "Sắp diễn ra", "Đang diễn ra", "Kết thúc"

    @Column(name = "loai_phieu")
    private String loaiPhieu; // "Tiền mặt" hoặc "Phần trăm"

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_sua")
    private LocalDateTime ngaySua;

    @OneToMany(mappedBy = "phieuGiamGia")
    private List<HoaDon> hoaDons;
}
