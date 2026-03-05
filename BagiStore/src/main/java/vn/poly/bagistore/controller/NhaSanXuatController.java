package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.poly.bagistore.Service.NhaSanXuatService;
import vn.poly.bagistore.model.NhaSanXuat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/nha-san-xuat")
public class NhaSanXuatController {

    @Autowired
    private NhaSanXuatService nhaSanXuatService;

    // Lấy tất cả nhà sản xuất
    @GetMapping
    public ResponseEntity<List<NhaSanXuat>> getAllNhaSanXuat() {
        try {
            List<NhaSanXuat> nhaSanXuatList = nhaSanXuatService.findAllNhaSanXuat();
            return new ResponseEntity<>(nhaSanXuatList, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Lấy nhà sản xuất theo ID
    @GetMapping("/{id}")
    public ResponseEntity<NhaSanXuat> getNhaSanXuatById(@PathVariable Integer id) {
        try {
            Optional<NhaSanXuat> nhaSanXuat = nhaSanXuatService.findById(id);
            if (nhaSanXuat.isPresent()) {
                return new ResponseEntity<>(nhaSanXuat.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Tạo nhà sản xuất mới
    @PostMapping
    public ResponseEntity<?> createNhaSanXuat(@RequestBody NhaSanXuat nhaSanXuat) {
        try {
            // Validate dữ liệu
            if (nhaSanXuat.getTenNhaSanXuat() == null || nhaSanXuat.getTenNhaSanXuat().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tên nhà sản xuất không được để trống");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            NhaSanXuat savedNhaSanXuat = nhaSanXuatService.saveNhaSanXuat(nhaSanXuat);
            return new ResponseEntity<>(savedNhaSanXuat, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi tạo nhà sản xuất");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Cập nhật nhà sản xuất
    @PutMapping("/{id}")
    public ResponseEntity<?> updateNhaSanXuat(@PathVariable Integer id, @RequestBody NhaSanXuat nhaSanXuatDetails) {
        try {
            // Validate dữ liệu
            if (nhaSanXuatDetails.getTenNhaSanXuat() == null || nhaSanXuatDetails.getTenNhaSanXuat().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tên nhà sản xuất không được để trống");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            NhaSanXuat updatedNhaSanXuat = nhaSanXuatService.updateNhaSanXuat(id, nhaSanXuatDetails);
            return new ResponseEntity<>(updatedNhaSanXuat, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi cập nhật nhà sản xuất");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Xóa nhà sản xuất
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNhaSanXuat(@PathVariable Integer id) {
        try {
            nhaSanXuatService.deleteNhaSanXuat(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa nhà sản xuất thành công");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi xóa nhà sản xuất");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Tìm kiếm nhà sản xuất theo tên
    @GetMapping("/search")
    public ResponseEntity<List<NhaSanXuat>> searchNhaSanXuat(@RequestParam String tenNhaSanXuat) {
        try {
            List<NhaSanXuat> nhaSanXuatList = nhaSanXuatService.searchByTenNhaSanXuat(tenNhaSanXuat);
            return new ResponseEntity<>(nhaSanXuatList, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Kiểm tra tên nhà sản xuất đã tồn tại chưa
    @GetMapping("/check-exists")
    public ResponseEntity<Map<String, Boolean>> checkTenNhaSanXuatExists(@RequestParam String tenNhaSanXuat) {
        try {
            boolean exists = nhaSanXuatService.existsByTenNhaSanXuat(tenNhaSanXuat);
            Map<String, Boolean> response = new HashMap<>();
            response.put("exists", exists);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}