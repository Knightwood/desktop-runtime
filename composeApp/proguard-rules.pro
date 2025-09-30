# 基本配置
# 代码混淆压缩比，在0~7之间，默认为5，一般不做修改
-optimizationpasses 5

# 混合时不使用大小写混合，混合后的类名为小写
-dontusemixedcaseclassnames

# 混淆时不记录日志
#-verbose

# 指定不去忽略非公共库的类
#-dontskipnonpubliclibraryclasses

# 指定不去忽略非公共库的类成员
#-dontskipnonpubliclibraryclassmembers

# 不做预校验，preverify是proguard的四个步骤之一，Android不需要preverify，去掉这一步能够加快混淆速度。
#-dontpreverify

# 忽略警告
#-ignorewarning

# 优化不优化输入的类文件
#-dontoptimize

# 抛出异常时保留代码行号 可以提升我们的 StackSource 查找效率
-keepattributes SourceFile,LineNumberTable

# 生成混淆映射文件的位置和文件名
-printmapping mapping.txt


#避免混淆注解类
#-dontwarn android.annotation
-keepattributes *Annotation*

# 避免混淆泛型
-keepattributes Signature

#避免混淆内部类
-keepattributes InnerClasses

# 保留本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留枚举类不被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

#-allowaccessmodification # for some reason, app crashed
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses
# ====================================================================================

# ##################################### androidX #####################################
#material3
-dontnote androidx.**
#-keep class androidx.compose.material3.Shapes { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.collection.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.compose.ui.text.platform.ReflectionUtil { *; }
# We're excluding Material 2 from the project as we're using Material 3
-dontwarn androidx.compose.material.**
# for desktop TextField
-keepclasseswithmembernames class androidx.compose.foundation.text.** { *; }
-keepnames class androidx.datastore.preferences.** { *; }
-keep class ui.navigation.* { *; }

# slf4j
# 日志输出已经保留了slf4j，还需要保留一下slf4j-kotlin-api部分
-keep class com.github.knightwood.slf4j.kotlin.** { *; }
-keep class org.slf4j.** { *; }
# Logback
-keep class ch.qos.logback.** { *; }
-dontwarn ch.qos.logback.**


# Kotlin & java
-dontwarn jakarta.**
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
public static void check*(...);
public static void throw*(...);
}
-assumenosideeffects public class kotlin.coroutines.jvm.internal.DebugMetadataKt {
private static ** getDebugMetadataAnnotation(...);
}
-assumenosideeffects class java.util.Objects {
public static ** requireNonNull(...);
}


# =================== kotlin序列化 ===================

## Referenced by kotlinx.datetime
#-dontwarn kotlinx.serialization.KSerializer
#-dontwarn kotlinx.serialization.Serializable
## ai给出的建议
## 保留所有带有 @Serializable 注解的类及其构造函数
#-keepclassmembers class * {
#    @kotlinx.serialization.Serializable <init>(...);
#}
#
## 保留 kotlinx.serialization 包中的所有类和成员
#-keep class kotlinx.serialization.** { *; }
#
## 保留所有带有 @SerialName 注解的方法和字段
#-keepclassmembers class * {
#    @kotlinx.serialization.SerialName <fields>;
#    @kotlinx.serialization.SerialName <methods>;
#}


# https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro
# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Don't print notes about potential mistakes or omissions in the configuration for kotlinx-serialization classes
# See also https://github.com/Kotlin/kotlinx.serialization/issues/1900
-dontnote kotlinx.serialization.**

# Serialization core uses `java.lang.ClassValue` for caching inside these specified classes.
# If there is no `java.lang.ClassValue` (for example, in Android), then R8/ProGuard will print a warning.
# However, since in this case they will not be used, we can disable these warnings
-dontwarn kotlinx.serialization.internal.ClassValueReferences

# disable optimisation for descriptor field because in some versions of ProGuard, optimization generates incorrect bytecode that causes a verification error
# see https://github.com/Kotlin/kotlinx.serialization/issues/2719
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}
# =================== kotlin序列化 ===================


# =================== 导航、导航中使用序列化 ===================
# 或许是导航所需
# NavArgsLazy creates NavArgs instances using reflection
-if public class ** implements androidx.navigation.NavArgs
-keepclassmembers public class <1> {
    ** fromBundle(android.os.Bundle);
}
# Retain the @Navigator.Name annotation on each subclass of Navigator.
# R8 full mode only retains annotations on items matched by a -keep rule,
# hence the extra -keep rule for the subclasses of Navigator.
#
# A -keep rule for the Navigator.Name annotation class is not required
# since the annotation is referenced from the code.
-keepattributes RuntimeVisibleAnnotations
-keep,allowobfuscation,allowshrinking class * extends androidx.navigation.Navigator

# https://github.com/Kotlin/kotlinx.serialization/issues/1105
-keepattributes Annotation, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.SerializationKt

# Keep Serializers
# 我不知道导航使用序列化为什么报错，其他代码就没问题，但是把导航用到的可序列化类放到这个路径下就没问题了
# 可序列化类注意包名org.lodestone.app.entity，如果有其他包下的，或许也要像这样添加规则
# compose 1.8.1之前需要添加如下规则
#-keep,includedescriptorclasses class org.lodestone.app.entity.**$$serializer { *; }
#-keepclassmembers class org.lodestone.app.entity.** {
#    *** Companion;
#}
#-keepclasseswithmembers class org.lodestone.app.entity.** {
#    kotlinx.serialization.KSerializer serializer(...);
#}

# When kotlinx.serialization.json.JsonObjectSerializer occurs
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# =================== 在导航中使用序列化 ===================


# =================== 协程 ===================
# Kotlinx coroutines rules seems to be outdated with the latest version of Kotlin and Proguard
#-keep class kotlinx.coroutines.** { *; }
#-keep class kotlin.coroutines.** { *; }

# ServiceLoader support
#-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
#-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Most of volatile fields are updated with AFU and should not be mangled
#-keepclassmembers class kotlinx.coroutines.** {
#    volatile <fields>;
#}

# Same story for the standard library's SafeContinuation that also uses AtomicReferenceFieldUpdater
#-keepclassmembers class kotlin.coroutines.SafeContinuation {
#    volatile <fields>;
#}

# These classes are only required by kotlinx.coroutines.debug.internal.AgentPremain, which is only loaded when
# kotlinx-coroutines-core is used as a Java agent, so these are not needed in contexts where ProGuard is used.
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.Signal

# Only used in `kotlinx.coroutines.internal.ExceptionsConstructor`.
# The case when it is not available is hidden in a `try`-`catch`, as well as a check for Android.
-dontwarn java.lang.ClassValue

# An annotation used for build tooling, won't be directly accessed.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
# =================== 协程===================


# 协程和swing问题
# https://github.com/Kotlin/kotlinx.coroutines/issues/4025
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory { *; }
#-dontobfuscate
-keepnames class kotlinx.coroutines.swing.**

# fluent ui
-dontwarn com.konyaco.fluent.component.RememberResourcePainterKt


# 保护 SQLite 相关类 -room所需
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keepnames class androidx.sqlite.** { *; }
-keepnames class org.sqlite.** { *; }
-dontwarn androidx.room.paging.**
-dontwarn androidx.lifecycle.LiveData

# OKIO
-dontwarn okio.**
-keepnames class okio.** { *; }

# COIL
-keep class coil3.* { *; }

# 托盘
-dontwarn dorkbox.executor.**

# flatlaf不可以被混淆，混淆会造成显示异常
-dontwarn com.formdev.flatlaf.**
-keep class com.formdev.flatlaf.** { *; }

#jvisa
#-keepnames class xyz.froud.**{*;}

# JserialComm
-keepnames class com.fazecast.jSerialComm.** { *; }
-dontwarn com.fazecast.jSerialComm.**

# jna
-dontwarn com.sun.jna.internal.**
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-dontwarn java.awt.*
-keepclassmembers class * extends com.sun.jna.* { public *; }



# ##################################### kandy绘图库需要保留的内容##############################
# lets-plot
# 保持 lets-plot 包下的所有类和成员
-keep class org.jetbrains.letsPlot.** { *; }

# 保持 lets-plot 包下的所有类名和成员名
-keepnames class org.jetbrains.letsPlot.** { *; }

# 保持所有带有注解的类和成员
-keep @interface org.jetbrains.letsPlot.**

# 保持所有通过反射访问的类和成员
-keepclassmembers class * {
    @org.jetbrains.letsPlot.* <methods>;
}

# 忽略 lets-plot 包下的所有警告
-dontwarn org.jetbrains.letsPlot.**


# ##################################### easyexcel #####################################
-dontwarn com.alibaba.excel.**

# ##################################### poi #####################################
# poi-ooxml 1.7M
-keep class org.apache.poi.ooxml.** { *; }
-keep class org.apache.poi.openxml4j.** { *; }
-keep class org.apache.poi.crypt.** { *; }
-keep class org.apache.poi.usermodel.** { *; }
-keep class org.apache.poi.xdgf.** { *; }
-keep class org.apache.poi.xslf.** { *; }
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.xwpf.** { *; }
-keep class org.apache.poi.sl.usermodel.MetroShapeProvider
-keep class * implements org.apache.poi.sl.usermodel.MetroShapeProvider {
    <init>(...);
    public *;
}
-keep class org.apache.poi.sl.usermodel.SlideShowProvider
-keep class * implements org.apache.poi.sl.usermodel.SlideShowProvider {
    <init>(...);
    public *;
}
-keep class org.apache.poi.ss.usermodel.WorkbookProvider
-keep class * implements org.apache.poi.ss.usermodel.WorkbookProvider {
    <init>(...);
    public *;
}
-keep class org.apache.poi.extractor.ExtractorProvider
-keep class * implements org.apache.poi.extractor.ExtractorProvider {
    <init>(...);
    public *;
}
-keep class org.apache.poi.sl.draw.ImageRenderer
-keep class * implements org.apache.poi.sl.draw.ImageRenderer {
    <init>(...);
    public *;
}
-keep class * extends java.util.ServiceLoader {
    public <methods>;
}
-keepdirectories META-INF/services

# poi-ooxml-lite 4.7M
-dontwarn com.microsoft.schemas.**
-dontwarn org.etsi.uri.x01903.**
-dontwarn org.w3.x2000.x09.**
-dontwarn org.openxmlformats.**
-dontwarn org.apache.poi.schemas.**
# 一些非代码资源，不可移除
-keep class org.apache.poi.schemas.ooxml.** { *; }
-keep class org.openxmlformats.schemas.** { *; }


# poi库 org.apache.poi 2.5M
-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }

# xmlbeans 1.9M
-dontwarn org.apache.xmlbeans.**
-keep class org.apache.xmlbeans.**{ *; }
#-keep class org.apache.xmlbeans.metadata.**{ *; }
#-keep,allowobfuscation class org.apache.xmlbeans.** { *; }

# 其他
-keep class org.apache.logging.slf4j.** { *; }
-dontwarn org.apache.commons.**
-dontwarn org.ehcache.**
-dontwarn org.terracotta.utilities.**
# 我们用于填充模板的数据类
-keep class io.dev.connector.output.excel.bean.** { *; }


# ##################################### jxls需要保留的内容##############################
# commons-jexl3 400k
-dontwarn org.apache.logging.log4j.**
-keep class org.apache.commons.logging.** { *; }
-keep class org.apache.commons.jexl3.** { *; }
# 保持包下的所有类和成员
-keep class org.jxls.** { *; }
# 保持所有带有注解的类和成员
-keep @interface org.jxls.**
# 保持所有通过反射访问的类和成员
-keepclassmembers class * {
    @org.jxls.* <methods>;
}

# ##################################### XChart图表库 #####################################
-keep class org.knowm.xchart.** { *; }

# 这都他妈的怎么引进来的？？？
-dontwarn org.junit.platform.engine.**
-dontwarn org.junit.platform.commons.**
-dontwarn org.junit.jupiter.**

# ##################################### pdfbox需要保留的内容##############################
-dontwarn org.apache.pdfbox.**
-keep class org.apache.fontbox.unicode.** { *; }
-keep class org.apache.fontbox.cmap.** { *; }

# ##################################### jfreechart需要保留的内容##############################
-dontwarn org.jfree.chart.**

# ##################################### 插件化需要保留的内容##############################
-keep class io.dev_api.** { *; }
-dontwarn org.pf4j.**
-keepnames class org.pf4j.**{*;}

# ##################################### 使用委托机制获取属性名-需要保留属性名称 ##############################
# 委托机制获取属性名称本质上使用了反射，混淆不是稳定的，同一个属性不同次的混淆，产生的名称也不一样，必须保护起来
# 示例：
#-keepclassmembers class com.example.MyClass {
#    *** userName;
#}

# ##################################### ktorfit & ktor #####################################
-keep class de.jensklingenberg.ktorfit.** { *; }
-keepclassmembers class de.jensklingenberg.ktorfit.** { *; }
#-keepnames class io.ktor.client.** { *; }
-keep class io.ktor.** { *; }

# ##################################### 本地库 ##############################
-dontwarn androidx.paging.**
-dontwarn jetbrains.paging.compose.**
#-keep class jetbrains.compose.accompanist.ui.log.**
-dontwarn com.example.room.time.**
# =================== tabrow ======================
-keep class androidx.compose.material3.TabPosition{*;}
-dontwarn jetbrains.compose.accompanist.ui.components.**

# #################################### jvm system spi需要保留的内容############################
# 仅有这一条不顶用，还是得保留接口和实现类
-keep @com.google.auto.service.AutoService class * { *; }

# 托盘实现
-keep class androidx.jvm.system.ui.tray.ISystemTray { *; }
-keep class * implements androidx.jvm.system.ui.tray.ISystemTray { *; }

# pathProvider相关
-keep class androidx.jvm.system.core.AppBasePathProvider { *; }
-keep class * implements androidx.jvm.system.core.AppBasePathProvider { *; }