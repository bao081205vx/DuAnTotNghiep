package vn.poly.bagistore.dto;

public class EmployeeResponseDTO {
    private Integer id;
    private String maNhanVien;
    private String hoTenNhanVien;
    private String email;
    private String soDienThoai;
    private String diaChi;
    private Integer vaiTroId;
    private String vaiTroTen;
    private Boolean trangThai;
    private String anhNhanVien;
    private String tenDangNhap;
    // JWT token returned after successful authentication (optional)
    private String token;
    // Temporary plain password returned on creation for admin to communicate to the employee
    private String tempPassword;
    // Citizen ID / CCCD
    private String canCuocCongDan;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(String maNhanVien) { this.maNhanVien = maNhanVien; }
    public String getHoTenNhanVien() { return hoTenNhanVien; }
    public void setHoTenNhanVien(String hoTenNhanVien) { this.hoTenNhanVien = hoTenNhanVien; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public Integer getVaiTroId() { return vaiTroId; }
    public void setVaiTroId(Integer vaiTroId) { this.vaiTroId = vaiTroId; }
    public String getVaiTroTen() { return vaiTroTen; }
    public void setVaiTroTen(String vaiTroTen) { this.vaiTroTen = vaiTroTen; }
    public Boolean getTrangThai() { return trangThai; }
    public void setTrangThai(Boolean trangThai) { this.trangThai = trangThai; }
    public String getAnhNhanVien() { return anhNhanVien; }
    public void setAnhNhanVien(String anhNhanVien) { this.anhNhanVien = anhNhanVien; }
    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTempPassword() { return tempPassword; }
    public void setTempPassword(String tempPassword) { this.tempPassword = tempPassword; }
    public String getCanCuocCongDan() { return canCuocCongDan; }
    public void setCanCuocCongDan(String canCuocCongDan) { this.canCuocCongDan = canCuocCongDan; }
}
