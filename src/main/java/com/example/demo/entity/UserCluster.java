package com.example.demo.entity;

public class UserCluster {
    private String meterId;
    private Double totalPower;
    private Double maxPower;
    private Integer userCluster;

    // Getter 和 Setter 方法 (让 Spring Boot 能读写这些字段)
    public String getMeterId() { return meterId; }
    public void setMeterId(String meterId) { this.meterId = meterId; }
    public Double getTotalPower() { return totalPower; }
    public void setTotalPower(Double totalPower) { this.totalPower = totalPower; }
    public Double getMaxPower() { return maxPower; }
    public void setMaxPower(Double maxPower) { this.maxPower = maxPower; }
    public Integer getUserCluster() { return userCluster; }
    public void setUserCluster(Integer userCluster) { this.userCluster = userCluster; }
}