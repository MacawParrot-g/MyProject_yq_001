package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


//这个程序设计之初是我用来减轻工作量的，系统投入使用之后再也不用手动去json里抄数据了，跑完数据之后点一下自动显示事件和归因，真的太爽了（邪恶）。
//未来我可能会考虑做一个在线版打包传到服务器上共小伙伴们一起用，在线版会阉割一些功能，但是核心功能保留大多数。
//此版本是我自己的自用版，禁止分享，违者必究。
@EnableScheduling
@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class);
    }
}
