package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.poly.bagistore.model.KhachHang;
import vn.poly.bagistore.repository.KhachHangRepository;
import vn.poly.bagistore.model.DiaChiKhachHang;
import vn.poly.bagistore.dto.CustomerCreateDTO;
import vn.poly.bagistore.dto.CustomerCreateDTO.AddressDTO;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private KhachHangRepository khachHangRepository;

    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(@PathVariable Integer id){
        Optional<KhachHang> opt = khachHangRepository.findByIdWithAddresses(id);
        if(!opt.isPresent()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(opt.get());
    }

    @GetMapping
    public List<KhachHang> getCustomers() {
        // return customers with addresses (fetch-join used in repository)
        return khachHangRepository.findAllWithAddresses();
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Integer id, @RequestBody CustomerCreateDTO payload) {
        if (payload == null) return ResponseEntity.badRequest().body("Empty payload");
        Optional<KhachHang> opt = khachHangRepository.findById(id);
        if (!opt.isPresent()) return ResponseEntity.notFound().build();
        KhachHang exist = opt.get();

        if (payload.getHoTenKhachHang() != null) exist.setHoTenKhachHang(payload.getHoTenKhachHang());
        if (payload.getSoDienThoai() != null) exist.setSoDienThoai(payload.getSoDienThoai());
        if (payload.getEmail() != null) {
            String newEmail = payload.getEmail();
            if (!newEmail.equalsIgnoreCase(exist.getEmail()) && khachHangRepository.existsByEmail(newEmail)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists: " + newEmail);
            }
            exist.setEmail(newEmail);
        }

        // map gioiTinh string to Boolean (same rules as create)
        String g = payload.getGioiTinh();
        if (g != null) {
            String low = g.trim().toLowerCase();
            if (low.equals("male") || low.equals("nam") || low.equals("true")) exist.setGioiTinh(Boolean.TRUE);
            else if (low.equals("female") || low.equals("nu") || low.equals("nữ") || low.equals("false")) exist.setGioiTinh(Boolean.FALSE);
            else exist.setGioiTinh(null);
        }

        // update status if provided
        if (payload.getTrangThai() != null) {
            exist.setTrangThai(payload.getTrangThai());
        }

        // replace addresses if and only if payload contains addresses
        if (payload.getDiaChiKhachHangs() != null) {
            List<DiaChiKhachHang> newList = new ArrayList<>();
            boolean seenDefault = false;
            for (AddressDTO a : payload.getDiaChiKhachHangs()) {
                DiaChiKhachHang d = new DiaChiKhachHang();
                d.setDiaChiCuThe(a.getDiaChiCuThe());
                d.setXaPhuong(a.getXaPhuong());
                d.setQuanHuyen(a.getQuanHuyen());
                d.setThanhPhoTinh(a.getThanhPhoTinh());
                boolean isDef = a.getMacDinh() != null && a.getMacDinh();
                if (isDef && !seenDefault) { d.setMacDinh(true); seenDefault = true; }
                else d.setMacDinh(false);
                d.setKhachHang(exist);
                newList.add(d);
            }
            exist.getDiaChiKhachHangs().clear();
            exist.getDiaChiKhachHangs().addAll(newList);
        }

        KhachHang saved = khachHangRepository.save(exist);
        return ResponseEntity.ok(saved);
    }

    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody CustomerCreateDTO payload) {
        if (payload == null) return ResponseEntity.badRequest().body("Empty payload");

        // basic validation: email uniqueness
        if (payload.getEmail() != null && khachHangRepository.existsByEmail(payload.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists: " + payload.getEmail());
        }

        KhachHang kh = new KhachHang();
        kh.setHoTenKhachHang(payload.getHoTenKhachHang());
        kh.setSoDienThoai(payload.getSoDienThoai());
        kh.setEmail(payload.getEmail());

        // ensure non-nullable fields have defaults to avoid DB constraint errors
        if (kh.getMatKhau() == null) kh.setMatKhau("");
        if (kh.getAnhKhachHang() == null) kh.setAnhKhachHang("");
        // trangThai is NOT NULL in DB; default to true (active)
        kh.setTrangThai(Boolean.TRUE);

        // map gioiTinh string to Boolean
        String g = payload.getGioiTinh();
        if (g != null) {
            String low = g.trim().toLowerCase();
            if (low.equals("male") || low.equals("nam") || low.equals("true") ) kh.setGioiTinh(Boolean.TRUE);
            else if (low.equals("female") || low.equals("nu") || low.equals("nữ") || low.equals("false")) kh.setGioiTinh(Boolean.FALSE);
            else kh.setGioiTinh(null);
        } else kh.setGioiTinh(null);

        // map addresses
        List<DiaChiKhachHang> list = new ArrayList<>();
        if (payload.getDiaChiKhachHangs() != null) {
            boolean seenDefault = false;
            for (AddressDTO a : payload.getDiaChiKhachHangs()) {
                DiaChiKhachHang d = new DiaChiKhachHang();
                d.setDiaChiCuThe(a.getDiaChiCuThe());
                d.setXaPhuong(a.getXaPhuong());
                d.setQuanHuyen(a.getQuanHuyen());
                d.setThanhPhoTinh(a.getThanhPhoTinh());
                boolean isDef = a.getMacDinh() != null && a.getMacDinh();
                if (isDef && !seenDefault) { d.setMacDinh(true); seenDefault = true; }
                else d.setMacDinh(false);
                d.setKhachHang(kh);
                list.add(d);
            }
        }
        kh.setDiaChiKhachHangs(list);

        try {
            KhachHang saved = khachHangRepository.save(kh);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // return a readable 409 rather than 500 for common constraint violations
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Data integrity error: " + ex.getMessage());
        }
    }
}
