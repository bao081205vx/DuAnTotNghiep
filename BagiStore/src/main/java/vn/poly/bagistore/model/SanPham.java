package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "san_pham")
@Data
public class SanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_san_pham", insertable = false, updatable = false)
    private String maSanPham;

    @ManyToOne
    @JoinColumn(name = "id_nha_san_xuat")
    private NhaSanXuat nhaSanXuat;

    @ManyToOne
    @JoinColumn(name = "id_xuat_xu")
    private XuatXu xuatXu;

    @ManyToOne
    @JoinColumn(name = "id_dung_tich")
    private DungTich dungTich;

    @ManyToOne
    @JoinColumn(name = "id_chat_lieu")
    private ChatLieu chatLieu;

    @ManyToOne
    @JoinColumn(name = "id_thuong_hieu")
    private ThuongHieu thuongHieu;

    @Column(name = "ten_san_pham", nullable = false)

    private String tenSanPham;

    @Column(name = "ngay_tao", columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_sua")
    private LocalDateTime ngaySua;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "BIT DEFAULT 1")
    private Boolean trangThai;

//    @OneToMany(mappedBy = "sanPham")
//    private List<SanPhamChiTiet> sanPhamChiTiets;
@Override
public String toString() {
    return "SanPham{" +
            "id=" + id +
            ", maSanPham='" + maSanPham + '\'' +
            ", tenSanPham='" + tenSanPham + '\'' +
            ", ngayTao=" + ngayTao +
            ", ngaySua=" + ngaySua +
            ", trangThai=" + trangThai +
            ", nhaSanXuat=" + (nhaSanXuat != null ? nhaSanXuat.getTenNhaSanXuat() : null) +
            ", xuatXu=" + (xuatXu != null ? xuatXu.getTenXuatXu() : null) +
            ", dungTich=" + (dungTich != null ? dungTich.getTenDungTich() : null) +
            ", chatLieu=" + (chatLieu != null ? chatLieu.getTenChatLieu() : null) +
            ", thuongHieu=" + (thuongHieu != null ? thuongHieu.getTenThuongHieu() : null) +
            '}';
}
}
