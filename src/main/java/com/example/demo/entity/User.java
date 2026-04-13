package com.example.demo.entity;

import java.util.Date;

public class User {
    private Integer id;
    private String username;
    private String password;
    private String role;
    private Date createTime;

    // Getter 和 Setter (建议直接右键生成)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}