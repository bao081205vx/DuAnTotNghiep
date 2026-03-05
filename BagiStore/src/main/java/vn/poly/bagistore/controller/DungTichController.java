package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.poly.bagistore.Service.DungTichService;
import vn.poly.bagistore.model.DungTich;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dung-tich")
public class DungTichController {

    @Autowired
    private DungTichService dungTichService;

    // Lấy tất cả dung tích
    @GetMapping
    public ResponseEntity<List<DungTich>> getAllDungTich() {
        try {
            List<DungTich> dungTichList = dungTichService.findAllDungTich();
            return new ResponseEntity<>(dungTichList, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Lấy dung tích theo ID
    @GetMapping("/{id}")
    public ResponseEntity<DungTich> getDungTichById(@PathVariable Integer id) {
        try {
            Optional<DungTich> dungTich = dungTichService.findById(id);
            if (dungTich.isPresent()) {
                return new ResponseEntity<>(dungTich.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Tạo dung tích mới
    @PostMapping
    public ResponseEntity<?> createDungTich(@RequestBody DungTich dungTich) {
        try {
            // Validate dữ liệu
            if (dungTich.getTenDungTich() == null || dungTich.getTenDungTich().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tên dung tích không được để trống");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            DungTich savedDungTich = dungTichService.saveDungTich(dungTich);
            return new ResponseEntity<>(savedDungTich, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi tạo dung tích");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Cập nhật dung tích
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDungTich(@PathVariable Integer id, @RequestBody DungTich dungTichDetails) {
        try {
            // Validate dữ liệu
            if (dungTichDetails.getTenDungTich() == null || dungTichDetails.getTenDungTich().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tên dung tích không được để trống");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            DungTich updatedDungTich = dungTichService.updateDungTich(id, dungTichDetails);
            return new ResponseEntity<>(updatedDungTich, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi cập nhật dung tích");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Xóa dung tích
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDungTich(@PathVariable Integer id) {
        try {
            dungTichService.deleteDungTich(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa dung tích thành công");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi xóa dung tích");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Tìm kiếm dung tích theo tên
    @GetMapping("/search")
    public ResponseEntity<List<DungTich>> searchDungTich(@RequestParam String tenDungTich) {
        try {
            List<DungTich> dungTichList = dungTichService.searchByTenDungTich(tenDungTich);
            return new ResponseEntity<>(dungTichList, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Kiểm tra tên dung tích đã tồn tại chưa
    @GetMapping("/check-exists")
    public ResponseEntity<Map<String, Boolean>> checkTenDungTichExists(@RequestParam String tenDungTich) {
        try {
            boolean exists = dungTichService.existsByTenDungTich(tenDungTich);
            Map<String, Boolean> response = new HashMap<>();
            response.put("exists", exists);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}