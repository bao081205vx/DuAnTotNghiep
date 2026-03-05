package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hoa_don")
@Data
public class HoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_hoa_don", insertable = false, updatable = false)
    private String maHoaDon;

    @ManyToOne
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia phieuGiamGia;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    @Column(name = "loai_hoa_don")
    private String loaiHoaDon;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "tong_tien", nullable = false)
    @DecimalMin(value = "0.0", message = "Total amount must be non-negative")
    private Double tongTien;

    @Column(name = "tong_tien_sau_giam")
    private Double tongTienSauGiam;

    @Column(name = "ngay_tao", columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_thanh_toan")
    private LocalDateTime ngayThanhToan;

    @Column(name = "trang_thai")
    private String trangThai;

    @OneToMany(mappedBy = "hoaDon")
    private List<ChiTietHoaDon> chiTietHoaDons;

    @OneToMany(mappedBy = "hoaDon")
    private List<HoaDonLichSu> hoaDonLichSus;

    @OneToMany(mappedBy = "hoaDon")
    private List<HinhThucThanhToan> hinhThucThanhToans;

    @OneToMany(mappedBy = "hoaDon")
    private List<LichSuThanhToan> lichSuThanhToans;
}
