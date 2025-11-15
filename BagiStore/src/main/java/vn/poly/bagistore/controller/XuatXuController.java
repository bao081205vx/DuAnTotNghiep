package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.poly.bagistore.Service.XuatXuService;
import vn.poly.bagistore.model.XuatXu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/xuat-xu")
public class XuatXuController {

    @Autowired
    private XuatXuService xuatXuService;

    // Lấy tất cả xuất xứ
    @GetMapping
    public ResponseEntity<List<XuatXu>> getAllXuatXu() {
        try {
            List<XuatXu> xuatXuList = xuatXuService.findAllXuatXu();
            return new ResponseEntity<>(xuatXuList, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Lấy xuất xứ theo ID
    @GetMapping("/{id}")
    public ResponseEntity<XuatXu> getXuatXuById(@PathVariable Integer id) {
        try {
            Optional<XuatXu> xuatXu = xuatXuService.findById(id);
            if (xuatXu.isPresent()) {
                return new ResponseEntity<>(xuatXu.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Tạo xuất xứ mới
    @PostMapping
    public ResponseEntity<?> createXuatXu(@RequestBody XuatXu xuatXu) {
        try {
            // Validate dữ liệu
            if (xuatXu.getTenXuatXu() == null || xuatXu.getTenXuatXu().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tên xuất xứ không được để trống");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            XuatXu savedXuatXu = xuatXuService.saveXuatXu(xuatXu);
            return new ResponseEntity<>(savedXuatXu, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi tạo xuất xứ");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Cập nhật xuất xứ
    @PutMapping("/{id}")
    public ResponseEntity<?> updateXuatXu(@PathVariable Integer id, @RequestBody XuatXu xuatXuDetails) {
        try {
            // Validate dữ liệu
            if (xuatXuDetails.getTenXuatXu() == null || xuatXuDetails.getTenXuatXu().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tên xuất xứ không được để trống");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            XuatXu updatedXuatXu = xuatXuService.updateXuatXu(id, xuatXuDetails);
            return new ResponseEntity<>(updatedXuatXu, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi cập nhật xuất xứ");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Xóa xuất xứ
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteXuatXu(@PathVariable Integer id) {
        try {
            xuatXuService.deleteXuatXu(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa xuất xứ thành công");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi xóa xuất xứ");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Tìm kiếm xuất xứ theo tên
    @GetMapping("/search")
    public ResponseEntity<List<XuatXu>> searchXuatXu(@RequestParam String tenXuatXu) {
        try {
            List<XuatXu> xuatXuList = xuatXuService.searchByTenXuatXu(tenXuatXu);
            return new ResponseEntity<>(xuatXuList, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Kiểm tra tên xuất xứ đã tồn tại chưa
    @GetMapping("/check-exists")
    public ResponseEntity<Map<String, Boolean>> checkTenXuatXuExists(@RequestParam String tenXuatXu) {
        try {
            boolean exists = xuatXuService.existsByTenXuatXu(tenXuatXu);
            Map<String, Boolean> response = new HashMap<>();
            response.put("exists", exists);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}