package com.debug.kill.server.service;



import com.debug.kill.model.dto.KillSuccessUserInfo;
import com.debug.kill.model.entity.ItemKillSuccess;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import com.debug.kill.server.dto.KillDto;
import com.debug.kill.server.dto.MailDto;

import com.debug.kill.server.service.impl.KillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
/**
 *RabbitMq接收消息服务
 **/
@Service
public class RabbitReceiverService {

    public  static  final Logger log = LoggerFactory.getLogger(RabbitReceiverService.class);
    @Autowired
    private MailService mailService;
    @Autowired
    private KillService killService;
    @Autowired
    private Environment env;
    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @RabbitListener(queues = {"${mq.kill.item.success.email.queue}"},containerFactory = "singleListenerContainer")
    public void consumeEmailMsg(KillSuccessUserInfo info){
        try{
            log.info("秒杀异步邮件通知-接收消息：{}",info);
            //TODO:异步拿到订单信息，用于发送邮件
            final String content = String.format(env.getProperty("mail.kill.item.success.content"),info.getItemName(),info.getCode());
            MailDto dto = new MailDto(env.getProperty("mail.kill.item.success.subject"), content, new String[]{info.getEmail()});
            //TODO:mailService.sendSimpleEmail(dto);
            mailService.sendHTMLMail(dto);
        }catch (Exception e){
            log.error("秒杀异步邮件通知-接收消息-发生异常：",e.fillInStackTrace());
        }
    }

    /**
     * 用户秒杀成功后超时未支付-监听者
     * @param info
     */
    @RabbitListener(queues = {"${mq.kill.item.success.kill.dead.real.queue}"},containerFactory = "singleListenerContainer")
    public void consumeExpireOrder(KillSuccessUserInfo info){
        try{
            log.info("用户秒杀成功后超时未支付-监听者-接收消息：{}",info);
            if(info!=null){
                ItemKillSuccess entity = itemKillSuccessMapper.selectByPrimaryKey(info.getCode());
                if(entity!=null && entity.getStatus().intValue()==0){
                    itemKillSuccessMapper.expireOrder(info.getCode());
                }
            }

        }catch (Exception e){
            log.error("用户秒杀成功后超时未支付-监听者-发生异常：",e.fillInStackTrace());
        }
    }

    //秒杀时异步接收Mq消息-监听者
    @RabbitListener(queues = {"${mq.kill.item.execute.limit.queue.name}"},containerFactory = "multiListenerContainer")
    public void consumeKillExecuteMqMsg(KillDto dto){
        try {
            if (dto!=null){
                //采用任何一种加分布锁的处理方法都是可行的 killItemV5也行
                killService.killItemV4(dto.getKillId(),dto.getUserId());
            }
        }catch (Exception e){
            log.error("用户秒杀成功后超时未支付-监听者-发生异常：",e.fillInStackTrace());
        }
    }
}
