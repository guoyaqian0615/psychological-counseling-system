package net.suncaper.psychological;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("net.suncaper.psychological.mapper")
public class PsychologicalApplication {
	public static void main(String[] args) {
		SpringApplication.run(PsychologicalApplication.class, args);
	}
}