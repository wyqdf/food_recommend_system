package com.foodrecommend.letmecook.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 验证汇总表是否已创建
 */
public class VerifySummaryTables {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/food_recommend?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = ""; // 如果有密码请在这里填入
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            Statement stmt = conn.createStatement();
            
            System.out.println("=== 验证汇总表是否存在 ===");
            
            // 检查汇总表是否存在
            String[] tablesToCheck = {
                "statistics_overview",
                "user_trend_daily", 
                "recipe_trend_daily",
                "comment_trend_daily",
                "category_distribution",
                "difficulty_distribution",
                "top_recipes_hourly",
                "top_commented_recipes_daily",
                "interaction_daily"
            };
            
            for (String table : tablesToCheck) {
                String checkTableSql = "SELECT COUNT(*) as count FROM information_schema.tables WHERE table_schema = 'food_recommend' AND table_name = '" + table + "'";
                ResultSet rs = stmt.executeQuery(checkTableSql);
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        System.out.println("✅ 表 " + table + " 已存在");
                        
                        // 检查表中是否有数据
                        String checkDataSql = "SELECT COUNT(*) as count FROM " + table;
                        ResultSet dataRs = stmt.executeQuery(checkDataSql);
                        if (dataRs.next()) {
                            int dataCount = dataRs.getInt("count");
                            System.out.println("   └─ 数据行数: " + dataCount);
                        }
                        dataRs.close();
                    } else {
                        System.out.println("❌ 表 " + table + " 不存在");
                    }
                }
                rs.close();
            }
            
            // 检查触发器是否存在
            System.out.println("\n=== 验证触发器是否存在 ===");
            String checkTriggersSql = "SELECT trigger_name FROM information_schema.triggers WHERE trigger_schema = 'food_recommend'";
            ResultSet triggerRs = stmt.executeQuery(checkTriggersSql);
            while (triggerRs.next()) {
                String triggerName = triggerRs.getString("trigger_name");
                if (triggerName.startsWith("trg_")) {
                    System.out.println("✅ 触发器 " + triggerName + " 已存在");
                }
            }
            triggerRs.close();
            
            stmt.close();
            conn.close();
            
            System.out.println("\n=== 验证完成 ===");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}