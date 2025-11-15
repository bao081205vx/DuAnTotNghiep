package vn.poly.bagistore.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.poly.bagistore.model.ThuongHieu;
import vn.poly.bagistore.repository.ThuongHieuRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ThuongHieuService {

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    public List<ThuongHieu> findAllThuongHieu() {
        return thuongHieuRepository.findAllOrderByNgayTaoDesc();
    }

    public Optional<ThuongHieu> findById(Integer id) {
        return thuongHieuRepository.findById(id);
    }

    public ThuongHieu saveThuongHieu(ThuongHieu thuongHieu) {
        // Kiểm tra nếu tên thương hiệu đã tồn tại
        ThuongHieu existing = thuongHieuRepository.findByTenThuongHieu(thuongHieu.getTenThuongHieu());
        if (existing != null && !existing.getId().equals(thuongHieu.getId())) {
            throw new RuntimeException("Tên thương hiệu đã tồn tại");
        }

        // Tạo mã thương hiệu tự động nếu là mới
        if (thuongHieu.getId() == null) {
            String maThuongHieu = "TH" + System.currentTimeMillis();
            thuongHieu.setMaThuongHieu(maThuongHieu);
            thuongHieu.setNgayTao(LocalDateTime.now());
        }

        // Đảm bảo trạng thái luôn là true (hoạt động) khi tạo mới
        if (thuongHieu.getTrangThai() == null) {
            thuongHieu.setTrangThai(true);
        }

        return thuongHieuRepository.save(thuongHieu);
    }

    public ThuongHieu updateThuongHieu(Integer id, ThuongHieu thuongHieuDetails) {
        Optional<ThuongHieu> optionalThuongHieu = thuongHieuRepository.findById(id);
        if (optionalThuongHieu.isPresent()) {
            ThuongHieu existingThuongHieu = optionalThuongHieu.get();

            // Kiểm tra nếu tên thương hiệu đã tồn tại (trừ chính nó)
            ThuongHieu duplicate = thuongHieuRepository.findByTenThuongHieu(thuongHieuDetails.getTenThuongHieu());
            if (duplicate != null && !duplicate.getId().equals(id)) {
                throw new RuntimeException("Tên thương hiệu đã tồn tại");
            }

            // Cập nhật tên thương hiệu
            existingThuongHieu.setTenThuongHieu(thuongHieuDetails.getTenThuongHieu());
            // Nếu client gửi trangThai, cho phép cập nhật trạng thái
            if (thuongHieuDetails.getTrangThai() != null) {
                existingThuongHieu.setTrangThai(thuongHieuDetails.getTrangThai());
            }

            return thuongHieuRepository.save(existingThuongHieu);
        } else {
            throw new RuntimeException("Không tìm thấy thương hiệu với ID: " + id);
        }
    }

    public void deleteThuongHieu(Integer id) {
        Optional<ThuongHieu> optionalThuongHieu = thuongHieuRepository.findById(id);
        if (optionalThuongHieu.isPresent()) {
            thuongHieuRepository.deleteById(id);
        } else {
            throw new RuntimeException("Không tìm thấy thương hiệu với ID: " + id);
        }
    }

    public List<ThuongHieu> searchByTenThuongHieu(String tenThuongHieu) {
        return thuongHieuRepository.findByTenThuongHieuContaining(tenThuongHieu);
    }

    public boolean existsByTenThuongHieu(String tenThuongHieu) {
        return thuongHieuRepository.findByTenThuongHieu(tenThuongHieu) != null;
    }
}