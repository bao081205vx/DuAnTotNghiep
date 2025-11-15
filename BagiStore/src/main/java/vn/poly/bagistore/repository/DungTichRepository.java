package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.DungTich;

import java.util.List;
import java.util.Optional;

@Repository
public interface DungTichRepository extends JpaRepository<DungTich, Integer> {

    @Query("SELECT d FROM DungTich d WHERE d.tenDungTich = :tenDungTich")
    DungTich findByDungTich(@Param("tenDungTich") String tenDungTich);

    @Query("SELECT d FROM DungTich d WHERE d.tenDungTich LIKE %:tenDungTich%")
    List<DungTich> findByTenDungTichContaining(@Param("tenDungTich") String tenDungTich);

    Optional<DungTich> findByMaDungTich(String maDungTich);

    List<DungTich> findByTrangThai(Boolean trangThai);

    @Query("SELECT d FROM DungTich d ORDER BY d.ngayTao DESC")
    List<DungTich> findAllOrderByNgayTaoDesc();
}