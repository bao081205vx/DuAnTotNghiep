package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "mau_sac")
@Data
public class MauSac {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_mau_sac", insertable = false, updatable = false)
    private String maMauSac;

    @Column(name = "ten_mau_sac", nullable = false)
    private String tenMauSac;
    @Column(name = "trang_thai", nullable = false, columnDefinition = "BIT DEFAULT 1")
    private Boolean trangThai;

    @Override
    public String toString() {
        return "MauSac{" +
                "id=" + id +
                ", maMauSac='" + maMauSac + '\'' +
                ", tenMauSac='" + tenMauSac + '\'' +
                ", trangThai=" + trangThai +
                '}';
    }
}
