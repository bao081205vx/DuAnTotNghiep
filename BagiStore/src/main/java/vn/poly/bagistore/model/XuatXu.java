package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "xuat_xu")
@Data
public class XuatXu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_xuat_xu", insertable = false, updatable = false)
    private String maXuatXu;

    @Column(name = "ten_xuat_xu", nullable = false)
    private String tenXuatXu;

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
        if (maXuatXu == null) {
            maXuatXu = "XX" + System.currentTimeMillis();
        }
    }

    @Override
    public String toString() {
        return "XuatXu{" +
                "id=" + id +
                ", maXuatXu='" + maXuatXu + '\'' +
                ", tenXuatXu='" + tenXuatXu + '\'' +
                ", ngayTao=" + ngayTao +
                ", trangThai=" + trangThai +
                '}';
    }
}
