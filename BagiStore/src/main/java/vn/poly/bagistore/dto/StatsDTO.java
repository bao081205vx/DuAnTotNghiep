package vn.poly.bagistore.dto;

import java.util.List;

public class StatsDTO {
    private List<OrderStatusCountDTO> statusCounts;
    private List<RevenuePointDTO> revenue;
    private List<TopProductDTO> topProducts;
    private Long totalOrders;
    private Double totalRevenue;
    private Long totalCustomers;
    // KPI breakdowns
    private Double todayRevenue;
    private Long todayOrders;
    private Long todayProductsSold;

    private Double weekRevenue;
    private Long weekOrders;
    private Long weekProductsSold;

    private Double monthRevenue;
    private Long monthOrders;
    private Long monthProductsSold;

    private Double yearRevenue;
    private Long yearOrders;
    private Long yearProductsSold;

    private List<StatsDetailRowDTO> detailRows;

    public List<OrderStatusCountDTO> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(List<OrderStatusCountDTO> statusCounts) { this.statusCounts = statusCounts; }

    public List<RevenuePointDTO> getRevenue() { return revenue; }
    public void setRevenue(List<RevenuePointDTO> revenue) { this.revenue = revenue; }

    public List<TopProductDTO> getTopProducts() { return topProducts; }
    public void setTopProducts(List<TopProductDTO> topProducts) { this.topProducts = topProducts; }

    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public Long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(Long totalCustomers) { this.totalCustomers = totalCustomers; }

    public Double getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(Double todayRevenue) { this.todayRevenue = todayRevenue; }

    public Long getTodayOrders() { return todayOrders; }
    public void setTodayOrders(Long todayOrders) { this.todayOrders = todayOrders; }

    public Long getTodayProductsSold() { return todayProductsSold; }
    public void setTodayProductsSold(Long todayProductsSold) { this.todayProductsSold = todayProductsSold; }

    public Double getWeekRevenue() { return weekRevenue; }
    public void setWeekRevenue(Double weekRevenue) { this.weekRevenue = weekRevenue; }

    public Long getWeekOrders() { return weekOrders; }
    public void setWeekOrders(Long weekOrders) { this.weekOrders = weekOrders; }

    public Long getWeekProductsSold() { return weekProductsSold; }
    public void setWeekProductsSold(Long weekProductsSold) { this.weekProductsSold = weekProductsSold; }

    public Double getMonthRevenue() { return monthRevenue; }
    public void setMonthRevenue(Double monthRevenue) { this.monthRevenue = monthRevenue; }

    public Long getMonthOrders() { return monthOrders; }
    public void setMonthOrders(Long monthOrders) { this.monthOrders = monthOrders; }

    public Long getMonthProductsSold() { return monthProductsSold; }
    public void setMonthProductsSold(Long monthProductsSold) { this.monthProductsSold = monthProductsSold; }

    public Double getYearRevenue() { return yearRevenue; }
    public void setYearRevenue(Double yearRevenue) { this.yearRevenue = yearRevenue; }

    public Long getYearOrders() { return yearOrders; }
    public void setYearOrders(Long yearOrders) { this.yearOrders = yearOrders; }

    public Long getYearProductsSold() { return yearProductsSold; }
    public void setYearProductsSold(Long yearProductsSold) { this.yearProductsSold = yearProductsSold; }

    public List<StatsDetailRowDTO> getDetailRows() { return detailRows; }
    public void setDetailRows(List<StatsDetailRowDTO> detailRows) { this.detailRows = detailRows; }
}
