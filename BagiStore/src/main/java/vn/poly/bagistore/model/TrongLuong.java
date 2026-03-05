package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "trong_luong")
@Data
public class TrongLuong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_trong_luong", insertable = false, updatable = false)
    private String maTrongLuong;

    @Column(name = "ten_trong_luong", nullable = false)
    private String tenTrongLuong;
    @Column(name = "trang_thai", nullable = false, columnDefinition = "BIT DEFAULT 1")
    private Boolean trangThai;

    @Override
    public String toString() {
        return "TrongLuong{" +
                "id=" + id +
                ", maTrongLuong='" + maTrongLuong + '\'' +
                ", tenTrongLuong='" + tenTrongLuong + '\'' +
                ", trangThai=" + trangThai +
                '}';
    }
}
