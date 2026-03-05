package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.KhachHang;

import java.util.List;
import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {

    // Fetch customers with their addresses to avoid N+1 problems
    @Query("select distinct k from KhachHang k left join fetch k.diaChiKhachHangs")
    List<KhachHang> findAllWithAddresses();

    // Fetch single customer with addresses to support edit form loading
    @Query("select k from KhachHang k left join fetch k.diaChiKhachHangs where k.id = ?1")
    Optional<KhachHang> findByIdWithAddresses(Integer id);

    // check existence by email to prevent unique constraint violations
    boolean existsByEmail(String email);

    // find by username or email for authentication
    java.util.Optional<KhachHang> findByTenTaiKhoan(String tenTaiKhoan);
    java.util.Optional<KhachHang> findByEmail(String email);

}
