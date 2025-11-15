package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "thuong_hieu")
@Data
public class ThuongHieu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_thuong_hieu", insertable = false, updatable = false)
    private String maThuongHieu;

    @Column(name = "ten_thuong_hieu", nullable = false)
    private String tenThuongHieu;

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
        if (maThuongHieu == null) {
            // Tự động tạo mã thương hiệu nếu chưa có
            maThuongHieu = "TH" + System.currentTimeMillis();
        }
    }

    @Override
    public String toString() {
        return "Brand{" +
                "id=" + id +
                ", maThuongHieu='" + maThuongHieu + '\'' +
                ", tenThuongHieu='" + tenThuongHieu + '\'' +
                ", ngayTao=" + ngayTao +
                ", trangThai=" + trangThai +
                '}';
    }
}
