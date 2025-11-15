package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "kich_thuoc")
@Data
public class KichThuoc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_kich_thuoc", insertable = false, updatable = false)
    private String maKichThuoc;

    @Column(name = "ten_kich_thuoc", nullable = false)
    private String tenKichThuoc;
    @Column(name = "trang_thai", nullable = false, columnDefinition = "BIT DEFAULT 1")
    private Boolean trangThai;

    @Override
    public String toString() {
        return "KichThuoc{" +
                "id=" + id +
                ", maKichThuoc='" + maKichThuoc + '\'' +
                ", tenKichThuoc='" + tenKichThuoc + '\'' +
                ", trangThai=" + trangThai +
                '}';
    }
}
