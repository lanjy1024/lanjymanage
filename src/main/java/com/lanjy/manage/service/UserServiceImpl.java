package com.lanjy.manage.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lanjy.manage.dao.RoleMapper;
import com.lanjy.manage.dao.UserMapper;
import com.lanjy.manage.dao.UserRoleMapper;
import com.lanjy.manage.entity.UserDTO;
import com.lanjy.manage.entity.UserRoleDTO;
import com.lanjy.manage.entity.UserRolesVO;
import com.lanjy.manage.entity.UserSearchDTO;
import com.lanjy.manage.pojo.Role;
import com.lanjy.manage.pojo.User;
import com.lanjy.manage.pojo.UserRoleKey;
import com.lanjy.manage.shiro.ShiroRealm;
import com.lanjy.manage.utils.DateUtil;
import com.lanjy.manage.utils.PageDataResult;
import com.lanjy.manage.utils.SendMsgServer;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @项目名称：lanjy-manage
 * @包名：com.lanjy.manage.service
 * @类描述：
 * @创建人：lanjy
 * @创建时间：2017-12-20 15:53
 * @version：V1.0
 */
@Service
public class UserServiceImpl implements UserService {
	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
	@Autowired
	private UserMapper userMapper;
	@Autowired
	private RoleMapper roleMapper;
	@Autowired
	private UserRoleMapper userRoleMapper;

	@Override
	public PageDataResult getUsers(UserSearchDTO userSearch, int page, int limit) {
		// 时间处理
		if (null != userSearch) {
			if (StringUtils.isNotEmpty(userSearch.getInsertTimeStart())
					&& StringUtils.isEmpty(userSearch.getInsertTimeEnd())) {
				userSearch.setInsertTimeEnd(DateUtil.format(new Date()));
			} else if (StringUtils.isEmpty(userSearch.getInsertTimeStart())
					&& StringUtils.isNotEmpty(userSearch.getInsertTimeEnd())) {
				userSearch.setInsertTimeStart(DateUtil.format(new Date()));
			}
			if (StringUtils.isNotEmpty(userSearch.getInsertTimeStart())
					&& StringUtils.isNotEmpty(userSearch.getInsertTimeEnd())) {
				if (userSearch.getInsertTimeEnd().compareTo(
						userSearch.getInsertTimeStart()) < 0) {
					String temp = userSearch.getInsertTimeStart();
					userSearch.setInsertTimeStart(userSearch.getInsertTimeEnd());
					userSearch.setInsertTimeEnd(temp);
				}
			}
		}
		PageDataResult pdr = new PageDataResult();
		PageHelper.startPage(page, limit);
		List<UserRoleDTO> urList = userMapper.getUsers(userSearch);
		// 获取分页查询后的数据
		PageInfo<UserRoleDTO> pageInfo = new PageInfo<>(urList);
		// 设置获取到的总记录数total：
		pdr.setTotals(Long.valueOf(pageInfo.getTotal()).intValue());
		// 将角色名称提取到对应的字段中
		if (null != urList && urList.size() > 0) {
			for (UserRoleDTO ur : urList) {
				List<Role> roles = roleMapper.getRoleByUserId(ur.getId());
				if (null != roles && roles.size() > 0) {
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < roles.size(); i++) {
						Role r = roles.get(i);
						sb.append(r.getRoleName());
						if (i != (roles.size() - 1)) {
							sb.append("，");
						}
					}
					ur.setRoleNames(sb.toString());
				}
			}
		}
		pdr.setList(urList);
		return pdr;
	}

	@Override
	public String setDelUser(Integer id, Integer isDel, Integer insertUid,
			Integer version) {
		User dataUser = this.userMapper.selectByPrimaryKey(id);
		// 版本不一致
		if (null != dataUser
				&& null != dataUser.getVersion()
				&& !String.valueOf(version).equals(
						String.valueOf(dataUser.getVersion()))) {
			return "操作失败，请您稍后再试";
		}
		return this.userMapper.setDelUser(id, isDel, insertUid) == 1 ? "ok"
				: "删除失败，请您稍后再试";
	}

	/**
	 * 基于@Transactional 注解的方式声明事务
	 * propagation	事务的传播行为，默认值为 REQUIRED。
	 * isolation	事务的隔离度，默认值采用 DEFAULT。
	 * timeout	事务的超时时间，默认值为-1。如果超过该时间限制但事务还没有完成，则自动回滚事务。
	 * rollbackFor	用于指定能够触发事务回滚的异常类型，如果有多个异常类型需要指定，各类型之间可以通过逗号分隔。
	 *
	 * @Transactional 注解放在类级别时，表示所有该类的公共方法都配置相同的事务属性信息。
	 * 类级别和方法级同时配置了@Transactional 时，方法级的配置信息优先
	 * @Transactional 只能应用到 public 方法才有效
	 * @param user
	 * @param roleIds
	 * @return
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT, timeout = 30000, rollbackFor = {
			RuntimeException.class, Exception.class })
	public String setUser(User user, String roleIds) {
		int userId;
		if (user.getId() != null) {
			// 判断用户是否已经存在
			User existUser = this.userMapper.findUserByMobile(user.getMobile());
			if (null != existUser
					&& !String.valueOf(existUser.getId()).equals(String.valueOf(user.getId()))) {
				return "该手机号已经存在";
			}
			User exist = this.userMapper.findUserByName(user.getUsername());
			if (null != exist
					&& !String.valueOf(exist.getId()).equals(String.valueOf(user.getId()))) {
				return "该用户名已经存在";
			}
			User dataUser = this.userMapper.selectByPrimaryKey(user.getId());
			// 版本不一致
			if (null != dataUser
					&& null != dataUser.getVersion()
					&& !String.valueOf(user.getVersion()).equals(String.valueOf(dataUser.getVersion()))) {
				return "操作失败，请您稍后再试";
			}
			// 更新用户
			userId = user.getId();
			user.setUpdateTime(new Date());
			// 设置加密密码
			if (StringUtils.isNotBlank(user.getPassword())) {
				user.setPassword(DigestUtils.md5Hex(user.getPassword()));
			}
			this.userMapper.updateByPrimaryKeySelective(user);
			// 删除之前的角色
			List<UserRoleKey> urs = this.userRoleMapper.findByUserId(userId);
			if (null != urs && urs.size() > 0) {
				for (UserRoleKey ur : urs) {
					this.userRoleMapper.deleteByPrimaryKey(ur);
				}
			}
			// 如果是自己，修改完成之后，直接退出；重新登录
			User adminUser = (User) SecurityUtils.getSubject().getPrincipal();
			if (adminUser != null
					&& adminUser.getId().intValue() == user.getId().intValue()) {
				logger.debug("更新自己的信息，退出重新登录！adminUser=" + adminUser);
				SecurityUtils.getSubject().logout();
			}
			// 方案一【不推荐】：通过SessionDAO拿到所有在线的用户，Collection<Session> sessions =
			// sessionDAO.getActiveSessions();
			// 遍历找到匹配的，更新他的信息【不推荐，分布式或用户数量太大的时候，会有问题。】；
			// 方案二【推荐】：用户信息价格flag（或version）标记，写个拦截器，每次请求判断flag（或version）是否改动，如有改动，请重新登录或自动更新用户信息（推荐）；

			// 清除ehcache中所有用户权限缓存，必须触发鉴权方法才能执行授权方法doGetAuthorizationInfo
			RealmSecurityManager rsm = (RealmSecurityManager) SecurityUtils
					.getSecurityManager();
			ShiroRealm authRealm = (ShiroRealm) rsm.getRealms().iterator()
					.next();
			authRealm.clearCachedAuth();
			logger.debug("清除所有用户权限缓存！！！");
		} else {
			// 判断用户是否已经存在
			User existUser = this.userMapper.findUserByMobile(user.getMobile());
			if (null != existUser) {
				return "该手机号已经存在";
			}
			User exist = this.userMapper.findUserByName(user.getUsername());
			if (null != exist) {
				return "该用户名已经存在";
			}
			// 新增用户
			user.setInsertTime(new Date());
			user.setIsDel(false);
			user.setIsJob(false);
			// 设置加密密码
			if (StringUtils.isNotBlank(user.getPassword())) {
				user.setPassword(DigestUtils.md5Hex(user.getPassword()));
			} else {
				user.setPassword(DigestUtils.md5Hex("654321"));
			}
			this.userMapper.insert(user);
			userId = user.getId();
		}
		// 给用户授角色
		String[] arrays = roleIds.split(",");
		for (String roleId : arrays) {
			UserRoleKey urk = new UserRoleKey();
			urk.setRoleId(Integer.valueOf(roleId));
			urk.setUserId(userId);
			this.userRoleMapper.insert(urk);
		}
		return "ok";
	}

	public static void main(String[] args) {
		System.out.println(DigestUtils.md5Hex("654321"));
	}

	@Override
	public String setJobUser(Integer id, Integer isJob, Integer insertUid,
			Integer version) {
		User dataUser = this.userMapper.selectByPrimaryKey(id);
		// 版本不一致
		if (null != dataUser
				&& null != dataUser.getVersion()
				&& !String.valueOf(version).equals(
						String.valueOf(dataUser.getVersion()))) {
			return "操作失败，请您稍后再试";
		}
		return this.userMapper.setJobUser(id, isJob, insertUid) == 1 ? "ok"
				: "操作失败，请您稍后再试";
	}

	@Override
	public UserRolesVO getUserAndRoles(Integer id) {
		// 获取用户及他对应的roleIds
		return this.userMapper.getUserAndRoles(id);

	}

	@Override
	public String sendMsg(UserDTO user) {
		// 校验用户名和密码 是否正确
		User existUser = this.userMapper.findUser(user.getUsername(),
				DigestUtils.md5Hex(user.getPassword()));
		if (null != existUser && existUser.getMobile().equals(user.getMobile())) {
			String mobileCode = "";
			if (existUser.getSendTime() != null) {
				long beginTime = existUser.getSendTime().getTime();
				long endTime = new Date().getTime();
				// 1分钟内有效
				if (((endTime - beginTime) < 60000)) {
					logger.debug("发送短信验证码【lanjy-manager-->UserServiceImpl.sendMsg】用户信息=existUser:"
							+ existUser);
					mobileCode = existUser.getMcode();
				}
			}
			if (StringUtils.isBlank(mobileCode)) {
				// 1分钟以内，有效
				mobileCode = String
						.valueOf((int) ((Math.random() * 9 + 1) * 100000));
				// 保存短信
				existUser.setMcode(mobileCode);
			}
			// 更新验证码时间，延长至当前时间
			existUser.setSendTime(new Date());
			this.userMapper.updateByPrimaryKeySelective(existUser);
			// 发送短信验证码 ok、no
			return SendMsgServer.SendMsg(mobileCode + "(验证码)，如不是本人操作，请忽略此消息。",
					user.getMobile());
		} else {
			return "您输入的用户信息有误，请您重新输入";
		}
	}

	@Override
	public User findUserByMobile(String mobile) {
		return this.userMapper.findUserByMobile(mobile);
	}

	@Override
	public String sendMessage(int userId, String mobile) {
		//String mobile_code = String.valueOf((Math.random() * 9 + 1) * 100000);
		// 保存短信
		String mobile_code = "111111";//未接入短信发送功能，这里使用固定的验证码：6个1
		User user = new User();
		user.setId(userId);
		user.setMcode(mobile_code);
		user.setSendTime(new Date());
		this.userMapper.updateByPrimaryKeySelective(user);
		// 发送短信验证码 ok、no
		return SendMsgServer.SendMsg(mobile_code + "(验证码)，如不是本人操作，请忽略此消息。", user.getMobile());
	}

	@Override
	public int updatePwd(Integer id, String password) {
		return this.userMapper.updatePwd(id, password);
	}

	@Override
	public int updateLastLoginTime(User user) {
		return this.userMapper.updateByPrimaryKeySelective(user);
	}

	@Override
	public int setUserLockNum(Integer id, int isLock) {
		return this.userMapper.setUserLockNum(id, isLock);
	}
}
