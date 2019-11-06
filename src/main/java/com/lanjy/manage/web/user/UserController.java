package com.lanjy.manage.web.user;

import com.lanjy.manage.entity.ResponseResult;
import com.lanjy.manage.entity.UserRolesVO;
import com.lanjy.manage.entity.UserSearchDTO;
import com.lanjy.manage.pojo.Role;
import com.lanjy.manage.pojo.User;
import com.lanjy.manage.service.AuthService;
import com.lanjy.manage.service.UserService;
import com.lanjy.manage.utils.IStatusMessage;
import com.lanjy.manage.utils.PageDataResult;
import com.lanjy.manage.utils.ValidateUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @项目名称：lanjy-manage
 * @包名：com.lanjy.manage.web.user
 * @类描述：
 * @创建人：lanjy
 * @创建时间：2017-12-31 14:22
 * @version：V1.0
 */
@Controller
@RequestMapping("/user")
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	@Autowired
	private UserService userService;
	@Autowired
	private AuthService authService;


	//private static final Pattern MOBILE_PATTERN = Pattern.compile("^1\\d{10}$");






	@RequestMapping("/userList")
	public String toUserList() {
		return "/auth/userList";
	}





	/**
	 * 分页查询用户列表
	 * @return ok/fail
	 */
	@RequestMapping(value = "/getUsers", method = RequestMethod.POST)
	@ResponseBody
	@RequiresPermissions(value = "usermanage")
	public PageDataResult getUsers(@RequestParam("page") Integer page,
			@RequestParam("limit") Integer limit, UserSearchDTO userSearch) {
		logger.debug("分页查询用户列表！搜索条件：userSearch：" + userSearch + ",page:" + page + ",每页记录数量limit:" + limit);
		PageDataResult pdr = new PageDataResult();
		try {
			if (null == page) {
				page = 1;
			}
			if (null == limit) {
				limit = 10;
			}
			// 获取用户和角色列表
			pdr = userService.getUsers(userSearch, page, limit);
			logger.debug("用户列表查询=pdr:" + pdr);
		} catch (Exception e) {
			//e.printStackTrace();
			logger.error("用户列表查询异常！", e);
		}
		return pdr;
	}

	/**
	 * 设置用户是否离职
	 * @return ok/fail
	 */
	@RequestMapping(value = "/setJobUser", method = RequestMethod.POST)
	@ResponseBody
	public String setJobUser(@RequestParam("id") Integer id,
			@RequestParam("job") Integer isJob,
			@RequestParam("version") Integer version) {

		logger.debug("设置用户是否离职！id:" + id + ",isJob:" + isJob + ",version:" + version);
		String msg = "";
		try {
			if (null == id || null == isJob || null == version) {
				logger.debug("设置用户是否离职，结果=请求参数有误，请您稍后再试");
				return "请求参数有误，请您稍后再试";
			}
			User existUser = (User) SecurityUtils.getSubject().getPrincipal();
			if (null == existUser) {
				logger.debug("设置用户是否离职，结果=您未登录或登录超时，请您登录后再试");
				return "您未登录或登录超时，请您登录后再试";
			}
			// 设置用户是否离职
			msg = userService.setJobUser(id, isJob, existUser.getId(),version);
			logger.info("设置用户是否离职成功！userID=" + id + ",isJob:" + isJob + "，操作的用户ID=" + existUser.getId());
		} catch (Exception e) {
			//e.printStackTrace();
			logger.error("设置用户是否离职异常！", e);
			msg = "操作异常，请您稍后再试！";
		}
		return msg;
	}

	/**
	 * 设置用户[新增或更新]
	 * @return ok/fail
	 */
	@RequestMapping(value = "/setUser", method = RequestMethod.POST)
	@ResponseBody
	public String setUser(@RequestParam("roleIds") String roleIds, User user) {
		logger.debug("设置用户[新增或更新]！user:" + user + ",roleIds:" + roleIds);
		try {
			if (null == user) {
				logger.debug("置用户[新增或更新]，结果=请您填写用户信息");
				return "请您填写用户信息";
			}
			if (StringUtils.isEmpty(roleIds)) {
				logger.debug("置用户[新增或更新]，结果=请您给用户设置角色");
				return "请您给用户设置角色";
			}
			User existUser = (User) SecurityUtils.getSubject().getPrincipal();
			if (null == existUser) {
				logger.debug("置用户[新增或更新]，结果=您未登录或登录超时，请您登录后再试");
				return "您未登录或登录超时，请您登录后再试";
			}
			user.setInsertUid(existUser.getId());
			// 设置用户[新增或更新]
			logger.info("设置用户[新增或更新]成功！user=" + user + ",roleIds=" + roleIds + "，操作的用户ID=" + existUser.getId());
			return userService.setUser(user, roleIds);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("设置用户[新增或更新]异常！", e);
			return "操作异常，请您稍后再试";
		}
	}

	/**
	 * 删除用户
	 * @return ok/fail
	 */
	@RequestMapping(value = "/delUser", method = RequestMethod.POST)
	@ResponseBody
	public String delUser(@RequestParam("id") Integer id,
			@RequestParam("version") Integer version) {
		logger.debug("删除用户！id:" + id + ",version:" + version);
		String msg = "";
		try {
			if (null == id || null == version) {
				logger.debug("删除用户，结果=请求参数有误，请您稍后再试");
				return "请求参数有误，请您稍后再试";
			}
			User existUser = (User) SecurityUtils.getSubject().getPrincipal();
			if (null == existUser) {
				logger.debug("删除用户，结果=您未登录或登录超时，请您登录后再试");
				return "您未登录或登录超时，请您登录后再试";
			}
			// 删除用户
			msg = userService.setDelUser(id, 1, existUser.getId(), version);
			logger.info("删除用户:" + msg + "！userId=" + id + "，操作用户id:" + existUser.getId() + ",version:" + version);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("删除用户异常！", e);
			msg = "操作异常，请您稍后再试";
		}
		return msg;
	}

	/**
	 *
	 * @描述：恢复用户
	 * @创建人：lanjy
	 * @创建时间：2018年4月27日 上午9:49:14
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/recoverUser", method = RequestMethod.POST)
	@ResponseBody
	public String recoverUser(@RequestParam("id") Integer id,
			@RequestParam("version") Integer version) {
		logger.debug("恢复用户！id:" + id + ",version:" + version);
		String msg = "";
		try {
			User existUser = (User) SecurityUtils.getSubject().getPrincipal();
			if (null == existUser) {
				return "您未登录或登录超时，请您登录后再试";
			}
			if (null == id || null == version) {
				return "请求参数有误，请您稍后再试";
			}
			// 删除用户
			msg = userService.setDelUser(id, 0, existUser.getId(), version);
			logger.info("恢复用户【" + this.getClass().getName() + ".recoverUser】"
					+ msg + "。用户userId=" + id + "，操作的用户ID=" + existUser.getId() + ",version:" + version);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("恢复用户【" + this.getClass().getName() + ".recoverUser】用户异常！", e);
			msg = "操作异常，请您稍后再试";
		}
		return msg;
	}

	/**
	 * 查询用户数据
	 * @return map
	 */
	@RequestMapping(value = "/getUserAndRoles", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> getUserAndRoles(@RequestParam("id") Integer id) {
		logger.debug("查询用户数据！id:" + id);
		Map<String, Object> map = new HashMap<>();
		try {
			if (null == id) {
				logger.debug("查询用户数据==请求参数有误，请您稍后再试");
				map.put("msg", "请求参数有误，请您稍后再试");
				return map;
			}
			// 查询用户
			UserRolesVO urvo = userService.getUserAndRoles(id);
			logger.debug("查询用户数据！urvo=" + urvo);
			if (null != urvo) {
				map.put("user", urvo);
				// 获取全部角色数据
				List<Role> roles = this.authService.getRoles();
				logger.debug("查询角色数据！roles=" + roles);
				if (null != roles && roles.size() > 0) {
					map.put("roles", roles);
				}
				map.put("msg", "ok");
			} else {
				map.put("msg", "查询用户信息有误，请您稍后再试");
			}
			logger.debug("查询用户数据成功！map=" + map);
			return map;
		} catch (Exception e) {
			e.printStackTrace();
			map.put("msg", "查询用户错误，请您稍后再试");
			logger.error("查询用户数据异常！", e);
		}
		return map;
	}


	/**
	 * 修改密码之确认手机号
	 * @param mobile
	 * @param picCode
	 * @return
	 */
	@RequestMapping(value = "updatePwd", method = RequestMethod.POST)
	@ResponseBody
	public ResponseResult updatePwd(@RequestParam("mobile") String mobile,
			@RequestParam("picCode") String picCode,
			@RequestParam("mobileCode") String mobileCode) {
		logger.debug("修改密码之确认手机号！mobile:" + mobile + ",picCode=" + picCode + ",mobileCode=" + mobileCode);
		ResponseResult responseResult = new ResponseResult();
		responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
		try {
			if (!ValidateUtil.isMobilephone(mobile)) {
				responseResult.setMessage("手机号格式有误，请您重新填写");
				logger.debug("修改密码之确认手机号，结果=responseResult:" + responseResult);
				return responseResult;
			}
			if (!ValidateUtil.isPicCode(picCode)) {
				responseResult.setMessage("图片验证码有误，请您重新填写");
				logger.debug("发修改密码之确认手机号，结果=responseResult:" + responseResult);
				return responseResult;
			}
			if (!ValidateUtil.isCode(mobileCode)) {
				responseResult.setMessage("短信验证码有误，请您重新填写");
				logger.debug("发修改密码之确认手机号，结果=responseResult:" + responseResult);
				return responseResult;
			}
			// 判断用户是否登录
			User existUser = (User) SecurityUtils.getSubject().getPrincipal();
			if (null == existUser) {
				responseResult.setMessage("您未登录或登录超时，请您重新登录后再试");
				logger.debug("修改密码之确认手机号，结果=responseResult:" + responseResult);
				return responseResult;
			} else {
				// 校验验证码
				/*if(!existUser.getMcode().equals(user.getSmsCode())){ //不等
				 responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
				 responseResult.setMessage("短信验证码输入有误");
				 logger.debug("用户登录，结果=responseResult:"+responseResult);
				 return responseResult;
				} //1分钟
				long beginTime =existUser.getSendTime().getTime();
				long endTime = new Date().getTime();
				if(((endTime-beginTime)-60000>0)){
					 responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
					 responseResult.setMessage("短信验证码超时");
					 logger.debug("用户登录，结果=responseResult:"+responseResult);
					 return responseResult;
				}*/
			}
		} catch (Exception e) {
			e.printStackTrace();
			responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
			responseResult.setMessage("操作失败，请您稍后再试");
			logger.error("修改密码之确认手机号异常！", e);
			return responseResult;
		}
		responseResult.setCode(IStatusMessage.SystemStatus.SUCCESS.getCode());
		responseResult.setMessage("SUCCESS");
		logger.debug("修改密码之确认手机号，结果=responseResult:" + responseResult);
		return responseResult;
	}

	/**
	 * 修改密码
	 * @param pwd
	 * @param isPwd
	 * @return
	 */
	@RequestMapping(value = "setPwd", method = RequestMethod.POST)
	@ResponseBody
	public ResponseResult setPwd(@RequestParam("pwd") String pwd, @RequestParam("isPwd") String isPwd) {
		logger.debug("修改密码！pwd:" + pwd + ",isPwd=" + isPwd);
		ResponseResult responseResult = new ResponseResult();
		try {
			if (!ValidateUtil.isSimplePassword(pwd)
					|| !ValidateUtil.isSimplePassword(isPwd)) {
				responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
				responseResult.setMessage("密码格式有误，请您重新填写");
				logger.debug("修改密码，结果=responseResult:" + responseResult);
				return responseResult;
			}
			if (!pwd.equals(isPwd)) {
				responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
				responseResult.setMessage("两次密码输入不一致，请您重新填写");
				logger.debug("发修改密码，结果=responseResult:" + responseResult);
				return responseResult;
			}
			// 判断用户是否登录
			User existUser = (User) SecurityUtils.getSubject().getPrincipal();
			if (null == existUser) {
				responseResult.setCode(IStatusMessage.SystemStatus.NO_LOGIN.getCode());
				responseResult.setMessage("您未登录或登录超时，请您重新登录后再试");
				logger.debug("修改密码，结果=responseResult:" + responseResult);
				return responseResult;
			}
			// 修改密码
			int num = this.userService.updatePwd(existUser.getId(), DigestUtils.md5Hex(pwd));
			if (num != 1) {
				responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
				responseResult.setMessage("操作失败，请您稍后再试");
				logger.debug("修改密码失败，已经离职或该用户被删除！结果=responseResult:" + responseResult);
			}
		} catch (Exception e) {
			e.printStackTrace();
			responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
			responseResult.setMessage("操作失败，请您稍后再试");
			logger.error("修改密码异常！", e);
		}
		logger.debug("修改密码，结果=responseResult:" + responseResult);
		return responseResult;
	}




}
