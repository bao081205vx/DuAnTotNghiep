package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.XuatXu;

import java.util.List;
import java.util.Optional;

@Repository
public interface XuatXuRepository extends JpaRepository<XuatXu, Integer> {

    @Query("SELECT x FROM XuatXu x WHERE x.tenXuatXu = :tenXuatXu")
    XuatXu findByTenXuatXu(@Param("tenXuatXu") String tenXuatXu);

    @Query("SELECT x FROM XuatXu x WHERE x.tenXuatXu LIKE %:tenXuatXu%")
    List<XuatXu> findByTenXuatXuContaining(@Param("tenXuatXu") String tenXuatXu);

    Optional<XuatXu> findByMaXuatXu(String maXuatXu);

    List<XuatXu> findByTrangThai(Boolean trangThai);

    @Query("SELECT x FROM XuatXu x ORDER BY x.ngayTao DESC")
    List<XuatXu> findAllOrderByNgayTaoDesc();
}