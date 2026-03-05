package vn.poly.bagistore.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.poly.bagistore.model.PhieuGiamGia;
import vn.poly.bagistore.repository.PhieuGiamGiaRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PhieuGiamGiaAppService {

    @Autowired
    private PhieuGiamGiaRepository repository;

    public List<PhieuGiamGia> findAll() {
        return repository.findAll();
    }

    public Optional<PhieuGiamGia> findById(Integer id) {
        return repository.findById(id);
    }

    public PhieuGiamGia save(PhieuGiamGia entity) {
        return repository.save(entity);
    }

}
