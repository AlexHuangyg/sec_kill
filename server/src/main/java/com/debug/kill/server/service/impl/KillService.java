package com.debug.kill.server.service.impl;

import com.debug.kill.model.entity.ItemKill;
import com.debug.kill.model.entity.ItemKillSuccess;
import com.debug.kill.model.mapper.ItemKillMapper;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import com.debug.kill.server.enums.SysConstant;
import com.debug.kill.server.service.IKillService;
import com.debug.kill.server.service.RabbitSenderService;
import com.debug.kill.server.utils.RandomUtil;
import com.debug.kill.server.utils.SnowFlake;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.joda.time.DateTime;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class KillService implements IKillService {
    private static final Logger log= LoggerFactory.getLogger(KillService.class);

    private SnowFlake snowFlake=new SnowFlake(2,3);

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @Autowired
    private ItemKillMapper itemKillMapper;

    @Autowired
    private RabbitSenderService rabbitSenderService;


    @Override
    public Boolean killItem(Integer killId, Integer userId) throws Exception {
        Boolean result =false;
        //TODO:判断当前用户是否已经抢购了当前商品
        if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
            //TODO:判断当前代抢购的商品库存是否充足、以及是否出在可抢的时间段内 - canKill
            ItemKill itemKill=itemKillMapper.selectById(killId);
            if (itemKill!=null && 1==itemKill.getCanKill()){

                //TODO:扣减库存-减1
                int res=itemKillMapper.updateKillItem(killId);
                if (res>0){
                    //TODO:判断是否扣减成功了?是-生成秒杀成功的订单、同时通知用户秒杀已经成功（在一个通用的方法里面实现）
                    this.commonRecordKillSuccessInfo(itemKill,userId);

                    result=true;
                }
            }
        }else{
            throw new Exception("您已经抢购过该商品了！");
        }

        return result;
    }

    /**
     * mysql优化后
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItemV2(Integer killId, Integer userId) throws Exception {
        Boolean result =false;
        //TODO:判断当前用户是否已经抢购了当前商品
        if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
            //TODO:判断当前代抢购的商品库存是否充足、以及是否出在可抢的时间段内 - canKill
            ItemKill itemKill=itemKillMapper.selectByIdV2(killId);
            if (itemKill!=null && 1==itemKill.getCanKill()){

                //TODO:扣减库存-减1
                int res=itemKillMapper.updateKillItemV2(killId);
                if (res>0){
                    //TODO:判断是否扣减成功了?是-生成秒杀成功的订单、同时通知用户秒杀已经成功（在一个通用的方法里面实现）
                    this.commonRecordKillSuccessInfo(itemKill,userId);

                    result=true;
                }
            }
        }else{
            throw new Exception("您已经抢购过该商品了！");
        }

        return result;
    }



    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * redis分布式锁
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItemV3(Integer killId, Integer userId) throws Exception {
        Boolean result =false;
        //TODO:判断当前用户是否已经抢购了当前商品
        if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
            //TODO:借助redis的原子操作实现分布式锁
            ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
            final String key = new StringBuffer().append(killId).append(userId).append("-RedisLock").toString();
            final String value = RandomUtil.generateOrderCode();
            Boolean cacheRes = valueOperations.setIfAbsent(key, value);
            if(cacheRes){
                stringRedisTemplate.expire(key,30, TimeUnit.SECONDS);
                try {
                    //TODO:判断当前代抢购的商品库存是否充足、以及是否出在可抢的时间段内 - canKill
                    ItemKill itemKill=itemKillMapper.selectByIdV2(killId);
                    if (itemKill!=null && 1==itemKill.getCanKill()){

                        //TODO:扣减库存-减1
                        int res=itemKillMapper.updateKillItemV2(killId);
                        if (res>0){
                            //TODO:判断是否扣减成功了?是-生成秒杀成功的订单、同时通知用户秒杀已经成功（在一个通用的方法里面实现）
                            this.commonRecordKillSuccessInfo(itemKill,userId);

                            result=true;
                        }
                    }
                }catch (Exception e){
                    throw new Exception("还没到抢购时间、已经过了抢购时间或者已经被抢购完毕！");
                }finally {
                    if(value.equals(valueOperations.get(key).toString())){
                        stringRedisTemplate.delete(key);
                    }
                }
            }

        }else{
            throw new Exception("您已经抢购过该商品了！");
        }

        return result;
    }

    @Autowired
    private RedissonClient redissonClient;
    /**
     * redission分布式锁
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItemV4(Integer killId, Integer userId) throws Exception {
        Boolean result =false;
        final String lockKey = new StringBuffer().append(killId).append(userId).append("-RedisLock").toString();
        RLock lock = redissonClient.getLock(lockKey);

        try{
            //十秒操作时间
            //lock.lock(10,TimeUnit.SECONDS);
            //等待30秒，10秒操作时间，过十秒释放锁
            boolean cacheRes = lock.tryLock(30, 10, TimeUnit.SECONDS);
            if(cacheRes){
                //TODO:判断当前用户是否已经抢购了当前商品
                if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
                    //TODO:判断当前代抢购的商品库存是否充足、以及是否出在可抢的时间段内 - canKill
                    ItemKill itemKill=itemKillMapper.selectByIdV2(killId);
                    if (itemKill!=null && 1==itemKill.getCanKill()){

                        //TODO:扣减库存-减1
                        int res=itemKillMapper.updateKillItemV2(killId);
                        if (res>0){
                            //TODO:判断是否扣减成功了?是-生成秒杀成功的订单、同时通知用户秒杀已经成功（在一个通用的方法里面实现）
                            this.commonRecordKillSuccessInfo(itemKill,userId);

                            result=true;
                        }
                    }
                }else{
                    throw new Exception("您已经抢购过该商品了！");
                }
            }

        }catch (Exception e){
            throw new Exception("还没到抢购时间、已经过了抢购时间或者已经被抢购完毕！");
        }finally {
            lock.unlock();
            //lock.forceUnlock();
        }


        return result;
    }



    @Autowired
    private CuratorFramework curatorFramework;

    private static final String pathPrefix = "/kill/zkLock/";
    /**
     * zookeeper分布式锁
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItemV5(Integer killId, Integer userId) throws Exception {
        Boolean result =false;
        InterProcessMutex mutex = new InterProcessMutex(curatorFramework,pathPrefix+killId+userId+"-lock-zookeeper");
        try{
            if(mutex.acquire(10L,TimeUnit.SECONDS)){
                //TODO:判断当前用户是否已经抢购了当前商品
                if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
                    //TODO:判断当前代抢购的商品库存是否充足、以及是否出在可抢的时间段内 - canKill
                    ItemKill itemKill=itemKillMapper.selectByIdV2(killId);
                    if (itemKill!=null && 1==itemKill.getCanKill()){

                        //TODO:扣减库存-减1
                        int res=itemKillMapper.updateKillItemV2(killId);
                        if (res>0){
                            //TODO:判断是否扣减成功了?是-生成秒杀成功的订单、同时通知用户秒杀已经成功（在一个通用的方法里面实现）
                            this.commonRecordKillSuccessInfo(itemKill,userId);

                            result=true;
                        }
                    }
                }else{
                    throw new Exception("您已经抢购过该商品了！");
                }
            }

        }catch (Exception e){
            throw new Exception("还没到抢购时间、已经过了抢购时间或者已经被抢购完毕！");
        }finally {
            if(mutex!=null){
                mutex.release();
            }

        }





        return result;
    }



    /**
     * 通用的方法-记录用户秒杀成功后生成的订单-并进行异步邮件消息的通知
     * @param kill
     * @param userId
     * @throws Exception
     */
    private void commonRecordKillSuccessInfo(ItemKill kill, Integer userId) throws Exception{
        //TODO:记录抢购成功后生成的秒杀订单记录

        ItemKillSuccess entity=new ItemKillSuccess();
        String orderNo=String.valueOf(snowFlake.nextId());

        //entity.setCode(RandomUtil.generateOrderCode());   //传统时间戳+N位随机数
        entity.setCode(orderNo); //雪花算法
        entity.setItemId(kill.getItemId());
        entity.setKillId(kill.getId());
        entity.setUserId(userId.toString());
        entity.setStatus(SysConstant.OrderStatus.SuccessNotPayed.getCode().byteValue());
        entity.setCreateTime(DateTime.now().toDate());
        //TODO:学以致用，举一反三 -> 仿照单例模式的双重检验锁写法
        if (itemKillSuccessMapper.countByKillUserId(kill.getId(),userId) <= 0){
            int res=itemKillSuccessMapper.insertSelective(entity);

            if (res>0){

                //TODO:进行异步邮件消息的通知=rabbitmq+mail
                rabbitSenderService.sendKillSuccessEmailMsg(orderNo);
//
//                //TODO:入死信队列，用于 “失效” 超过指定的TTL时间时仍然未支付的订单
                rabbitSenderService.sendKillSuccessOrderExpireMsg(orderNo);
            }
        }
    }

}
