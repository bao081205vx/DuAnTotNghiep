package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "anh_san_pham")
@Data
public class AnhSanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "duong_dan", columnDefinition = "nvarchar(max)")
    private String duongDan;
    @Column(name = "trang_thai", nullable = false, columnDefinition = "BIT DEFAULT 1")
    private Boolean trangThai;
    @OneToMany(mappedBy = "anhSanPham", fetch = FetchType.LAZY)
    private List<SanPhamChiTiet> sanPhamChiTiets;
    @Override
    public String toString() {
        return "AnhSanPham{" +
                "id=" + id +
                ", duongDan='" + (duongDan != null ? duongDan.substring(0, Math.min(50, duongDan.length())) + "..." : null) + '\'' +
                ", trangThai=" + trangThai +
                '}';
    }
}
