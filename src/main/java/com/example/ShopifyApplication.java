package com.example;

import com.example.utils.MD5;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShopifyApplication {

	public static void main(String[] args) {
		System.out.println(MD5.md5("Grechka4kg"));
		System.out.println(MD5.md5("Gashnich1bog"));
		SpringApplication.run(ShopifyApplication.class, args);
	}

}
