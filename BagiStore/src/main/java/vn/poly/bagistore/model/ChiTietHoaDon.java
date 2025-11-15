package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "chi_tiet_hoa_don")
@Data
public class ChiTietHoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don", nullable = false)
    private HoaDon hoaDon;

    @ManyToOne
    @JoinColumn(name = "id_chi_tiet_san_pham", nullable = false)
    private SanPhamChiTiet sanPhamChiTiet;

    @Column(name = "so_luong", nullable = false)
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer soLuong;

    @Column(name = "don_gia", nullable = false)
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    private Double donGia;

    @Column(name = "mo_ta")
    private String moTa;
}
