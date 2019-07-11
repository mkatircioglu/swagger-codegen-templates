package io.github.mkatircioglu.swagger.codegen.templates;

import com.google.common.base.CharMatcher;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.swagger.codegen.*;
import io.swagger.codegen.languages.AbstractJavaCodegen;
import io.swagger.codegen.languages.features.BeanValidationFeatures;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;


public class SpringInterfaces extends AbstractJavaCodegen implements BeanValidationFeatures {
    public static final String DEFAULT_LIBRARY = "spring-boot";
    public static final String TITLE = "title";
    public static final String CONFIG_PACKAGE = "configPackage";
    public static final String BASE_PACKAGE = "basePackage";
    public static final String INTERFACE_ONLY = "interfaceOnly";
    public static final String DELEGATE_PATTERN = "useDelegatePattern";
    public static final String SINGLE_CONTENT_TYPES = "singleContentTypes";
    public static final String JAVA_8 = "java8";
    public static final String RESPONSE_WRAPPER = "responseWrapper";
    public static final String USE_TAGS = "useTags";
    public static final String SPRING_MVC_LIBRARY = "spring-mvc";
    public static final String SPRING_CLOUD_LIBRARY = "spring-cloud";
    public static final String IMPLICIT_HEADERS = "implicitHeaders";
    public static final String FEIGN_NAME = "feignName";
    public static final String FEIGN_URL = "feignUrl";

    private static final String IMPORTS = "imports";
    private static final String PAYLOAD_CLASS = "payloadClass";
    private static final String PAYLOAD_CLASS_COLLECTOR = "payloadClassCollector";
    private static final String PAYLOAD_CLASS_CASTING = "payloadClassCasting";

    protected String title = "swagger-petstore";
    protected String configPackage = "io.github.mkatircioglu.configuration";
    protected String basePackage = "io.github.mkatircioglu";
    protected String feignName = null;
    protected String feignUrl = null;
    protected boolean singleContentTypes = false;
    protected boolean java8 = true;
    protected String responseWrapper = "";
    protected boolean interfaceOnly = false;
    protected boolean useDelegatePattern = false;
    protected boolean useTags = false;
    protected boolean useBeanValidation = true;
    protected boolean implicitHeaders = false;

    public SpringInterfaces() {
        super();
        outputFolder = "generated-code/springInterfaces";
        apiTestTemplateFiles.clear(); // TODO: add test template
        embeddedTemplateDir = templateDir = "SpringInterfaces";
        apiPackage = "io.github.mkatircioglu.controller";
        modelPackage = "io.github.mkatircioglu.model";
        invokerPackage = "io.github.mkatircioglu.api";
        artifactId = "swagger-spring";

        additionalProperties.put(CONFIG_PACKAGE, configPackage);
        additionalProperties.put(BASE_PACKAGE, basePackage);

        // spring uses the jackson lib
        additionalProperties.put("jackson", "true");

        cliOptions.add(new CliOption(TITLE, "server title name or client service name"));
        cliOptions.add(new CliOption(CONFIG_PACKAGE, "configuration package for generated code"));
        cliOptions.add(new CliOption(BASE_PACKAGE, "base package for generated code"));
        cliOptions.add(CliOption.newBoolean(INTERFACE_ONLY, "Whether to generate only API interface stubs without the server files."));
        cliOptions.add(CliOption.newBoolean(DELEGATE_PATTERN, "Whether to generate the server files using the delegate pattern"));
        cliOptions.add(CliOption.newBoolean(SINGLE_CONTENT_TYPES, "Whether to select only one produces/consumes content-type by operation."));
        cliOptions.add(CliOption.newBoolean(JAVA_8, "use java8 default interface"));
        cliOptions.add(new CliOption(RESPONSE_WRAPPER, "wrap the responses in given type (Future,Callable,CompletableFuture,ListenableFuture,DeferredResult,HystrixCommand,RxObservable,RxSingle or fully qualified type)"));
        cliOptions.add(CliOption.newBoolean(USE_TAGS, "use tags for creating interface and controller classnames"));
        cliOptions.add(CliOption.newBoolean(USE_BEANVALIDATION, "Use BeanValidation API annotations"));
        cliOptions.add(CliOption.newBoolean(IMPLICIT_HEADERS, "Use of @ApiImplicitParams for headers."));
        cliOptions.add(CliOption.newBoolean(FEIGN_NAME, "If given, will generate clients instead of controllers and will be used as client class name for discovery if enabled"));
        cliOptions.add(CliOption.newBoolean(FEIGN_URL, "If given, will set client urls property to this"));

        supportedLibraries.put(DEFAULT_LIBRARY, "Spring-boot Server application.");
        supportedLibraries.put(SPRING_MVC_LIBRARY, "Spring-MVC Server application.");
        supportedLibraries.put(SPRING_CLOUD_LIBRARY, "Spring-Cloud-Feign client with Spring-Boot auto-configured settings.");
        setLibrary(DEFAULT_LIBRARY);

        CliOption library = new CliOption(CodegenConstants.LIBRARY, "library template (sub-template) to use");
        library.setDefault(DEFAULT_LIBRARY);
        library.setEnum(supportedLibraries);
        library.setDefault(DEFAULT_LIBRARY);
        cliOptions.add(library);
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public void postProcessParameter(CodegenParameter parameter) {
        if (hasEncryptedId(parameter)) {
            parameter.dataType = "EntityId";
            parameter.vendorExtensions.put("isEncryptedId", true);
        }
    }

    private boolean hasEncryptedId(CodegenParameter parameter) {
        Object encryptedId = parameter.vendorExtensions.get("x-encrypted-id");
        return (encryptedId != null && encryptedId.toString().equalsIgnoreCase("TRUE"));
    }

    @Override
    public String getName() {
        return "springInterfaces";
    }

    @Override
    public String getHelp() {
        return "Generates a Java SpringBoot Server application using the Spring Cloud Integration.";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        typeMapping.put("file", "Resource");
        importMapping.put("Resource", "org.springframework.core.io.Resource");
        importMapping.put("Sort", "org.springframework.data.domain.Sort");
        importMapping.put("Pageable", "org.springframework.data.domain.Pageable");
        importMapping.put("Page", "org.springframework.data.domain.Page");
        importMapping.put("PageImpl", "org.springframework.data.domain.PageImpl");
        importMapping.put("Collectors", "java.util.stream.Collectors");
        importMapping.put("BigDecimal", "java.math.BigDecimal");

        if (this.interfaceOnly && this.useDelegatePattern) {
            throw new IllegalArgumentException(String.format("Can not generate code with `%s` and `%s` both true.", DELEGATE_PATTERN, INTERFACE_ONLY));
        }

        processDocumentationOption();
        processAdditionalPropertiesOptions();
        processInterfaceOnlyOption();
        processDelegatePatternOption();
        processFeignClientOption();
        processJava8Option();
        processResposeWrapperOption();

        // add lambda for mustache templates
        additionalProperties.put("lambdaEscapeDoubleQuote", new Mustache.Lambda() {
            @Override
            public void execute(Template.Fragment fragment, Writer writer) throws IOException {
                writer.write(fragment.execute().replaceAll("\"", Matcher.quoteReplacement("\\\"")));
            }
        });
        additionalProperties.put("lambdaRemoveLineBreak", new Mustache.Lambda() {
            @Override
            public void execute(Template.Fragment fragment, Writer writer) throws IOException {
                writer.write(fragment.execute().replaceAll("\\r|\\n", ""));
            }
        });
    }

    private void processDocumentationOption() {
        // clear model and api doc template as this codegen
        // does not support auto-generated markdown doc at the moment
        //TODO: add doc templates
        modelDocTemplateFiles.remove("model_doc.mustache");
        apiDocTemplateFiles.remove("api_doc.mustache");
    }

    private void processInterfaceOnlyOption() {
        if (!this.interfaceOnly) {
            apiTemplateFiles.put("apiController.mustache", "Controller.java");
            apiTemplateFiles.remove("api.mustache");
        }
    }

    private void processAdditionalPropertiesOptions() {
        if (additionalProperties.containsKey(TITLE)) {
            this.setTitle((String) additionalProperties.get(TITLE));
        }

        if (additionalProperties.containsKey(CONFIG_PACKAGE)) {
            this.setConfigPackage((String) additionalProperties.get(CONFIG_PACKAGE));
        }

        if (additionalProperties.containsKey(BASE_PACKAGE)) {
            this.setBasePackage((String) additionalProperties.get(BASE_PACKAGE));
        }

        if (additionalProperties.containsKey(INTERFACE_ONLY)) {
            this.setInterfaceOnly(Boolean.valueOf(additionalProperties.get(INTERFACE_ONLY).toString()));
        }

        if (additionalProperties.containsKey(DELEGATE_PATTERN)) {
            this.setUseDelegatePattern(Boolean.valueOf(additionalProperties.get(DELEGATE_PATTERN).toString()));
        }

        if (additionalProperties.containsKey(FEIGN_NAME)) {
            this.setFeignName((String) additionalProperties.get(FEIGN_NAME));
        }

        if (additionalProperties.containsKey(FEIGN_URL)) {
            this.setFeignUrl((String) additionalProperties.get(FEIGN_URL));
        }

        if (additionalProperties.containsKey(SINGLE_CONTENT_TYPES)) {
            this.setSingleContentTypes(Boolean.valueOf(additionalProperties.get(SINGLE_CONTENT_TYPES).toString()));
        }

        if (additionalProperties.containsKey(JAVA_8)) {
            this.setJava8(Boolean.valueOf(additionalProperties.get(JAVA_8).toString()));
        }

        if (additionalProperties.containsKey(RESPONSE_WRAPPER)) {
            this.setResponseWrapper((String) additionalProperties.get(RESPONSE_WRAPPER));
        }

        if (additionalProperties.containsKey(USE_TAGS)) {
            this.setUseTags(Boolean.valueOf(additionalProperties.get(USE_TAGS).toString()));
        }

        if (additionalProperties.containsKey(USE_BEANVALIDATION)) {
            this.setUseBeanValidation(convertPropertyToBoolean(USE_BEANVALIDATION));
        }

        if (useBeanValidation) {
            writePropertyBack(USE_BEANVALIDATION, useBeanValidation);
        }


        if (additionalProperties.containsKey(IMPLICIT_HEADERS)) {
            this.setImplicitHeaders(Boolean.valueOf(additionalProperties.get(IMPLICIT_HEADERS).toString()));
        }
    }

    private void processDelegatePatternOption() {
        if (this.useDelegatePattern) {
            additionalProperties.put("useDelegatePattern", "true");
            apiTemplateFiles.put("apiDelegate.mustache", "Delegate.java");
        }
    }

    private void processFeignClientOption() {
        if(StringUtils.isNotEmpty(this.feignUrl) &&  StringUtils.isNotEmpty(this.feignName)){
            additionalProperties.put(FEIGN_URL, "${" + feignUrl + "}");
            apiTemplateFiles.put("apiClient.mustache", "Client.java");
            apiTemplateFiles.remove("apiController.mustache");
            apiTemplateFiles.remove("apiDelegate.mustache");
            apiTemplateFiles.remove("api.mustache");
        }else if(StringUtils.isEmpty(this.feignUrl) && StringUtils.isEmpty(this.feignName)){
            return;
        }else{
            throw new IllegalArgumentException(String.format("Can not generate Feign Client with both %s and %s is not empty", FEIGN_NAME, FEIGN_URL));
        }
    }

    private void processJava8Option() {
        if (this.java8) {
            additionalProperties.put("javaVersion", "1.8");
            additionalProperties.put("jdk8", "true");
            typeMapping.put("date", "LocalDate");
            typeMapping.put("DateTime", "OffsetDateTime");
            importMapping.put("LocalDate", "java.time.LocalDate");
            importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");
        }
    }

    private void processResposeWrapperOption() {
        // Some well-known Spring or Spring-Cloud response wrappers
        switch (this.responseWrapper) {
            case "Future":
            case "Callable":
            case "CompletableFuture":
                additionalProperties.put(RESPONSE_WRAPPER, "java.util.concurrent." + this.responseWrapper);
                break;
            case "ListenableFuture":
                additionalProperties.put(RESPONSE_WRAPPER, "org.springframework.util.concurrent.ListenableFuture");
                break;
            case "DeferredResult":
                additionalProperties.put(RESPONSE_WRAPPER, "org.springframework.web.context.request.DeferredResult");
                break;
            case "HystrixCommand":
                additionalProperties.put(RESPONSE_WRAPPER, "com.netflix.hystrix.HystrixCommand");
                break;
            case "RxObservable":
                additionalProperties.put(RESPONSE_WRAPPER, "rx.Observable");
                break;
            case "RxSingle":
                additionalProperties.put(RESPONSE_WRAPPER, "rx.Single");
                break;
            default:
                break;
        }
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation codegenOperation, Map<String, List<CodegenOperation>> codegenOperations) {
        if ((library.equals(DEFAULT_LIBRARY) || library.equals(SPRING_MVC_LIBRARY)) && !useTags) {
            String basePath = resourcePath;
            if (basePath.startsWith("/")) {
                basePath = basePath.substring(1);
            }
            int pos = basePath.indexOf("/");
            if (pos > 0) {
                basePath = basePath.substring(0, pos);
            }

            if (basePath.equals("")) {
                basePath = "default";
            } else {
                codegenOperation.subresourceOperation = !codegenOperation.path.isEmpty();
            }
            List<CodegenOperation> opList = codegenOperations.get(basePath);
            if (opList == null) {
                opList = new ArrayList<>();
                codegenOperations.put(basePath, opList);
            }
            opList.add(codegenOperation);
            codegenOperation.baseName = basePath;
        } else {
            super.addOperationToGroup(tag, resourcePath, operation, codegenOperation, codegenOperations);
        }
    }

    @Override
    public void preprocessSwagger(Swagger swagger) {
        super.preprocessSwagger(swagger);
        if ("/".equals(swagger.getBasePath())) {
            swagger.setBasePath("");
        }

        if (!additionalProperties.containsKey(TITLE)) {
            // From the title, compute a reasonable name for the package and the API
            String title = swagger.getInfo().getTitle();

            // Drop any API suffix
            if (title != null) {
                title = title.trim().replace(" ", "-");
                if (title.toUpperCase().endsWith("API")) {
                    title = title.substring(0, title.length() - 3);
                }

                this.title = camelize(sanitizeName(title), true);
            }
            additionalProperties.put(TITLE, this.title);
        }

        String host = swagger.getHost();
        String port = "8080";
        if (host != null) {
            String[] parts = host.split(":");
            if (parts.length > 1) {
                port = parts[1];
            }
        }

        this.additionalProperties.put("serverPort", port);
        if (swagger.getPaths() != null) {
            for (String pathname : swagger.getPaths().keySet()) {
                Path path = swagger.getPath(pathname);
                if (path.getOperations() != null) {
                    for (Operation operation : path.getOperations()) {
                        if (operation.getTags() != null) {
                            List<Map<String, String>> tags = new ArrayList<>();
                            for (String tag : operation.getTags()) {
                                Map<String, String> value = new HashMap<>();
                                value.put("tag", tag);
                                value.put("hasMore", "true");
                                tags.add(value);
                            }
                            if (tags.isEmpty()) {
                                tags.get(tags.size() - 1).remove("hasMore");
                            }
                            if (operation.getTags().isEmpty()) {
                                String tag = operation.getTags().get(0);
                                operation.setTags(Arrays.asList(tag));
                            }
                            operation.setVendorExtension("x-tags", tags);
                        }
                    }
                }
            }
        }
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, Map<String, Model> definitions, Swagger swagger) {
        CodegenOperation codegenOperation = super.fromOperation(path, httpMethod, operation, definitions, swagger);
        codegenOperation.vendorExtensions.put("pathForDelegate", CharMatcher.inRange('{', '}').removeFrom(StringUtils.replacePattern(path, "(?<=\\{)(.*?)(?=\\})", "1")));
        return codegenOperation;
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objects) {
        Set<String> vendorImports = new TreeSet<>();
        Map<String, Object> operations = (Map<String, Object>) objects.get("operations");
        List<Map<String, String>> imports = (List<Map<String, String>>) objects.get(IMPORTS);
        if (operations != null) {
            List<CodegenOperation> codegenOperations = (List<CodegenOperation>) operations.get("operation");
            for (CodegenOperation operation : codegenOperations) {
                List<CodegenResponse> responses = operation.responses;
                if (responses != null) {
                    for (CodegenResponse resp : responses) {
                        if ("0".equals(resp.code)) {
                            resp.code = "200";
                        }
                    }
                }

                if (operation.returnType == null) {
                    if (operation.isRestfulCreate()) {
                        operation.returnType = "Void";
                        operation.vendorExtensions.put("isReturnRequired", true);
                    } else {
                        operation.returnType = "void";
                    }
                } else {
                    operation.vendorExtensions.put("isReturnRequired", true);
                    if (operation.returnType.startsWith("List")) {
                        String returnType = operation.returnType;
                        int end = returnType.lastIndexOf('>');
                        if (end > 0) {
                            operation.returnType = returnType.substring("List<".length(), end).trim();
                            operation.returnContainer = "List";
                        }
                    } else if (operation.returnType.startsWith("Map")) {
                        String returnType = operation.returnType;
                        int end = returnType.lastIndexOf('>');
                        if (end > 0) {
                            operation.returnType = returnType.substring("Map<".length(), end).split(",")[1].trim();
                            operation.returnContainer = "Map";
                        }
                    } else if (operation.returnType.startsWith("Set")) {
                        String returnType = operation.returnType;
                        int end = returnType.lastIndexOf('>');
                        if (end > 0) {
                            operation.returnType = returnType.substring("Set<".length(), end).trim();
                            operation.returnContainer = "Set";
                        }
                    }
                }

                if (implicitHeaders) {
                    processHeaderParameters(operation);
                }
                processIgnoredParameters(operation);
                processParameterValidation(operation, vendorImports);
                processParameterEncryption(operation, vendorImports);
                processParameterClassReference(operation, vendorImports);
                processOperationMethodSecurity(operation, vendorImports);
                processOperationPayloadClass(operation, vendorImports);
            }
        }
        for (String nextImport : vendorImports) {
            Map<String, String> vendorImport = new LinkedHashMap<>();
            vendorImport.put("import", nextImport);
            imports.add(vendorImport);
        }
        return objects;
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        if (library.equals(SPRING_CLOUD_LIBRARY)) {
            List<CodegenSecurity> authMethods = (List<CodegenSecurity>) objs.get("authMethods");
            if (authMethods != null) {
                for (CodegenSecurity authMethod : authMethods) {
                    authMethod.name = camelize(sanitizeName(authMethod.name), true);
                }
            }
        }
        return objs;
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);

        if ("null".equals(property.example)) {
            property.example = null;
        }

        //Add imports for Jackson
        if (!Boolean.TRUE.equals(model.isEnum)) {
            model.imports.add("JsonProperty");

            if (Boolean.TRUE.equals(model.hasEnums)) {
                model.imports.add("JsonValue");
            }
        } else { // enum class
            //Needed imports for Jackson's JsonCreator
            if (additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonCreator");
            }
        }
        model.imports.remove("ApiModelProperty");
        model.imports.remove("ApiModel");
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objects) {
        objects = super.postProcessModels(objects);

        List<Map<String, String>> imports = (List<Map<String, String>>) objects.get(IMPORTS);
        List<Object> models = (List<Object>) objects.get("models");
        for (Object model : models) {
            Map<String, Object> modelMap = (Map<String, Object>) model;
            CodegenModel codegenModel = (CodegenModel) modelMap.get("model");
            if (Boolean.TRUE.equals(codegenModel.isEnum) ) {
            }
        }
        return objects;
    }

    @Override
    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        objs = super.postProcessModelsEnum(objs);

        //Add imports for Jackson
        List<Map<String, String>> imports = (List<Map<String, String>>) objs.get(IMPORTS);
        List<Object> models = (List<Object>) objs.get("models");
        for (Object model : models) {
            Map<String, Object> modelMap = (Map<String, Object>) model;
            CodegenModel codegenModel = (CodegenModel) modelMap.get("model");
            // for enum model
            if (Boolean.TRUE.equals(codegenModel.isEnum) && codegenModel.allowableValues != null) {
                codegenModel.imports.add(importMapping.get("JsonValue"));
                Map<String, String> item = new HashMap<>();
                item.put("import", importMapping.get("JsonValue"));
                imports.add(item);
            }
        }
        return objs;
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultApi";
        }
        name = sanitizeName(name);
        return camelize(name) + "Api";
    }

    @Override
    public void setParameterExampleValue(CodegenParameter parameter) {
        String type = parameter.baseType;
        if (type == null) {
            type = parameter.dataType;
        }

        if ("File".equals(type)) {
            String example;

            if (parameter.defaultValue == null) {
                example = parameter.example;
            } else {
                example = parameter.defaultValue;
            }

            if (example == null) {
                example = "/path/to/file";
            }
            example = "new org.springframework.core.io.FileSystemResource(new java.io.File(\"" + escapeText(example) + "\"))";
            parameter.example = example;
        } else {
            super.setParameterExampleValue(parameter);
        }
    }

    private void processOperationPayloadClass(CodegenOperation operation, Set<String> imports) {
        if (!operation.returnSimpleType || !operation.returnTypeIsPrimitive) {
            Object payloadClass = operation.vendorExtensions.get("x-payload-class");
            imports.add(this.importMapping().get("Collectors"));
            if (payloadClass == null) {
                operation.vendorExtensions.put(PAYLOAD_CLASS, "List");
                operation.vendorExtensions.put(PAYLOAD_CLASS_COLLECTOR, "Collectors.toList()");
                operation.vendorExtensions.put(PAYLOAD_CLASS_CASTING, "(List)");
            } else {
                final String clazz = payloadClass.toString();
                if ("Set".equals(clazz)) {
                    operation.vendorExtensions.put(PAYLOAD_CLASS, "Set");
                    operation.vendorExtensions.put(PAYLOAD_CLASS_COLLECTOR, "Collectors.toSet()");
                    operation.vendorExtensions.put(PAYLOAD_CLASS_CASTING, "(Set)");
                } else if ("Page".equals(clazz)) {
                    imports.add(this.importMapping().get("Pageable"));
                    imports.add(this.importMapping().get("Sort"));
                    imports.add(this.importMapping().get("Page"));
                    imports.add(this.importMapping().get("PageImpl"));
                    operation.vendorExtensions.put("usePageable", true);
                    operation.vendorExtensions.put(PAYLOAD_CLASS, "Page");
                    operation.vendorExtensions.put(PAYLOAD_CLASS_COLLECTOR, "Collectors.toList())");
                    operation.vendorExtensions.put(PAYLOAD_CLASS_CASTING, "new PageImpl((List)");
                    operation.vendorExtensions.put("payloadClassClient", "PageImpl");
                } else {
                    operation.vendorExtensions.put(PAYLOAD_CLASS_COLLECTOR, "Collectors.toMap()");
                    operation.vendorExtensions.put(PAYLOAD_CLASS_CASTING, "(Map)");
                }
            }
        }
    }

    private void processOperationMethodSecurity(CodegenOperation operation, Set<String> imports) {
        if (operation.vendorExtensions.containsKey("x-method-security")) {
            Map<String, String> methodSecurity = (Map<String, String>) operation.vendorExtensions.get("x-method-security");
            operation.vendorExtensions.put("methodSecurity", String.format("@%s(\"%s\")",methodSecurity.get("annotation"), methodSecurity.get("expression")));
            String mapping = this.importMapping().get(operation.vendorExtensions.containsKey("x-method-security"));
            if (mapping != null) {
                imports.add(mapping);
            }
        }
    }

    /**
     * This method processes parameters' validation.
     *
     * @param operation list of all parameters
     */
    private void processParameterClassReference(CodegenOperation operation, Set<String> imports) {
        for (CodegenParameter parameter : operation.allParams) {
            if (parameter.vendorExtensions.containsKey("x-change-reference")) {
                final String changeReference = parameter.vendorExtensions.get("x-change-reference").toString();
                if (changeReference != null) {
                    parameter.datatypeWithEnum = changeReference;
                    parameter.dataType = changeReference;
                    parameter.baseType = changeReference;
                    String mapping = this.importMapping().get(changeReference);
                    if (mapping != null) {
                        imports.add(mapping);
                    }
                }
            }
        }
    }

    //FIXME: This should be replaced with x-change-reference vendor definition.
    private void processParameterEncryption(CodegenOperation operation, Set<String> imports) {
        for (CodegenParameter parameter : operation.allParams) {
            if (parameter.vendorExtensions.containsKey("x-encrypted-id")) {
                String mapping = this.importMapping().get("EncryptedId");
                if (mapping != null) {
                    imports.add(mapping);
                }
            }
        }
    }

    /**
     * This method processes parameters' validation.
     *
     * @param operation list of all parameters
     */
    private void processParameterValidation(CodegenOperation operation, Set<String> imports) {
        for (CodegenParameter parameter : operation.allParams) {
            if (parameter.vendorExtensions.containsKey("x-validation-class")) {
                final String validationClasses = (String) parameter.vendorExtensions.get("x-validation-class");
                for (String validationClass : Arrays.asList(validationClasses.replace(" ", "").split(","))) {
                    String mapping = this.importMapping().get(validationClass);
                    if (mapping != null) {
                        imports.add(mapping);
                    }
                    parameter.vendorExtensions.put("validationClass", overrideValidationClass(validationClasses, parameter.baseName));
                }
            }
        }
    }

    private static String overrideValidationClass(String validationClassStr, String baseName) {
        String[] validationClasses = validationClassStr.split(",");
        StringBuilder validations = new StringBuilder();
        for (int index = 0; index < validationClasses.length; index++) {
            validations.append("@");
            validations.append(validationClasses[index].trim());
            validations.append("(message = \"invalid data provided for field: ");
            validations.append(StringUtils.capitalize(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(baseName), StringUtils.SPACE)));
            validations.append("\")\n  ");
        }
        String convertedValidation = validations.toString();
        return convertedValidation.substring(0, convertedValidation.length() - 3);
    }


    /**
     * This method processes ignored parameters from the list of parameters and
     * also corrects last allParams hasMore state.
     *
     * @param operation list of all parameters
     */
    private void processIgnoredParameters(CodegenOperation operation) {
        List<CodegenParameter> paramsToRemove = new ArrayList<>();
        for (CodegenParameter parameter : operation.allParams) {
            if (parameter.vendorExtensions.containsKey("x-ignore-param")) {
                paramsToRemove.add(parameter);
            }
        }
        operation.allParams.removeAll(paramsToRemove);
        if (!paramsToRemove.isEmpty()) {
            operation.allParams.get(operation.allParams.size() - 1).hasMore = false;
        }
    }

    /**
     * This method removes header parameters from the list of parameters and
     * also corrects last allParams hasMore state.
     *
     * @param operation list of all parameters
     */
    private void processHeaderParameters(CodegenOperation operation) {
        if (operation.allParams.isEmpty()) {
            return;
        }
        final ArrayList<CodegenParameter> copy = new ArrayList<>(operation.allParams);
        operation.allParams.clear();

        for (CodegenParameter parameter : copy) {
            if (!parameter.isHeaderParam) {
                operation.allParams.add(parameter);
            }
        }
        operation.allParams.get(operation.allParams.size() - 1).hasMore = false;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setConfigPackage(String configPackage) {
        this.configPackage = configPackage;
    }

    public void setBasePackage(String configPackage) {
        this.basePackage = configPackage;
    }

    public void setInterfaceOnly(boolean interfaceOnly) {
        this.interfaceOnly = interfaceOnly;
    }

    public void setUseDelegatePattern(boolean useDelegatePattern) {
        this.useDelegatePattern = useDelegatePattern;
    }

    public void setFeignName(String feignName) {
        this.feignName = feignName;
    }

    public void setFeignUrl(String feignUrl) {
        this.feignUrl = feignUrl;
    }

    public void setSingleContentTypes(boolean singleContentTypes) {
        this.singleContentTypes = singleContentTypes;
    }

    public void setJava8(boolean java8) {
        this.java8 = java8;
    }

    public void setResponseWrapper(String responseWrapper) {
        this.responseWrapper = responseWrapper;
    }

    public void setUseTags(boolean useTags) {
        this.useTags = useTags;
    }

    public void setImplicitHeaders(boolean implicitHeaders) {
        this.implicitHeaders = implicitHeaders;
    }

    public void setUseBeanValidation(boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
    }

}
