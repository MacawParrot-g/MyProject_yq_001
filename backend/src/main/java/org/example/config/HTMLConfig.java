package org.example.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class HTMLConfig implements WebMvcConfigurer{
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/html/**")
                .addResourceLocations("classpath:/html/");
    }
}
//这个类是我以前在学校里写的，目的是给让SpringBoot识别掉resources下的html视图目录，让这个目录下的前端试图可以正常的加载
//但是现在我的前端已经改用Vite构建的Vue架构了，这个类就失去了用处
//当然这个配置类算是非常古老的一个代码块，从我今年三月开始写毕业项目，一直到我进入公司，变得是时代，不变的还是这串代码。
//当时我还连续几个小时帮女同学调项目呢，她的项目就用了我的这个配置类，哈哈
//按理来说，这个类不应该存在于这里，但是我还是留着了，算是给自己留点念想吧。
