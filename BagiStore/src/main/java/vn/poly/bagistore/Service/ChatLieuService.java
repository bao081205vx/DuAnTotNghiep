package vn.poly.bagistore.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.poly.bagistore.model.ChatLieu;
import vn.poly.bagistore.repository.ChatLieuRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChatLieuService {

    @Autowired
    private ChatLieuRepository chatLieuRepository;

    public List<ChatLieu> findAllChatLieu() {
        return chatLieuRepository.findAllOrderByNgayTaoDesc();
    }

    public Optional<ChatLieu> findById(Integer id) {
        return chatLieuRepository.findById(id);
    }

    public ChatLieu saveChatLieu(ChatLieu chatLieu) {
        // Kiểm tra nếu tên chất liệu đã tồn tại
        ChatLieu existing = chatLieuRepository.findByTenChatLieu(chatLieu.getTenChatLieu());
        if (existing != null && !existing.getId().equals(chatLieu.getId())) {
            throw new RuntimeException("Tên chất liệu đã tồn tại");
        }

        // Tạo mã chất liệu tự động nếu là mới
        if (chatLieu.getId() == null) {
            String maChatLieu = "CL" + System.currentTimeMillis();
            chatLieu.setMaChatLieu(maChatLieu);
            chatLieu.setNgayTao(LocalDateTime.now());
        }

        // Đảm bảo trạng thái luôn là true (hoạt động) khi tạo mới
        if (chatLieu.getTrangThai() == null) {
            chatLieu.setTrangThai(true);
        }

        return chatLieuRepository.save(chatLieu);
    }

    public ChatLieu updateChatLieu(Integer id, ChatLieu chatLieuDetails) {
        Optional<ChatLieu> optionalChatLieu = chatLieuRepository.findById(id);
        if (optionalChatLieu.isPresent()) {
            ChatLieu existingChatLieu = optionalChatLieu.get();

            // Kiểm tra nếu tên chất liệu đã tồn tại (trừ chính nó)
            ChatLieu duplicate = chatLieuRepository.findByTenChatLieu(chatLieuDetails.getTenChatLieu());
            if (duplicate != null && !duplicate.getId().equals(id)) {
                throw new RuntimeException("Tên chất liệu đã tồn tại");
            }

            // Cập nhật tên chất liệu
            existingChatLieu.setTenChatLieu(chatLieuDetails.getTenChatLieu());
            // Nếu client gửi trangThai, cho phép cập nhật trạng thái
            if (chatLieuDetails.getTrangThai() != null) {
                existingChatLieu.setTrangThai(chatLieuDetails.getTrangThai());
            }

            return chatLieuRepository.save(existingChatLieu);
        } else {
            throw new RuntimeException("Không tìm thấy chất liệu với ID: " + id);
        }
    }

    public void deleteChatLieu(Integer id) {
        Optional<ChatLieu> optionalChatLieu = chatLieuRepository.findById(id);
        if (optionalChatLieu.isPresent()) {
            chatLieuRepository.deleteById(id);
        } else {
            throw new RuntimeException("Không tìm thấy chất liệu với ID: " + id);
        }
    }

    public List<ChatLieu> searchByTenChatLieu(String tenChatLieu) {
        return chatLieuRepository.findByTenChatLieuContaining(tenChatLieu);
    }

    public boolean existsByTenChatLieu(String tenChatLieu) {
        return chatLieuRepository.findByTenChatLieu(tenChatLieu) != null;
    }
}