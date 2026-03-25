package com.foodrecommend.letmecook.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.foodrecommend.letmecook.mapper")
public class MyBatisConfig {
}
