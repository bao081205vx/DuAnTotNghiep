package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

@Entity
@Table(name = "khach_hang")
@Data
public class KhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_khach_hang", insertable = false, updatable = false)
    private String maKhachHang;

    @Column(name = "ho_ten_khach_hang", nullable = false)
    private String hoTenKhachHang;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "mat_khau")
    @JsonIgnore
    private String matKhau;

    @Column(name = "so_dien_thoai")
    @Pattern(regexp = "[0-9]+", message = "Phone number must contain only digits")
    private String soDienThoai;

    @Column(name = "gioi_tinh")
    private Boolean gioiTinh;

    @Column(name = "anh_khach_hang")
    private String anhKhachHang;

    @Column(name = "trang_thai" ,nullable = false, columnDefinition = "BIT DEFAULT 1")
    private Boolean trangThai;

    @Column(name = "ten_tai_khoan", unique = true)
    private String tenTaiKhoan;

    @OneToMany(mappedBy = "khachHang", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaChiKhachHang> diaChiKhachHangs;

    @OneToMany(mappedBy = "khachHang")
    @JsonIgnore
    private List<HoaDon> hoaDons;

    @OneToMany(mappedBy = "khachHang")
    @JsonIgnore
    private List<HoaDonLichSu> hoaDonLichSus;
}
