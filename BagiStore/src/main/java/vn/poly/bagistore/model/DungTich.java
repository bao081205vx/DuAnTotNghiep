package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dung_tich")
@Data
public class DungTich {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_dung_tich", insertable = false, updatable = false)
    private String maDungTich;

    @Column(name = "dung_tich", nullable = false)
    private String tenDungTich;
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
        if (maDungTich == null) {
            maDungTich = "DT" + System.currentTimeMillis();
        }
    }

    @Override
    public String toString() {
        return "DungTich{" +
                "id=" + id +
                ", maDungTich='" + maDungTich + '\'' +
                ", tenDungTich='" + tenDungTich + '\'' +
                ", ngayTao=" + ngayTao +
                ", trangThai=" + trangThai +
                '}';
    }
}
