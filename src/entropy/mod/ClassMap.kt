package entropy.mod

import entropy.world.PowerProjector
import entropy.world.PowerProjectorNode

object ClassMap {
    val classMap = HashMap<String, Class<*>>()
    fun addClass(name: String, classType: Class<*>): Boolean {
        if (classMap.containsKey(name)) return false
        classMap[name] = classType
        return true
    }
    fun getClass(name: String) = classMap[name]
    fun getClass(name: String, default: Class<*>) = classMap[name] ?: default
    fun hasClass(name: String) = getClass(name) != null
    fun removeClass(name: String) = classMap.remove(name)
    fun clear() = classMap.clear()
    fun register(name: String, classType: Class<*>): Boolean {
        if (classMap.containsKey(name)) return false
        classMap[name] = classType
        //首字母小写
        classMap[name.firstCharLowerCase()] = classType
        return true
    }
    fun String.firstCharLowerCase() = if (isEmpty()) this else replaceFirstChar { it.lowercase() }


    init {
        register("PowerProjectorNode", PowerProjectorNode::class.java)
        register("PowerProjector", PowerProjector::class.java)
    }
}