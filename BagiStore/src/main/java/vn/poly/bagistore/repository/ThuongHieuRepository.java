package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.ThuongHieu;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, Integer> {

    @Query("SELECT t FROM ThuongHieu t WHERE t.tenThuongHieu = :tenThuongHieu")
    ThuongHieu findByTenThuongHieu(@Param("tenThuongHieu") String tenThuongHieu);

    @Query("SELECT t FROM ThuongHieu t WHERE t.tenThuongHieu LIKE %:tenThuongHieu%")
    List<ThuongHieu> findByTenThuongHieuContaining(@Param("tenThuongHieu") String tenThuongHieu);

    Optional<ThuongHieu> findByMaThuongHieu(String maThuongHieu);

    List<ThuongHieu> findByTrangThai(Boolean trangThai);

    @Query("SELECT t FROM ThuongHieu t ORDER BY t.ngayTao DESC")
    List<ThuongHieu> findAllOrderByNgayTaoDesc();
}