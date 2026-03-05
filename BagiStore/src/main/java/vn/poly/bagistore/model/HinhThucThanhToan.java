package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "hinh_thuc_thanh_toan")
@Data
public class HinhThucThanhToan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_hinh_thuc_thanh_toan", insertable = false, updatable = false)
    private String maHinhThucThanhToan;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don", nullable = false)
    private HoaDon hoaDon;

    @ManyToOne
    @JoinColumn(name = "id_phuong_thuc_thanh_toan", nullable = false)
    private PhuongThucThanhToan phuongThucThanhToan;

    @Column(name = "ten_thanh_toan")
    private String tenThanhToan;
}
