package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "nhan_vien")
@Data
public class NhanVien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_nhan_vien", insertable = false, updatable = false)
    private String maNhanVien;

    @ManyToOne
    @JoinColumn(name = "id_vai_tro", nullable = false)
    private VaiTro vaiTro;

    @Column(name = "ho_ten_nhan_vien", nullable = false)
    private String hoTenNhanVien;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "mat_khau", nullable = false)
    private String matKhau;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    @Column(name = "dia_chi")
    private String diaChi;

    @Column(name = "can_cuoc_cong_dan", unique = true)
    private String canCuocCongDan;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "BIT DEFAULT 1")
    private Boolean trangThai;

    @Column(name = "anh_nhan_vien")
    private String anhNhanVien;

    @Column(name = "ten_dang_nhap", unique = true, nullable = false)
    private String tenDangNhap;

    @OneToMany(mappedBy = "nhanVien")
    private List<HoaDon> hoaDons;

    @OneToMany(mappedBy = "nhanVien")
    private List<HoaDonLichSu> hoaDonLichSus;
}
