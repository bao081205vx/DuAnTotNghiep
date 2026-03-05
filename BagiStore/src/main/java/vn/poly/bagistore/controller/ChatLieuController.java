package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.poly.bagistore.Service.ChatLieuService;
import vn.poly.bagistore.model.ChatLieu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat-lieu")
public class ChatLieuController {

    @Autowired
    private ChatLieuService chatLieuService;

    // Lấy tất cả chất liệu
    @GetMapping
    public ResponseEntity<List<ChatLieu>> getAllChatLieu() {
        try {
            List<ChatLieu> chatLieuList = chatLieuService.findAllChatLieu();
            return new ResponseEntity<>(chatLieuList, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Lấy chất liệu theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ChatLieu> getChatLieuById(@PathVariable Integer id) {
        try {
            Optional<ChatLieu> chatLieu = chatLieuService.findById(id);
            if (chatLieu.isPresent()) {
                return new ResponseEntity<>(chatLieu.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Tạo chất liệu mới
    @PostMapping
    public ResponseEntity<?> createChatLieu(@RequestBody ChatLieu chatLieu) {
        try {
            // Validate dữ liệu
            if (chatLieu.getTenChatLieu() == null || chatLieu.getTenChatLieu().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tên chất liệu không được để trống");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            ChatLieu savedChatLieu = chatLieuService.saveChatLieu(chatLieu);
            return new ResponseEntity<>(savedChatLieu, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi tạo chất liệu");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Cập nhật chất liệu
    @PutMapping("/{id}")
    public ResponseEntity<?> updateChatLieu(@PathVariable Integer id, @RequestBody ChatLieu chatLieuDetails) {
        try {
            // Validate dữ liệu
            if (chatLieuDetails.getTenChatLieu() == null || chatLieuDetails.getTenChatLieu().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tên chất liệu không được để trống");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            ChatLieu updatedChatLieu = chatLieuService.updateChatLieu(id, chatLieuDetails);
            return new ResponseEntity<>(updatedChatLieu, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi cập nhật chất liệu");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Xóa chất liệu
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteChatLieu(@PathVariable Integer id) {
        try {
            chatLieuService.deleteChatLieu(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa chất liệu thành công");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi xóa chất liệu");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Tìm kiếm chất liệu theo tên
    @GetMapping("/search")
    public ResponseEntity<List<ChatLieu>> searchChatLieu(@RequestParam String tenChatLieu) {
        try {
            List<ChatLieu> chatLieuList = chatLieuService.searchByTenChatLieu(tenChatLieu);
            return new ResponseEntity<>(chatLieuList, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Kiểm tra tên chất liệu đã tồn tại chưa
    @GetMapping("/check-exists")
    public ResponseEntity<Map<String, Boolean>> checkTenChatLieuExists(@RequestParam String tenChatLieu) {
        try {
            boolean exists = chatLieuService.existsByTenChatLieu(tenChatLieu);
            Map<String, Boolean> response = new HashMap<>();
            response.put("exists", exists);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}