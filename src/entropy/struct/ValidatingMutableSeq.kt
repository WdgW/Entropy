package entropy.struct

import arc.func.*
import arc.struct.*
import arc.util.Eachable

class ValidatingMutableSeq<T>(
    private val delegate: Seq<T>,
    private val predicate: (T) -> Boolean
) : Iterable<T> by delegate, Eachable<T> by delegate {

    // ---- 查询 / 只读 ----

    val size: Int get() = delegate.size
    val isEmpty: Boolean get() = delegate.isEmpty
    val isNotEmpty: Boolean get() = !delegate.isEmpty

    fun get(index: Int): T = delegate.get(index)
    fun indexOf(value: T): Int = delegate.indexOf(value)
    fun indexOf(value: T, identity: Boolean): Int = delegate.indexOf(value, identity)
    fun contains(value: T): Boolean = delegate.contains(value)
    fun contains(value: T, identity: Boolean): Boolean = delegate.contains(value, identity)
    fun first(): T = delegate.first()
    fun last(): T = delegate.last()
    fun peek(): T = delegate.peek()
    fun toArray(): Array<T> = delegate.toArray()

    fun <K, V> asMap(keygen: Func<T, K>, valgen: Func<T, V>): ObjectMap<K, V> = delegate.asMap(keygen, valgen)
    fun <K> asMap(keygen: Func<T, K>): ObjectMap<K, T> = delegate.asMap(keygen)
    fun <K> asSet(keygen: (T) -> K): ObjectSet<T> = ObjectSet.with(delegate)

    // ---- 查询 / 只读: 聚合（直接委托 delegate）----

    /**
     * 对每个元素应用 Floatf 并返回总和（只读，不修改集合）。
     */
    fun sumf(summer: Floatf<T>): Float = delegate.sumf(summer)

    /**
     * 对每个元素应用 Intf 并返回总和（只读，不修改集合）。
     */
    fun sum(summer: Intf<T>): Int = delegate.sum(summer)

    /**
     * 使用初始值和归约函数对集合元素进行归约（只读，不修改集合）。
     */
    fun <R> reduce(initial: R, reducer: Func2<T, R, R>): R = delegate.reduce(initial, reducer)

    /**
     * 检查所有元素是否满足谓词（只读，不修改集合）。
     */
    fun allMatch(predicate: Boolf<T>): Boolean = delegate.allMatch(predicate)

    /**
     * 检查是否存在满足谓词的元素（只读，不修改集合）。
     */
    fun contains(pred: Boolf<T>): Boolean = delegate.contains(pred)

    // ---- 查询 / 只读: 映射与扁平化（直接委托 delegate）----

    /**
     * 对每个元素应用映射函数，返回新的 Seq（只读，不影响当前集合）。
     */
    fun <R> map(mapper: Func<T, R>): Seq<R> = delegate.map(mapper)

    /**
     * 对每个元素应用映射函数产生 Iterable，然后扁平化（只读）。
     */
    fun <R> flatMap(mapper: Func<T, Iterable<R>>): Seq<R> = delegate.flatMap(mapper)

    /**
     * 将 Seq-of-Seqs 扁平化（只读）。
     */
    fun <R> flatten(): Seq<R> = delegate.flatten()

    /**
     * 对每个元素应用 Intf 映射，返回 IntSeq（只读）。
     */
    fun mapInt(mapper: Intf<T>): IntSeq = delegate.mapInt(mapper)

    /**
     * 只保留满足 retain 谓词的元素进行 Intf 映射，返回 IntSeq（只读）。
     */
    fun mapInt(mapper: Intf<T>, retain: Boolf<T>): IntSeq = delegate.mapInt(mapper, retain)

    /**
     * 对每个元素应用 Floatf 映射，返回 FloatSeq（只读）。
     */
    fun mapFloat(mapper: Floatf<T>): FloatSeq = delegate.mapFloat(mapper)

    /**
     * 只对满足 pred 的元素调用 consumer（只读，不修改集合）。
     */
    fun <E : T> each(pred: Boolf<in T>, consumer: Cons<E>) = delegate.each(pred, consumer)

    // ---- 查询 / 只读: 极值、查找、索引、转换（直接委托 delegate）----

    /**
     * 返回比较器下的最小元素（只读）。
     */
    fun min(comparator: Comparator<T>): T = delegate.min(comparator)

    /**
     * 返回比较器下的最大元素（只读）。
     */
    fun max(comparator: Comparator<T>): T = delegate.max(comparator)

    /**
     * 返回 Floatf 映射下的最小元素（只读）。
     */
    fun min(func: Floatf<T>): T = delegate.min(func)

    /**
     * 返回 Floatf 映射下的最大元素（只读）。
     */
    fun max(func: Floatf<T>): T = delegate.max(func)

    /**
     * 只在满足 filter 的元素中，返回 Floatf 映射最小的元素（只读）。
     */
    fun min(filter: Boolf<T>, func: Floatf<T>): T = delegate.min(filter, func)

    /**
     * 只在满足 filter 的元素中，返回比较器最小的元素（只读）。
     */
    fun min(filter: Boolf<T>, comparator: Comparator<T>): T = delegate.min(filter, comparator)

    /**
     * 返回第一个满足谓词的元素，找不到返回 null（只读）。
     */
    fun find(predicate: Boolf<T>): T? = delegate.find(predicate)

    /**
     * 返回第一个满足谓词的元素索引，找不到返回 -1（只读）。
     */
    fun indexOf(predicate: Boolf<T>): Int = delegate.indexOf(predicate)

    /**
     * 从末尾搜索 value 并返回其索引（只读）。
     */
    fun lastIndexOf(value: T, identity: Boolean): Int = delegate.lastIndexOf(value, identity)

    /**
     * 按比例索引返回元素（只读）。
     */
    fun getFrac(index: Float): T? = delegate.getFrac(index)

    /**
     * 检查 seq 中的每个元素是否都存在于当前集合中（只读）。
     */
    fun containsAll(seq: Seq<T>): Boolean = delegate.containsAll(seq)

    /**
     * 使用 identity 语义检查 seq 中的每个元素是否都存在于当前集合中（只读）。
     */
    fun containsAll(seq: Seq<T>, identity: Boolean): Boolean = delegate.containsAll(seq, identity)

    /**
     * 将当前集合转换为 ArrayList（只读）。
     */
    fun list(): ArrayList<T> = delegate.list()

    /**
     * 将当前集合转换为 ObjectSet（只读）。
     */
    fun asSet(): ObjectSet<T> = delegate.asSet()

    // ---- 写入: 经过 predicate 校验 ----

    /**
     * 添加单个元素。若元素不通过 predicate 则返回 false 且不修改集合。
     */
    fun add(value: T): Boolean {
        if (!predicate(value)) return false
        delegate.add(value)
        return true
    }

    /**
     * 按 index 插入元素（需先通过 predicate 校验）。
     */
    fun add(index: Int, value: T): Boolean {
        if (!predicate(value)) return false
        delegate.insert(index, value)
        return true
    }

    /**
     * 按 index 插入元素（需先通过 predicate 校验）。
     */
    fun insert(index: Int, value: T): Boolean {
        if (!predicate(value)) return false
        delegate.insert(index, value)
        return true
    }

    /**
     * 从 Iterable 逐个添加，跳过非法元素；若有被跳过则返回 false。
     */
    fun addAll(iterable: Iterable<T>): Boolean {
        var allOk = true
        for (item in iterable) {
            if (!predicate(item)) {
                allOk = false
                continue
            }
            delegate.add(item)
        }
        return allOk
    }

    /**
     * 从 Kotlin Array 逐个添加，跳过非法元素；若有被跳过则返回 false。
     */
    fun addAll(array: Array<out T>): Boolean {
        var allOk = true
        for (item in array) {
            if (!predicate(item)) {
                allOk = false
                continue
            }
            delegate.add(item)
        }
        return allOk
    }

    /**
     * 添加单个元素，如果不存在于集合中则添加（需通过 predicate 校验）。
     */
    fun addUnique(value: T): Boolean {
        if (!predicate(value)) return false
        delegate.addUnique(value)
        return true
    }

    /**
     * 用 new 替换第一个等于 old 的元素（new 需通过 predicate 校验）。
     */
    fun replace(old: T, new: T): Boolean {
        if (!predicate(new)) return false
        val idx = delegate.indexOf(old)
        if (idx < 0) return false
        delegate.set(idx, new)
        return true
    }

    // ---- 写入: 多元素 add（需全部通过 predicate 校验，否则原子性失败）----

    /**
     * 一次性添加两个元素。任意一个不通过 predicate 则全部不添加，返回 false。
     */
    fun add(v1: T, v2: T): Boolean {
        if (!predicate(v1) || !predicate(v2)) return false
        delegate.add(v1, v2)
        return true
    }

    /**
     * 一次性添加三个元素。任意一个不通过 predicate 则全部不添加，返回 false。
     */
    fun add(v1: T, v2: T, v3: T): Boolean {
        if (!predicate(v1) || !predicate(v2) || !predicate(v3)) return false
        delegate.add(v1, v2, v3)
        return true
    }

    /**
     * 一次性添加四个元素。任意一个不通过 predicate 则全部不添加，返回 false。
     */
    fun add(v1: T, v2: T, v3: T, v4: T): Boolean {
        if (!predicate(v1) || !predicate(v2) || !predicate(v3) || !predicate(v4)) return false
        delegate.add(v1, v2, v3, v4)
        return true
    }

    /**
     * 从另一个 Seq 批量添加元素。先对 Seq 中每个元素做 predicate 校验；全部通过才实际添加，否则返回 false。
     */
    fun add(array: Seq<out T>): Boolean {
        for (i in 0 until array.size) {
            if (!predicate(array[i])) return false
        }
        delegate.add(array)
        return true
    }

    /**
     * 从 Kotlin Array 批量添加元素。先对每个元素做 predicate 校验；全部通过才实际添加，否则返回 false。
     */
    fun add(array: Array<out T>): Boolean {
        for (item in array) {
            if (!predicate(item)) return false
        }
        delegate.add(array)
        return true
    }

    /**
     * 从另一个 Seq 的 [start, start+count) 范围添加元素。先对范围内每个元素做 predicate 校验；全部通过才实际添加。
     */
    fun addAll(array: Seq<out T>, start: Int, count: Int): Boolean {
        for (i in start until start + count) {
            if (i >= array.size) break
            if (!predicate(array[i])) return false
        }
        delegate.addAll(array, start, count)
        return true
    }

    // ---- 写入: 全量替换 set（原子语义：先验证全部通过才清空+添加）----

    /**
     * 用另一个 Seq 的内容替换当前集合。先对每个元素做 predicate 校验；
     * 全部通过才清空当前集合并批量添加，否则不修改原集合并返回 false。
     */
    fun set(array: Seq<out T>): Boolean {
        for (i in 0 until array.size) {
            if (!predicate(array[i])) return false
        }
        delegate.clear()
        delegate.addAll(array)
        return true
    }

    /**
     * 用 Array 的内容替换当前集合。先对每个元素做 predicate 校验；
     * 全部通过才清空当前集合并批量添加，否则不修改原集合并返回 false。
     */
    fun set(array: Array<out T>): Boolean {
        for (item in array) {
            if (!predicate(item)) return false
        }
        delegate.clear()
        delegate.addAll(array, 0, array.size)
        return true
    }

    // ---- 写入: 函数式 replace 与链式 with ----

    /**
     * 对每个元素应用 mapper 后，若新值通过 predicate 则替换，否则保留原值。
     * 返回成功替换的元素数量。
     */
    fun replace(mapper: Func<T, T>): Int {
        var count = 0
        for (i in 0 until delegate.size) {
            val oldVal = delegate[i]
            val newVal = mapper.get(oldVal)
            if (predicate(newVal)) {
                delegate.set(i, newVal)
                count++
            }
        }
        return count
    }

    /**
     * 链式配置当前 ValidatingMutableSeq。参数接收 ValidatingMutableSeq 自身类型，
     * 避免绕过验证直接操作原始 Seq。返回 this 以支持链式调用。
     */
    fun with(cons: Cons<ValidatingMutableSeq<T>>): ValidatingMutableSeq<T> {
        cons.get(this)
        return this
    }

    // ---- 删除 / 结构变更 ----

    fun remove(value: T): Boolean = delegate.remove(value)
    fun remove(value: T, identity: Boolean): Boolean = delegate.remove(value, identity)
    fun remove(index: Int): T = delegate.remove(index)
    fun remove(value: Boolf<T>) = delegate.remove(value)
    fun pop(): T = delegate.pop()
    fun clear() = delegate.clear()

    fun retainAll(pred: Boolf<T>): ValidatingMutableSeq<T> {
        delegate.retainAll(pred)
        return this
    }

    fun removeAll(array: Seq<T>): Boolean = delegate.removeAll(array)
    fun removeAll(array: Seq<T>, identity: Boolean): Boolean = delegate.removeAll(array, identity)

    /**
     * 截断集合到指定大小（直接委托 delegate）。
     */
    fun truncate(newSize: Int) = delegate.truncate(newSize)

    /**
     * 删除 [start, end) 范围的元素（直接委托 delegate）。
     */
    fun removeRange(start: Int, end: Int) = delegate.removeRange(start, end)

    /**
     * 就地筛选保留满足 test 的元素（直接委托 delegate）。
     */
    fun select(test: Boolf<T>) = delegate.select(test)

    /**
     * 缩容（直接委托 delegate）。
     */
    fun shrink() = delegate.shrink()

    // ---- 遍历 / 排序 ----

    override fun each(c: Cons<in T>) = delegate.each(c)
    fun each(block: (T) -> Unit) = delegate.each(Cons(block))

    fun sort(comparator: Comparator<in T>) = delegate.sort(comparator)
    fun sort() = delegate.sort()
    fun reverse() = delegate.reverse()
    fun shuffle() = delegate.shuffle()
    fun swap(a: Int, b: Int) = delegate.swap(a, b)

    fun ensureCapacity(size: Int) = delegate.ensureCapacity(size)
    fun copy(): Seq<T> = Seq.with(delegate)

    fun any(): Boolean = delegate.any()
    fun count(pred: Boolf<T>): Int = delegate.count(pred)

    // ---- Kotlin 运算符重载 ----

    /**
     * 按索引设置元素（需通过 predicate 校验，否则返回 false 且不修改）。
     */
    operator fun set(index: Int, value: T): Boolean {
        if (!predicate(value)) return false
        delegate.set(index, value)
        return true
    }

    operator fun plusAssign(value: T) {
        add(value)
    }

    operator fun minusAssign(value: T) {
        remove(value)
    }
}