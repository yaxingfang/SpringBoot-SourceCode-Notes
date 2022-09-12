package com.yaxing.entity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author fangyaxing
 * @date 2022/9/12
 */
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "commonbean")
public class CommonBean {

	/**
	 * 姓名
	 */
	private String name;

	/**
	 * 年龄
	 */
	private int age;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "CommonBean{" +
				"name='" + name + '\'' +
				", age=" + age +
				'}';
	}
}
