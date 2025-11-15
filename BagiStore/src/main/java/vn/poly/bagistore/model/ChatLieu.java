package vn.poly.bagistore.model;

import lombok.Data;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_lieu")
@Data
public class ChatLieu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_chat_lieu", insertable = false, updatable = false)
    private String maChatLieu;

    @Column(name = "ten_chat_lieu", nullable = false)
    private String tenChatLieu;
    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;
    @Column(name = "trang_thai", nullable = false, columnDefinition = "BIT DEFAULT 1")
    private Boolean trangThai;
    @PrePersist
    protected void onCreate() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
        if (trangThai == null) {
            trangThai = true;
        }
        if (maChatLieu == null) {
            // Tự động tạo mã chất liệu nếu chưa có
            maChatLieu = "CL" + System.currentTimeMillis();
        }
    }
    @Override
    public String toString() {
        return "ChatLieu{" +
                "id=" + id +
                ", maChatLieu='" + maChatLieu + '\'' +
                ", tenChatLieu='" + tenChatLieu + '\'' +
                ", ngayTao=" + ngayTao +
                ", trangThai=" + trangThai +
                '}';
    }
}
