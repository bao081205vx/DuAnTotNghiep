package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "phuong_thuc_thanh_toan")
@Data
public class PhuongThucThanhToan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_phuong_thuc_thanh_toan", insertable = false, updatable = false)
    private String maPhuongThucThanhToan;

    @Column(name = "ten_phuong_thuc_thanh_toan", nullable = false)
    private String tenPhuongThucThanhToan;

    @OneToMany(mappedBy = "phuongThucThanhToan")
    private List<HinhThucThanhToan> hinhThucThanhToans;

    @OneToMany(mappedBy = "phuongThucThanhToan")
    private List<LichSuThanhToan> lichSuThanhToans;
}
