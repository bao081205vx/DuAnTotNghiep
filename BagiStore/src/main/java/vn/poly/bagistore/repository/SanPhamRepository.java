package vn.poly.bagistore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.SanPham;

import java.util.List;

@Repository
// SanPhamRepository.java
public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    // Lấy danh sách tên sản phẩm không trùng
    @Query("SELECT DISTINCT sp.tenSanPham FROM SanPham sp WHERE sp.tenSanPham IS NOT NULL ORDER BY sp.tenSanPham")
    List<String> findDistinctProductNames();

    // Tìm kiếm tên sản phẩm theo keyword
    @Query("SELECT DISTINCT sp.tenSanPham FROM SanPham sp WHERE " +
            "LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND sp.tenSanPham IS NOT NULL ORDER BY sp.tenSanPham")
    List<String> findDistinctProductNamesByKeyword(String keyword);
}
