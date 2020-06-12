package com.debug.kill.server.controller;




import jodd.util.StringUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {
    private static Logger log = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private Environment env;


    @RequestMapping(value = {"to/login","/unauth"})
    public String toLogin(){
        return "login";
    }

    /**
     * 登录认证
     * @param userName
     * @param password
     * @param modelMap
     * @return
     */
    @RequestMapping(value = "/login",method = RequestMethod.POST)
    public String login(@RequestParam String userName, @RequestParam String password, ModelMap modelMap){
        String errorMsg ="";
        try{
            if(!SecurityUtils.getSubject().isAuthenticated()){
                String salt = env.getProperty("shiro.encrypt.password.salt");
                Md5Hash md5Hash = new Md5Hash(password,salt);
                String passwordMD5 = md5Hash.toString();
                UsernamePasswordToken token = new UsernamePasswordToken(userName,passwordMD5);
                SecurityUtils.getSubject().login(token);
            }
        }catch (UnknownAccountException e){
            errorMsg=e.getMessage();
            modelMap.addAttribute("userName",userName);
        }catch (DisabledAccountException e){
            errorMsg=e.getMessage();
            modelMap.addAttribute("userName",userName);

        }catch (IncorrectCredentialsException e){
            errorMsg=e.getMessage();
            modelMap.addAttribute("userName",userName);
        }catch(Exception e){
            errorMsg="未知异常，请联系管理员！";
            e.printStackTrace();
        }
        if(StringUtil.isBlank(errorMsg)){
            return "redirect:/index";
        }else{
            modelMap.addAttribute("errorMsg",errorMsg);
            return "login";
        }

    }
    @RequestMapping(value = "/logout")
    public String logout(){
        SecurityUtils.getSubject().logout();
        return "login";
    }
}
