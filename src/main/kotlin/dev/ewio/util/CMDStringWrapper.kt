package dev.ewio.util


class CMDStringWrapper{
    private val raw: String

    constructor(raw: String){
        this.raw = raw.replace("\"","")
    }

    constructor(args: List<String>, startIndex: Int){
        val sb = StringBuilder()
        for(i in startIndex until args.size){
            sb.append(args[i])
            if(i != args.size - 1){
                sb.append(" ")
            }
        }
        this.raw = sb.toString().replace("\"","")
    }

    fun toCMDString(): String {
        return "\"$raw\""
    }

    fun getPlain(): String {
        return raw
    }
}