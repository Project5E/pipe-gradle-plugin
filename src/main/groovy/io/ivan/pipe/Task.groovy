package io.ivan.pipe

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.builder.model.SigningConfig
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class Task extends DefaultTask {

    private static final String DOT_APK = ".apk"
    private static final String PROTECT_TYPE = "360"

    @Input ApplicationVariantImpl variant
    @Input Project targetProject

    @Internal String jiaguJava
    @Internal String jiaguFile
    @Internal String jiaguCommand

    @Internal String apksignerFile
    @Internal String apksignerCommand

    @Internal String vasDollyFile
    @Internal String vasDollyCommand

    @Internal String apkFilePath
    @Internal String baseApkFilePath
    @Internal String buildPath
    @Internal String tempApkPath
    @Internal String tempDirPath

    void setup() {
        def extension = Extension.getConfig(targetProject)

        FileUtils.deleteAllInDir(extension.apkOutputFolder)

        jiaguJava = "${extension.toolsPath}/java/bin/java"
        jiaguFile = "${extension.toolsPath}/jiagu.jar"
        jiaguCommand = "${jiaguJava} -jar ${jiaguFile}"

        apksignerFile = "${extension.toolsPath}/apksigner.jar"
        apksignerCommand = "java -jar ${apksignerFile}"

        vasDollyFile = "${extension.toolsPath}/VasDolly.jar"
        vasDollyCommand = "java -jar ${vasDollyFile}"
    }

    @TaskAction
    void action() {
        checkParameter()

        def iterator = variant.outputs.iterator()
        while (iterator.hasNext()) {
            def apkFile = iterator.next().outputFile
            if (apkFile == null || !apkFile.exists()) {
                throw new GradleException("[pipe]: ${apkFile} is not existed!")
            }

            initPath(apkFile)

            execLock()
            execReSign()
            execVasDolly()

            println "任务完成！"
        }
    }

    private void checkParameter() {
        def extension = Extension.getConfig(project)
        if (StringUtils.isEmpty(extension.apkOutputFolder)) {
            throw new GradleException("[pipe]: apkOutputFolder is not configured , please check it !")
        }
        if (StringUtils.isEmpty(extension.channelFile)) {
            throw new GradleException("[pipe]: channelFile is not configured , please check it !")
        }
        if (StringUtils.isEmpty(extension.toolsPath)) {
            throw new GradleException("[pipe]: toolsPath is not configured , please check it !")
        }
        if (getSigningConfig() == null) {
            throw new GradleException("[pipe]: SigningConfig is null , please check it !")
        }
    }

    @Internal
    SigningConfig getSigningConfig() {
        return variant.buildType.signingConfig == null ? variant.mergedFlavor.signingConfig : variant.buildType.signingConfig
    }

    private void initPath(File apkFile) {
        buildPath = apkFile.parent
        apkFilePath = apkFile.absolutePath
        baseApkFilePath = apkFilePath.replace(DOT_APK, "")
        createApkTempDir(buildPath)
    }

    private void createApkTempDir(String buildPath) {
        tempDirPath = "${buildPath}/temp"
        FileUtils.createOrExistsDir(tempDirPath)
    }

    private void execLock() {
        switch (PROTECT_TYPE) {
            case "360":
                exec360Jiagu()
                tempApkPath = get360Apk()
                break
        }
    }

    private void exec360Jiagu() {
        def extension = Extension.getConfig(targetProject)
        def signingConfig = getSigningConfig()

        def inputPath = apkFilePath
        def outputPath = tempDirPath
        [
                "chmod -R 755 ${extension.toolsPath}",
                "${jiaguCommand} -version",
                "${jiaguCommand} -update",
                "${jiaguCommand} -login ${extension.username} ${extension.password}",
                "${jiaguCommand} -importsign ${signingConfig.storeFile.absolutePath} ${signingConfig.storePassword} ${signingConfig.keyAlias} ${signingConfig.keyPassword}",
                "${jiaguCommand} -config -crashlog -x86 -analyse",
                "${jiaguCommand} -showconfig",
                "${jiaguCommand} -jiagu ${inputPath} ${outputPath} -autosign"
        ].each { exec(it) }
    }

    private void execReSign() {
        if (!FileUtils.isFileExists(apksignerFile)) {
            FileIOUtils.writeFileFromIS(apksignerFile, getClass().getResourceAsStream("/apksigner.jar"))
        }
        def signingConfig = getSigningConfig()
        def inputPath = tempApkPath
        def outputPath = "${baseApkFilePath}_resign${DOT_APK}"

        [
                "chmod 755 ${apksignerFile}",
                "${apksignerCommand} sign --ks ${signingConfig.storeFile.absolutePath} --ks-pass pass:${signingConfig.storePassword} " +
                        "--ks-key-alias ${signingConfig.keyAlias} --key-pass pass:${signingConfig.keyPassword} " +
                        "--out ${outputPath} ${inputPath}",

                "${apksignerCommand} verify -v --print-certs ${outputPath}"
        ].each { exec(it) }

        tempApkPath = outputPath
    }

    private void execVasDolly() {
        if (!FileUtils.isFileExists(vasDollyFile)) {
            FileIOUtils.writeFileFromIS(vasDollyFile, getClass().getResourceAsStream("/VasDolly.jar"))
        }
        def extension = Extension.getConfig(targetProject)
        def inputPath = tempApkPath

        println inputPath
        FileUtils.createOrExistsDir(extension.apkOutputFolder)

        [
                "chmod 755 ${vasDollyFile}",
                "${vasDollyCommand} put -c ${extension.channelFile} ${inputPath} ${extension.apkOutputFolder}"
        ].each { exec(it) }
    }

    /**
     * 获取360加固后的APK
     * @return
     */
    private String get360Apk() {
        return new File(tempDirPath).listFiles()[0].absolutePath
    }

    void exec(String str) {
        println "****************************************************************************************************"
        println str
        println "****************************************************************************************************"

        def process = str.execute()
//        process.waitFor()
        def text = process.err.text
        if (!StringUtils.isEmpty(text)) {
            println "发生了异常，任务已停止！"
            println text
            System.exit(0)
        } else {
            println process.text
        }
    }

}
