package vn.poly.bagistore.dto;

public class TopProductDTO {
    private Integer id;
    private String name;
    private Long qtySold;
    private Double revenue;
    private Double price;
    private String image;

    public TopProductDTO() {}

    // constructor used by JPQL projection / service
    public TopProductDTO(Integer id, String name, Long qtySold, Double revenue) {
        this.id = id;
        this.name = name;
        this.qtySold = qtySold;
        this.revenue = revenue;
    }

    public TopProductDTO(Integer id, String name, Long qtySold, Double price, Double revenue, String image) {
        this.id = id;
        this.name = name;
        this.qtySold = qtySold;
        this.price = price;
        this.revenue = revenue;
        this.image = image;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getQtySold() { return qtySold; }
    public void setQtySold(Long qtySold) { this.qtySold = qtySold; }

    public Double getRevenue() { return revenue; }
    public void setRevenue(Double revenue) { this.revenue = revenue; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
