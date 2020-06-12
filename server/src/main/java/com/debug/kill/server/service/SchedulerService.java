package com.debug.kill.server.service;

import com.debug.kill.model.entity.ItemKillSuccess;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * 定时任务
 */
@Service
public class SchedulerService {
    public  static  final Logger log = LoggerFactory.getLogger(SchedulerService.class);
    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;
    @Autowired
    private Environment env;
    /**
     *定时获取status=0的订单并判断是否超时，然后进行失效
     */
    @Scheduled(cron ="0/30 * * * * ?" )
    public void scheduleExpireOrders(){
        log.info("v1的定时任务");
        try{
            List<ItemKillSuccess> list = itemKillSuccessMapper.selectExpireOrders();
            if(list!=null && !list.isEmpty()){

                list.stream().forEach(item -> {
                    if(item!=null && item.getDiffTime()>env.getProperty("scheduler.expire.orders.time",Integer.class)){
                        itemKillSuccessMapper.expireOrder(item.getCode());
                    }
                });
            }
        }catch (Exception e){
            log.error("定时获取status=0的订单并判断是否超时，然后进行失效-发生异常:",e.fillInStackTrace());
        }
    }
//    @Scheduled(cron ="0/10 * * * * ?" )
//    public void scheduleExpireOrdersV2(){
//        log.info("v2的定时任务");
//    }
//    @Scheduled(cron ="0/11 * * * * ?" )
//    public void scheduleExpireOrdersV3(){
//        log.info("v3的定时任务");
//    }
}
