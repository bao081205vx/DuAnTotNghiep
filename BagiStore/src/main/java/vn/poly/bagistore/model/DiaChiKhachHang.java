package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "dia_chi_khach_hang")
@Data
public class DiaChiKhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_dia_chi", insertable = false, updatable = false)
    private String maDiaChi;

    @ManyToOne
    @JoinColumn(name = "id_khach_hang", nullable = false)
    @JsonIgnore
    private KhachHang khachHang;

    @Column(name = "thanh_pho_tinh")
    private String thanhPhoTinh;

    @Column(name = "xa_phuong")
    private String xaPhuong;

    @Column(name = "quan_huyen")
    private String quanHuyen;

    @Column(name = "dia_chi_cu_the")
    private String diaChiCuThe;

    @Column(name = "mac_dinh", columnDefinition = "BIT DEFAULT 0")
    private Boolean macDinh;
}
