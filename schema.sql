-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: spark_db
-- ------------------------------------------------------
-- Server version	8.0.45-0ubuntu0.22.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `anomaly_records`
--

DROP TABLE IF EXISTS `anomaly_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `anomaly_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `meter_id` varchar(50) NOT NULL COMMENT '电表编号',
  `detect_date` date NOT NULL COMMENT '检测日期',
  `daily_usage` decimal(10,2) DEFAULT NULL COMMENT '当日用电量(kWh)',
  `avg_usage` decimal(10,2) DEFAULT NULL COMMENT '历史均值(kWh)',
  `z_score` decimal(10,2) DEFAULT NULL COMMENT 'Z-Score波动指数(3-Sigma)',
  `is_suspect` tinyint DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1531 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Spark离线计算-异常窃电预警表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cluster_result`
--

DROP TABLE IF EXISTS `cluster_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cluster_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `meter_id` varchar(50) NOT NULL COMMENT '电表编号',
  `cluster_label` int NOT NULL COMMENT '聚类类别编号(如0, 1, 2)',
  `cluster_name` varchar(50) DEFAULT NULL COMMENT '类别名称(如: 高耗能型, 昼伏夜出型, 规律型)',
  `analyze_date` date DEFAULT NULL COMMENT '分析执行日期',
  PRIMARY KEY (`id`),
  KEY `idx_cluster` (`cluster_label`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Spark离线计算-用户画像聚类结果表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `daily_usage_stat`
--

DROP TABLE IF EXISTS `daily_usage_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_usage_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `meter_id` varchar(50) NOT NULL COMMENT '电表编号',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_usage` decimal(10,2) DEFAULT NULL COMMENT '日总用电量(kWh)',
  `peak_usage` decimal(10,2) DEFAULT NULL COMMENT '峰谷差值(kWh)',
  `max_load` decimal(10,2) DEFAULT NULL COMMENT '日最大负荷',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_meter_date` (`meter_id`,`stat_date`)
) ENGINE=InnoDB AUTO_INCREMENT=301 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Spark离线计算-每日用电特征表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_quality_daily`
--

DROP TABLE IF EXISTS `data_quality_daily`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_quality_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_date` date NOT NULL,
  `metric_key` varchar(80) NOT NULL,
  `metric_value` decimal(18,4) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_check_metric` (`check_date`,`metric_key`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meter_cluster_result`
--

DROP TABLE IF EXISTS `meter_cluster_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meter_cluster_result` (
  `meter_id` longtext,
  `cluster_label` int NOT NULL,
  `cluster_name` longtext NOT NULL,
  `analyze_date` date NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meter_data_raw`
--

DROP TABLE IF EXISTS `meter_data_raw`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meter_data_raw` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `meter_id` varchar(50) NOT NULL COMMENT '电表编号',
  `record_time` datetime NOT NULL COMMENT '采集时间(如精确到每15分钟或每小时)',
  `power_usage` decimal(10,4) DEFAULT NULL COMMENT '该时段用电量(kWh)',
  `voltage` decimal(10,2) DEFAULT NULL COMMENT '电压(V)',
  `current` decimal(10,2) DEFAULT NULL COMMENT '电流(A)',
  PRIMARY KEY (`id`),
  KEY `idx_meter_time` (`meter_id`,`record_time`)
) ENGINE=InnoDB AUTO_INCREMENT=7201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电表原始采集数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meter_info`
--

DROP TABLE IF EXISTS `meter_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meter_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `meter_id` varchar(50) NOT NULL COMMENT '电表编号',
  `user_name` varchar(50) NOT NULL COMMENT '户主姓名',
  `address` varchar(100) NOT NULL COMMENT '安装地址',
  `meter_type` varchar(50) NOT NULL COMMENT '电表类型',
  `install_date` date NOT NULL COMMENT '安装日期',
  `current_load` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '当前负荷(MW)',
  `daily_usage` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '今日用电(kWh)',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态: 1正常 2异常 3离线',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `repair_work_order`
--

DROP TABLE IF EXISTS `repair_work_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `repair_work_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(40) NOT NULL,
  `meter_id` varchar(64) NOT NULL,
  `detect_date` varchar(32) NOT NULL,
  `order_status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `reason` varchar(255) DEFAULT NULL,
  `source` varchar(32) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_order_no` (`order_no`),
  UNIQUE KEY `uq_meter_day` (`meter_id`,`detect_date`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dashboard`
--

DROP TABLE IF EXISTS `sys_dashboard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dashboard` (
  `id` int NOT NULL AUTO_INCREMENT,
  `total_meters` int DEFAULT '0' COMMENT '总电表数',
  `total_load` double DEFAULT '0' COMMENT '总负荷',
  `anomaly_count` int DEFAULT '0' COMMENT '异常户数',
  `spark_time` double DEFAULT '0' COMMENT 'Spark计算耗时',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_load_forecast`
--

DROP TABLE IF EXISTS `sys_load_forecast`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_load_forecast` (
  `id` int NOT NULL AUTO_INCREMENT,
  `time_point` varchar(20) DEFAULT NULL,
  `forecast_value` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3049 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_load_trend`
--

DROP TABLE IF EXISTS `sys_load_trend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_load_trend` (
  `id` int NOT NULL AUTO_INCREMENT,
  `time_point` varchar(20) NOT NULL COMMENT '时间点(如 02:00)',
  `load_value` double DEFAULT '0' COMMENT '负荷值(MW)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=97 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_log`
--

DROP TABLE IF EXISTS `sys_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `operator` varchar(50) DEFAULT NULL,
  `operation` varchar(100) DEFAULT NULL,
  `method` varchar(200) DEFAULT NULL,
  `params` text,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_region_load`
--

DROP TABLE IF EXISTS `sys_region_load`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_region_load` (
  `id` int NOT NULL AUTO_INCREMENT,
  `region_name` varchar(50) NOT NULL COMMENT '辖区名称',
  `load_value` double DEFAULT '0' COMMENT '辖区总负荷',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '登录密码(加密)',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `status` tinyint DEFAULT '1' COMMENT '账号状态(0:停用, 1:正常)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统管理员/用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_clusters`
--

DROP TABLE IF EXISTS `user_clusters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_clusters` (
  `meter_id` varchar(50) DEFAULT NULL,
  `total_power` double DEFAULT NULL,
  `max_power` double DEFAULT NULL,
  `user_cluster` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-21 17:15:25
