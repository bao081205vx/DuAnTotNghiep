package vn.poly.bagistore.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "nha_san_xuat")
@Data
public class NhaSanXuat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_nha_san_xuat", insertable = false, updatable = false)
    private String maNhaSanXuat;

    @Column(name = "ten_nha_san_xuat", nullable = false)
    private String tenNhaSanXuat;
    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "BIT DEFAULT 1")
    private Boolean trangThai;

    @PrePersist
    protected void onCreate() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
        if (trangThai == null) {
            trangThai = true;
        }
        if (maNhaSanXuat == null) {
            maNhaSanXuat = "NSX" + System.currentTimeMillis();
        }
    }

    @Override
    public String toString() {
        return "NhaSanXuat{" +
                "id=" + id +
                ", maNhaSanXuat='" + maNhaSanXuat + '\'' +
                ", tenNhaSanXuat='" + tenNhaSanXuat + '\'' +
                ", ngayTao=" + ngayTao +
                ", trangThai=" + trangThai +
                '}';
    }
}
