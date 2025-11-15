package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.SanPhamChiTiet;

import java.util.List;
import java.util.Optional;

@Repository
public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Integer> {

        @EntityGraph(attributePaths = {"sanPham", "mauSac", "kichThuoc", "trongLuong", "anhSanPham"})
        @Query("SELECT spct FROM SanPhamChiTiet spct WHERE spct.sanPham.id = :productId")
        List<SanPhamChiTiet> findBySanPhamId(@Param("productId") Integer productId);

        @EntityGraph(attributePaths = {"sanPham", "mauSac", "kichThuoc", "trongLuong", "anhSanPham"})
        Optional<SanPhamChiTiet> findById(Integer id);
}
