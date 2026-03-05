package vn.poly.bagistore.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.poly.bagistore.model.XuatXu;
import vn.poly.bagistore.repository.XuatXuRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class XuatXuService {

    @Autowired
    private XuatXuRepository xuatXuRepository;

    public List<XuatXu> findAllXuatXu() {
        return xuatXuRepository.findAllOrderByNgayTaoDesc();
    }

    public Optional<XuatXu> findById(Integer id) {
        return xuatXuRepository.findById(id);
    }

    public XuatXu saveXuatXu(XuatXu xuatXu) {
        // Kiểm tra nếu tên xuất xứ đã tồn tại
        XuatXu existing = xuatXuRepository.findByTenXuatXu(xuatXu.getTenXuatXu());
        if (existing != null && !existing.getId().equals(xuatXu.getId())) {
            throw new RuntimeException("Tên xuất xứ đã tồn tại");
        }

        // Tạo mã xuất xứ tự động nếu là mới
        if (xuatXu.getId() == null) {
            String maXuatXu = "XX" + System.currentTimeMillis();
            xuatXu.setMaXuatXu(maXuatXu);
            xuatXu.setNgayTao(LocalDateTime.now());
        }

        // Đảm bảo trạng thái luôn là true (hoạt động) khi tạo mới
        if (xuatXu.getTrangThai() == null) {
            xuatXu.setTrangThai(true);
        }

        return xuatXuRepository.save(xuatXu);
    }

    public XuatXu updateXuatXu(Integer id, XuatXu xuatXuDetails) {
        Optional<XuatXu> optionalXuatXu = xuatXuRepository.findById(id);
        if (optionalXuatXu.isPresent()) {
            XuatXu existingXuatXu = optionalXuatXu.get();

            // Kiểm tra nếu tên xuất xứ đã tồn tại (trừ chính nó)
            XuatXu duplicate = xuatXuRepository.findByTenXuatXu(xuatXuDetails.getTenXuatXu());
            if (duplicate != null && !duplicate.getId().equals(id)) {
                throw new RuntimeException("Tên xuất xứ đã tồn tại");
            }

            // Cập nhật tên xuất xứ
            existingXuatXu.setTenXuatXu(xuatXuDetails.getTenXuatXu());
            // Nếu client gửi trangThai, cho phép cập nhật trạng thái
            if (xuatXuDetails.getTrangThai() != null) {
                existingXuatXu.setTrangThai(xuatXuDetails.getTrangThai());
            }

            return xuatXuRepository.save(existingXuatXu);
        } else {
            throw new RuntimeException("Không tìm thấy xuất xứ với ID: " + id);
        }
    }

    public void deleteXuatXu(Integer id) {
        Optional<XuatXu> optionalXuatXu = xuatXuRepository.findById(id);
        if (optionalXuatXu.isPresent()) {
            xuatXuRepository.deleteById(id);
        } else {
            throw new RuntimeException("Không tìm thấy xuất xứ với ID: " + id);
        }
    }

    public List<XuatXu> searchByTenXuatXu(String tenXuatXu) {
        return xuatXuRepository.findByTenXuatXuContaining(tenXuatXu);
    }

    public boolean existsByTenXuatXu(String tenXuatXu) {
        return xuatXuRepository.findByTenXuatXu(tenXuatXu) != null;
    }
}