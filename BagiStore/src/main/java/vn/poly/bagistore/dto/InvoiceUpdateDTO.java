package vn.poly.bagistore.dto;

import java.util.List;

public class InvoiceUpdateDTO {
    private String trangThai;
    private String loaiHoaDon;
    private String ghiChu;
    private List<InvoiceItemUpdateDTO> items;

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getLoaiHoaDon() { return loaiHoaDon; }
    public void setLoaiHoaDon(String loaiHoaDon) { this.loaiHoaDon = loaiHoaDon; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public List<InvoiceItemUpdateDTO> getItems() { return items; }
    public void setItems(List<InvoiceItemUpdateDTO> items) { this.items = items; }
}
