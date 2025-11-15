package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.MauSac;

@Repository
public interface MauSacRepository extends JpaRepository<MauSac, Integer> {
    @Query("SELECT m FROM MauSac m WHERE m.tenMauSac = :tenMauSac")
    MauSac findByTenMauSac(@Param("tenMauSac") String tenMauSac);
}
