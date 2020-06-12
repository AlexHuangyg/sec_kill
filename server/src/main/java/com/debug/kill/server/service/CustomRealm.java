package com.debug.kill.server.service;

import com.debug.kill.model.entity.User;
import com.debug.kill.model.mapper.UserMapper;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 用户自定义的reaml用于shiro认证授权
 */
@Service
public class CustomRealm extends AuthorizingRealm {

    private static final Logger log = LoggerFactory.getLogger(CustomRealm.class);
    private static final Long  sessionkeyTimeout = 30_000L;
    @Autowired
    private UserMapper userMapper;

    /**
     * 授权
     * @param principalCollection
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        return null;
    }

    /**
     * 认证-登录
     * @param authenticationToken
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        String username = token.getUsername();
        String password = String.valueOf(token.getPassword());
        log.info("当前登录用户为{}密码是{}",username,password);
        User user = userMapper.selectByUserName(username);

        if(user==null){
            throw new UnknownAccountException("用户名不存在");
        }
        if(!Objects.equals(1,user.getIsActive().intValue())){
            throw new DisabledAccountException("当前用户已被禁用");
        }
        if(!user.getPassword().equals(password)){
            throw new IncorrectCredentialsException("用户名密码不匹配");
        }

        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user.getUserName(),password,getName());
        setSession("uid",user.getId());
        return info;
    }

    /**
     * 将key，value塞入redis-session去管理
     * @param key
     * @param value
     */
    private void setSession(String key,Object value){
        Session session = SecurityUtils.getSubject().getSession();
        if(session!=null){
            session.setAttribute(key,value);
            session.setTimeout(sessionkeyTimeout);
        }
    }
}
