package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.KichThuoc;

@Repository
public interface KichThuocRepository extends JpaRepository<KichThuoc, Integer> {
    @Query("SELECT k FROM KichThuoc k WHERE k.tenKichThuoc = :tenKichThuoc")
    KichThuoc findByTenKichThuoc(@Param("tenKichThuoc") String tenKichThuoc);
}
