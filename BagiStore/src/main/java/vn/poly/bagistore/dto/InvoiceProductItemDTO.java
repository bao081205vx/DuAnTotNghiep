package vn.poly.bagistore.dto;

public class InvoiceProductItemDTO {
    private Integer id; // chi tiet hoa don id
    private Integer variantId; // id_chi_tiet_san_pham
    private String tenSanPham;
    private String kichThuoc;
    private String mauSac;
    private Integer soLuong;
    private Double donGia;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getVariantId() { return variantId; }
    public void setVariantId(Integer variantId) { this.variantId = variantId; }
    public String getTenSanPham() { return tenSanPham; }
    public void setTenSanPham(String tenSanPham) { this.tenSanPham = tenSanPham; }
    public String getKichThuoc() { return kichThuoc; }
    public void setKichThuoc(String kichThuoc) { this.kichThuoc = kichThuoc; }
    public String getMauSac() { return mauSac; }
    public void setMauSac(String mauSac) { this.mauSac = mauSac; }
    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }
    public Double getDonGia() { return donGia; }
    public void setDonGia(Double donGia) { this.donGia = donGia; }
}
