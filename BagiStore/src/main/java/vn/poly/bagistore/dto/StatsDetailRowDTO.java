package vn.poly.bagistore.dto;

public class StatsDetailRowDTO {
    private String label;
    private Double revenue;
    private Long orders;
    private Long productsSold;
    private Double avgPerOrder;
    private String growth; // e.g. +0%

    public StatsDetailRowDTO() {}

    public StatsDetailRowDTO(String label, Double revenue, Long orders, Long productsSold, Double avgPerOrder, String growth) {
        this.label = label;
        this.revenue = revenue;
        this.orders = orders;
        this.productsSold = productsSold;
        this.avgPerOrder = avgPerOrder;
        this.growth = growth;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Double getRevenue() { return revenue; }
    public void setRevenue(Double revenue) { this.revenue = revenue; }

    public Long getOrders() { return orders; }
    public void setOrders(Long orders) { this.orders = orders; }

    public Double getAvgPerOrder() { return avgPerOrder; }
    public void setAvgPerOrder(Double avgPerOrder) { this.avgPerOrder = avgPerOrder; }

    public String getGrowth() { return growth; }
    public void setGrowth(String growth) { this.growth = growth; }

    public Long getProductsSold() { return productsSold; }
    public void setProductsSold(Long productsSold) { this.productsSold = productsSold; }
}
