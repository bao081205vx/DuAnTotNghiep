package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.poly.bagistore.dto.EmployeeResponseDTO;
import vn.poly.bagistore.dto.RoleDTO;
import vn.poly.bagistore.model.NhanVien;
import vn.poly.bagistore.model.VaiTro;
import vn.poly.bagistore.repository.NhanVienRepository;
import vn.poly.bagistore.repository.VaiTroRepository;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import vn.poly.bagistore.Service.CloudinaryService;
import vn.poly.bagistore.Service.MailService;

import java.security.SecureRandom;
import java.text.Normalizer;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@org.springframework.web.bind.annotation.CrossOrigin(origins = {"http://127.0.0.1:5500","http://localhost:5500","http://localhost:8080"})
public class EmployeeRestController {

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private VaiTroRepository vaiTroRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private MailService mailService;

    @GetMapping("/employees")
    public List<EmployeeResponseDTO> getEmployees(){
        List<NhanVien> list = nhanVienRepository.findAll();
        List<EmployeeResponseDTO> out = new ArrayList<>();
        for(NhanVien e: list){
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
            out.add(d);
        }
        return out;
    }

    @GetMapping("/roles")
    public List<RoleDTO> getRoles(){
        List<VaiTro> list = vaiTroRepository.findAll();
        List<RoleDTO> out = new ArrayList<>();
        for(VaiTro r: list){
            RoleDTO rd = new RoleDTO();
            rd.setId(r.getId());
            rd.setMaVaiTro(r.getMaVaiTro());
            rd.setTenVaiTro(r.getTenVaiTro());
            out.add(rd);
        }
        return out;
    }

    @PatchMapping("/employees/{id}/status")
    public EmployeeResponseDTO updateEmployeeStatus(@PathVariable Integer id, @RequestBody java.util.Map<String,Object> payload){
        java.util.Optional<NhanVien> opt = nhanVienRepository.findById(id);
        if(!opt.isPresent()) return null;
        NhanVien e = opt.get();
        Object v = payload.get("trangThai");
        Boolean tt = null;
        if(v instanceof Boolean) tt = (Boolean)v;
        else if(v instanceof Number) tt = ((Number)v).intValue() != 0;
        else if(v instanceof String) tt = "1".equals(v) || "true".equalsIgnoreCase((String)v);
        if(tt != null) {
            e.setTrangThai(tt);
            nhanVienRepository.save(e);
        }
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
        return d;
    }

    @PostMapping("/employees")
    public EmployeeResponseDTO createEmployee(@RequestBody EmployeeResponseDTO dto){
        // prevent duplicate email insertion
        if(dto.getEmail() != null && nhanVienRepository.existsByEmail(dto.getEmail())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists: " + dto.getEmail());
        }
        NhanVien e = new NhanVien();
        // Note: maNhanVien is database generated or managed, so we don't set it here
        e.setHoTenNhanVien(dto.getHoTenNhanVien());
        e.setEmail(dto.getEmail());
        e.setSoDienThoai(dto.getSoDienThoai());
        e.setDiaChi(dto.getDiaChi());
        e.setAnhNhanVien(dto.getAnhNhanVien());
        e.setTrangThai(dto.getTrangThai() == null ? true : dto.getTrangThai());
        if(dto.getVaiTroTen() != null){
            VaiTro vt = vaiTroRepository.findByTenVaiTro(dto.getVaiTroTen());
            if(vt != null) e.setVaiTro(vt);
        }
        // persist provided CCCD if any
        if(dto.getCanCuocCongDan() != null && !dto.getCanCuocCongDan().isEmpty()){
            e.setCanCuocCongDan(dto.getCanCuocCongDan());
        }
        // generate a random password, hash it and save to DB
        String plainPassword = generateRandomPassword(8);
        // hash the password using SHA-256 (no external BCrypt dependency)
        e.setMatKhau(hashPasswordSha256(plainPassword));
        // ensure canCuocCongDan is not null to avoid unique-index NULL collisions in DB
        if(e.getCanCuocCongDan() == null || e.getCanCuocCongDan().isEmpty()){
            e.setCanCuocCongDan(generateShortUniqueCCCD());
        }
        // ensure tenDangNhap isn't null (DB requires non-null). Use normalized name as a base before insert.
        String baseUsername = generateUsername(e.getHoTenNhanVien(), null);
        if(baseUsername == null || baseUsername.isEmpty()) baseUsername = "user";
        e.setTenDangNhap(baseUsername);
        e = nhanVienRepository.save(e);
        // now append id to make username unique and persist again
        String username = generateUsername(e.getHoTenNhanVien(), e.getId());
        e.setTenDangNhap(username);
        e = nhanVienRepository.save(e);
        EmployeeResponseDTO out = new EmployeeResponseDTO();
        out.setId(e.getId());
        out.setMaNhanVien(e.getMaNhanVien());
        out.setHoTenNhanVien(e.getHoTenNhanVien());
        out.setEmail(e.getEmail());
        out.setSoDienThoai(e.getSoDienThoai());
        out.setDiaChi(e.getDiaChi());
        out.setCanCuocCongDan(e.getCanCuocCongDan());
        if(e.getVaiTro()!=null){
            out.setVaiTroId(e.getVaiTro().getId());
            out.setVaiTroTen(e.getVaiTro().getTenVaiTro());
        }
        out.setTrangThai(e.getTrangThai());
        out.setAnhNhanVien(e.getAnhNhanVien());
        out.setTenDangNhap(e.getTenDangNhap());
        // include the plain, temporary password in response so admin can communicate it
        out.setTempPassword(plainPassword);
        // send welcome email (best-effort)
        try{
            mailService.sendWelcomeEmail(e.getEmail(), e.getHoTenNhanVien(), e.getTenDangNhap(), plainPassword);
        }catch(Exception ex){
            System.err.println("Error sending welcome email: " + ex.getMessage());
        }
        return out;
    }

    @PostMapping(value = "/employees-multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmployeeResponseDTO createEmployeeMultipart(
            @RequestParam("hoTenNhanVien") String hoTen,
            @RequestParam("email") String email,
            @RequestParam(value = "canCuocCongDan", required = false) String canCuocCongDan,
            @RequestParam(value = "soDienThoai", required = false) String soDienThoai,
            @RequestParam(value = "diaChi", required = false) String diaChi,
            @RequestParam(value = "vaiTroTen", required = false) String vaiTroTen,
            @RequestParam(value = "trangThai", required = false) Boolean trangThai,
            @RequestPart(value = "file", required = false) MultipartFile file
    ){
        // prevent duplicate email insertion
        if(email != null && nhanVienRepository.existsByEmail(email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists: " + email);
        }

        NhanVien e = new NhanVien();
        e.setHoTenNhanVien(hoTen);
        e.setEmail(email);
        e.setSoDienThoai(soDienThoai);
        e.setDiaChi(diaChi);
        e.setTrangThai(trangThai == null ? true : trangThai);
        if(vaiTroTen != null){
            VaiTro vt = vaiTroRepository.findByTenVaiTro(vaiTroTen);
            if(vt != null) e.setVaiTro(vt);
        }
        // accept provided CCCD from multipart form if present
        if(canCuocCongDan != null && !canCuocCongDan.isEmpty()){
            e.setCanCuocCongDan(canCuocCongDan);
        }
        // upload file to Cloudinary (server-side) if provided
        if(file != null && !file.isEmpty()){
            try{
                String url = cloudinaryService.upload(file);
                if(url != null) e.setAnhNhanVien(url);
            }catch(Exception ex){
                // log and continue without image
                System.err.println("Cloudinary upload failed: " + ex.getMessage());
            }
        }
        // generate password and username
        String plainPassword = generateRandomPassword(8);
        // hash the password using SHA-256 (no external BCrypt dependency)
        e.setMatKhau(hashPasswordSha256(plainPassword));
        // ensure canCuocCongDan is not null to avoid unique-index NULL collisions in DB
        if(e.getCanCuocCongDan() == null || e.getCanCuocCongDan().isEmpty()){
            e.setCanCuocCongDan(generateShortUniqueCCCD());
        }
        // ensure tenDangNhap isn't null before insert
        String baseUsername2 = generateUsername(e.getHoTenNhanVien(), null);
        if(baseUsername2 == null || baseUsername2.isEmpty()) baseUsername2 = "user";
        e.setTenDangNhap(baseUsername2);
        e = nhanVienRepository.save(e);
        // now append id to make username unique and persist again
        String username2 = generateUsername(e.getHoTenNhanVien(), e.getId());
        e.setTenDangNhap(username2);
        e = nhanVienRepository.save(e);

        EmployeeResponseDTO out = new EmployeeResponseDTO();
        out.setId(e.getId());
        out.setMaNhanVien(e.getMaNhanVien());
        out.setHoTenNhanVien(e.getHoTenNhanVien());
        out.setEmail(e.getEmail());
        out.setSoDienThoai(e.getSoDienThoai());
        out.setDiaChi(e.getDiaChi());
        out.setCanCuocCongDan(e.getCanCuocCongDan());
        if(e.getVaiTro()!=null){
            out.setVaiTroId(e.getVaiTro().getId());
            out.setVaiTroTen(e.getVaiTro().getTenVaiTro());
        }
        out.setTrangThai(e.getTrangThai());
        out.setAnhNhanVien(e.getAnhNhanVien());
        out.setTenDangNhap(e.getTenDangNhap());
        out.setTempPassword(plainPassword);
        // send welcome email (best-effort)
        try{
            mailService.sendWelcomeEmail(e.getEmail(), e.getHoTenNhanVien(), e.getTenDangNhap(), plainPassword);
        }catch(Exception ex){
            System.err.println("Error sending welcome email: " + ex.getMessage());
        }
        return out;
    }

    @PutMapping("/employees/{id}")
    public EmployeeResponseDTO updateEmployee(@PathVariable Integer id, @RequestBody java.util.Map<String,Object> payload){
        java.util.Optional<NhanVien> opt = nhanVienRepository.findById(id);
        if(!opt.isPresent()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + id);
        NhanVien e = opt.get();
        // update allowed fields from payload (tolerant to various keys)
        if(payload.get("hoTenNhanVien") != null) e.setHoTenNhanVien(String.valueOf(payload.get("hoTenNhanVien")));
        if(payload.get("email") != null) e.setEmail(String.valueOf(payload.get("email")));
        if(payload.get("soDienThoai") != null) e.setSoDienThoai(String.valueOf(payload.get("soDienThoai")));
        if(payload.get("diaChi") != null) e.setDiaChi(String.valueOf(payload.get("diaChi")));
        if(payload.get("anhNhanVien") != null) e.setAnhNhanVien(String.valueOf(payload.get("anhNhanVien")));
        // frontend sends cccd in payload under key 'cccd' — persist to canCuocCongDan
        if(payload.get("cccd") != null){
            e.setCanCuocCongDan(String.valueOf(payload.get("cccd")));
        } else if(payload.get("canCuocCongDan") != null){
            e.setCanCuocCongDan(String.valueOf(payload.get("canCuocCongDan")));
        }
        if(payload.get("vaiTroTen") != null){
            String vtName = String.valueOf(payload.get("vaiTroTen"));
            VaiTro vt = vaiTroRepository.findByTenVaiTro(vtName);
            if(vt != null) e.setVaiTro(vt);
        }
        if(payload.get("trangThai") != null){
            Object v = payload.get("trangThai");
            Boolean tt = null;
            if(v instanceof Boolean) tt = (Boolean)v;
            else if(v instanceof Number) tt = ((Number)v).intValue() != 0;
            else if(v instanceof String) tt = "1".equals(v) || "true".equalsIgnoreCase(String.valueOf(v));
            if(tt != null) e.setTrangThai(tt);
        }
        e = nhanVienRepository.save(e);
        EmployeeResponseDTO out = new EmployeeResponseDTO();
        out.setId(e.getId());
        out.setMaNhanVien(e.getMaNhanVien());
        out.setHoTenNhanVien(e.getHoTenNhanVien());
        out.setEmail(e.getEmail());
        out.setSoDienThoai(e.getSoDienThoai());
        out.setDiaChi(e.getDiaChi());
        out.setCanCuocCongDan(e.getCanCuocCongDan());
        if(e.getVaiTro()!=null){
            out.setVaiTroId(e.getVaiTro().getId());
            out.setVaiTroTen(e.getVaiTro().getTenVaiTro());
        }
        out.setTrangThai(e.getTrangThai());
        out.setAnhNhanVien(e.getAnhNhanVien());
        return out;
    }

    // helper - simple random password generator
    private String generateRandomPassword(int length){
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for(int i=0;i<length;i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private String generateUsername(String fullName, Integer id){
        if(fullName == null) fullName = "";
        // remove accents/diacritics
        String normalized = Normalizer.normalize(fullName, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // remove non-alphanumeric (keep spaces briefly), remove spaces, toLower
        normalized = normalized.replaceAll("[^A-Za-z0-9 ]", "");
        normalized = normalized.replaceAll("\\s+", "");
        normalized = normalized.toLowerCase();
        String suffix = id != null ? String.valueOf(id) : "";
        return normalized + suffix;
    }

    private String hashPasswordSha256(String plain){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for(byte b: hash) sb.append(String.format("%02x", b));
            return sb.toString();
        }catch(Exception ex){
            // fallback to plain (shouldn't happen)
            return plain;
        }
    }

    // generate a short unique placeholder for canCuocCongDan (fits typical DB varchar lengths)
    private String generateShortUniqueCCCD(){
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for(int i=0;i<12;i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return "cccd_" + sb.toString();
    }

}
