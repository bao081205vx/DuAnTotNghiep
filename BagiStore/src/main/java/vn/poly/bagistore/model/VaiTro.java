package vn.poly.bagistore.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vai_tro")
@Data
public class VaiTro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_vai_tro", insertable = false, updatable = false)
    private String maVaiTro;

    @Column(name = "ten_vai_tro", nullable = false)
    private String tenVaiTro;
}
