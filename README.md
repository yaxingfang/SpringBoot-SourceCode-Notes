# SpringBoot源码剖析

## 源码环境搭建

1. 下载 https://github.com/spring-projects/spring-boot/releases/tag/v2.2.9.RELEASE
	2.3.x之后源码是使用gradle构建的，为了便于用maven构建，下载2.2.9.RELEASE版本。

2. 环境准备

	1. JDK1.8+
	2. Maven3.5+

3. 编译源码

	1. 进入源码根目录
	2. 执行 `mvn clean install -DskipTests -Pfast` 跳过测试用例，会下载大量jar包，时间较长，出现Build Success表明构建成功
		<img src="https://yaxingfang-typora.oss-cn-hangzhou.aliyuncs.com/image-20220909174146933.png" alt="image-20220909174146933" style="zoom:50%;" />

4. IDEA导入项目

	1. 打开pom.xml添加disable.checks属性关闭maven代码检查

		```properties
		<properties>
				<revision>2.2.9.RELEASE</revision>
				<main.basedir>${basedir}</main.basedir>
				<disable.checks>true</disable.checks>
		</properties>
		```

	2. 新建一个module

		springboot的模块，com.yaxing.spring-boot-mytest，修改此模块的springboot版本2.2.9.RELEASE

	3. 新建一个controller
		com.yaxing.controller.TestController（注意将主启动类移动至与controller包同级下这样才能扫描到注解）

		```java
		@RestController
		public class TestController {
		
			@RequestMapping("/test")
			public String test() {
				System.out.println("源码环境构建成功");
				return "源码环境构建成功";
			}
		}
		```

		run起来，访问 http://localhost:8080/test 得到 源码环境构建成功

## 依赖管理

### spring-boot-starter-parent

主pom

```xml
<parent>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-parent</artifactId>
   <version>2.2.9.RELEASE</version>
   <relativePath/> <!-- lookup parent from repository -->
</parent>
```

父pom spring-boot-starter-parent

```xml
<parent>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-dependencies</artifactId>
   <version>${revision}</version>
   <relativePath>../../spring-boot-dependencies</relativePath>
</parent>
```

父pom的父pom spring-boot-dependencies

在该pom文件中定义了properties以及dependencyManagement，因此主pom中部分依赖不需要版本号，他会从该pom中继承获得版本号

### spring-boot-starter-web

spring-boot-starter-web依赖启动器打包了Web开发场景所需的底层所有依赖，基于依赖传递，当前项目也存在对应的依赖jar包

具体的依赖jar的版本也是由spring-boot-starter-parent依赖进行统一管理

但是对于一些第三方依赖启动器时，需要配置指定版本号

## 自动配置

### @SpringBootApplication

自动配置：根据我们添加的jar包依赖，自动将一些配置类的bean注册到ioc容器中，我们需要的时候使用@Autowired或@Resource来使用

```java
@SpringBootConfiguration	// 表明该类为配置类
@EnableAutoConfiguration	// 启动自动装配功能
@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
		@Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
```

### @SpringBootConfiguration 

就是对@Configuration的一层包装，也就是配置类

```java
@Configuration
public @interface SpringBootConfiguration{}
```

### @EnableAutoConfiguration

Spring中有很多以@Enable开头的注解，其作用就是借助@Import来收集并注册特定场景相关的Bean，并加载到容器

例如，@EnableAutoConfiguration就是借助@Enable来收集所有符合自动装配条件的Bean并加载到容器

1. 组合注解

	```java
	@AutoConfigurationPackage   // 自动配置包
	@Import(AutoConfigurationImportSelector.class)
	public @interface EnableAutoConfiguration {
	```

2. **@AutoConfigurationPackage**

	```java
	// @Import是Spring的底层注解，导入组件到容器中
	// 该处就是导入AutoConfigurationPackages.Registrar.class组件
	@Import(AutoConfigurationPackages.Registrar.class)
	public @interface AutoConfigurationPackage {
	
	}
	```

	AutoConfigurationPackages.Registrar.class

	```java
	static class Registrar implements ImportBeanDefinitionRegistrar, DeterminableImports {
	
	   @Override
	   public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
	      // 将注解标注的元信息传入，获取到相应的包名
	      register(registry, new PackageImport(metadata).getPackageName());
	   }
	
	   @Override
	   public Set<Object> determineImports(AnnotationMetadata metadata) {
	      return Collections.singleton(new PackageImport(metadata));
	   }
	
	}
	
	public static void register(BeanDefinitionRegistry registry, String... packageNames) {
			if (registry.containsBeanDefinition(BEAN)) {
				// 如果该bean已经注册了，就将注册包名称添加进去
				BeanDefinition beanDefinition = registry.getBeanDefinition(BEAN);
				ConstructorArgumentValues constructorArguments = beanDefinition.getConstructorArgumentValues();
				constructorArguments.addIndexedArgumentValue(0, addBasePackages(constructorArguments, packageNames));
			}
			else {
				// 如果该bean未注册，则注册该bean，参数中提供的包名称会被设置到bean定义中去
				GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
				beanDefinition.setBeanClass(BasePackages.class);
				beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, packageNames);
				beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				registry.registerBeanDefinition(BEAN, beanDefinition);
			}
		}
	```

	AutoConfigurationPackages.Registrar这个类注册org.springframework.boot.autoconfigure.AutoConfigurationPackages.**BasePackages**这个Bean，注册时有一个参数，使用@AutoConfigurationPackage这个注解的类所在的包路径，保存自动配置类以供之后使用

3. **@Import(AutoConfigurationImportSelector.class)**

	将AutoConfigurationImportSelector这个类导入到Spring容器中，AutoConfigurationImportSelector可以帮助SpringBoot应用将所有符合条件的@Configuration配置都加载到当前SpringBoot创建并使用的IOC容器中

	<img src="https://yaxingfang-typora.oss-cn-hangzhou.aliyuncs.com/image-20220909204224504.png" alt="image-20220909204224504" style="zoom:50%;" />

	AutoConfigurationImportSelector重点实现了DeferredImportSelector接口其继承ImportSelector接口，还有各种Aware接口，分别表示在某个时机会被回调

	```java
	public void process(AnnotationMetadata annotationMetadata, DeferredImportSelector deferredImportSelector) {
	  Assert.state(deferredImportSelector instanceof AutoConfigurationImportSelector,
	               () -> String.format("Only %s implementations are supported, got %s",
	                                   AutoConfigurationImportSelector.class.getSimpleName(),
	                                   deferredImportSelector.getClass().getName()));
	  // 1 得到自动配置类
	  AutoConfigurationEntry autoConfigurationEntry = ((AutoConfigurationImportSelector) deferredImportSelector)
	    .getAutoConfigurationEntry(getAutoConfigurationMetadata(), annotationMetadata);
	  // 2 将得到的自动配置类的工厂类封装的entry放入集合
	  this.autoConfigurationEntries.add(autoConfigurationEntry);
	  // 3 遍历自动配置类的工厂全路径
	  for (String importClassName : autoConfigurationEntry.getConfigurations()) {
	    // 以自动配置类作为key，annotationMetadata作为值放进entries集合
	    this.entries.putIfAbsent(importClassName, annotationMetadata);
	  }
	}
	
	protected AutoConfigurationEntry getAutoConfigurationEntry(
	                                            AutoConfigurationMetadata autoConfigurationMetadata,
	                                            AnnotationMetadata annotationMetadata) {
	  // 获取是否有配置spring.boot.enableautoconfiguration属性，默认返回true
	  if (!isEnabled(annotationMetadata)) {
	    return EMPTY_ENTRY;
	  }
	  AnnotationAttributes attributes = getAttributes(annotationMetadata);
	  // 加载classpath上所有jar包下的META-INF目录下名称为spring.factories配置文件
	  // 根据EnableAutoConfiguration的key，加载对应的自动配置类的工厂类的全路径并封装为集合
	  List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);
	  // 移除重复的配置类，若我们自己写的starter可能存在重复的
	  configurations = removeDuplicates(configurations);
	  // 得到要排除的配置类 @SpringBootApplication(exclude = xxx.class)
	  Set<String> exclusions = getExclusions(annotationMetadata, attributes);
	  // 校验排除类（exclusions指定的类必须是自动配置类，否则抛出异常）
	  checkExcludedClasses(configurations, exclusions);
	  configurations.removeAll(exclusions);
	  // 过滤掉不加载一些不必要的自动配置类
	  // @ConditionalOnClass ： 某个class位于类路径上，才会实例化这个Bean
	  // @ConditionalOnMissingClass ： classpath中不存在该类时起效
	  // @ConditionalOnBean ： DI容器中存在该类型Bean时起效
	  // @ConditionalOnMissingBean ： DI容器中不存在该类型Bean时起效
	  // @ConditionalOnSingleCandidate ： DI容器中该类型Bean只有一个或@Primary的只有一个时起效
	  // @ConditionalOnExpression ： SpEL表达式结果为true时
	  // @ConditionalOnProperty ： 参数设置或者值一致时起效
	  // @ConditionalOnResource ： 指定的文件存在时起效
	  // @ConditionalOnJndi ： 指定的JNDI存在时起效
	  // @ConditionalOnJava ： 指定的Java版本存在时起效
	  // @ConditionalOnWebApplication ： Web应用环境下起效
	  // @ConditionalOnNotWebApplication ： 非Web应用环境下起效
	
	  // 总结一下判断是否要加载某个类的两种方式：
	  // 根据spring-autoconfigure-metadata.properties进行判断。
	  // 要判断@Conditional是否满足
	  // 如@ConditionalOnClass({ SqlSessionFactory.class, SqlSessionFactoryBean.class })
	  // 表示需要在类路径中存在SqlSessionFactory.class、SqlSessionFactoryBean.class这两个类才能完成自动注册。
	  configurations = filter(configurations, autoConfigurationMetadata);
	  // 6. 将自动配置导入事件通知监听器
	  // 当AutoConfigurationImportSelector过滤完成后会自动加载类路径下Jar包中META-INF/spring.factories文件中 AutoConfigurationImportListener的实现类，
	  // 并触发fireAutoConfigurationImportEvents事件。
	  fireAutoConfigurationImportEvents(configurations, exclusions);
	  // 将符合条件和要排出的自动配置类封装并返回
	  return new AutoConfigurationEntry(configurations, exclusions);
	}
	
	protected List<String> getCandidateConfigurations(AnnotationMetadata metadata, 
	                                                  AnnotationAttributes attributes) {
	  // getSpringFactoriesLoaderFactoryClass() = EnableAutoConfiguration.class
	  // getBeanClassLoader = beanClassLoader 类加载器
	  List<String> configurations = SpringFactoriesLoader.loadFactoryNames(getSpringFactoriesLoaderFactoryClass(),
	                                       getBeanClassLoader());
	  Assert.notEmpty(configurations, 
	                  "No auto configuration classes found in META-INF/spring.factories. If you "
	                  + "are using a custom packaging, make sure that file is correct.");
	  return configurations;
	}
	
	public static List<String> loadFactoryNames(Class<?> factoryType, @Nullable ClassLoader classLoader) {
	  String factoryTypeName = factoryType.getName();
	  return (List)loadSpringFactories(classLoader)
	    .getOrDefault(factoryTypeName, Collections.emptyList());
	}
	
	private static Map<String, List<String>> loadSpringFactories(@Nullable ClassLoader classLoader) {
	  MultiValueMap<String, String> result = (MultiValueMap)cache.get(classLoader);
	  if (result != null) {
	    return result;
	  } else {
	    try {
	      Enumeration<URL> urls = classLoader != null ? classLoader.getResources("META-INF/spring.factories") : ClassLoader.getSystemResources("META-INF/spring.factories");
	      LinkedMultiValueMap result = new LinkedMultiValueMap();
	
	      while(urls.hasMoreElements()) {
	        URL url = (URL)urls.nextElement();
	        UrlResource resource = new UrlResource(url);
	        Properties properties = PropertiesLoaderUtils.loadProperties(resource);
	        Iterator var6 = properties.entrySet().iterator();
	
	        while(var6.hasNext()) {
	          Entry<?, ?> entry = (Entry)var6.next();
	          String factoryTypeName = ((String)entry.getKey()).trim();
	          String[] var9 = StringUtils.commaDelimitedListToStringArray((String)entry.getValue());
	          int var10 = var9.length;
	
	          for(int var11 = 0; var11 < var10; ++var11) {
	            String factoryImplementationName = var9[var11];
	            result.add(factoryTypeName, factoryImplementationName.trim());
	          }
	        }
	      }
	
	      cache.put(classLoader, result);
	      return result;
	    } catch (IOException var13) {
	      throw new IllegalArgumentException("Unable to load factories from location [META-INF/spring.factories]", var13);
	    }
	  }
	}
	```

自动装配与原理总结：

1. 从META-INF/spring.factories配置文件中加载自动配置类
2. 加载的自动配置类中排除掉exclude掉的指定的自动配置类
3. 当自动配置类的@Conditinalxxx注解满足条件时，才会将其返回，不满足的也不会自动装配

### @ComponentScan

```java
@ComponentScan(excludeFilters = 
               { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
      					 @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
```

@ComponentScan作用时从定义的扫描路径中，找出标识了需要装配的类自动装配到spring容器中

常用属性如下：

```java
String[] basePackages() default {};	// 指定扫描的包路径，如果为空则以该注解所在的类所在的包为基本的扫描包路径，也就是SpringBoot主启动类的包路径
Class<?>[] basePackageClasses() default {};	// 指定具体扫描的类
ComponentScan.Filter[] includeFilters() default {};	// 指定满足Filter条件的类
ComponentScan.Filter[] excludeFilters() default {};	// 指定排除Filter条件的类
```

## Run方法执行流程

> @EnableAutoConfiguration注解通过@Import注解进行了自动配置类的加载，@ComponetScan注解自动进行注解扫描，
>
> 那么真正的根据包扫描，把组建类生成实例对象存到IOC容器中，是怎样完成的？

```java
@SpringBootApplication
public class SpringBootMytestApplication {
   public static void main(String[] args) {
      SpringApplication.run(SpringBootMytestApplication.class, args);
   }
}

public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
	return run(new Class<?>[] { primarySource }, args);
}

public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
	// 1 初始化SpringApplication 2 执行run方法
	return new SpringApplication(primarySources).run(args);
}
```

### 初始化SpringApplication

```java
public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
   this.resourceLoader = resourceLoader;
   Assert.notNull(primarySources, "PrimarySources must not be null");
   this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));
   // 1 推断应用类型，后面会根据类型初始化对应的环境，常用的一般都是servlet环境
   this.webApplicationType = WebApplicationType.deduceFromClasspath();
   // 2 初始化classpath下META-INF/spring.factories中已配置的ApplicationContextInitializer
   setInitializers((Collection) getSpringFactoriesInstances(ApplicationContextInitializer.class));
   // 3 初始化classpath下META-INF/spring.factories中已配置的ApplicationListener
   setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
   // 4 根据调用栈推断出main方法的类名
   this.mainApplicationClass = deduceMainApplicationClass();
}
```

### 执行run方法

#### 1. 获取并启动监听器

负责在SpringBoot启动的不同阶段广播出不同消息，传递给ApplicationListener监听器实现类

```java
SpringApplicationRunListeners listeners = getRunListeners(args);
listeners.starting();

void starting() {
  for (SpringApplicationRunListener listener : this.listeners) {
    listener.starting();
  }
}

public void starting() {
  this.initialMulticaster.multicastEvent(new ApplicationStartingEvent(this.application, this.args));
}
```

#### 2. 构造应用上下文环境

```java
ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);

private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners,
                                                   ApplicationArguments applicationArguments) {
  // Create and configure the environment
  // 创建并配置相应的环境（根据应用类型对应进行实例化）
  ConfigurableEnvironment environment = getOrCreateEnvironment();
  // 根据用户配置，配置environment系统环境
  configureEnvironment(environment, applicationArguments.getSourceArgs());
  ConfigurationPropertySources.attach(environment);
  // 启动相应的监听器，其中一个重要的监听器ConfigFileApplicationListener就是加载项目配置文件的监听器
  listeners.environmentPrepared(environment);
  bindToSpringApplication(environment);
  if (!this.isCustomEnvironment) {
    environment = new EnvironmentConverter(getClassLoader()).convertEnvironmentIfNecessary(environment,
                                                                                           deduceEnvironmentClass());
  }
  ConfigurationPropertySources.attach(environment);
  return environment;
}
```

#### 3. 初始化应用上下文

得到context为org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext，

同时在实例化context的时候，也触发了GenericApplicationContext的构造函数，

从而IOC容器也创建了org.springframework.beans.factory.support.DefaultListableBeanFactory

```java
context = createApplicationContext();

protected ConfigurableApplicationContext createApplicationContext() {
  Class<?> contextClass = this.applicationContextClass;
  if (contextClass == null) {
    try {
      switch (this.webApplicationType) {
        case SERVLET:
          contextClass = Class.forName(DEFAULT_SERVLET_WEB_CONTEXT_CLASS);
          break;
        case REACTIVE:
          contextClass = Class.forName(DEFAULT_REACTIVE_WEB_CONTEXT_CLASS);
          break;
        default:
          contextClass = Class.forName(DEFAULT_CONTEXT_CLASS);
      }
    }
    catch (ClassNotFoundException ex) {
      throw new IllegalStateException(
        "Unable create a default ApplicationContext, please specify an ApplicationContextClass", ex);
    }
  }
  return (ConfigurableApplicationContext) BeanUtils.instantiateClass(contextClass);
}
```

#### 4. 刷新应用上下文前的准备阶段



#### 5. 刷新应用上下文（IOC容器初始化过程）



#### 6. 刷新应用上下文后的扩展接口

















