load("//tools/base/bazel:bazel.bzl", "fileset")
load("//tools/base/bazel:kotlin.bzl", "kotlin_library", "kotlin_test")
load("//tools/base/bazel:maven.bzl", "maven_java_library", "maven_pom")

fileset(
    name = "resources",
    srcs = glob(["src/main/resources/**"]) + [
        "//tools/external/desugar:desugar_deploy.jar",
        "//tools/external/desugar/java/com/google/devtools/build/android/desugar/runtime:libthrowable_extension.jar",
    ],
    mappings = {
        "//tools/buildSrc/base:": "com/android/builder/",
        "//tools/external/desugar:": "",
        "//tools/external/desugar/java/com/google/devtools/build/android/desugar/runtime:": "",
        "//prebuilts/tools/common/aapt/jni:": "",
        "src/main/resources/": "",
    },
)

kotlin_library(
    name = "builder",
    srcs = ["src/main/java"],
    bundled_deps = [
        "//tools/base/build-system/profile",
        "//tools/dx/dalvik/dx",
        "//prebuilts/r8:d8-master",
    ],
    javacopts = ["-Xep:MissingCasesInEnumSwitch:WARN"],
    pom = ":pom",
    resource_strip_prefix = "tools/base/build-system/builder",
    resources = [":resources"],
    visibility = ["//visibility:public"],
    deps = [
        "//tools/analytics-library/protos/src/main/proto",
        "//tools/analytics-library/shared:tools.analytics-shared",
        "//tools/analytics-library/tracker:tools.analytics-tracker",
        "//tools/apkzlib",
        "//tools/base/annotations",
        "//tools/base/build-system:tools.apksig",
        "//tools/base/build-system:tools.manifest-merger",
        "//tools/base/build-system/builder-model",
        "//tools/base/build-system/builder-test-api:tools.builder-test-api",
        "//tools/base/common:tools.common",
        "//tools/base/ddmlib:tools.ddmlib",
        "//tools/base/layoutlib-api:tools.layoutlib-api",
        "//tools/base/repository:tools.repository",
        "//tools/base/sdk-common:tools.sdk-common",
        "//tools/base/sdklib:tools.sdklib",
        "//tools/base/third_party:com.google.code.findbugs_jsr305",
        "//tools/base/third_party:com.google.code.gson_gson",
        "//tools/base/third_party:com.google.guava_guava",
        "//tools/base/third_party:com.google.protobuf_protobuf-java",
        "//tools/base/third_party:com.googlecode.json-simple_json-simple",
        "//tools/base/third_party:com.squareup_javawriter",
        "//tools/base/third_party:commons-codec_commons-codec",
        "//tools/base/third_party:it.unimi.dsi_fastutil",
        "//tools/base/third_party:net.sf.jopt-simple_jopt-simple",
        "//tools/base/third_party:org.bouncycastle_bcpkix-jdk15on",
        "//tools/base/third_party:org.bouncycastle_bcprov-jdk15on",
        "//tools/base/third_party:org.jetbrains.kotlin_kotlin-stdlib-jre8",
        "//tools/base/third_party:org.ow2.asm_asm",
        "//tools/base/third_party:org.ow2.asm_asm-commons",
        "//tools/base/third_party:org.ow2.asm_asm-tree",
        "//tools/base/third_party:org.ow2.asm_asm-util",
    ],
)

maven_pom(
    name = "pom",
    artifact = "builder",
    group = "com.android.tools.build",
    source = "//tools/buildSrc/base:build_version",
)

kotlin_test(
    name = "tests",
    srcs = ["src/test/java"],
    data = [
        "//prebuilts/studio/sdk:build-tools/latest",
        "//prebuilts/studio/sdk:platforms/latest",
        "//tools/base/build-system/aapt2:aapt2_for_tests",
    ],
    jvm_flags = ["-Dtest.suite.jar=tests.jar"],
    resources = glob(["src/test/resources/**"]),
    tags = [
        "no_test_windows",  # b/77288863
        "slow",
    ],
    test_class = "com.android.testutils.JarTestSuite",
    runtime_deps = [
        # Need to put this on the classpath before TestRunner_deploy.jar which contains
        # old JUnit classes. See https://github.com/bazelbuild/bazel/issues/2146.
        "//tools/base/third_party:junit_junit",
    ],
    deps = [
        ":builder",
        "//tools/apkzlib",
        "//tools/base/annotations",
        "//tools/base/build-system:tools.manifest-merger",
        "//tools/base/build-system/builder-model",
        "//tools/base/build-system/builder-test-api:tools.builder-test-api",
        "//tools/base/common:tools.common",
        "//tools/base/ddmlib:tools.ddmlib",
        "//tools/base/layoutlib-api:tools.layoutlib-api",
        "//tools/base/repository:tools.repository",
        "//tools/base/sdk-common:tools.sdk-common",
        "//tools/base/sdklib:tools.sdklib",
        "//tools/base/testutils:tools.testutils",
        "//tools/base/third_party:com.google.jimfs_jimfs",
        "//tools/base/third_party:com.google.truth_truth",
        "//tools/base/third_party:junit_junit",
        "//tools/base/third_party:nl.jqno.equalsverifier_equalsverifier",
        "//tools/base/third_party:org.bouncycastle_bcpkix-jdk15on",
        "//tools/base/third_party:org.jetbrains.kotlin_kotlin-stdlib-jre8",
        "//tools/base/third_party:org.jetbrains.kotlin_kotlin-test",
        "//tools/base/third_party:org.mockito_mockito-core",
        "//tools/base/third_party:org.ow2.asm_asm",
    ],
)
