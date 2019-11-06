package com.lanjy.manage.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * @Configuration	//注册到springboot容器，相当于原来xml文件里的<beans>
 * @MapperScan(basePackages = "com.lanjy.manage.dao", sqlSessionTemplateRef  = "testSqlSessionTemplate")
 * //指明了扫描dao层，并且给dao层注入指定的SqlSessionTemplate
 * @项目名称：lanjy-common
 * @包名：com.lanjy.manage.config
 * @类描述：数据源配置
 * @创建人：lanjy
 * @创建时间：2018-02-27 13:33
 * @version：V1.0
 */
@Configuration
@MapperScan(basePackages = "com.lanjy.manage.dao", sqlSessionTemplateRef  = "testSqlSessionTemplate")
public class DataSourceConfig {
	/**
	 * 创建datasource对象
	 * @Bean(name = "testDataSource") 注册到 testDataSource 这个容器
	 * @ConfigurationProperties(prefix="")//表示取application.properties配置文件中的前缀
	 * prefix值必须是 application.properteis 中对应属性的前缀
	 * @Primary#primary是设置优先，因为有多个数据源，在没有明确指定用哪个的情况下，会用带有primary的，这个注解必须有一个数据源要添加
	 * @return
	 */
	@Bean(name = "testDataSource")
	@ConfigurationProperties(prefix = "slave.datasource.test")
	@Primary
	public DataSource testDataSource() {
		return DataSourceBuilder.create().build();
	}

	/**
	 * 创建sql工程
	 * @Qualifier("xxx")的含义是告诉他使用哪个DataSource
	 * @param dataSource
	 * @return
	 * @throws Exception
	 */
	@Bean(name = "testSqlSessionFactory")
	@Primary
	public SqlSessionFactory testSqlSessionFactory(
			@Qualifier("testDataSource") DataSource dataSource) throws Exception {
		SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
		bean.setDataSource(dataSource);
		//对应mybatis.type-aliases-package配置
		bean.setTypeAliasesPackage("com.lanjy.manage.pojo");
		//对应mybatis.mapper-locations配置
		bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
		//开启驼峰映射
		bean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
		return bean.getObject();
	}

	/**
	 * 配置事务管理
	 * @param dataSource
	 * @return
	 * 	<tx:annotation-driven />
		<bean id="testTransactionManager"
		class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
		<property name="testDataSource" ref="testDataSource" />
		</bean>

	 */
	@Bean(name = "testTransactionManager")
	@Primary
	public DataSourceTransactionManager testTransactionManager(
			@Qualifier("testDataSource") DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	/**
	 * sqlSession模版，用于配置自动扫描pojo实体类
	 * @param sqlSessionFactory
	 * @return
	 * @throws Exception
	 */
	@Bean(name = "testSqlSessionTemplate")
	@Primary
	public SqlSessionTemplate testSqlSessionTemplate(
			@Qualifier("testSqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
		return new SqlSessionTemplate(sqlSessionFactory);
	}
}
