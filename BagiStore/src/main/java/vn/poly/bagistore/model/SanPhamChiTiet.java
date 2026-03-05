package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "san_pham_chi_tiet")
@Data
public class SanPhamChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @ManyToOne
    @JoinColumn(name = "id_kich_thuoc")
    private KichThuoc kichThuoc;

    @ManyToOne
    @JoinColumn(name = "id_mau_sac")
    private MauSac mauSac;

    @ManyToOne
    @JoinColumn(name = "id_trong_luong")
    private TrongLuong trongLuong;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "gia", nullable = false)
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private Double gia;

    @Column(name = "so_luong", nullable = false)
    @Min(value = 0, message = "Quantity must be non-negative")
    private Integer soLuong;

    @Column(name = "ngay_tao", columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_sua")
    private LocalDateTime ngaySua;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "BIT DEFAULT 1")
    private Boolean trangThai;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_anh_san_pham")
    private AnhSanPham anhSanPham;
    public SanPhamChiTiet() {}
        @Override
        public String toString() {
            return "SanPhamChiTiet{" +
                    "id=" + id +
                    ", gia=" + gia +
                    ", soLuong=" + soLuong +
                    ", moTa='" + (moTa != null ? moTa.substring(0, Math.min(50, moTa.length())) + "..." : null) + '\'' +
                    ", ngayTao=" + ngayTao +
                    ", ngaySua=" + ngaySua +
                    ", trangThai=" + trangThai +
                    ", sanPham=" + (sanPham != null ? sanPham.getId() : null) +
                    ", mauSac=" + (mauSac != null ? mauSac.getTenMauSac() : null) +
                    ", kichThuoc=" + (kichThuoc != null ? kichThuoc.getTenKichThuoc() : null) +
                    ", trongLuong=" + (trongLuong != null ? trongLuong.getTenTrongLuong() : null) +
                    ", anhSanPham=" + (anhSanPham != null ? anhSanPham.getId() : null) +
                    '}';
        }
}
