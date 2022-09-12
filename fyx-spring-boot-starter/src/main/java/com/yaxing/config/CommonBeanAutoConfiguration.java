package com.yaxing.config;

import com.yaxing.entity.CommonBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author fangyaxing
 * @date 2022/9/12
 */
@Configuration
@ConditionalOnBean(ConfigMarker.class)
public class CommonBeanAutoConfiguration {


	static {
		System.out.println("CommonBeanAutoConfiguration init...");
	}

	@Bean
	public CommonBean commonBean() {
		return new CommonBean();
	}
}
