# PluginTestDemoApplication


理解Android插件原理的DEMO练手，之后的插件相关都使用此工程，此工程的APK插件使用[AndroidCodeDemoTest](https://github.com/sparkfengbo/AndroidCodeDemoTest)的练手工程代码编译的APK。

比较好的参考文章有：

- [Android插件化原理解析——概要](http://weishu.me/2016/01/28/understand-plugin-framework-overview/)
- [苹果核 - Android插件化实践(1)--动态代理](http://pingguohe.net/2017/12/25/android-plugin-practice-part-1.html)
- [插件化知识详细分解](http://blog.csdn.net/yulong0809/article/details/56841993)


目前遇到的问题：

- 如何支持插件中使用注解？   目前的启动插件中使用注解的Activity会报错：`java.lang.NullPointerException: null receiver`
- ....

总而言之，插件化是提升大型APP灵活性的一种解决方案，要求开发者对Android系统FrameWork层的理解和版本之间的区别有极高的要求。

研究插件化 也有助于 RD的自我提升
