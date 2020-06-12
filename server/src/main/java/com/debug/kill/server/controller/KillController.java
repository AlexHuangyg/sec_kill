package com.debug.kill.server.controller;


import com.debug.kill.api.enums.StatusCode;
import com.debug.kill.api.response.BaseResponse;
import com.debug.kill.model.dto.KillSuccessUserInfo;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import com.debug.kill.server.dto.KillDto;
import com.debug.kill.server.service.IKillService;
import com.debug.kill.server.service.RabbitSenderService;
import com.google.common.collect.Maps;
import jodd.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class KillController {
    private static final Logger log= LoggerFactory.getLogger(KillController.class);
    private static final String prefix = "kill";

    @Autowired
    private IKillService killService;

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @RequestMapping(value=prefix+"/execute",method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse execute(@RequestBody @Validated KillDto dto, BindingResult result, HttpSession session){

        if(result.hasErrors() || dto.getKillId()<=0){
            return new BaseResponse(StatusCode.InvalidParams);
        }
        Object uid = session.getAttribute("uid");
        if(uid==null){
            return new BaseResponse(StatusCode.Fail.getCode(),"当前用户未登录");
        }
        Integer userId = (Integer) uid;
        BaseResponse response = new BaseResponse(StatusCode.Success);
        try{
            Boolean res = killService.killItem(dto.getKillId(), userId);
            if(!res){
                return new BaseResponse(StatusCode.Fail.getCode(),"商品已经抢购完了或者不在时间段");
            }

        }catch (Exception e){
            response = new BaseResponse(StatusCode.Fail.getCode(),e.getMessage());
        }
        return response;
    }

    /**
     * 用于压力测试
     * @param dto
     * @param result
     * @return
     */
    @RequestMapping(value=prefix+"/execute/lock",method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse executeLock(@RequestBody @Validated KillDto dto, BindingResult result){
        if(result.hasErrors() || dto.getKillId()<=0){
            return new BaseResponse(StatusCode.InvalidParams);
        }
        BaseResponse response = new BaseResponse(StatusCode.Success);
        try{
            Boolean res = killService.killItemV4(dto.getKillId(), dto.getUserId());
            if(!res){
                return new BaseResponse(StatusCode.Fail.getCode(),"不加分布式锁-商品已经抢购完了或者不在时间段");
            }

        }catch (Exception e){
            response = new BaseResponse(StatusCode.Fail.getCode(),e.getMessage());
        }
        return response;
    }


    @RequestMapping(value = prefix+"/record/detail/{orderNo}",method = RequestMethod.GET)
    public String killRecordDetail(@PathVariable String orderNo, ModelMap modelMap){
        if(StringUtil.isBlank(orderNo)){
            return "error";
        }
        KillSuccessUserInfo info = itemKillSuccessMapper.selectByCode(orderNo);
        if(info==null){
            return "error";
        }
        modelMap.put("info",info);
        return "killRecord";
    }
    @Autowired
    private RabbitSenderService rabbitSenderService;

    //商品秒杀核心业务逻辑-mq限流
    @RequestMapping(value = prefix+"/execute/mq",method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse executeMq(@RequestBody @Validated KillDto dto, BindingResult result, HttpSession session){
        if (result.hasErrors() || dto.getKillId()<=0){
            return new BaseResponse(StatusCode.InvalidParams);
        }
        Object uId=session.getAttribute("uid");
        if (uId==null){
            return new BaseResponse(StatusCode.UserNotLogin);
        }
        Integer userId= (Integer)uId ;

        BaseResponse response=new BaseResponse(StatusCode.Success);
        Map<String,Object> dataMap= Maps.newHashMap();
        try {
            dataMap.put("killId",dto.getKillId());
            dataMap.put("userId",userId);
            response.setData(dataMap);

            dto.setUserId(userId);
            rabbitSenderService.sendKillExecuteMqMsg(dto);
        }catch (Exception e){
            response=new BaseResponse(StatusCode.Fail.getCode(),e.getMessage());
        }
        return response;
    }


    //商品秒杀核心业务逻辑-mq限流-立马跳转至抢购结果页
    @RequestMapping(value = prefix+"/execute/mq/to/result",method = RequestMethod.GET)
    public String executeToResult(@RequestParam Integer killId,HttpSession session,ModelMap modelMap){
        Object uId=session.getAttribute("uid");
        if (uId!=null){
            Integer userId= (Integer)uId ;

            modelMap.put("killId",killId);
            modelMap.put("userId",userId);
        }
        return "executeMqResult";
    }

    //抢购成功跳转页面
    @RequestMapping(value = prefix+"/execute/success",method = RequestMethod.GET)
    public String executeSuccess(){
        return "executeSuccess";
    }

    //抢购失败跳转页面
    @RequestMapping(value = prefix+"/execute/fail",method = RequestMethod.GET)
    public String executeFail(){
        return "executeFail";
    }

}
