package vn.poly.bagistore.dto;

import java.time.LocalDateTime;
import java.util.List;

public class InvoiceDetailDTO {
    private Integer id;
    private String maHoaDon;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayThanhToan;
    private String trangThai;
    private String loaiHoaDon;
    private String ghiChu;
    private Double tongTien;
    private Double tongTienSauGiam;

    private CustomerDTO khachHang;
    private EmployeeDTO nhanVien;

    private List<InvoiceItemDTO> items;
    private List<PaymentHistoryDTO> payments;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(String maHoaDon) { this.maHoaDon = maHoaDon; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public LocalDateTime getNgayThanhToan() { return ngayThanhToan; }
    public void setNgayThanhToan(LocalDateTime ngayThanhToan) { this.ngayThanhToan = ngayThanhToan; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getLoaiHoaDon() { return loaiHoaDon; }
    public void setLoaiHoaDon(String loaiHoaDon) { this.loaiHoaDon = loaiHoaDon; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public Double getTongTien() { return tongTien; }
    public void setTongTien(Double tongTien) { this.tongTien = tongTien; }
    public Double getTongTienSauGiam() { return tongTienSauGiam; }
    public void setTongTienSauGiam(Double tongTienSauGiam) { this.tongTienSauGiam = tongTienSauGiam; }
    public CustomerDTO getKhachHang() { return khachHang; }
    public void setKhachHang(CustomerDTO khachHang) { this.khachHang = khachHang; }
    public EmployeeDTO getNhanVien() { return nhanVien; }
    public void setNhanVien(EmployeeDTO nhanVien) { this.nhanVien = nhanVien; }
    public List<InvoiceItemDTO> getItems() { return items; }
    public void setItems(List<InvoiceItemDTO> items) { this.items = items; }
    public List<PaymentHistoryDTO> getPayments() { return payments; }
    public void setPayments(List<PaymentHistoryDTO> payments) { this.payments = payments; }
}
