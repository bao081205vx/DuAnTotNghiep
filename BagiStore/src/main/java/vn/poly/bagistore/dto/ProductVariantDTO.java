package vn.poly.bagistore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // Add NoArgsConstructor annotation
public class ProductVariantDTO {
    private Integer id;
    private Integer productId;  // Add product ID field
    private String maVariant;
    private String tenSanPham;
    private String mauSac;
    private String kichThuoc;
    private Integer soLuong;
    private Boolean trangThai;
    private String moTa;
    private String anhUrl;
    private Double gia;
    private String tenTrongLuong;
    private String maMau;

    // Constructor đầy đủ
    public ProductVariantDTO(Integer id, String maVariant, String tenSanPham,
                             String mauSac, String kichThuoc, Integer soLuong,
                             Boolean trangThai, String moTa, String anhUrl,
                             Double gia, String tenTrongLuong) {
        this.id = id;
        this.maVariant = maVariant;
        this.tenSanPham = tenSanPham;
        this.mauSac = mauSac;
        this.kichThuoc = kichThuoc;
        this.soLuong = soLuong;
        this.trangThai = trangThai;
        this.moTa = moTa;
        this.anhUrl = anhUrl;
        this.gia = gia;
        this.tenTrongLuong = tenTrongLuong;
    }

    // Constructor with productId
    public ProductVariantDTO(Integer id, Integer productId, String maVariant, String tenSanPham,
                             String mauSac, String kichThuoc, Integer soLuong,
                             Boolean trangThai, String moTa, String anhUrl,
                             Double gia, String tenTrongLuong) {
        this.id = id;
        this.productId = productId;
        this.maVariant = maVariant;
        this.tenSanPham = tenSanPham;
        this.mauSac = mauSac;
        this.kichThuoc = kichThuoc;
        this.soLuong = soLuong;
        this.trangThai = trangThai;
        this.moTa = moTa;
        this.anhUrl = anhUrl;
        this.gia = gia;
        this.tenTrongLuong = tenTrongLuong;
    }

    // Constructor cũ để tương thích
    public ProductVariantDTO(Integer id, String maVariant, String tenSanPham,
                             String mauSac, String kichThuoc, Integer soLuong,
                             Boolean trangThai, String moTa, String anhUrl) {
        this(id, maVariant, tenSanPham, mauSac, kichThuoc, soLuong, trangThai, moTa, anhUrl, 0.0, null);
    }
}