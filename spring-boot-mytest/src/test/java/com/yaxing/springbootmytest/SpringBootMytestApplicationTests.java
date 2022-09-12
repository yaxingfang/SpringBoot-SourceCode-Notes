package com.yaxing.springbootmytest;

import com.yaxing.entity.CommonBean;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
class SpringBootMytestApplicationTests {

	@Autowired
	private CommonBean commonBean;

	@Test
	void contextLoads() {
		System.out.println(commonBean);
	}

}
