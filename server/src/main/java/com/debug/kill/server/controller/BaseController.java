package com.debug.kill.server.controller;

import com.debug.kill.api.enums.StatusCode;
import com.debug.kill.api.response.BaseResponse;
import jodd.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/base")
public class BaseController {
    private static final Logger log = LoggerFactory.getLogger(BaseController.class);
    @GetMapping("/welcome")
    public String welcome(String name, ModelMap modelMap){
        if(StringUtil.isBlank(name)){
            name = "这是welcome！";
        }
        modelMap.put("name",name);
        return "welcome";
    }
    @RequestMapping(value = "/data",method = RequestMethod.GET)
    @ResponseBody
    public String data(String name){
        if(StringUtil.isBlank(name)){
            name = "这是welcome！";
        }
        return name;
    }
    @RequestMapping(value = "/response",method = RequestMethod.GET)
    @ResponseBody
    public BaseResponse response(String name){
        BaseResponse response = new BaseResponse(StatusCode.Success);
        if(StringUtil.isBlank(name)){
            name = "这是welcome！";
        }
        response.setData(name);
        return response;
    }
    @GetMapping(value = "/error")
    public String error(){

        return "error";
    }
}
