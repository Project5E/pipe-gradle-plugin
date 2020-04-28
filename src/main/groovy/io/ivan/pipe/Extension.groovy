package io.ivan.pipe

import org.gradle.api.Project


class Extension {

    /**
     * 指定渠道包的输出路径
     */
    String apkOutputFolder

    /**
     * 渠道配置文件
     */
    String channelFile

    /**
     * 360加固工具路径
     */
    String toolsPath

    /**
     * 360授权
     */
    String username
    String password

    Extension(Project project) {
    }

    static Extension getConfig(Project project) {
        Extension config = project.getExtensions().findByType(Extension.class)
        if (config == null) {
            config = new Extension()
        }
        return config
    }

}
