package com.pallas.launcher.domain.search

import java.util.Locale
import kotlin.math.*

// =========================================================================
// Wolfram AST, Lexer & Parser
// =========================================================================

internal enum class TokenType {
    NUMBER, IDENT, LBRACKET, RBRACKET, LBRACE, RBRACE, LPAREN, RPAREN,
    COMMA, PLUS, MINUS, STAR, SLASH, CARET, PERCENT, EXCLAMATION, EOF
}

internal data class Token(val type: TokenType, val text: String, val value: Double = 0.0)

internal class Tokenizer(private val input: String) {
    private var pos = 0

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (pos < input.length) {
            val c = input[pos]
            when {
                c.isWhitespace() -> pos++
                c.isDigit() || (c == '.' && pos + 1 < input.length && input[pos + 1].isDigit()) -> {
                    val start = pos
                    while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
                    if (pos < input.length && (input[pos] == 'e' || input[pos] == 'E')) {
                        pos++
                        if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) pos++
                        while (pos < input.length && input[pos].isDigit()) pos++
                    }
                    val str = input.substring(start, pos)
                    val num = str.toDoubleOrNull() ?: 0.0
                    tokens.add(Token(TokenType.NUMBER, str, num))
                }
                c.isLetter() || c == 'π' || c == '∞' -> {
                    val start = pos
                    while (pos < input.length && (input[pos].isLetter() || input[pos].isDigit() || input[pos] == '_' || input[pos] == 'π' || input[pos] == '∞')) {
                        pos++
                    }
                    val str = input.substring(start, pos)
                    tokens.add(Token(TokenType.IDENT, str))
                }
                c == '[' -> { tokens.add(Token(TokenType.LBRACKET, "[")); pos++ }
                c == ']' -> { tokens.add(Token(TokenType.RBRACKET, "]")); pos++ }
                c == '{' -> { tokens.add(Token(TokenType.LBRACE, "{")); pos++ }
                c == '}' -> { tokens.add(Token(TokenType.RBRACE, "}")); pos++ }
                c == '(' -> { tokens.add(Token(TokenType.LPAREN, "(")); pos++ }
                c == ')' -> { tokens.add(Token(TokenType.RPAREN, ")")); pos++ }
                c == ',' -> { tokens.add(Token(TokenType.COMMA, ",")); pos++ }
                c == '+' -> { tokens.add(Token(TokenType.PLUS, "+")); pos++ }
                c == '-' -> { tokens.add(Token(TokenType.MINUS, "-")); pos++ }
                c == '*' || c == '×' -> { tokens.add(Token(TokenType.STAR, "*")); pos++ }
                c == '/' || c == '÷' -> { tokens.add(Token(TokenType.SLASH, "/")); pos++ }
                c == '^' -> { tokens.add(Token(TokenType.CARET, "^")); pos++ }
                c == '%' -> { tokens.add(Token(TokenType.PERCENT, "%")); pos++ }
                c == '!' -> { tokens.add(Token(TokenType.EXCLAMATION, "!")); pos++ }
                else -> pos++
            }
        }
        tokens.add(Token(TokenType.EOF, ""))
        return tokens
    }
}

sealed interface MathNode {
    fun eval(env: Map<String, MathValue> = emptyMap()): MathValue

    data class NumberNode(val value: Double) : MathNode {
        override fun eval(env: Map<String, MathValue>): MathValue = MathValue.NumberVal(value)
    }

    data class BooleanNode(val value: Boolean) : MathNode {
        override fun eval(env: Map<String, MathValue>): MathValue = MathValue.BooleanVal(value)
    }

    data class ConstantValueNode(val value: MathValue) : MathNode {
        override fun eval(env: Map<String, MathValue>): MathValue = value
    }

    object OutNode : MathNode {
        override fun eval(env: Map<String, MathValue>): MathValue =
            env["%"] ?: env["out"] ?: MathStatsEngine.lastEvaluatedValue ?: MathValue.NumberVal(0.0)
    }

    data class IdentNode(val name: String) : MathNode {
        override fun eval(env: Map<String, MathValue>): MathValue {
            val lower = name.lowercase(Locale.ROOT)
            env[lower]?.let { return it }
            return when (lower) {
                "pi", "π" -> MathValue.NumberVal(PI)
                "e" -> MathValue.NumberVal(E)
                "infinity", "inf", "∞" -> MathValue.NumberVal(Double.POSITIVE_INFINITY)
                "true" -> MathValue.BooleanVal(true)
                "false" -> MathValue.BooleanVal(false)
                "%", "out" -> env["%"] ?: env["out"] ?: MathStatsEngine.lastEvaluatedValue ?: MathValue.NumberVal(0.0)
                else -> MathValue.SymbolVal(name)
            }
        }
    }

    data class ListNode(val items: List<MathNode>) : MathNode {
        override fun eval(env: Map<String, MathValue>): MathValue {
            val list = mutableListOf<Double>()
            items.forEach { node ->
                when (val v = node.eval(env)) {
                    is MathValue.NumberVal -> list.add(v.value)
                    is MathValue.ListVal -> list.addAll(v.elements)
                    is MathValue.BooleanVal -> list.add(if (v.value) 1.0 else 0.0)
                    is MathValue.DistVal, is MathValue.ExprVal, is MathValue.SymbolVal -> {}
                }
            }
            return MathValue.ListVal(list)
        }
    }

    data class UnaryOpNode(val op: Char, val child: MathNode) : MathNode {
        override fun eval(env: Map<String, MathValue>): MathValue {
            val v = child.eval(env)
            return when (op) {
                '-' -> when (v) {
                    is MathValue.NumberVal -> MathValue.NumberVal(-v.value)
                    is MathValue.ListVal -> MathValue.ListVal(v.elements.map { -it })
                    else -> error("Cannot negate $v")
                }
                '+' -> v
                '!' -> when (v) {
                    is MathValue.NumberVal -> {
                        val n = kotlin.math.round(v.value).toInt()
                        if (n < 0 || abs(v.value - n) > 1e-9) MathValue.NumberVal(Double.NaN)
                        else MathValue.NumberVal(MathStatsEngine.exactFactorial(n))
                    }
                    is MathValue.ListVal -> MathValue.ListVal(v.elements.map {
                        val n = kotlin.math.round(it).toInt()
                        if (n < 0 || abs(it - n) > 1e-9) Double.NaN else MathStatsEngine.exactFactorial(n)
                    })
                    else -> error("Cannot compute factorial of $v")
                }
                else -> error("Unknown unary operator: $op")
            }
        }
    }

    data class BinaryOpNode(val op: Char, val left: MathNode, val right: MathNode) : MathNode {
        override fun eval(env: Map<String, MathValue>): MathValue {
            val l = left.eval(env)
            val r = right.eval(env)

            fun applyOp(a: Double, b: Double): Double = when (op) {
                '+' -> a + b
                '-' -> a - b
                '*' -> a * b
                '/' -> a / b
                '%' -> a % b
                '^' -> a.pow(b)
                else -> error("Unknown operator $op")
            }

            return when {
                l is MathValue.NumberVal && r is MathValue.NumberVal ->
                    MathValue.NumberVal(applyOp(l.value, r.value))
                l is MathValue.NumberVal && r is MathValue.ListVal ->
                    MathValue.ListVal(r.elements.map { applyOp(l.value, it) })
                l is MathValue.ListVal && r is MathValue.NumberVal ->
                    MathValue.ListVal(l.elements.map { applyOp(it, r.value) })
                l is MathValue.ListVal && r is MathValue.ListVal -> {
                    val size = minOf(l.elements.size, r.elements.size)
                    val res = (0 until size).map { applyOp(l.elements[it], r.elements[it]) }
                    MathValue.ListVal(res)
                }
                else -> error("Cannot apply binary operator $op to $l and $r")
            }
        }
    }

    data class CallNode(val funcName: String, val args: List<MathNode>) : MathNode {
        override fun eval(env: Map<String, MathValue>): MathValue {
            val fn = funcName.lowercase(Locale.ROOT)

            // Special forms that delay or control argument evaluation
            when (fn) {
                "table" -> {
                    if (args.isEmpty()) error("Table requires arguments")
                    val exprNode = args[0]
                    if (args.size == 1) return exprNode.eval(env)

                    val specNode = args[1]
                    if (specNode is ListNode) {
                        val varNode = specNode.items.getOrNull(0) as? IdentNode
                        val varName = varNode?.name ?: "x"
                        val min: Double
                        val max: Double
                        val step: Double
                        when (specNode.items.size) {
                            1 -> {
                                min = 1.0
                                max = specNode.items[0].eval(env).asDouble()
                                step = 1.0
                            }
                            2 -> {
                                min = 1.0
                                max = specNode.items[1].eval(env).asDouble()
                                step = 1.0
                            }
                            3 -> {
                                min = specNode.items[1].eval(env).asDouble()
                                max = specNode.items[2].eval(env).asDouble()
                                step = 1.0
                            }
                            else -> {
                                min = specNode.items[1].eval(env).asDouble()
                                max = specNode.items[2].eval(env).asDouble()
                                step = specNode.items[3].eval(env).asDouble()
                            }
                        }

                        val result = mutableListOf<Double>()
                        val limit = 5000
                        var curr = min
                        if (step > 0.0) {
                            while (curr <= max + 1e-9 && result.size < limit) {
                                val subEnv = env + (varName.lowercase(Locale.ROOT) to MathValue.NumberVal(curr))
                                val item = exprNode.eval(subEnv)
                                result.addAll(item.asList())
                                curr += step
                            }
                        } else if (step < 0.0) {
                            while (curr >= max - 1e-9 && result.size < limit) {
                                val subEnv = env + (varName.lowercase(Locale.ROOT) to MathValue.NumberVal(curr))
                                val item = exprNode.eval(subEnv)
                                result.addAll(item.asList())
                                curr += step
                            }
                        }
                        return MathValue.ListVal(result)
                    } else {
                        val n = specNode.eval(env).asDouble().toInt().coerceIn(0, 5000)
                        val result = mutableListOf<Double>()
                        for (i in 0 until n) {
                            result.addAll(exprNode.eval(env).asList())
                        }
                        return MathValue.ListVal(result)
                    }
                }
                "nest" -> {
                    val fnName = when (val fnNode = args.getOrNull(0)) {
                        is IdentNode -> fnNode.name
                        else -> error("Expected function name as first argument to Nest")
                    }
                    var curr = args.getOrNull(1)?.eval(env) ?: error("Expected expression in Nest")
                    val n = args.getOrNull(2)?.eval(env)?.asDouble()?.toInt()?.coerceIn(0, 1000) ?: 1
                    for (step in 0 until n) {
                        curr = CallNode(fnName, listOf(ConstantValueNode(curr))).eval(env)
                    }
                    return curr
                }
                "fold" -> {
                    val fnName = when (val fnNode = args.getOrNull(0)) {
                        is IdentNode -> fnNode.name
                        else -> error("Expected function name as first argument to Fold")
                    }
                    if (args.size >= 3) {
                        var acc = args[1].eval(env)
                        val list = args[2].eval(env).asList()
                        for (item in list) {
                            acc = CallNode(fnName, listOf(ConstantValueNode(acc), NumberNode(item))).eval(env)
                        }
                        return acc
                    } else if (args.size == 2) {
                        val list = args[1].eval(env).asList()
                        if (list.isEmpty()) return MathValue.NumberVal(Double.NaN)
                        var acc: MathValue = MathValue.NumberVal(list[0])
                        for (i in 1 until list.size) {
                            acc = CallNode(fnName, listOf(ConstantValueNode(acc), NumberNode(list[i]))).eval(env)
                        }
                        return acc
                    } else {
                        error("Expected 2 or 3 arguments in Fold")
                    }
                }
            }

            val evaluatedArgs = args.map { it.eval(env) }

            // Check distribution constructors
            when (fn) {
                "normaldistribution" -> {
                    val mu = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val sigma = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(NormalDist(mu, sigma))
                }
                "uniformdistribution" -> {
                    var min = 0.0
                    var max = 1.0
                    if (evaluatedArgs.isNotEmpty()) {
                        val first = evaluatedArgs[0]
                        if (first is MathValue.ListVal && first.elements.size >= 2) {
                            min = first.elements[0]
                            max = first.elements[1]
                        } else {
                            min = first.asDouble()
                            max = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                        }
                    }
                    return MathValue.DistVal(UniformDist(min, max))
                }
                "exponentialdistribution" -> {
                    val lam = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    return MathValue.DistVal(ExponentialDist(lam))
                }
                "poissondistribution" -> {
                    val mu = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    return MathValue.DistVal(PoissonDist(mu))
                }
                "binomialdistribution" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble()?.toInt() ?: 1
                    val p = evaluatedArgs.getOrNull(1)?.asDouble() ?: 0.5
                    return MathValue.DistVal(BinomialDist(n, p))
                }
                "studenttdistribution" -> {
                    val nu = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    return MathValue.DistVal(StudentTDist(nu))
                }
                "cauchydistribution" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(CauchyDist(a, b))
                }
                "paretodistribution" -> {
                    val k = if (evaluatedArgs.size >= 2) evaluatedArgs[0].asDouble() else 1.0
                    val alpha = if (evaluatedArgs.size >= 2) evaluatedArgs[1].asDouble() else (evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0)
                    return MathValue.DistVal(ParetoDist(k, alpha))
                }
                "levydistribution" -> {
                    val mu = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val c = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(LevyDist(mu, c))
                }
                "frechetdistribution" -> {
                    val alpha = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    val s = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    val m = evaluatedArgs.getOrNull(2)?.asDouble() ?: 0.0
                    return MathValue.DistVal(FrechetDist(alpha, s, m))
                }
                "weibulldistribution" -> {
                    val k = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    val lam = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(WeibullDist(k, lam))
                }
                "lognormaldistribution" -> {
                    val mu = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val sigma = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(LogNormalDist(mu, sigma))
                }
                "gammadistribution" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(GammaDist(a, b))
                }
                "betadistribution" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(BetaDist(a, b))
                }
                "geometricdistribution" -> {
                    val p = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.5
                    return MathValue.DistVal(GeometricDist(p))
                }
            }

            // Probability / Distribution operations
            when (fn) {
                "pdf" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in PDF")
                    val arg = evaluatedArgs.getOrNull(1) ?: error("Expected x in PDF")
                    return when (arg) {
                        is MathValue.SymbolVal -> MathValue.ExprVal(dist.pdfSymbolic(arg.name))
                        is MathValue.ExprVal -> MathValue.ExprVal(dist.pdfSymbolic(arg.text))
                        is MathValue.NumberVal -> MathValue.NumberVal(dist.pdf(arg.value))
                        is MathValue.ListVal -> MathValue.ListVal(arg.elements.map { dist.pdf(it) })
                        else -> MathValue.NumberVal(dist.pdf(arg.asDouble()))
                    }
                }
                "cdf" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in CDF")
                    val arg = evaluatedArgs.getOrNull(1) ?: error("Expected x in CDF")
                    return when (arg) {
                        is MathValue.SymbolVal -> MathValue.ExprVal(dist.cdfSymbolic(arg.name))
                        is MathValue.ExprVal -> MathValue.ExprVal(dist.cdfSymbolic(arg.text))
                        is MathValue.NumberVal -> MathValue.NumberVal(dist.cdf(arg.value))
                        is MathValue.ListVal -> MathValue.ListVal(arg.elements.map { dist.cdf(it) })
                        else -> MathValue.NumberVal(dist.cdf(arg.asDouble()))
                    }
                }
                "survivalfunction", "sf" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in SurvivalFunction")
                    val arg = evaluatedArgs.getOrNull(1) ?: error("Expected x in SurvivalFunction")
                    return when (arg) {
                        is MathValue.SymbolVal -> MathValue.ExprVal(dist.survivalSymbolic(arg.name))
                        is MathValue.ExprVal -> MathValue.ExprVal(dist.survivalSymbolic(arg.text))
                        is MathValue.NumberVal -> MathValue.NumberVal(dist.survival(arg.value))
                        is MathValue.ListVal -> MathValue.ListVal(arg.elements.map { dist.survival(it) })
                        else -> MathValue.NumberVal(dist.survival(arg.asDouble()))
                    }
                }
                "hazardfunction", "hf" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in HazardFunction")
                    val arg = evaluatedArgs.getOrNull(1) ?: error("Expected x in HazardFunction")
                    return when (arg) {
                        is MathValue.SymbolVal -> MathValue.ExprVal(dist.hazardSymbolic(arg.name))
                        is MathValue.ExprVal -> MathValue.ExprVal(dist.hazardSymbolic(arg.text))
                        is MathValue.NumberVal -> MathValue.NumberVal(dist.hazard(arg.value))
                        is MathValue.ListVal -> MathValue.ListVal(arg.elements.map { dist.hazard(it) })
                        else -> MathValue.NumberVal(dist.hazard(arg.asDouble()))
                    }
                }
                "inversecdf" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in InverseCDF")
                    val arg = evaluatedArgs.getOrNull(1) ?: error("Expected q in InverseCDF")
                    return when (arg) {
                        is MathValue.SymbolVal -> MathValue.ExprVal("InverseCDF[${dist.name}[${dist.paramsString}], ${arg.name}]")
                        is MathValue.ExprVal -> MathValue.ExprVal("InverseCDF[${dist.name}[${dist.paramsString}], ${arg.text}]")
                        is MathValue.NumberVal -> MathValue.NumberVal(dist.quantile(arg.value))
                        is MathValue.ListVal -> MathValue.ListVal(arg.elements.map { dist.quantile(it) })
                        else -> MathValue.NumberVal(dist.quantile(arg.asDouble()))
                    }
                }
                "expectation" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in Expectation")
                    return MathValue.NumberVal(dist.mean())
                }
                "moment" -> {
                    val first = evaluatedArgs.getOrNull(0)
                    val r = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: 1
                    if (first is MathValue.DistVal) {
                        val d = first.dist
                        return when (r) {
                            1 -> MathValue.NumberVal(d.mean())
                            2 -> MathValue.NumberVal(d.variance() + d.mean().pow(2))
                            else -> {
                                val samples = 5000
                                var sum = 0.0
                                for (i in 0 until samples) {
                                    sum += d.randomVariate(MathStatsEngine.rng).pow(r)
                                }
                                MathValue.NumberVal(sum / samples)
                            }
                        }
                    } else {
                        val list = first?.asList() ?: emptyList()
                        if (list.isEmpty()) return MathValue.NumberVal(Double.NaN)
                        val m = list.map { it.pow(r) }.average()
                        return MathValue.NumberVal(m)
                    }
                }
                "centralmoment" -> {
                    val first = evaluatedArgs.getOrNull(0)
                    val r = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: 1
                    if (first is MathValue.DistVal) {
                        val d = first.dist
                        return when (r) {
                            1 -> MathValue.NumberVal(0.0)
                            2 -> MathValue.NumberVal(d.variance())
                            else -> {
                                val mean = d.mean()
                                if (mean.isNaN()) MathValue.NumberVal(Double.NaN)
                                else {
                                    val samples = 5000
                                    var sum = 0.0
                                    for (i in 0 until samples) {
                                        sum += (d.randomVariate(MathStatsEngine.rng) - mean).pow(r)
                                    }
                                    MathValue.NumberVal(sum / samples)
                                }
                            }
                        }
                    } else {
                        val list = first?.asList() ?: emptyList()
                        if (list.isEmpty()) return MathValue.NumberVal(Double.NaN)
                        val avg = list.average()
                        val cm = list.map { (it - avg).pow(r) }.average()
                        return MathValue.NumberVal(cm)
                    }
                }
                "randomvariate" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in RandomVariate")
                    val n = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                    return if (n == null) {
                        MathValue.NumberVal(dist.randomVariate(MathStatsEngine.rng))
                    } else {
                        val count = n.coerceIn(0, 5000)
                        val list = (0 until count).map { dist.randomVariate(MathStatsEngine.rng) }
                        MathValue.ListVal(list)
                    }
                }
                "randominteger" -> {
                    var min = 0L
                    var max = 1L
                    var count: Int? = null

                    if (evaluatedArgs.isEmpty()) {
                        min = 0L
                        max = 1L
                    } else {
                        val first = evaluatedArgs[0]
                        if (first is MathValue.ListVal && first.elements.isNotEmpty()) {
                            if (first.elements.size >= 2) {
                                min = first.elements[0].toLong()
                                max = first.elements[1].toLong()
                            } else {
                                min = 0L
                                max = first.elements[0].toLong()
                            }
                            count = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                        } else {
                            val v = first.asDouble().toLong()
                            if (evaluatedArgs.size >= 2) {
                                min = 0L
                                max = v
                                count = evaluatedArgs[1].asDouble().toInt()
                            } else {
                                min = 0L
                                max = v
                            }
                        }
                    }

                    fun randInt(a: Long, b: Long): Long {
                        val low = minOf(a, b)
                        val high = maxOf(a, b)
                        if (low == high) return low
                        val range = high - low + 1L
                        val r = if (range in 1..Int.MAX_VALUE) MathStatsEngine.rng.nextInt(range.toInt()).toLong() else (MathStatsEngine.rng.nextDouble() * range).toLong()
                        return low + r
                    }

                    return if (count == null) {
                        MathValue.NumberVal(randInt(min, max).toDouble())
                    } else {
                        val n = count.coerceIn(0, 5000)
                        MathValue.ListVal((0 until n).map { randInt(min, max).toDouble() })
                    }
                }
                "randomreal" -> {
                    var min = 0.0
                    var max = 1.0
                    var count: Int? = null

                    if (evaluatedArgs.isEmpty()) {
                        min = 0.0
                        max = 1.0
                    } else {
                        val first = evaluatedArgs[0]
                        if (first is MathValue.ListVal && first.elements.isNotEmpty()) {
                            if (first.elements.size >= 2) {
                                min = first.elements[0]
                                max = first.elements[1]
                            } else {
                                min = 0.0
                                max = first.elements[0]
                            }
                            count = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                        } else {
                            val v = first.asDouble()
                            if (evaluatedArgs.size >= 2) {
                                min = 0.0
                                max = v
                                count = evaluatedArgs[1].asDouble().toInt()
                            } else {
                                min = 0.0
                                max = v
                            }
                        }
                    }

                    fun randReal(a: Double, b: Double): Double {
                        val low = minOf(a, b)
                        val high = maxOf(a, b)
                        return low + MathStatsEngine.rng.nextDouble() * (high - low)
                    }

                    return if (count == null) {
                        MathValue.NumberVal(randReal(min, max))
                    } else {
                        val n = count.coerceIn(0, 5000)
                        MathValue.ListVal((0 until n).map { randReal(min, max) })
                    }
                }
                "randomchoice" -> {
                    val first = evaluatedArgs.getOrNull(0) ?: error("Expected list in RandomChoice")
                    val list = first.asList()
                    if (list.isEmpty()) error("Empty list in RandomChoice")
                    val count = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                    return if (count == null) {
                        MathValue.NumberVal(list[MathStatsEngine.rng.nextInt(list.size)])
                    } else {
                        val n = count.coerceIn(0, 5000)
                        val res = (0 until n).map { list[MathStatsEngine.rng.nextInt(list.size)] }
                        MathValue.ListVal(res)
                    }
                }
                "randomsample" -> {
                    val first = evaluatedArgs.getOrNull(0) ?: error("Expected list in RandomSample")
                    val list = first.asList()
                    val count = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: list.size
                    val k = count.coerceIn(0, list.size)
                    val shuffled = list.toMutableList()
                    for (i in shuffled.lastIndex downTo 1) {
                        val j = MathStatsEngine.rng.nextInt(i + 1)
                        val tmp = shuffled[i]
                        shuffled[i] = shuffled[j]
                        shuffled[j] = tmp
                    }
                    return MathValue.ListVal(shuffled.take(k))
                }
            }

            // List creation & sequences
            when (fn) {
                "range" -> {
                    val n1 = evaluatedArgs.getOrNull(0)?.asDouble()?.toInt() ?: 1
                    val n2 = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                    val step = evaluatedArgs.getOrNull(2)?.asDouble()?.toInt() ?: 1
                    val list = mutableListOf<Double>()
                    if (n2 == null) {
                        val max = minOf(n1, 5000)
                        for (i in 1..max) list.add(i.toDouble())
                    } else {
                        if (step > 0 && n1 <= n2) {
                            var curr = n1
                            while (curr <= n2 && list.size < 5000) {
                                list.add(curr.toDouble())
                                curr += step
                            }
                        } else if (step < 0 && n1 >= n2) {
                            var curr = n1
                            while (curr >= n2 && list.size < 5000) {
                                list.add(curr.toDouble())
                                curr += step
                            }
                        }
                    }
                    return MathValue.ListVal(list)
                }
                "length", "count" -> {
                    val first = evaluatedArgs.getOrNull(0)
                    return if (first is MathValue.ListVal) {
                        if (evaluatedArgs.size > 1) {
                            val target = evaluatedArgs[1].asDouble()
                            MathValue.NumberVal(first.elements.count { abs(it - target) < 1e-9 }.toDouble())
                        } else {
                            MathValue.NumberVal(first.elements.size.toDouble())
                        }
                    } else MathValue.NumberVal(1.0)
                }
                "first" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    return MathValue.NumberVal(list.firstOrNull() ?: Double.NaN)
                }
                "last" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    return MathValue.NumberVal(list.lastOrNull() ?: Double.NaN)
                }
                "take" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val n = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: list.size
                    return if (n >= 0) MathValue.ListVal(list.take(n)) else MathValue.ListVal(list.takeLast(-n))
                }
                "drop" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val n = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: 0
                    return if (n >= 0) MathValue.ListVal(list.drop(n)) else MathValue.ListVal(list.dropLast(-n))
                }
                "reverse" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    return MathValue.ListVal(list.reversed())
                }
                "sort" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    return MathValue.ListVal(list.sorted())
                }
                "differences" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    if (list.size < 2) return MathValue.ListVal(emptyList())
                    val diffs = (0 until list.size - 1).map { list[it + 1] - list[it] }
                    return MathValue.ListVal(diffs)
                }
                "accumulate" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    var sum = 0.0
                    val acc = list.map { sum += it; sum }
                    return MathValue.ListVal(acc)
                }
                "deleteduplicates" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val unique = mutableListOf<Double>()
                    for (item in list) {
                        if (unique.none { abs(it - item) < 1e-11 }) {
                            unique.add(item)
                        }
                    }
                    return MathValue.ListVal(unique)
                }
            }

            // Statistics & Aggregations
            val firstArg = evaluatedArgs.getOrNull(0)
            if (firstArg is MathValue.DistVal) {
                val d = firstArg.dist
                when (fn) {
                    "mean" -> return MathValue.NumberVal(d.mean())
                    "median" -> return MathValue.NumberVal(d.median())
                    "variance", "var" -> return MathValue.NumberVal(d.variance())
                    "standarddeviation", "stddev", "sd" -> return MathValue.NumberVal(d.standardDeviation())
                    "quantile" -> {
                        val arg = evaluatedArgs.getOrNull(1) ?: MathValue.NumberVal(0.5)
                        return when (arg) {
                            is MathValue.NumberVal -> MathValue.NumberVal(d.quantile(arg.value))
                            is MathValue.ListVal -> MathValue.ListVal(arg.elements.map { d.quantile(it) })
                            else -> MathValue.NumberVal(d.quantile(arg.asDouble()))
                        }
                    }
                }
            }

            val numbers: List<Double> = when (firstArg) {
                is MathValue.ListVal -> firstArg.elements
                is MathValue.NumberVal -> evaluatedArgs.map { it.asDouble() }
                else -> emptyList()
            }

            when (fn) {
                "total", "sum" -> return MathValue.NumberVal(numbers.sum())
                "mean" -> return MathValue.NumberVal(if (numbers.isEmpty()) Double.NaN else numbers.average())
                "median" -> {
                    if (numbers.isEmpty()) return MathValue.NumberVal(Double.NaN)
                    val s = numbers.sorted()
                    val m = if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2.0
                    return MathValue.NumberVal(m)
                }
                "variance", "var" -> {
                    if (numbers.size < 2) return MathValue.NumberVal(Double.NaN)
                    val avg = numbers.average()
                    val sumSq = numbers.sumOf { (it - avg).pow(2) }
                    return MathValue.NumberVal(sumSq / (numbers.size - 1))
                }
                "standarddeviation", "stddev", "sd" -> {
                    if (numbers.size < 2) return MathValue.NumberVal(Double.NaN)
                    val avg = numbers.average()
                    val sumSq = numbers.sumOf { (it - avg).pow(2) }
                    return MathValue.NumberVal(sqrt(sumSq / (numbers.size - 1)))
                }
                "quantile" -> {
                    if (numbers.isEmpty()) return MathValue.NumberVal(Double.NaN)
                    val q = evaluatedArgs.getOrNull(1)?.asDouble()?.coerceIn(0.0, 1.0) ?: 0.5
                    val s = numbers.sorted()
                    val pos = q * (s.size - 1)
                    val low = floor(pos).toInt()
                    val high = ceil(pos).toInt()
                    val weight = pos - low
                    val res = s[low] * (1.0 - weight) + s[high] * weight
                    return MathValue.NumberVal(res)
                }
                "percentile" -> {
                    if (numbers.isEmpty()) return MathValue.NumberVal(Double.NaN)
                    val p = (evaluatedArgs.getOrNull(1)?.asDouble() ?: 50.0) / 100.0
                    val q = p.coerceIn(0.0, 1.0)
                    val s = numbers.sorted()
                    val pos = q * (s.size - 1)
                    val low = floor(pos).toInt()
                    val high = ceil(pos).toInt()
                    val weight = pos - low
                    val res = s[low] * (1.0 - weight) + s[high] * weight
                    return MathValue.NumberVal(res)
                }
                "interquartilerange", "iqr" -> {
                    if (numbers.isEmpty()) return MathValue.NumberVal(Double.NaN)
                    val s = numbers.sorted()
                    fun qAt(q: Double): Double {
                        val pos = q * (s.size - 1)
                        val low = floor(pos).toInt()
                        val high = ceil(pos).toInt()
                        return s[low] * (1.0 - (pos - low)) + s[high] * (pos - low)
                    }
                    return MathValue.NumberVal(qAt(0.75) - qAt(0.25))
                }
                "geometricmean" -> {
                    if (numbers.isEmpty() || numbers.any { it <= 0.0 }) return MathValue.NumberVal(Double.NaN)
                    val avgLn = numbers.map { ln(it) }.average()
                    return MathValue.NumberVal(exp(avgLn))
                }
                "harmonicmean" -> {
                    if (numbers.isEmpty() || numbers.any { it <= 0.0 }) return MathValue.NumberVal(Double.NaN)
                    val sumInv = numbers.sumOf { 1.0 / it }
                    return MathValue.NumberVal(numbers.size / sumInv)
                }
                "rootmeansquare", "rms" -> {
                    if (numbers.isEmpty()) return MathValue.NumberVal(Double.NaN)
                    val meanSq = numbers.map { it * it }.average()
                    return MathValue.NumberVal(sqrt(meanSq))
                }
                "skewness" -> {
                    if (numbers.size < 3) return MathValue.NumberVal(Double.NaN)
                    val avg = numbers.average()
                    val m2 = numbers.map { (it - avg).pow(2) }.average()
                    val m3 = numbers.map { (it - avg).pow(3) }.average()
                    val s = sqrt(m2)
                    return MathValue.NumberVal(if (s == 0.0) 0.0 else m3 / s.pow(3))
                }
                "kurtosis" -> {
                    if (numbers.size < 4) return MathValue.NumberVal(Double.NaN)
                    val avg = numbers.average()
                    val m2 = numbers.map { (it - avg).pow(2) }.average()
                    val m4 = numbers.map { (it - avg).pow(4) }.average()
                    return MathValue.NumberVal(if (m2 == 0.0) 0.0 else (m4 / (m2 * m2)) - 3.0)
                }
                "covariance" -> {
                    val l1 = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val l2 = evaluatedArgs.getOrNull(1)?.asList() ?: emptyList()
                    val n = minOf(l1.size, l2.size)
                    if (n < 2) return MathValue.NumberVal(Double.NaN)
                    val a1 = l1.take(n).average()
                    val a2 = l2.take(n).average()
                    var cov = 0.0
                    for (i in 0 until n) cov += (l1[i] - a1) * (l2[i] - a2)
                    return MathValue.NumberVal(cov / (n - 1))
                }
                "correlation" -> {
                    val l1 = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val l2 = evaluatedArgs.getOrNull(1)?.asList() ?: emptyList()
                    val n = minOf(l1.size, l2.size)
                    if (n < 2) return MathValue.NumberVal(Double.NaN)
                    val a1 = l1.take(n).average()
                    val a2 = l2.take(n).average()
                    var cov = 0.0
                    var v1 = 0.0
                    var v2 = 0.0
                    for (i in 0 until n) {
                        val d1 = l1[i] - a1
                        val d2 = l2[i] - a2
                        cov += d1 * d2
                        v1 += d1 * d1
                        v2 += d2 * d2
                    }
                    val denom = sqrt(v1 * v2)
                    return MathValue.NumberVal(if (denom == 0.0) 0.0 else cov / denom)
                }
                "standardize" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    if (list.size < 2) return MathValue.ListVal(list)
                    val avg = list.average()
                    val variance = list.sumOf { (it - avg).pow(2) } / (list.size - 1)
                    val sd = sqrt(variance)
                    if (sd == 0.0) return MathValue.ListVal(list.map { 0.0 })
                    return MathValue.ListVal(list.map { (it - avg) / sd })
                }
                "movingaverage" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val r = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: 1
                    if (r < 1 || r > list.size) return MathValue.ListVal(emptyList())
                    val ma = mutableListOf<Double>()
                    var windowSum = (0 until r).sumOf { list[it] }
                    ma.add(windowSum / r)
                    for (i in r until list.size) {
                        windowSum += list[i] - list[i - r]
                        ma.add(windowSum / r)
                    }
                    return MathValue.ListVal(ma)
                }
                "maxdrawdown" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    if (list.isEmpty()) return MathValue.NumberVal(0.0)
                    var peak = list[0]
                    var maxDd = 0.0
                    for (p in list) {
                        if (p > peak) peak = p
                        val dd = if (peak != 0.0) (peak - p) / abs(peak) else 0.0
                        if (dd > maxDd) maxDd = dd
                    }
                    return MathValue.NumberVal(maxDd)
                }
                "hillestimator" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val pos = list.filter { it > 0.0 }.sortedDescending()
                    if (pos.size < 3) return MathValue.NumberVal(Double.NaN)
                    val kArg = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                    val k = (kArg ?: maxOf(2, minOf(pos.size / 4, 100))).coerceIn(1, pos.size - 1)
                    val thresh = pos[k]
                    val logThresh = ln(thresh)
                    var sumLog = 0.0
                    for (i in 0 until k) {
                        sumLog += (ln(pos[i]) - logThresh)
                    }
                    val gamma = sumLog / k
                    if (gamma <= 0.0) return MathValue.NumberVal(Double.NaN)
                    val alpha = 1.0 / gamma
                    return MathValue.NumberVal(alpha)
                }
                "expectedshortfall", "cvar" -> {
                    val first = evaluatedArgs.getOrNull(0)
                    val q = evaluatedArgs.getOrNull(1)?.asDouble() ?: 0.95
                    if (first is MathValue.DistVal) {
                        val d = first.dist
                        val qClamped = q.coerceIn(0.001, 0.999)
                        val steps = 100
                        if (qClamped >= 0.5) {
                            val stepSize = (1.0 - qClamped) / steps
                            var sum = 0.0
                            for (i in 0 until steps) {
                                val mid = qClamped + (i + 0.5) * stepSize
                                sum += d.quantile(mid)
                            }
                            return MathValue.NumberVal(sum / steps)
                        } else {
                            val stepSize = qClamped / steps
                            var sum = 0.0
                            for (i in 0 until steps) {
                                val mid = (i + 0.5) * stepSize
                                sum += d.quantile(mid)
                            }
                            return MathValue.NumberVal(sum / steps)
                        }
                    } else {
                        val list = first?.asList() ?: emptyList()
                        if (list.isEmpty()) return MathValue.NumberVal(Double.NaN)
                        val sorted = list.sorted()
                        val qClamped = q.coerceIn(0.0, 1.0)
                        val pos = qClamped * (sorted.size - 1)
                        val low = floor(pos).toInt().coerceIn(0, sorted.size - 1)
                        val high = ceil(pos).toInt().coerceIn(0, sorted.size - 1)
                        val weight = pos - low
                        val qVal = if (low == high) sorted[low] else sorted[low] * (1.0 - weight) + sorted[high] * weight

                        val tail = if (qClamped >= 0.5) {
                            sorted.filter { it >= qVal - 1e-9 }
                        } else {
                            sorted.filter { it <= qVal + 1e-9 }
                        }
                        val es = if (tail.isNotEmpty()) tail.average() else qVal
                        return MathValue.NumberVal(es)
                    }
                }
                "min" -> return MathValue.NumberVal(numbers.minOrNull() ?: Double.NaN)
                "max" -> return MathValue.NumberVal(numbers.maxOrNull() ?: Double.NaN)
            }

            // Element-wise unary functions
            fun applyUnary(func: (Double) -> Double): MathValue {
                val first = evaluatedArgs.getOrNull(0) ?: return MathValue.NumberVal(Double.NaN)
                return when (first) {
                    is MathValue.NumberVal -> MathValue.NumberVal(func(first.value))
                    is MathValue.ListVal -> MathValue.ListVal(first.elements.map(func))
                    else -> MathValue.NumberVal(Double.NaN)
                }
            }

            when (fn) {
                "sqrt" -> return applyUnary { sqrt(it) }
                "cuberoot" -> return applyUnary { if (it >= 0.0) it.pow(1.0 / 3.0) else -(-it).pow(1.0 / 3.0) }
                "abs" -> return applyUnary { abs(it) }
                "sign" -> return applyUnary { sign(it) }
                "round" -> {
                    val step = evaluatedArgs.getOrNull(1)?.asDouble()
                    return if (step != null && step > 0.0) applyUnary { kotlin.math.round(it / step) * step }
                    else applyUnary { kotlin.math.round(it) }
                }
                "floor" -> return applyUnary { floor(it) }
                "ceiling", "ceil" -> return applyUnary { ceil(it) }
                "sin" -> return applyUnary { sin(it) }
                "cos" -> return applyUnary { cos(it) }
                "tan" -> return applyUnary { tan(it) }
                "cot" -> return applyUnary { 1.0 / tan(it) }
                "sec" -> return applyUnary { 1.0 / cos(it) }
                "csc" -> return applyUnary { 1.0 / sin(it) }
                "arcsin", "asin" -> return applyUnary { asin(it) }
                "arccos", "acos" -> return applyUnary { acos(it) }
                "arctan", "atan" -> {
                    if (evaluatedArgs.size >= 2) {
                        val y = evaluatedArgs[0].asDouble()
                        val x = evaluatedArgs[1].asDouble()
                        return MathValue.NumberVal(atan2(y, x))
                    }
                    return applyUnary { atan(it) }
                }
                "sinh" -> return applyUnary { sinh(it) }
                "cosh" -> return applyUnary { cosh(it) }
                "tanh" -> return applyUnary { tanh(it) }
                "deg" -> return applyUnary { it * 180.0 / PI }
                "rad" -> return applyUnary { it * PI / 180.0 }
                "exp" -> return applyUnary { exp(it) }
                "log", "ln" -> {
                    if (evaluatedArgs.size >= 2) {
                        val b = evaluatedArgs[0].asDouble()
                        val x = evaluatedArgs[1].asDouble()
                        return MathValue.NumberVal(ln(x) / ln(b))
                    }
                    return applyUnary { ln(it) }
                }
                "log2" -> return applyUnary { ln(it) / ln(2.0) }
                "log10" -> return applyUnary { log10(it) }
                "mod" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.NumberVal(a % b)
                }
                "quotient" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.NumberVal(floor(a / b))
                }
                "divisible" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble()?.toLong() ?: 0L
                    val b = evaluatedArgs.getOrNull(1)?.asDouble()?.toLong() ?: 1L
                    return MathValue.BooleanVal(b != 0L && a % b == 0L)
                }
                "gcd" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble()?.toLong() ?: 0L
                    val b = evaluatedArgs.getOrNull(1)?.asDouble()?.toLong() ?: 0L
                    return MathValue.NumberVal(MathStatsEngine.gcd(a, b).toDouble())
                }
                "lcm" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble()?.toLong() ?: 0L
                    val b = evaluatedArgs.getOrNull(1)?.asDouble()?.toLong() ?: 0L
                    return MathValue.NumberVal(MathStatsEngine.lcm(a, b).toDouble())
                }
                "factorial" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val k = kotlin.math.round(n).toInt()
                    return MathValue.NumberVal(if (k < 0 || abs(n - k) > 1e-9) Double.NaN else MathStatsEngine.exactFactorial(k))
                }
                "binomial" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble()?.toInt() ?: 0
                    val k = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: 0
                    return MathValue.NumberVal(MathStatsEngine.exactBinomial(n, k))
                }
                "primeq" -> {
                    val d = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    if (abs(d) > 1e14) return MathValue.BooleanVal(false)
                    val n = d.toLong()
                    return MathValue.BooleanVal(MathStatsEngine.isPrime(n))
                }
                "nextprime" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble()?.toLong() ?: 0L
                    if (n < 0 || n >= 1_000_000_000_000L) return MathValue.NumberVal(Double.NaN)
                    val np = MathStatsEngine.nextPrime(n)
                    return MathValue.NumberVal(if (np == -1L) Double.NaN else np.toDouble())
                }
                "fibonacci" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble()?.toInt() ?: 0
                    if (n < 0 || n > 92) return MathValue.NumberVal(Double.NaN)
                    val fib = MathStatsEngine.fibonacci(n)
                    return MathValue.NumberVal(if (fib < 0) Double.NaN else fib.toDouble())
                }
                "clip" -> {
                    val x = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    var min = 0.0
                    var max = 1.0
                    val second = evaluatedArgs.getOrNull(1)
                    if (second is MathValue.ListVal && second.elements.size >= 2) {
                        min = second.elements[0]
                        max = second.elements[1]
                    } else {
                        min = second?.asDouble() ?: 0.0
                        max = evaluatedArgs.getOrNull(2)?.asDouble() ?: 1.0
                    }
                    return MathValue.NumberVal(x.coerceIn(min, max))
                }
                "plus" -> return MathValue.NumberVal(evaluatedArgs.sumOf { it.asDouble() })
                "subtract" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 0.0
                    return MathValue.NumberVal(a - b)
                }
                "times" -> {
                    var prod = 1.0
                    evaluatedArgs.forEach { prod *= it.asDouble() }
                    return MathValue.NumberVal(prod)
                }
                "divide" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.NumberVal(a / b)
                }
                "power" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.NumberVal(a.pow(b))
                }
            }

            error("Unknown function call: $funcName")
        }
    }
}

internal class WolframParser(private val tokens: List<Token>) {
    private var idx = 0
    private var depth = 0
    private val MAX_DEPTH = 40

    private inline fun <T> withDepth(block: () -> T): T {
        depth++
        if (depth > MAX_DEPTH) error("Recursion depth limit exceeded ($MAX_DEPTH)")
        try {
            return block()
        } finally {
            depth--
        }
    }

    private fun peek(): Token = if (idx < tokens.size) tokens[idx] else Token(TokenType.EOF, "")

    private fun match(type: TokenType): Boolean {
        if (peek().type == type) {
            idx++
            return true
        }
        return false
    }

    private fun consume(type: TokenType): Token {
        val t = peek()
        if (t.type == type) {
            idx++
            return t
        }
        error("Expected $type but got ${t.type}")
    }

    fun parse(): MathNode? {
        val expr = parseExpression()
        if (peek().type != TokenType.EOF) return null
        return expr
    }

    private fun parseExpression(): MathNode = withDepth {
        var node = parseTerm()
        while (peek().type in setOf(TokenType.PLUS, TokenType.MINUS)) {
            val op = consume(peek().type).text[0]
            val right = parseTerm()
            node = MathNode.BinaryOpNode(op, node, right)
        }
        node
    }

    private fun parseTerm(): MathNode = withDepth {
        var node = parseUnary()
        while (peek().type in setOf(TokenType.STAR, TokenType.SLASH) ||
            (peek().type == TokenType.PERCENT && isBinaryPercent()) ||
            canStartImplicitMult()
        ) {
            val op = if (peek().type in setOf(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
                consume(peek().type).text[0]
            } else {
                '*' // implicit multiplication
            }
            val right = parseUnary()
            node = MathNode.BinaryOpNode(op, node, right)
        }
        node
    }

    private fun isBinaryPercent(): Boolean {
        if (idx + 1 >= tokens.size) return false
        val nextType = tokens[idx + 1].type
        return nextType in setOf(
            TokenType.NUMBER, TokenType.IDENT, TokenType.LPAREN, TokenType.LBRACKET, TokenType.LBRACE
        )
    }

    private fun canStartImplicitMult(): Boolean {
        val t = peek().type
        return t in setOf(TokenType.IDENT, TokenType.LPAREN, TokenType.LBRACKET, TokenType.LBRACE)
    }

    private fun parseUnary(): MathNode = withDepth {
        if (match(TokenType.PLUS)) return@withDepth parseUnary()
        if (match(TokenType.MINUS)) return@withDepth MathNode.UnaryOpNode('-', parseUnary())
        parsePower()
    }

    private fun parsePower(): MathNode = withDepth {
        var node = parsePostfix()
        if (match(TokenType.CARET)) {
            val right = parseUnary() // right-associative power
            node = MathNode.BinaryOpNode('^', node, right)
        }
        node
    }

    private fun parsePostfix(): MathNode = withDepth {
        var node = parsePrimary()
        while (match(TokenType.EXCLAMATION)) {
            node = MathNode.UnaryOpNode('!', node)
        }
        node
    }

    private fun parsePrimary(): MathNode = withDepth {
        val t = peek()
        when (t.type) {
            TokenType.NUMBER -> {
                consume(TokenType.NUMBER)
                MathNode.NumberNode(t.value)
            }
            TokenType.PERCENT -> {
                consume(TokenType.PERCENT)
                MathNode.OutNode
            }
            TokenType.IDENT -> {
                val name = consume(TokenType.IDENT).text
                if (match(TokenType.LBRACKET)) {
                    val args = parseArgList(TokenType.RBRACKET)
                    MathNode.CallNode(name, args)
                } else if (match(TokenType.LPAREN)) {
                    val args = parseArgList(TokenType.RPAREN)
                    MathNode.CallNode(name, args)
                } else {
                    val lower = name.lowercase(Locale.ROOT)
                    if (lower == "true") MathNode.BooleanNode(true)
                    else if (lower == "false") MathNode.BooleanNode(false)
                    else if (lower == "%" || lower == "out") MathNode.OutNode
                    else MathNode.IdentNode(name)
                }
            }
            TokenType.LPAREN -> {
                consume(TokenType.LPAREN)
                val expr = parseExpression()
                consume(TokenType.RPAREN)
                expr
            }
            TokenType.LBRACE -> {
                consume(TokenType.LBRACE)
                val items = parseArgList(TokenType.RBRACE)
                MathNode.ListNode(items)
            }
            else -> error("Unexpected token: ${t.text}")
        }
    }

    private fun parseArgList(closingType: TokenType): List<MathNode> = withDepth {
        val list = mutableListOf<MathNode>()
        if (match(closingType)) return@withDepth list
        while (true) {
            list.add(parseExpression())
            if (match(TokenType.COMMA)) continue
            consume(closingType)
            break
        }
        list
    }
}
