package vn.poly.bagistore.dto;

import java.util.List;

public class CustomerCreateDTO {
    private String hoTenKhachHang;
    private String soDienThoai;
    private String email;
    private String gioiTinh; // may come as 'male'/'female' or other strings from frontend
    private Boolean trangThai;
    private List<AddressDTO> diaChiKhachHangs;

    public String getHoTenKhachHang() { return hoTenKhachHang; }
    public void setHoTenKhachHang(String hoTenKhachHang) { this.hoTenKhachHang = hoTenKhachHang; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }
    public Boolean getTrangThai() { return trangThai; }
    public void setTrangThai(Boolean trangThai) { this.trangThai = trangThai; }
    public List<AddressDTO> getDiaChiKhachHangs() { return diaChiKhachHangs; }
    public void setDiaChiKhachHangs(List<AddressDTO> diaChiKhachHangs) { this.diaChiKhachHangs = diaChiKhachHangs; }

    public static class AddressDTO {
        private String diaChiCuThe;
        private String xaPhuong;
        private String quanHuyen;
        private String thanhPhoTinh;
        private Boolean macDinh;

        public String getDiaChiCuThe() { return diaChiCuThe; }
        public void setDiaChiCuThe(String diaChiCuThe) { this.diaChiCuThe = diaChiCuThe; }
        public String getXaPhuong() { return xaPhuong; }
        public void setXaPhuong(String xaPhuong) { this.xaPhuong = xaPhuong; }
        public String getQuanHuyen() { return quanHuyen; }
        public void setQuanHuyen(String quanHuyen) { this.quanHuyen = quanHuyen; }
        public String getThanhPhoTinh() { return thanhPhoTinh; }
        public void setThanhPhoTinh(String thanhPhoTinh) { this.thanhPhoTinh = thanhPhoTinh; }
        public Boolean getMacDinh() { return macDinh; }
        public void setMacDinh(Boolean macDinh) { this.macDinh = macDinh; }
    }
}
