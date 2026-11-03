# 混淆规则：只保留真正需要的部分。
# ProGuard rules: only what this project actually needs.

-keepattributes *Annotation*,InnerClasses,Signature,SourceFile,LineNumberTable

# 四大组件与自定义 View 由系统反射创建，不能改名。
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# LitePal 靠反射读写实体。
-keep class org.litepal.** { *; }
-keep class * extends org.litepal.crud.LitePalSupport { *; }
-keep class * extends org.litepal.LitePalApplication { *; }

# LibXposed API 102：入口类不能被裁剪，混淆后要把改写后的类名同步写回 java_init.list。
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}
