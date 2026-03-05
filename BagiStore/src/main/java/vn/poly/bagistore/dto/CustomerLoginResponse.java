package vn.poly.bagistore.dto;

public class CustomerLoginResponse {
    private Integer id;
    private String hoTen;
    private String email;
    private String soDienThoai;
    private String anhKhachHang;
    private String token;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getAnhKhachHang() { return anhKhachHang; }
    public void setAnhKhachHang(String anhKhachHang) { this.anhKhachHang = anhKhachHang; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
