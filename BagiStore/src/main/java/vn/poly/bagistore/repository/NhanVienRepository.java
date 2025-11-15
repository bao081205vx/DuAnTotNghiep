package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.NhanVien;
import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {
    Optional<NhanVien> findByEmail(String email);
    Optional<NhanVien> findByTenDangNhap(String tenDangNhap);
    boolean existsByEmail(String email);
}
