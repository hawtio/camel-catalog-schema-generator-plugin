package io.hawt.camelcatalog.maven.plugin;

import java.io.File;
import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * To generate camelModel.js from the Apache Camel release
 */
@Mojo(name = "generate-camel-model", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
            requiresDependencyResolution = ResolutionScope.COMPILE)
public class CamelModelGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}")
    protected File buildDir;

    @Parameter(defaultValue = "${schema-outdir}/camelModel.js")
    protected File schemaFile;

    /**
     * Execute goal.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Assembling Camel model schema");

        Artifact camelCatalog = findCamelCatalogArtifact(project);
        if (camelCatalog == null) {
            getLog().warn("Cannot find Apache Camel on the classpath");
            return;
        }
        getLog().info("Using Apache Camel " + camelCatalog.getVersion());
        if (camelCatalog.getFile() == null) {
            throw new MojoFailureException("Camel Catalog dependency has not yet been resolved");
        }
        getLog().info("Camel Catalog Artifact location: " + camelCatalog.getFile().getAbsolutePath());

        CamelModelGenerator generator = new CamelModelGenerator(getLog(), camelCatalog.getVersion(),
                camelCatalog.getFile(), schemaFile);
        generator.generate();
    }

    private static Artifact findCamelCatalogArtifact(MavenProject project) {
        @SuppressWarnings("deprecation")
        Iterator<?> it = project.getDependencyArtifacts().iterator();
        while (it.hasNext()) {
            Artifact artifact = (Artifact) it.next();
            if (artifact.getGroupId().equals("org.apache.camel") && artifact.getArtifactId().equals("camel-catalog")) {
                return artifact;
            }
        }
        return null;
    }

}
