package androidx.compose.desktop.runtime.savestate

/**
 * 接口：IToken
 *
 * 作用：作为 Token 值类的「装箱视图」暴露给 Java 使用。
 *
 * 背景：
 *  Kotlin 的 value class 在编译后会被「内联」为底层类型（这里是 String），
 *  其构造函数通常是私有的/合成的，并且任何出现在签名中的 value class
 *  都会被编译器做 name mangling（名称修饰）。
 *  因此 Java 无法直接 new Token(...)，也无法在方法签名里正常使用 Token 类型。
 *
 * 解决思路：
 *  让 value class 实现一个普通接口，Java 只依赖该接口类型，
 *  从而绕开 value class 的直接构造和签名问题。
 */
interface IToken {
    /**
     * 暴露底层值。
     * 注意：这是接口属性，Kotlin 会为 Java 生成对应的 getter：getValue()。
     */
    val value: String
}

/**
 * 值类：Token
 *
 * @JvmInline 表示这是一个 inline value class，运行时通常不产生对象，
 * 而是直接以 String 的形式存在（unboxed），用于减少包装开销。
 *
 * 注意：
 *  - 这里 override val value 来自接口 IToken，因此它同时具备「接口属性」的身份。
 *  - 当 Token 被当作 IToken 使用时（例如从 Tokens.of 返回），
 *    Kotlin 会进行装箱（boxing），此时才会真正生成一个 Token 实例。
 *  - Java 侧无法 new Token(...)，只能通过 Tokens.of(...) 获取 IToken。
 */
@JvmInline
value class Token(
    override val value: String,
) : IToken

/**
 * 伴生工厂：Tokens
 *
 * 用 object + @JvmStatic 的方式，向 Java 暴露一个静态工厂方法。
 *
 * 关键点：
 *  - 返回类型写成 IToken（接口），而不是 Token（value class）。
 *    这是为了让 Java 能正常调用：因为 value class 出现在签名中会被 name mangling，
 *    而接口不会，Java 可以毫无障碍地调用 of(...)。
 *  - 在 of 内部 `return Token(value)` 时，由于返回类型被声明为 IToken，
 *    编译器会执行「装箱」，实际返回一个持有 String 的 Token 实例。
 *  - @JvmStatic 让 Java 可以像调用静态方法一样调用：Tokens.of("abc")，
 *    而不需要写 Tokens.INSTANCE.of("abc")。
 */
object Tokens {

    /**
     * 工厂方法：根据字符串创建 IToken。
     *
     * Java 调用示例：
     *   IToken token = Tokens.of("hello");
     *   String v = token.getValue();
     *
     * @param value 底层字符串值
     * @return 以接口形式暴露的 Token 实例（装箱后的 value class）
     */
    @JvmStatic
    fun of(value: String): IToken {
        // 由于返回类型是 IToken，这里 Token(value) 会被装箱成对象
        return Token(value)
    }
}
