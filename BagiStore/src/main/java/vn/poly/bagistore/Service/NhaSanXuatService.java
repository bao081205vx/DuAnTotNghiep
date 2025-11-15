package vn.poly.bagistore.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.poly.bagistore.model.NhaSanXuat;
import vn.poly.bagistore.repository.NhaSanXuatRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NhaSanXuatService {

    @Autowired
    private NhaSanXuatRepository nhaSanXuatRepository;

    public List<NhaSanXuat> findAllNhaSanXuat() {
        return nhaSanXuatRepository.findAllOrderByNgayTaoDesc();
    }

    public Optional<NhaSanXuat> findById(Integer id) {
        return nhaSanXuatRepository.findById(id);
    }

    public NhaSanXuat saveNhaSanXuat(NhaSanXuat nhaSanXuat) {
        // Kiểm tra nếu tên nhà sản xuất đã tồn tại
        NhaSanXuat existing = nhaSanXuatRepository.findByTenNhaSanXuat(nhaSanXuat.getTenNhaSanXuat());
        if (existing != null && !existing.getId().equals(nhaSanXuat.getId())) {
            throw new RuntimeException("Tên nhà sản xuất đã tồn tại");
        }

        // Tạo mã nhà sản xuất tự động nếu là mới
        if (nhaSanXuat.getId() == null) {
            String maNhaSanXuat = "NSX" + System.currentTimeMillis();
            nhaSanXuat.setMaNhaSanXuat(maNhaSanXuat);
            nhaSanXuat.setNgayTao(LocalDateTime.now());
        }

        // Đảm bảo trạng thái luôn là true (hoạt động) khi tạo mới
        if (nhaSanXuat.getTrangThai() == null) {
            nhaSanXuat.setTrangThai(true);
        }

        return nhaSanXuatRepository.save(nhaSanXuat);
    }

    public NhaSanXuat updateNhaSanXuat(Integer id, NhaSanXuat nhaSanXuatDetails) {
        Optional<NhaSanXuat> optionalNhaSanXuat = nhaSanXuatRepository.findById(id);
        if (optionalNhaSanXuat.isPresent()) {
            NhaSanXuat existingNhaSanXuat = optionalNhaSanXuat.get();

            // Kiểm tra nếu tên nhà sản xuất đã tồn tại (trừ chính nó)
            NhaSanXuat duplicate = nhaSanXuatRepository.findByTenNhaSanXuat(nhaSanXuatDetails.getTenNhaSanXuat());
            if (duplicate != null && !duplicate.getId().equals(id)) {
                throw new RuntimeException("Tên nhà sản xuất đã tồn tại");
            }

            // Cập nhật tên nhà sản xuất
            existingNhaSanXuat.setTenNhaSanXuat(nhaSanXuatDetails.getTenNhaSanXuat());
            // Nếu client gửi trangThai, cho phép cập nhật trạng thái
            if (nhaSanXuatDetails.getTrangThai() != null) {
                existingNhaSanXuat.setTrangThai(nhaSanXuatDetails.getTrangThai());
            }

            return nhaSanXuatRepository.save(existingNhaSanXuat);
        } else {
            throw new RuntimeException("Không tìm thấy nhà sản xuất với ID: " + id);
        }
    }

    public void deleteNhaSanXuat(Integer id) {
        Optional<NhaSanXuat> optionalNhaSanXuat = nhaSanXuatRepository.findById(id);
        if (optionalNhaSanXuat.isPresent()) {
            nhaSanXuatRepository.deleteById(id);
        } else {
            throw new RuntimeException("Không tìm thấy nhà sản xuất với ID: " + id);
        }
    }

    public List<NhaSanXuat> searchByTenNhaSanXuat(String tenNhaSanXuat) {
        return nhaSanXuatRepository.findByTenNhaSanXuatContaining(tenNhaSanXuat);
    }

    public boolean existsByTenNhaSanXuat(String tenNhaSanXuat) {
        return nhaSanXuatRepository.findByTenNhaSanXuat(tenNhaSanXuat) != null;
    }
}