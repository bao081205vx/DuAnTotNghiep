package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.NhaSanXuat;

import java.util.List;
import java.util.Optional;

@Repository
public interface NhaSanXuatRepository extends JpaRepository<NhaSanXuat, Integer> {

    @Query("SELECT n FROM NhaSanXuat n WHERE n.tenNhaSanXuat = :tenNhaSanXuat")
    NhaSanXuat findByTenNhaSanXuat(@Param("tenNhaSanXuat") String tenNhaSanXuat);

    @Query("SELECT n FROM NhaSanXuat n WHERE n.tenNhaSanXuat LIKE %:tenNhaSanXuat%")
    List<NhaSanXuat> findByTenNhaSanXuatContaining(@Param("tenNhaSanXuat") String tenNhaSanXuat);

    Optional<NhaSanXuat> findByMaNhaSanXuat(String maNhaSanXuat);

    List<NhaSanXuat> findByTrangThai(Boolean trangThai);

    @Query("SELECT n FROM NhaSanXuat n ORDER BY n.ngayTao DESC")
    List<NhaSanXuat> findAllOrderByNgayTaoDesc();
}