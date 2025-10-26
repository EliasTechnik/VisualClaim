package dev.ewio.util


fun getCorrectlySplitArgs(args: List<String>, startIndex: Int = 0): List<String>{
    var combined: String = ""
    var inQuotes = false
    val newArgs: MutableList<String> = mutableListOf()

    for (i in 0..<args.size) {
        if(args[i].startsWith("\"") && args[i].endsWith("\"") && args[i].length > 1){
            //single word in quotes
            newArgs.add(args[i])
        }else if (args[i].startsWith("\"")) {
            //combine until we find the end
            inQuotes = true
            combined = args[i]//.replace("\"", "")
        } else if (args[i].endsWith("\"")) {
            //found the end
            combined = combined + " " + args[i]//.replace("\"", "")
            inQuotes = false
            newArgs.add(combined.trim())
        } else if (inQuotes) {
            combined = combined + " " + args[i]
        } else {
            newArgs.add(args[i])
        }
    }

    if (inQuotes) {
        //handle unclosed quotes by adding the combined string anyway
        newArgs.add(combined + "\"")
    }

    val cleanArgs = mutableListOf<String>()

    //remove quotes from arguments
    newArgs.forEach {
        cleanArgs.add(it.replace("\"", ""))
    }

    return cleanArgs.toList()
}

fun getQuotedStrings(strings: List<String>):List<String>{
    val quotedStrings = mutableListOf<String>()
    strings.forEach {
        quotedStrings.add("\"$it\"")
    }
    return quotedStrings.toList()
}