package vn.poly.bagistore.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.poly.bagistore.model.DungTich;
import vn.poly.bagistore.repository.DungTichRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DungTichService {

    @Autowired
    private DungTichRepository dungTichRepository;

    public List<DungTich> findAllDungTich() {
        return dungTichRepository.findAllOrderByNgayTaoDesc();
    }

    public Optional<DungTich> findById(Integer id) {
        return dungTichRepository.findById(id);
    }

    public DungTich saveDungTich(DungTich dungTich) {
        // Kiểm tra nếu tên dung tích đã tồn tại
        DungTich existing = dungTichRepository.findByDungTich(dungTich.getTenDungTich());
        if (existing != null && !existing.getId().equals(dungTich.getId())) {
            throw new RuntimeException("Tên dung tích đã tồn tại");
        }

        // Tạo mã dung tích tự động nếu là mới
        if (dungTich.getId() == null) {
            String maDungTich = "DT" + System.currentTimeMillis();
            dungTich.setMaDungTich(maDungTich);
            dungTich.setNgayTao(LocalDateTime.now());
        }

        // Đảm bảo trạng thái luôn là true (hoạt động) khi tạo mới
        if (dungTich.getTrangThai() == null) {
            dungTich.setTrangThai(true);
        }

        return dungTichRepository.save(dungTich);
    }

    public DungTich updateDungTich(Integer id, DungTich dungTichDetails) {
        Optional<DungTich> optionalDungTich = dungTichRepository.findById(id);
        if (optionalDungTich.isPresent()) {
            DungTich existingDungTich = optionalDungTich.get();

            // Kiểm tra nếu tên dung tích đã tồn tại (trừ chính nó)
            DungTich duplicate = dungTichRepository.findByDungTich(dungTichDetails.getTenDungTich());
            if (duplicate != null && !duplicate.getId().equals(id)) {
                throw new RuntimeException("Tên dung tích đã tồn tại");
            }

            // Cập nhật tên dung tích
            existingDungTich.setTenDungTich(dungTichDetails.getTenDungTich());
            // Nếu client gửi trangThai, cho phép cập nhật trạng thái
            if (dungTichDetails.getTrangThai() != null) {
                existingDungTich.setTrangThai(dungTichDetails.getTrangThai());
            }

            return dungTichRepository.save(existingDungTich);
        } else {
            throw new RuntimeException("Không tìm thấy dung tích với ID: " + id);
        }
    }

    public void deleteDungTich(Integer id) {
        Optional<DungTich> optionalDungTich = dungTichRepository.findById(id);
        if (optionalDungTich.isPresent()) {
            dungTichRepository.deleteById(id);
        } else {
            throw new RuntimeException("Không tìm thấy dung tích với ID: " + id);
        }
    }

    public List<DungTich> searchByTenDungTich(String tenDungTich) {
        return dungTichRepository.findByTenDungTichContaining(tenDungTich);
    }

    public boolean existsByTenDungTich(String tenDungTich) {
        return dungTichRepository.findByDungTich(tenDungTich) != null;
    }
}