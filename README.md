# pipe-gradle-plugin
 pipe (烟枪、烟斗)，取名灵感来自 [pipeline] 。  
 因为这就是一个 gradlePlugin 帮助您快速完成一个构建 android 加固渠道包的 pipeline。

所有目前只有一条 pipeline， `加固APK => 验证,对齐,签名 => 生成渠道包`。  
且目前加固只支持360加固保，渠道包生成使用的是 [VasDolly]  

当然，未来可以有更多 android 相关的 pipeline 添加进来，欢迎 issue, pr


## Setup
在位于项目的根目录 `build.gradle` 文件中添加Gradle插件的依赖， 如下：
```groovy
buildscript{
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies{
        classpath 'com.github.1van:pipe-gradle-plugin:1.0.0'
    }
}
```
  
并在当前App的 `build.gradle` 文件中apply这个插件，并添加上相关配置。  

```gradle
apply plugin: 'pipe'
pipe {
    apkOutputFolder = "${rootDir.absolutePath}/out"
    channelFile = "${rootDir.absolutePath}/channel"
    toolsPath = localProperties.getProperty("tools.dir")
    username = "username"
    password = "password"
}
```
配置项具体解释：
* apkOutputFolder：指定渠道包的输出路径
* channelFile：包含渠道配置信息的文件路径（支持使用#号添加注释）
* toolsPath：360加固保下的`jiagu`文件夹路径（在[360加固]下载对应环境的加固组手，解压取出它的`jiagu`文件夹，）
* username：360加固保账号
* password：360加固保密码


## Usage
```bash
./gradlew clean pipeAssemble${productFlavor}{$buildType}
```

用法示例：
```bash
./gradlew clean pipeAssembleRelease
./gradlew clean pipeAssembleQaDebug
```

### 获取渠道信息（同[VasDolly]一致）
在主App工程的build.gradle中，添加读取渠道信息的helper类库依赖：
```groovy
dependencies{
    implementation 'com.leon.channel:helper:2.0.3'
}
```
使用方法：  
通过helper类库中的ChannelReaderUtil类读取渠道信息。
```java
String channel = ChannelReaderUtil.getChannel(getApplicationContext());
```
如果没有渠道信息，那么这里返回null，开发者需要自己判断。  

---
#### 注意事项：
1. 所有配置项都是字符串，且必需
1. 路径相关配置项推荐使用绝对路径
1. toolsPath推荐放在一个公共的地方，其他项目可共用，且推荐把路径写在local.properties中（读取properties的工具类[utils.gradle]）
1. toolsPath对应的就是360加固保下的`jiagu`文件夹，除了里面的jiagu.jar和加固保魔改的java jdk，其他文件都可删除

[VasDolly]:https://github.com/Tencent/VasDolly
[pipeline]:https://www.jenkins.io/doc/pipeline/tour/hello-world/
[360加固]:https://jiagu.360.cn/#/global/download
[utils.gradle]:https://gist.github.com/1van/b865d104f44c94ef3e809f62ecdf15a0
