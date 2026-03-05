package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.poly.bagistore.Service.ThuongHieuService;
import vn.poly.bagistore.model.ThuongHieu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/thuong-hieu")
public class ThuongHieuController {

    @Autowired
    private ThuongHieuService thuongHieuService;

    // Lấy tất cả thương hiệu
    @GetMapping
    public ResponseEntity<List<ThuongHieu>> getAllThuongHieu() {
        try {
            List<ThuongHieu> thuongHieuList = thuongHieuService.findAllThuongHieu();
            return new ResponseEntity<>(thuongHieuList, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Lấy thương hiệu theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ThuongHieu> getThuongHieuById(@PathVariable Integer id) {
        try {
            Optional<ThuongHieu> thuongHieu = thuongHieuService.findById(id);
            if (thuongHieu.isPresent()) {
                return new ResponseEntity<>(thuongHieu.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Tạo thương hiệu mới
    @PostMapping
    public ResponseEntity<?> createThuongHieu(@RequestBody ThuongHieu thuongHieu) {
        try {
            // Validate dữ liệu
            if (thuongHieu.getTenThuongHieu() == null || thuongHieu.getTenThuongHieu().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tên thương hiệu không được để trống");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            ThuongHieu savedThuongHieu = thuongHieuService.saveThuongHieu(thuongHieu);
            return new ResponseEntity<>(savedThuongHieu, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi tạo thương hiệu");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Cập nhật thương hiệu
    @PutMapping("/{id}")
    public ResponseEntity<?> updateThuongHieu(@PathVariable Integer id, @RequestBody ThuongHieu thuongHieuDetails) {
        try {
            // Validate dữ liệu
            if (thuongHieuDetails.getTenThuongHieu() == null || thuongHieuDetails.getTenThuongHieu().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tên thương hiệu không được để trống");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            ThuongHieu updatedThuongHieu = thuongHieuService.updateThuongHieu(id, thuongHieuDetails);
            return new ResponseEntity<>(updatedThuongHieu, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi cập nhật thương hiệu");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Xóa thương hiệu
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteThuongHieu(@PathVariable Integer id) {
        try {
            thuongHieuService.deleteThuongHieu(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa thương hiệu thành công");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi xóa thương hiệu");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Tìm kiếm thương hiệu theo tên
    @GetMapping("/search")
    public ResponseEntity<List<ThuongHieu>> searchThuongHieu(@RequestParam String tenThuongHieu) {
        try {
            List<ThuongHieu> thuongHieuList = thuongHieuService.searchByTenThuongHieu(tenThuongHieu);
            return new ResponseEntity<>(thuongHieuList, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Kiểm tra tên thương hiệu đã tồn tại chưa
    @GetMapping("/check-exists")
    public ResponseEntity<Map<String, Boolean>> checkTenThuongHieuExists(@RequestParam String tenThuongHieu) {
        try {
            boolean exists = thuongHieuService.existsByTenThuongHieu(tenThuongHieu);
            Map<String, Boolean> response = new HashMap<>();
            response.put("exists", exists);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}