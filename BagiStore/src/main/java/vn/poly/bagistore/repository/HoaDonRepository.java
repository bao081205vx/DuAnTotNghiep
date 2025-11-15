package vn.poly.bagistore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.poly.bagistore.model.HoaDon;

import java.util.List;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {

    // Lightweight projection query to reduce payload
    @Query("select h from HoaDon h left join fetch h.khachHang left join fetch h.nhanVien order by h.ngayTao desc")
    List<HoaDon> findAllWithRelations();

    // Note: avoid fetching multiple collection-valued associations (bags) in a single query
    // to prevent Hibernate MultipleBagFetchException. We fetch the primary collections needed
    // for the invoice detail (chiTietHoaDons and nested product images) and load payments
    // in a separate query via findPaymentsByHoaDonId.
    @Query("select h from HoaDon h left join fetch h.khachHang left join fetch h.nhanVien left join fetch h.chiTietHoaDons cthd left join fetch cthd.sanPhamChiTiet spct left join fetch spct.anhSanPham where h.id = :id")
    HoaDon findByIdWithRelations(Integer id);

    @Query("select l from LichSuThanhToan l left join fetch l.phuongThucThanhToan where l.hoaDon.id = :hoaDonId order by l.ngayThanhToan")
    java.util.List<vn.poly.bagistore.model.LichSuThanhToan> findPaymentsByHoaDonId(Integer hoaDonId);

    // Return distinct non-null invoice type strings (loaiHoaDon)
    @Query("select distinct h.loaiHoaDon from HoaDon h where h.loaiHoaDon is not null")
    List<String> findDistinctLoaiHoaDon();

    // Find invoices between two datetimes (used for revenue aggregation)
    List<HoaDon> findByNgayTaoBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);
}
