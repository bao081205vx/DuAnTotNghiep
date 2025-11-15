package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hoa_don_lich_su")
@Data
public class HoaDonLichSu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don", nullable = false)
    private HoaDon hoaDon;

    @ManyToOne
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    @Column(name = "hanh_dong")
    private String hanhDong;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "ngay_cap_nhat", columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime ngayCapNhat;
}
