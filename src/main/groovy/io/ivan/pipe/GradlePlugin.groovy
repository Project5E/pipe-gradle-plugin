package io.ivan.pipe

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class GradlePlugin implements Plugin<Project> {

    public static final String sPluginExtensionName = "pipe"

    @Override
    void apply(Project project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw new GradleException("[pipe]: plugin 'com.android.application' must be apply")
        }
        applyExtension(project)
        applyTask(project)
    }

    private void applyExtension(Project project) {
        project.extensions.create(sPluginExtensionName, Extension, project)
    }

    private void applyTask(Project project) {
        project.afterEvaluate {
            project.android.applicationVariants.all { ApplicationVariantImpl variant ->
                def variantName = variant.name.capitalize()

                Task task = project.tasks.create("pipeAssemble${variantName}", Task) as Task
                task.targetProject = project
                task.variant = variant
                task.setup()

                if (variant.hasProperty('assembleProvider')) {
                    task.dependsOn variant.assembleProvider.get()
                } else {
                    task.dependsOn variant.assemble
                }
            }
        }
    }

}
