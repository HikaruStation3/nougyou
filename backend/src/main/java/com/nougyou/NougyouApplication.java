package com.nougyou;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.nougyou.**.mapper")
@SpringBootApplication
public class NougyouApplication {

    public static void main(String[] args) {
        SpringApplication.run(NougyouApplication.class, args);
    }
}
