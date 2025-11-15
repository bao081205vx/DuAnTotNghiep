package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import io.jsonwebtoken.Claims;
import vn.poly.bagistore.security.JwtUtil;
import org.springframework.web.server.ResponseStatusException;
import vn.poly.bagistore.model.NhanVien;
import vn.poly.bagistore.repository.NhanVienRepository;
import vn.poly.bagistore.model.KhachHang;
import vn.poly.bagistore.repository.KhachHangRepository;
import vn.poly.bagistore.dto.CustomerLoginResponse;
import vn.poly.bagistore.dto.EmployeeResponseDTO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@org.springframework.web.bind.annotation.CrossOrigin(origins = {"http://127.0.0.1:5500","http://localhost:5500","http://localhost:8080","http://localhost:5173","http://localhost:5174"})
public class AuthController {

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @PostMapping("/login")
    public EmployeeResponseDTO login(@RequestBody Map<String, Object> payload){
        String username = payload.getOrDefault("username", "").toString();
        String password = payload.getOrDefault("password", "").toString();
        if(username == null || username.isEmpty() || password == null || password.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password required");
        }

        // try find by username first, then by email
        Optional<NhanVien> opt = nhanVienRepository.findByTenDangNhap(username);
        if(!opt.isPresent()) opt = nhanVienRepository.findByEmail(username);
        if(!opt.isPresent()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        NhanVien e = opt.get();
        // compare password hashed with SHA-256 (project uses this hashing in existing code)
        String hashed = hashPasswordSha256(password);
        if(!hashed.equalsIgnoreCase(e.getMatKhau())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // build response DTO (do not include password)
        EmployeeResponseDTO d = new EmployeeResponseDTO();
        d.setId(e.getId());
        d.setMaNhanVien(e.getMaNhanVien());
        d.setHoTenNhanVien(e.getHoTenNhanVien());
        d.setEmail(e.getEmail());
        d.setSoDienThoai(e.getSoDienThoai());
        d.setDiaChi(e.getDiaChi());
        d.setCanCuocCongDan(e.getCanCuocCongDan());
        if(e.getVaiTro()!=null){
            d.setVaiTroId(e.getVaiTro().getId());
            d.setVaiTroTen(e.getVaiTro().getTenVaiTro());
        }
        d.setTrangThai(e.getTrangThai());
        d.setAnhNhanVien(e.getAnhNhanVien());
        d.setTenDangNhap(e.getTenDangNhap());
        // generate a JWT token (HMAC SHA-256).
        // NOTE: In production keep the secret in configuration and use a strong random key.
        try{
            String secret = System.getenv().getOrDefault("BAGISTORE_JWT_SECRET", "ChangeThisSecretToAStrongOneAtLeast32Chars!");
            // ensure secret bytes length is sufficient for HMAC-SHA
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            Key key = Keys.hmacShaKeyFor(java.util.Arrays.copyOf(keyBytes, Math.max(keyBytes.length, 32)));
            long now = System.currentTimeMillis();
            Date exp = new Date(now + 1000L * 60L * 60L * 24L); // 24h
            String jwt = Jwts.builder()
                    .setSubject(e.getTenDangNhap() == null ? e.getEmail() : e.getTenDangNhap())
                    .setId(String.valueOf(e.getId()))
                    .claim("name", e.getHoTenNhanVien())
                    .claim("role", e.getVaiTro()!=null? e.getVaiTro().getTenVaiTro() : null)
                    .setIssuedAt(new Date(now))
                    .setExpiration(exp)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
            d.setToken(jwt);
        }catch(Exception ex){
            // token generation failed - continue without token
            System.err.println("JWT generation failed: " + ex.getMessage());
        }
        return d;
    }

    // Customer login for storefront clients
    @PostMapping("/customer/login")
    public CustomerLoginResponse customerLogin(@RequestBody Map<String, Object> payload){
        String username = payload.getOrDefault("username", "").toString();
        String password = payload.getOrDefault("password", "").toString();
        if(username == null || username.isEmpty() || password == null || password.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password required");
        }

        // try find by username (tenTaiKhoan) first, then by email
        java.util.Optional<KhachHang> opt = khachHangRepository.findByTenTaiKhoan(username);
        if(!opt.isPresent()) opt = khachHangRepository.findByEmail(username);
        if(!opt.isPresent()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        KhachHang k = opt.get();
        String hashed = hashPasswordSha256(password);
        if(k.getMatKhau()==null || !hashed.equalsIgnoreCase(k.getMatKhau())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        CustomerLoginResponse resp = new CustomerLoginResponse();
        resp.setId(k.getId());
        resp.setHoTen(k.getHoTenKhachHang());
        resp.setEmail(k.getEmail());
        resp.setSoDienThoai(k.getSoDienThoai());
        resp.setAnhKhachHang(k.getAnhKhachHang());

        try{
            String secret = System.getenv().getOrDefault("BAGISTORE_JWT_SECRET", "ChangeThisSecretToAStrongOneAtLeast32Chars!");
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            Key key = Keys.hmacShaKeyFor(java.util.Arrays.copyOf(keyBytes, Math.max(keyBytes.length, 32)));
            long now = System.currentTimeMillis();
            Date exp = new Date(now + 1000L * 60L * 60L * 24L); // 24h
            String jwt = Jwts.builder()
                    .setSubject(k.getTenTaiKhoan() == null ? k.getEmail() : k.getTenTaiKhoan())
                    .setId(String.valueOf(k.getId()))
                    .claim("name", k.getHoTenKhachHang())
                    .setIssuedAt(new Date(now))
                    .setExpiration(exp)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
            resp.setToken(jwt);
        }catch(Exception ex){
            System.err.println("JWT generation failed for customer: " + ex.getMessage());
        }

        return resp;
    }

    // Return current authenticated user info based on Authorization: Bearer <token>
    @GetMapping("/me")
    public EmployeeResponseDTO me(HttpServletRequest request){
        try{
            String auth = request.getHeader("Authorization");
            if(auth == null) return null;
            if(auth.startsWith("Bearer ")) auth = auth.substring(7).trim();
            String secret = System.getenv().getOrDefault("BAGISTORE_JWT_SECRET", "ChangeThisSecretToAStrongOneAtLeast32Chars!");
            Claims claims = JwtUtil.parseClaims(auth, secret);
            if(claims == null) return null;
            String id = claims.getId();
            if(id == null) return null;
            Integer eid = Integer.valueOf(id);
            var opt = nhanVienRepository.findById(eid);
            if(!opt.isPresent()) return null;
            var e = opt.get();
            EmployeeResponseDTO d = new EmployeeResponseDTO();
            d.setId(e.getId());
            d.setMaNhanVien(e.getMaNhanVien());
            d.setHoTenNhanVien(e.getHoTenNhanVien());
            d.setEmail(e.getEmail());
            d.setSoDienThoai(e.getSoDienThoai());
            d.setDiaChi(e.getDiaChi());
            d.setVaiTroId(e.getVaiTro()!=null? e.getVaiTro().getId():null);
            d.setVaiTroTen(e.getVaiTro()!=null? e.getVaiTro().getTenVaiTro():null);
            d.setTrangThai(e.getTrangThai());
            d.setAnhNhanVien(e.getAnhNhanVien());
            d.setTenDangNhap(e.getTenDangNhap());
            d.setToken(auth);
            return d;
        }catch(Exception ex){
            return null;
        }
    }

    // Return current authenticated customer info based on Authorization: Bearer <token>
    @GetMapping("/customer/me")
    public CustomerLoginResponse customerMe(HttpServletRequest request){
        try{
            String auth = request.getHeader("Authorization");
            if(auth == null) return null;
            if(auth.startsWith("Bearer ")) auth = auth.substring(7).trim();
            String secret = System.getenv().getOrDefault("BAGISTORE_JWT_SECRET", "ChangeThisSecretToAStrongOneAtLeast32Chars!");
            Claims claims = JwtUtil.parseClaims(auth, secret);
            if(claims == null) return null;
            String id = claims.getId();
            if(id == null) return null;
            Integer cid = Integer.valueOf(id);
            var opt = khachHangRepository.findById(cid);
            if(!opt.isPresent()) return null;
            var k = opt.get();
            CustomerLoginResponse resp = new CustomerLoginResponse();
            resp.setId(k.getId());
            resp.setHoTen(k.getHoTenKhachHang());
            resp.setEmail(k.getEmail());
            resp.setSoDienThoai(k.getSoDienThoai());
            resp.setAnhKhachHang(k.getAnhKhachHang());
            resp.setToken(auth);
            return resp;
        }catch(Exception ex){
            return null;
        }
    }

    private String hashPasswordSha256(String plain){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for(byte b: hash) sb.append(String.format("%02x", b));
            return sb.toString();
        }catch(Exception ex){
            return plain;
        }
    }
}
