package com.lanjy.manage.web.user;

import com.lanjy.manage.entity.ResponseResult;
import com.lanjy.manage.entity.UserDTO;
import com.lanjy.manage.pojo.User;
import com.lanjy.manage.service.UserService;
import com.lanjy.manage.utils.IStatusMessage;
import com.lanjy.manage.utils.ValidateUtil;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.Validator;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @项目名称：lanjymanage
 * @包名： com.lanjy.manage.web.user
 * @类描述：
 * @创建人：lanjy
 * @创建时间：2019/11/6
 */
@Controller
@RequestMapping("/login")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);


    @Autowired
    private EhCacheManager cacheManager;

    @Autowired
    private UserService userService;


    /**
     * 登录【使用shiro中自带的HashedCredentialsMatcher结合ehcache（记录输错次数）配置进行密码输错次数限制】
     * </br>缺陷是，无法友好的在后台提供解锁用户的功能，当然，可以直接提供一种解锁操作，清除ehcache缓存即可，不记录在用户表中；
     * </br>
     * @param user
     * @param rememberMe
     * @return
     */
    //@RequestMapping(value = "login", method = RequestMethod.POST)
    @PostMapping
    @ResponseBody
    public ResponseResult login(UserDTO user, @RequestParam(value = "rememberMe", required = false) boolean rememberMe) {
        logger.debug("用户登录，请求参数=user:" + user + "，是否记住我：" + rememberMe);

        ResponseResult responseResult = new ResponseResult();
        responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());

        if (null == user) {
            responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
            responseResult.setMessage("请求参数有误，请您稍后再试");
            logger.debug("用户登录，结果=responseResult:" + responseResult);
            return responseResult;
        }

        if (!validatorRequestParam(user, responseResult)) {
            logger.debug("用户登录，结果=responseResult:" + responseResult);
            return responseResult;
        }
        // 用户是否存在
        User existUser = userService.findUserByMobile(user.getMobile());
        if (existUser == null) {
            responseResult.setMessage("该用户不存在，请您联系管理员");
            logger.debug("用户登录，结果=responseResult:" + responseResult);
            return responseResult;
        }
        // 是否离职 is_job:0-正常 IsJob false，1-离职 IsJob true
        if (existUser.getIsJob()) {
            responseResult.setMessage("登录用户已离职，请您联系管理员");
            logger.debug("用户登录，结果=responseResult:" + responseResult);
            return responseResult;
        }
        // 校验验证码
		/*if(!existUser.getMcode().equals(user.getSmsCode())){ //不等
		 responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
		 responseResult.setMessage("短信验证码输入有误");
		 logger.debug("用户登录，结果=responseResult:"+responseResult);
		 return responseResult;
		}*/

        //1分钟 短信验证码超时
		/*long beginTime =existUser.getSendTime().getTime();
		long endTime = new Date().getTime();
		if(((endTime-beginTime)-60000>0)){
			 responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
			 responseResult.setMessage("短信验证码超时");
			 logger.debug("用户登录，结果=responseResult:"+responseResult);
			 return responseResult;
		}*/
        // 用户登录
        try {
            // 1、 封装用户名、密码、是否记住我到token令牌对象 [支持记住我]
            AuthenticationToken token = new UsernamePasswordToken(
                    user.getMobile(),
                    DigestUtils.md5Hex(user.getPassword()),
                    rememberMe);
            // 2、 Subject调用login
            Subject subject = SecurityUtils.getSubject();
            // 在调用了login方法后,SecurityManager会收到AuthenticationToken,并将其发送给已配置的Realm执行必须的认证检查
            // 每个Realm都能在必要时对提交的AuthenticationTokens作出反应
            // 所以这一步在调用login(token)方法时,它会走到MyRealm.doGetAuthenticationInfo()方法中,具体验证方式详见此方法
            logger.debug("用户登录，用户验证开始！user=" + user.getMobile());
            subject.login(token);
            responseResult.setCode(IStatusMessage.SystemStatus.SUCCESS.getCode());
            if (null == existUser.getLastLoginTime()){
                responseResult.setMessage("首次登录系统，请您及时更改默认密码。");
                logger.info("用户登录，用户验证通过！user=" + user.getMobile()+",更新最后一次登录时间");
                existUser.setLastLoginTime(new Date());
                userService.updateLastLoginTime(existUser);
            }else{
                responseResult.setMessage("你上一次登录系统的时间是："+new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒").format(existUser.getLastLoginTime()));
            }
        } catch (UnknownAccountException uae) {
            logger.error("用户登录，用户验证未通过：未知用户！user=" + user.getMobile(), uae);
            responseResult.setMessage("该用户不存在，请您联系管理员");
        } catch (IncorrectCredentialsException ice) {
            // 获取输错次数
            logger.error("用户登录，用户验证未通过：错误的凭证，密码输入错误！user=" + user.getMobile(), ice);
            responseResult.setMessage("用户名或密码不正确");
        } catch (LockedAccountException lae) {
            logger.error("用户登录，用户验证未通过：账户已锁定！user=" + user.getMobile(), lae);
            responseResult.setMessage("账户已锁定");
        } catch (ExcessiveAttemptsException eae) {
            logger.error("用户登录，用户验证未通过：错误次数大于5次,账户已锁定！user=.getMobile()" + user, eae);
            responseResult.setMessage("用户名或密码错误次数大于5次,账户已锁定!" +
                    "</br><span style='color:red;font-weight:bold; '>2分钟后可再次登录，或联系管理员解锁</span>");
            // 这里结合了，另一种密码输错限制的实现，基于redis或mysql的实现；也可以直接使用RetryLimitHashedCredentialsMatcher限制5次
        } /*catch (DisabledAccountException sae){
			 logger.error("用户登录，用户验证未通过：帐号已经禁止登录！user=" +
			 user.getMobile(),sae);
			 responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
			 responseResult.setMessage("帐号已经禁止登录");
		}*/ catch (AuthenticationException ae) {
            // 通过处理Shiro的运行时AuthenticationException就可以控制用户登录失败或密码错误时的情景
            logger.error("用户登录，用户验证未通过：认证异常，异常信息如下！user=" + user.getMobile(), ae);
            responseResult.setMessage("用户名或密码不正确");
        } catch (Exception e) {
            logger.error("用户登录，用户验证未通过：操作异常，异常信息如下！user=" + user.getMobile(), e);
            responseResult.setMessage("用户登录失败，请您稍后再试");
        }
        Cache<String, AtomicInteger> passwordRetryCache = cacheManager.getCache("passwordRetryCache");
        if (null != passwordRetryCache) {
            int retryNum = (passwordRetryCache.get(existUser.getMobile()) == null ? 0 : passwordRetryCache.get(existUser.getMobile())).intValue();
            logger.debug("输错次数：" + retryNum);
            if (retryNum > 0 && retryNum < 6) {
                responseResult.setMessage("用户名或密码错误" + retryNum + "次,再输错" + (6 - retryNum) + "次账号将锁定");
            }
        }
        logger.debug("用户登录，user=" + user.getMobile() + ",登录结果=responseResult:" + responseResult);
        return responseResult;
    }





    /**
     * 发送短信验证码
     * @param user
     * @return
     */
    @RequestMapping(value = "sendMsg", method = RequestMethod.POST)
    @ResponseBody
    public ResponseResult sendMsg(UserDTO user) {
        logger.debug("发送短信验证码！user:" + user);
        ResponseResult responseResult = new ResponseResult();
        try {
            if (null == user) {
                responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
                responseResult.setMessage("请求参数有误，请您稍后再试");
                logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            if (!validatorRequestParam(user, responseResult)) {
                logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            // 送短信验证码
            // String msg=userService.sendMsg(user);
            String msg = "ok";
            if (msg != "ok") {
                responseResult.setCode(IStatusMessage.SystemStatus.ERROR
                        .getCode());
                responseResult.setMessage(msg == "no" ? "发送验证码失败，请您稍后再试" : msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
            responseResult.setMessage("发送短信验证码失败，请您稍后再试");
            logger.error("发送短信验证码异常！", e);
        }
        logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
        return responseResult;
    }



    /**
     * 登录【使用redis和mysql实现，用户密码输错次数限制，和锁定解锁用户的功能//TODO】
     * 该实现后续会提供！TODO
     * @param user
     * @param rememberMe
     * @return
     */
    @RequestMapping(value = "logina", method = RequestMethod.POST)
    @ResponseBody
    public ResponseResult logina(
            UserDTO user,
            @RequestParam(value = "rememberMe", required = false) boolean rememberMe) {
        logger.debug("用户登录，请求参数=user:" + user + "，是否记住我：" + rememberMe);
        ResponseResult responseResult = new ResponseResult();
        responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
        if (null == user) {
            responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR
                    .getCode());
            responseResult.setMessage("请求参数有误，请您稍后再试");
            logger.debug("用户登录，结果=responseResult:" + responseResult);
            return responseResult;
        }
        if (!validatorRequestParam(user, responseResult)) {
            logger.debug("用户登录，结果=responseResult:" + responseResult);
            return responseResult;
        }
        // 用户是否存在
        User existUser = userService.findUserByMobile(user.getMobile());
        if (existUser == null) {
            responseResult.setMessage("该用户不存在，请您联系管理员");
            logger.debug("用户登录，结果=responseResult:" + responseResult);
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
        // 是否锁定
        boolean flag = false;
        // 用户登录
        try {
            // 1、 封装用户名和密码到token令牌对象 [支持记住我]
            AuthenticationToken token = new UsernamePasswordToken(
                    user.getMobile(), DigestUtils.md5Hex(user.getPassword()),
                    rememberMe);
            // 2、 Subject调用login
            Subject subject = SecurityUtils.getSubject();
            // 在调用了login方法后,SecurityManager会收到AuthenticationToken,并将其发送给已配置的Realm执行必须的认证检查
            // 每个Realm都能在必要时对提交的AuthenticationTokens作出反应
            // 所以这一步在调用login(token)方法时,它会走到MyRealm.doGetAuthenticationInfo()方法中,具体验证方式详见此方法
            logger.debug("用户登录，用户验证开始！user=" + user.getMobile());
            subject.login(token);
            responseResult.setCode(IStatusMessage.SystemStatus.SUCCESS.getCode());

            logger.info("用户登录，用户验证通过！user=" + user.getMobile());
        } catch (UnknownAccountException uae) {
            logger.error("用户登录，用户验证未通过：未知用户！user=" + user.getMobile(), uae);
            responseResult.setMessage("该用户不存在，请您联系管理员");
        } catch (IncorrectCredentialsException ice) {
            // 获取输错次数
            logger.error("用户登录，用户验证未通过：错误的凭证，密码输入错误！user=" + user.getMobile(), ice);
            responseResult.setMessage("用户名或密码不正确");
        } catch (LockedAccountException lae) {
            logger.error("用户登录，用户验证未通过：账户已锁定！user=" + user.getMobile(), lae);
            responseResult.setMessage("账户已锁定");
        } catch (ExcessiveAttemptsException eae) {
            logger.error("用户登录，用户验证未通过：错误次数大于5次,账户已锁定！user=.getMobile()" + user, eae);
            responseResult.setMessage("用户名或密码错误次数大于5次,账户已锁定，2分钟后可再次登录或联系管理员解锁");
            // 这里结合了，另一种密码输错限制的实现，基于redis或mysql的实现；也可以直接使用RetryLimitHashedCredentialsMatcher限制5次
            flag = true;
        } /*catch (DisabledAccountException sae){
		 logger.error("用户登录，用户验证未通过：帐号已经禁止登录！user=" +
		 user.getMobile(),sae);
		 responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
		 responseResult.setMessage("帐号已经禁止登录");
		}*/catch (AuthenticationException ae) {
            // 通过处理Shiro的运行时AuthenticationException就可以控制用户登录失败或密码错误时的情景
            logger.error("用户登录，用户验证未通过：认证异常，异常信息如下！user=" + user.getMobile(), ae);
            responseResult.setMessage("用户名或密码不正确");
        } catch (Exception e) {
            logger.error("用户登录，用户验证未通过：操作异常，异常信息如下！user=" + user.getMobile(), e);
            responseResult.setMessage("用户登录失败，请您稍后再试");
        }
        if (flag) {
            // 已经输错6次了，将进行锁定！【也可以使用redis记录密码输错次数，然后进行锁定//TODO】
            int num = userService.setUserLockNum(existUser.getId(), 1);
            if (num < 1) {
                logger.info("用户登录，用户名或密码错误次数大于5次,账户锁定失败！user=" + user.getMobile());
            }
        }
        responseResult.setMessage("你上一次登录系统的时间是："+existUser.getLastLoginTime());
        logger.debug("用户登录，user=" + user.getMobile() + ",登录结果=responseResult:" + responseResult);
        return responseResult;
    }

    /**
     * 发送短信验证码
     * @param mobile
     * @param picCode
     * @return
     */
    @RequestMapping(value = "sendMessage", method = RequestMethod.POST)
    @ResponseBody
    public ResponseResult sendMessage(@RequestParam("mobile") String mobile,
                                      @RequestParam("picCode") String picCode) {
        logger.debug("发送短信验证码！mobile:" + mobile + ",picCode=" + picCode);
        ResponseResult responseResult = new ResponseResult();
        try {
            if (!ValidateUtil.isMobilephone(mobile)) {
                responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
                responseResult.setMessage("手机号格式有误，请您重新填写");
                logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            if (!ValidateUtil.isPicCode(picCode)) {
                responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
                responseResult.setMessage("图片验证码有误，请您重新填写");
                logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            // 判断用户是否登录
            User existUser = (User) SecurityUtils.getSubject().getPrincipal();
            if (null == existUser) {
                responseResult.setCode(IStatusMessage.SystemStatus.NO_LOGIN.getCode());
                responseResult.setMessage("您未登录或登录超时，请您重新登录后再试");
                logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            // 送短信验证码
            String msg=userService.sendMessage(existUser.getId(),mobile);
            //String msg = "ok";
            if (msg != "ok") {
                responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
                responseResult.setMessage(msg == "no" ? "发送验证码失败，请您稍后再试" : msg);
            }
        } catch (Exception e) {
            //e.printStackTrace();
            logger.error("发送短信验证码异常！", e);
            responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
            responseResult.setMessage("发送短信验证码失败，请您稍后再试");
        }
        logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
        return responseResult;
    }




    /**
     * @描述：校验请求参数
     * @param obj
     * @param response
     * @return
     */
    protected boolean validatorRequestParam(Object obj, ResponseResult response) {
        boolean flag = false;
        Validator validator = new Validator();
        List<ConstraintViolation> ret = validator.validate(obj);
        if (ret.size() > 0) {
            // 校验参数有误
            response.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
            response.setMessage(ret.get(0).getMessageTemplate());
        } else {
            flag = true;
        }
        return flag;
    }
}
