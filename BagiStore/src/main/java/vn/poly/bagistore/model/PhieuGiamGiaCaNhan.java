package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "phieu_giam_gia_ca_nhan")
@Data
public class PhieuGiamGiaCaNhan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_phieu_giam_gia_ca_nhan", insertable = false, updatable = false)
    private String maPhieuGiamGiaCaNhan;

    @ManyToOne
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "id_phieu_giam_gia", nullable = false)
    private PhieuGiamGia phieuGiamGia;

    @Column(name = "ten_phieu_giam_gia_ca_nhan")
    private String tenPhieuGiamGiaCaNhan;

    @Column(name = "ngay_nhan", columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime ngayNhan;

    @Column(name = "ngay_het_han")
    private LocalDateTime ngayHetHan;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "BIT DEFAULT 1")
    private Boolean trangThai;
}
