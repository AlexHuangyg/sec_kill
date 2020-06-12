package com.debug.kill.server.controller;

import com.debug.kill.model.entity.ItemKill;
import com.debug.kill.server.service.IItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@Controller
public class ItemController {
    private static final Logger log= LoggerFactory.getLogger(ItemController.class);
    private static final String prefix = "item";
    @Autowired
    private IItemService itemService;

    @RequestMapping(value = {"/index","/",prefix+"/list",prefix+"index.xml"},method = RequestMethod.GET)
    public String list(ModelMap modelMap){
        List<ItemKill> list = null;
        try {
            list = itemService.getItemKill();
            modelMap.put("list",list);
            log.info("获取带秒杀商品-数据：{}",list);
        } catch (Exception e) {
            log.error("获取列表发生异常：",e.fillInStackTrace());
            return "redirect:/base/error";
        }

        return "list";
    }
    @RequestMapping(value = prefix+"/detail/{id}",method = RequestMethod.GET)
    public String detail(@PathVariable Integer id, ModelMap modelMap){
        if(id==null || id<0){
            return "redirect:/base/error";
        }
        try {
            ItemKill detail = itemService.getKillDetail(id);
            modelMap.put("detail",detail);

        } catch (Exception e) {
            log.error("获取详情发生异常：id={}",id,e.fillInStackTrace());
            return "redirect:/base/error";
        }

        return "info";
    }
}
