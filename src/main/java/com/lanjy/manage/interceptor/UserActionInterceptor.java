package com.lanjy.manage.interceptor;

import com.lanjy.manage.entity.ResponseResult;
import com.lanjy.manage.pojo.User;
import com.lanjy.manage.service.UserService;
import com.lanjy.manage.utils.IStatusMessage;
import com.lanjy.manage.utils.ShiroFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义拦截器,判断用户信息是否已被后台更改（比如离职，别处登录）
 *
 * 拦截器可以获取IOC容器中的各个bean，而过滤器就不行，所以在拦截器里注入一个service，可以调用业务逻辑。
 *
 * 拦截器只能对controller请求进行拦截，对其他的一些比如直接访问静态资源的请求则没办法进行拦截处理。
 *
 * @创建人：lanjy
 * @创建时间：2018年5月2日 上午9:36:43 
 * @version：
 */
@Component
public class UserActionInterceptor implements HandlerInterceptor {

	private static Logger logger = LoggerFactory.getLogger(UserActionInterceptor.class);

	/**
	 * 退出后重定向的地址
	 */
	private final String TO_LOGIN_URL="/toLogin";

	@Autowired
	private UserService userService;


	/**
	 * 请求到达后台方法之前调用（controller之前）
	 * @param request
	 * @param response
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object obj) throws Exception {
		logger.debug("请求到达后台方法之前调用（controller之前）");
		User user = (User) SecurityUtils.getSubject().getPrincipal();
		if (user != null
				&& StringUtils.isNotEmpty(user.getMobile())
				&& null != user.getVersion()) {
			// 获取数据库中的用户数据
			User dataUser = userService.findUserByMobile(user.getMobile());
			// 对比session中用户的version和数据库中的是否一致
			if (dataUser != null
					&& null != dataUser.getVersion()
					&& user.getVersion() == dataUser.getVersion()) {
				//一样，放行
				return true;
			}
			//不一样，这里统一做退出登录处理；
			SecurityUtils.getSubject().logout();
			isAjaxResponse(request,response);
		}
		return false;
	}



	/**
	 * 判断是否Ajax 访问
	 * 1.如果是Ajax 访问，那么给予json返回值提示。
	 * 2.如果是普通请求，直接重定向登录页 TO_LOGIN_URL
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	private boolean isAjaxResponse(HttpServletRequest request,HttpServletResponse response) throws IOException {
		ResponseResult responseResult = new ResponseResult();
		if (ShiroFilterUtils.isAjax(request) ) {
			logger.debug(getClass().getName()+ "，当前用户的信息或权限已变更，重新登录后生效！");
			responseResult.setCode(IStatusMessage.SystemStatus.UPDATE.getCode());
			responseResult.setMessage("您的信息或权限已变更，重新登录后生效");
			ShiroFilterUtils.out(response, responseResult);
		}else{
			WebUtils.issueRedirect(request, response, TO_LOGIN_URL);
		}
		return false;
	}



	@Override
	public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

	}

	@Override
	public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

	}
}
