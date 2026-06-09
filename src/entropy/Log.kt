@file:Suppress("NOTHING_TO_INLINE")

package entropy

import arc.util.Log


inline fun <T>  T.log(){
    Log.log(Log.LogLevel.none,"[Entropy] $this")
}

inline fun <T> T.log(str: String){
    Log.log(Log.LogLevel.none,"[Entropy] $str $this")
}

inline fun <T>  T.info(){
    this.log("[I]")
}

inline fun <T>  T.warn(){
    this.log("[W]")
}

inline fun <T>  T.err(){
    this.log("[E]")
}

inline fun <T> T.debug(){
    this.log("[D]")
}
