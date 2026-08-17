/*
 Navicat Premium Dump SQL

 Source Server         : Linux
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : 10.1.15.69:3306
 Source Schema         : test_data

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 17/08/2026 15:11:28
*/
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for test_static
-- ----------------------------
DROP TABLE IF EXISTS `test_static`;
CREATE TABLE `test_static`  (
  `URL` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `bundleId` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `ascribe` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `event_number` int NULL DEFAULT NULL,
  `exception_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `record_data` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `recorder` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `isOutput` tinyint NULL DEFAULT NULL,
  INDEX `idx_ascribe`(`ascribe` ASC) USING BTREE,
  INDEX `idx_recorder`(`recorder` ASC) USING BTREE,
  INDEX `idx_isOutput`(`isOutput` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
