package com.debug.kill.server.service;

import com.debug.kill.server.dto.MailDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

@Service
@EnableAsync
public class MailService {
    public static final Logger log = LoggerFactory.getLogger(MailService.class);
    @Autowired
    private JavaMailSender sender;
    @Autowired
    private Environment env;
    @Async
    public void sendSimpleEmail(MailDto dto){
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(env.getProperty("mail.send.from"));
            message.setTo(dto.getTos());
            message.setSubject(dto.getSubject());
            message.setText(dto.getContent());
            sender.send(message);
            log.info("发送简单文本-发送成功");
        }catch (Exception e){
            log.error("发送简单文本-发生异常",e.fillInStackTrace());
        }
    }

    @Async
    public void sendHTMLMail(MailDto dto){
        try{
            MimeMessage message = sender.createMimeMessage();

            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true, "utf-8");
            messageHelper.setFrom(env.getProperty("mail.send.from"));
            messageHelper.setTo(dto.getTos());
            messageHelper.setSubject(dto.getSubject());
            messageHelper.setText(dto.getContent(),true);
            sender.send(message);
            log.info("发送HTML邮件-发送成功");
        }catch (Exception e){
            log.error("发送HTML邮件-发生异常",e.fillInStackTrace());
        }
    }
}
