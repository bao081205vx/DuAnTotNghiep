package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.TrongLuong;

import java.util.List;

@Repository
public interface TrongLuongRepository extends JpaRepository<TrongLuong, Integer> {
    @Query("SELECT t FROM TrongLuong t WHERE t.tenTrongLuong = :tenTrongLuong")
    TrongLuong findByTenTrongLuong(@Param("tenTrongLuong") String tenTrongLuong);
}
