package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.ChatLieu;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatLieuRepository extends JpaRepository<ChatLieu, Integer> {

    @Query("SELECT c FROM ChatLieu c WHERE c.tenChatLieu = :tenChatLieu")
    ChatLieu findByTenChatLieu(@Param("tenChatLieu") String tenChatLieu);

    @Query("SELECT c FROM ChatLieu c WHERE c.tenChatLieu LIKE %:tenChatLieu%")
    List<ChatLieu> findByTenChatLieuContaining(@Param("tenChatLieu") String tenChatLieu);

    Optional<ChatLieu> findByMaChatLieu(String maChatLieu);

    List<ChatLieu> findByTrangThai(Boolean trangThai);

    @Query("SELECT c FROM ChatLieu c ORDER BY c.ngayTao DESC")
    List<ChatLieu> findAllOrderByNgayTaoDesc();
}