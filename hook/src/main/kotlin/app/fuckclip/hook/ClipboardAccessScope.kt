package app.fuckclip.hook

/**
 * Thread-scoped markers tracking active clipboard interception and user-granted execution states.
 *
 * 跟踪当前线程剪贴板拦截状态与用户已授权执行状态的线程级作用域标记。
 */
internal object ClipboardAccessScope {
    private val intercepting = ThreadLocal<Boolean>()
    private val userGranted = ThreadLocal<Boolean>()

    /**
     * Whether the current thread is already inside a FuckClip clipboard decision.
     *
     * 当前线程是否已处于 FuckClip 的剪贴板决策流程之中。
     */
    fun isIntercepting(): Boolean = intercepting.get() == true

    /**
     * Whether the current thread executes an operation explicitly granted by FuckClip.
     *
     * 当前线程是否正在执行已被 FuckClip 明确授权的操作。
     */
    fun isUserGrantActive(): Boolean = userGranted.get() == true

    /**
     * Runs [block] while marking the current thread as being inside a clipboard decision.
     *
     * 在将当前线程标记为“剪贴板决策中”的状态下执行 [block]。
     */
    fun <T> intercept(block: () -> T): T = withFlag(intercepting, block)

    /**
     * Runs [block] while marking the current thread as executing a user-granted operation.
     *
     * 在将当前线程标记为“已获用户授权”的状态下执行 [block]。
     */
    fun <T> withUserGrant(block: () -> T): T = withFlag(userGranted, block)

    private fun <T> withFlag(flag: ThreadLocal<Boolean>, block: () -> T): T {
        val previous = flag.get()
        flag.set(true)
        try {
            return block()
        } finally {
            if (previous == null) flag.remove() else flag.set(previous)
        }
    }
}
