package com.backbase.oss.codegen.java;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.cli.MavenCli;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.platform.commons.util.StringUtils;

@Slf4j
class MavenProjectCompiler {

    private static final File GH_ACTIONS_M2_SETTINGS_FILE = new File("/home/runner/.m2/settings.xml");
    private static final File GH_ACTIONS_M2_REPOSITORY_DIR = new File("/home/runner/.m2/repository");

    private final ClassLoader contextClassLoader;
    private final File mavenRepositoryDir;
    private final File mavenSettingsFile = GH_ACTIONS_M2_SETTINGS_FILE;

    public MavenProjectCompiler(ClassLoader contextClassLoader) {
        this.contextClassLoader = contextClassLoader;
        File userHome = new File(System.getProperty("user.home"));
        File userDefaultRepoLocation = new File(userHome, ".m2" + File.separatorChar + "repository");
        File mavenRepositoryInUse;
        if (GH_ACTIONS_M2_REPOSITORY_DIR.exists()) {
            mavenRepositoryInUse = GH_ACTIONS_M2_REPOSITORY_DIR;
        } else if (userDefaultRepoLocation.exists()) {
            mavenRepositoryInUse = userDefaultRepoLocation;
        } else {
            try {
                File tempRepo = Files.createTempDirectory(
                    BoatSpringTemplatesTests.class.getSimpleName() + "_mvn_repo").toFile();
                log.warn("m2 repo not found in paths: {}, {}. Using temp repo in {} which may slow down test execution",
                    GH_ACTIONS_M2_REPOSITORY_DIR, userDefaultRepoLocation, tempRepo);
                mavenRepositoryInUse = tempRepo;
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        this.mavenRepositoryDir = mavenRepositoryInUse;
    }

    public int compile(File projectDir) {
        log.debug("Compiling mvn project in: {}", projectDir);
        var mavenCli = new MavenCli(new ClassWorld("myRealm", contextClassLoader));
        final String initialDir = System.getProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY);
        try {
            System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, projectDir.getAbsolutePath());
            String[] args = generateMavenCliArgs();
            log.debug("mvn cli args: {}", Arrays.toString(args));
            int compilationStatus = mavenCli.doMain(args, projectDir.getAbsolutePath(), System.out, System.out);
            log.debug("compilation status={}", compilationStatus);
            return compilationStatus;
        } finally {
            if (StringUtils.isBlank(initialDir)) {
                System.clearProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY);
            } else {
                System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, initialDir);
            }
        }
    }

    public ClassLoader getProjectClassLoader(File projectDir) {
        try {
            var classesDir = new File(projectDir, "target/classes");
            return URLClassLoader.newInstance(
                new URL[]{classesDir.toURI().toURL()},
                contextClassLoader
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private String[] generateMavenCliArgs() {
        List<String> args = new ArrayList<>();
        if (mavenSettingsFile != null && mavenSettingsFile.exists()) {
            args.add("--settings");
            args.add(mavenSettingsFile.getAbsolutePath());
        }
        args.add("-Dmaven.repo.local=" + mavenRepositoryDir.getAbsolutePath());
        args.add("clean");
        args.add("compile");
        return args.stream().toArray(String[]::new);
    }
}
