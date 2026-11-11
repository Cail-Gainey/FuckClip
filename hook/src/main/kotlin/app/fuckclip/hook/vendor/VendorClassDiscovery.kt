package app.fuckclip.hook.vendor

import android.annotation.SuppressLint
import android.os.IBinder
import java.lang.reflect.Field

/**
 * Shared reflection utilities for discovering vendor clipboard classes, Binder services, and delegates.
 *
 * 用于探测厂商剪贴板类、Binder 服务与委托实例的共享反射辅助工具。
 */
internal object VendorClassDiscovery {
    /**
     * Collects distinct candidate [ClassLoader] instances anchored on [primary] and [anchorClass].
     *
     * 基于主类加载器 [primary] 与锚点类 [anchorClass] 收集去重后的候选类加载器列表。
     */
    fun buildClassLoaders(primary: ClassLoader?, anchorClass: Class<*>): List<ClassLoader> {
        val result = LinkedHashSet<ClassLoader>()
        if (primary != null) result.add(primary)
        Thread.currentThread().contextClassLoader?.let { result.add(it) }
        anchorClass.classLoader?.let { result.add(it) }
        ClassLoader.getSystemClassLoader()?.let { result.add(it) }
        return result.toList()
    }

    /**
     * Loads the class named [name] without static initialization across candidate [loaders], returning null if absent.
     *
     * 按顺序从候选类加载器 [loaders] 查找并加载名为 [name] 的类（不触发初始化），未找到时返回 null。
     */
    fun findClass(name: String, loaders: List<ClassLoader>): Class<*>? {
        for (loader in loaders) {
            try {
                return Class.forName(name, false, loader)
            } catch (_: Throwable) {}
        }
        return null
    }

    /**
     * Queries `ServiceManager` for each entry in [serviceNames] and passes each resolved Binder implementation class to [onBinderClass].
     *
     * 向 `ServiceManager` 查询 [serviceNames] 中的每个服务名，并将解析到的 Binder 实现类传递给 [onBinderClass]。
     */
    @SuppressLint("PrivateApi")
    fun probeBinderServices(serviceNames: List<String>, onBinderClass: (Class<*>) -> Unit) {
        try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String::class.java)
            getServiceMethod.isAccessible = true
            for (serviceName in serviceNames) {
                val binder = getServiceMethod.invoke(null, serviceName) as? IBinder ?: continue
                onBinderClass(binder.javaClass)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Resolves an enclosing or backing service instance referenced by [service], returning null when none exists.
     *
     * 解析 [service] 所持有的外层或底层宿主服务实例，不存在时返回 null。
     */
    fun findOuterServiceInstance(service: Any): Any? {
        var current: Class<*>? = service.javaClass
        while (current != null && current != Any::class.java) {
            for (field in current.declaredFields) {
                if (field.name.startsWith("this$0") || field.name == "mService") {
                    try {
                        field.isAccessible = true
                        val outer = field.get(service)
                        if (outer != null && outer !== service) return outer
                    } catch (_: Throwable) {}
                }
            }
            current = current.superclass
        }
        return null
    }

    /**
     * Traverses fields declared on [clazz] and its ancestors, reporting concrete field types matching [matcher] to [onClass].
     *
     * 遍历 [clazz] 及其父类的声明字段，将满足 [matcher] 条件的非接口字段类型回调给 [onClass]。
     */
    fun inspectDelegateFieldTypes(
        clazz: Class<*>,
        matcher: (Field) -> Boolean,
        onClass: (Class<*>) -> Unit
    ) {
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            for (field in current.declaredFields) {
                val type = field.type
                if (!type.isInterface && matcher(field)) {
                    onClass(type)
                }
            }
            current = current.superclass
        }
    }

    /**
     * Reads runtime delegate instances matching [matcher] on [serviceInstance] and its outer service, passing each runtime class to [onInstanceClass].
     *
     * 读取 [serviceInstance] 及其外层宿主对象上满足 [matcher] 的运行时委托实例，并将其实际类回调给 [onInstanceClass]。
     */
    fun inspectDelegateInstances(
        serviceInstance: Any?,
        matcher: (Field) -> Boolean,
        onInstanceClass: (Class<*>) -> Unit
    ) {
        val instance = serviceInstance ?: return
        val targets = listOfNotNull(instance, findOuterServiceInstance(instance))
        for (target in targets) {
            var current: Class<*>? = target.javaClass
            while (current != null && current != Any::class.java) {
                for (field in current.declaredFields) {
                    if (matcher(field)) {
                        try {
                            field.isAccessible = true
                            val delegate = field.get(target)
                            if (delegate != null) {
                                onInstanceClass(delegate.javaClass)
                            }
                        } catch (_: Throwable) {}
                    }
                }
                current = current.superclass
            }
        }
    }
}
