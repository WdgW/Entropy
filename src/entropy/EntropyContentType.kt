package entropy

class EntropyContentType(val contentClass: Class<out EntropyContent>,val name: String){
    init{
        all.add(this)
    }
    companion object{
        val all: MutableList<EntropyContentType> = mutableListOf()
        val reaction = EntropyContentType(Reaction::class.java, "reaction")

    }
}
interface EntropyContent{
    fun getContentType(): EntropyContentType
}