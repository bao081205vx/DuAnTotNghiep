package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.AnhSanPham;
@Repository
public interface AnhSanPhamRepository extends JpaRepository<AnhSanPham, Integer> {
}
