package io.github.mkatircioglu.swagger.codegen.templates;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.swagger.codegen.ClientOptInput;
import io.swagger.codegen.ClientOpts;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.DefaultGenerator;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

public class SpringInterfacesGeneratorTest {

    private TemporaryFolder folder = new TemporaryFolder(generateOutputDir());

    @Before
    public void setUp() throws Exception {
        folder.create();
    }

    @After
    public void tearDown() throws Exception {
//        folder.delete();
    }

    private static File generateOutputDir() {
        File userDir = new File(System.getProperty("user.dir"));
        File outputDirectory = new File(userDir, "/target/generated-sources");
        if (!outputDirectory.delete()) {
            System.out.println("NOT_DELETED " + outputDirectory.getAbsolutePath());
        }
        if (!outputDirectory.mkdirs()) {
            System.out.println("NOT_CREATED at " + outputDirectory.getAbsolutePath());
        }

        System.out.println("OUTPUT TO : " + outputDirectory.getAbsolutePath());
        return outputDirectory;
    }

    @Test
    public void shouldGenerateApi() {
        final File output = folder.getRoot();

        final Swagger swagger = new SwaggerParser().read("src/test/resources/swagger.yaml");
        System.setProperty(CodegenConstants.APIS, "");

        SpringInterfaces codegenConfig = new SpringInterfaces();
        codegenConfig.setOutputDir(output.getAbsolutePath());

        ClientOptInput clientOptInput = new ClientOptInput().opts(new ClientOpts()).swagger(swagger).config(codegenConfig);
        new DefaultGenerator().opts(clientOptInput).generate();
    }

    @Test
    public void shouldGenerateModel() {
        final File output = folder.getRoot();

        final Swagger swagger = new SwaggerParser().read("src/test/resources/swagger.yaml");
        System.setProperty(CodegenConstants.MODELS, "");
        SpringInterfaces codegenConfig = new SpringInterfaces();
        codegenConfig.setOutputDir(output.getAbsolutePath());

        ClientOptInput clientOptInput = new ClientOptInput().opts(new ClientOpts()).swagger(swagger).config(codegenConfig);
        new DefaultGenerator().opts(clientOptInput).generate();
    }

    @Test
    public void shouldGenerateClient() {
        final File output = folder.getRoot();

        final Swagger swagger = new SwaggerParser().read("src/test/resources/swagger.yaml");
        System.setProperty(CodegenConstants.APIS, "");
        SpringInterfaces codegenConfig = new SpringInterfaces();
        codegenConfig.additionalProperties().put("feignName", "petstore");
        codegenConfig.additionalProperties().put("feignUrl", "petstore_url");
        codegenConfig.setFeignUrl("petstore_url");
        codegenConfig.setFeignName("petstore");
        codegenConfig.setOutputDir(output.getAbsolutePath());

        ClientOptInput clientOptInput = new ClientOptInput().opts(new ClientOpts()).swagger(swagger).config(codegenConfig);
        new DefaultGenerator().opts(clientOptInput).generate();
    }
}
