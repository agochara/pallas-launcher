package com.pallas.launcher.domain.search

import java.util.Locale
import kotlin.math.*

// =========================================================================
// Wolfram AST, Lexer & Parser
// =========================================================================

internal enum class TokenType {
    NUMBER, IDENT, LBRACKET, RBRACKET, LBRACE, RBRACE, LPAREN, RPAREN,
    COMMA, PLUS, MINUS, STAR, SLASH, CARET, PERCENT, EXCLAMATION,
    AT, // @
    POSTFIX, // //
    MAP, // /@
    APPLY, // @@
    APPLY_LEVEL, // @@@
    RIGHT_COMPOSITION, // /*
    ARROW, // -> or →
    REPLACE_ALL, // /.
    REPLACE_REPEATED, // //.
    AMP, // &
    SLOT, // #, #1, #2...
    SEMICOLON, // ;
    EQUAL, // =
    PLUS_EQUAL, // +=
    MINUS_EQUAL, // -=
    STAR_EQUAL, // *=
    SLASH_EQUAL, // /=
    PLUS_PLUS, // ++
    MINUS_MINUS, // --
    EQUAL_EQUAL, // ==
    NOT_EQUAL, // != or ≠
    LESS, // <
    LESS_EQUAL, // <= or ≤
    GREATER, // >
    GREATER_EQUAL, // >= or ≥
    AND, // &&
    OR, // ||
    EOF
}

internal data class Token(
    val type: TokenType,
    val text: String,
    val value: Double = 0.0,
    val intVal: Int = 0
)

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
                c == '@' -> {
                    if (input.startsWith("@@@", pos)) {
                        tokens.add(Token(TokenType.APPLY_LEVEL, "@@@"))
                        pos += 3
                    } else if (input.startsWith("@@", pos)) {
                        tokens.add(Token(TokenType.APPLY, "@@"))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.AT, "@"))
                        pos += 1
                    }
                }
                c == '/' || c == '÷' -> {
                    if (input.startsWith("//.", pos)) {
                        tokens.add(Token(TokenType.REPLACE_REPEATED, "//."))
                        pos += 3
                    } else if (input.startsWith("//", pos)) {
                        tokens.add(Token(TokenType.POSTFIX, "//"))
                        pos += 2
                    } else if (input.startsWith("/@", pos)) {
                        tokens.add(Token(TokenType.MAP, "/@"))
                        pos += 2
                    } else if (input.startsWith("/.", pos)) {
                        tokens.add(Token(TokenType.REPLACE_ALL, "/."))
                        pos += 2
                    } else if (input.startsWith("/*", pos)) {
                        tokens.add(Token(TokenType.RIGHT_COMPOSITION, "/*"))
                        pos += 2
                    } else if (input.startsWith("/=", pos)) {
                        tokens.add(Token(TokenType.SLASH_EQUAL, "/="))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.SLASH, "/"))
                        pos += 1
                    }
                }
                c == '-' -> {
                    if (input.startsWith("->", pos)) {
                        tokens.add(Token(TokenType.ARROW, "->"))
                        pos += 2
                    } else if (input.startsWith("-=", pos)) {
                        tokens.add(Token(TokenType.MINUS_EQUAL, "-="))
                        pos += 2
                    } else if (input.startsWith("--", pos)) {
                        tokens.add(Token(TokenType.MINUS_MINUS, "--"))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.MINUS, "-"))
                        pos += 1
                    }
                }
                c == '→' -> {
                    tokens.add(Token(TokenType.ARROW, "→"))
                    pos += 1
                }
                c == '+' -> {
                    if (input.startsWith("+=", pos)) {
                        tokens.add(Token(TokenType.PLUS_EQUAL, "+="))
                        pos += 2
                    } else if (input.startsWith("++", pos)) {
                        tokens.add(Token(TokenType.PLUS_PLUS, "++"))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.PLUS, "+"))
                        pos += 1
                    }
                }
                c == '*' || c == '×' -> {
                    if (input.startsWith("*=", pos)) {
                        tokens.add(Token(TokenType.STAR_EQUAL, "*="))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.STAR, "*"))
                        pos += 1
                    }
                }
                c == '&' -> {
                    if (input.startsWith("&&", pos)) {
                        tokens.add(Token(TokenType.AND, "&&"))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.AMP, "&"))
                        pos += 1
                    }
                }
                c == '|' -> {
                    if (input.startsWith("||", pos)) {
                        tokens.add(Token(TokenType.OR, "||"))
                        pos += 2
                    } else {
                        pos++
                    }
                }
                c == '=' -> {
                    if (input.startsWith("==", pos)) {
                        tokens.add(Token(TokenType.EQUAL_EQUAL, "=="))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.EQUAL, "="))
                        pos += 1
                    }
                }
                c == '!' -> {
                    if (input.startsWith("!=", pos)) {
                        tokens.add(Token(TokenType.NOT_EQUAL, "!="))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.EXCLAMATION, "!"))
                        pos += 1
                    }
                }
                c == '≠' -> {
                    tokens.add(Token(TokenType.NOT_EQUAL, "≠"))
                    pos += 1
                }
                c == '<' -> {
                    if (input.startsWith("<=", pos)) {
                        tokens.add(Token(TokenType.LESS_EQUAL, "<="))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.LESS, "<"))
                        pos += 1
                    }
                }
                c == '≤' -> {
                    tokens.add(Token(TokenType.LESS_EQUAL, "≤"))
                    pos += 1
                }
                c == '>' -> {
                    if (input.startsWith(">=", pos)) {
                        tokens.add(Token(TokenType.GREATER_EQUAL, ">="))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.GREATER, ">"))
                        pos += 1
                    }
                }
                c == '≥' -> {
                    tokens.add(Token(TokenType.GREATER_EQUAL, "≥"))
                    pos += 1
                }
                c == '[' -> { tokens.add(Token(TokenType.LBRACKET, "[")); pos++ }
                c == ']' -> { tokens.add(Token(TokenType.RBRACKET, "]")); pos++ }
                c == '{' -> { tokens.add(Token(TokenType.LBRACE, "{")); pos++ }
                c == '}' -> { tokens.add(Token(TokenType.RBRACE, "}")); pos++ }
                c == '(' -> { tokens.add(Token(TokenType.LPAREN, "(")); pos++ }
                c == ')' -> { tokens.add(Token(TokenType.RPAREN, ")")); pos++ }
                c == ',' -> { tokens.add(Token(TokenType.COMMA, ",")); pos++ }
                c == ';' -> { tokens.add(Token(TokenType.SEMICOLON, ";")); pos++ }
                c == '^' -> { tokens.add(Token(TokenType.CARET, "^")); pos++ }
                c == '%' -> { tokens.add(Token(TokenType.PERCENT, "%")); pos++ }
                c == '#' -> {
                    pos++
                    val start = pos
                    while (pos < input.length && input[pos].isDigit()) pos++
                    val slotNum = if (pos > start) input.substring(start, pos).toIntOrNull() ?: 1 else 1
                    tokens.add(Token(TokenType.SLOT, input.substring(start - 1, pos), intVal = slotNum))
                }
                c.isLetter() || c == 'π' || c == '∞' || c == '_' -> {
                    val start = pos
                    while (pos < input.length && (input[pos].isLetter() || input[pos].isDigit() || input[pos] == '_' || input[pos] == 'π' || input[pos] == '∞')) {
                        pos++
                    }
                    val str = input.substring(start, pos)
                    tokens.add(Token(TokenType.IDENT, str))
                }
                else -> pos++
            }
        }
        tokens.add(Token(TokenType.EOF, ""))
        return tokens
    }
}

// Execution Exceptions for Control Flow and Safety
class ComputationLimitException(message: String) : RuntimeException(message)
internal class BreakException : RuntimeException()
internal class ContinueException : RuntimeException()
internal class ReturnException(val value: MathValue) : RuntimeException()

class ExecutionContext(
    val maxSteps: Int = 10_000,
    val timeoutMs: Long = 150L,
    val env: MutableMap<String, MathValue> = mutableMapOf()
) {
    val startTime = System.currentTimeMillis()
    var steps = 0

    fun tick() {
        steps++
        if (steps > maxSteps) {
            throw ComputationLimitException("Computation exceeded step limit ($maxSteps steps)")
        }
        if ((steps and 63) == 0) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw ComputationLimitException("Computation timed out (> ${timeoutMs}ms)")
            }
        }
    }
}

sealed interface MathNode {
    fun eval(ctx: ExecutionContext): MathValue

    fun eval(env: Map<String, MathValue> = emptyMap()): MathValue {
        val ctx = ExecutionContext(env = env.toMutableMap())
        return eval(ctx)
    }

    data class NumberNode(val value: Double) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return MathValue.NumberVal(value)
        }
    }

    data class BooleanNode(val value: Boolean) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return MathValue.BooleanVal(value)
        }
    }

    data class ConstantValueNode(val value: MathValue) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return value
        }
    }

    object NullNode : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return MathValue.NullVal
        }
    }

    object OutNode : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return ctx.env["%"] ?: ctx.env["out"] ?: MathStatsEngine.lastEvaluatedValue ?: MathValue.NumberVal(0.0)
        }
    }

    data class SlotNode(val index: Int = 1) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return ctx.env["#$index"] ?: if (index == 1) ctx.env["#"] ?: MathValue.NumberVal(0.0) else MathValue.NumberVal(0.0)
        }
    }

    data class IdentNode(val name: String) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val lower = name.lowercase(Locale.ROOT)
            ctx.env[lower]?.let { return it }
            return when (lower) {
                "pi", "π" -> MathValue.NumberVal(PI)
                "e" -> MathValue.NumberVal(E)
                "infinity", "inf", "∞" -> MathValue.NumberVal(Double.POSITIVE_INFINITY)
                "true" -> MathValue.BooleanVal(true)
                "false" -> MathValue.BooleanVal(false)
                "null" -> MathValue.NullVal
                "%", "out" -> ctx.env["%"] ?: ctx.env["out"] ?: MathStatsEngine.lastEvaluatedValue ?: MathValue.NumberVal(0.0)
                else -> MathValue.SymbolVal(name)
            }
        }
    }

    data class ListNode(val items: List<MathNode>) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val list = mutableListOf<Double>()
            items.forEach { node ->
                when (val v = node.eval(ctx)) {
                    is MathValue.NumberVal -> list.add(v.value)
                    is MathValue.ListVal -> list.addAll(v.elements)
                    is MathValue.BooleanVal -> list.add(if (v.value) 1.0 else 0.0)
                    else -> {}
                }
            }
            return MathValue.ListVal(list)
        }
    }

    data class CompoundNode(val expressions: List<MathNode>) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            if (expressions.isEmpty()) return MathValue.NullVal
            var last: MathValue = MathValue.NullVal
            for (expr in expressions) {
                last = expr.eval(ctx)
            }
            return last
        }
    }

    data class AssignNode(val varName: String, val expr: MathNode, val op: Char? = null) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val lower = varName.lowercase(Locale.ROOT)
            val rhs = expr.eval(ctx)
            val finalVal: MathValue = when (op) {
                null -> rhs
                '+' -> {
                    val cur = ctx.env[lower]?.asDouble() ?: 0.0
                    MathValue.NumberVal(cur + rhs.asDouble())
                }
                '-' -> {
                    val cur = ctx.env[lower]?.asDouble() ?: 0.0
                    MathValue.NumberVal(cur - rhs.asDouble())
                }
                '*' -> {
                    val cur = ctx.env[lower]?.asDouble() ?: 1.0
                    MathValue.NumberVal(cur * rhs.asDouble())
                }
                '/' -> {
                    val cur = ctx.env[lower]?.asDouble() ?: 0.0
                    MathValue.NumberVal(cur / rhs.asDouble())
                }
                else -> rhs
            }
            ctx.env[lower] = finalVal
            return finalVal
        }
    }

    data class PrefixIncDecNode(val op: String, val varName: String) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val lower = varName.lowercase(Locale.ROOT)
            val cur = ctx.env[lower]?.asDouble() ?: 0.0
            val next = if (op == "++") cur + 1.0 else cur - 1.0
            val res = MathValue.NumberVal(next)
            ctx.env[lower] = res
            return res
        }
    }

    data class PostfixIncDecNode(val op: String, val varName: String) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val lower = varName.lowercase(Locale.ROOT)
            val cur = ctx.env[lower]?.asDouble() ?: 0.0
            val next = if (op == "++") cur + 1.0 else cur - 1.0
            ctx.env[lower] = MathValue.NumberVal(next)
            return MathValue.NumberVal(cur)
        }
    }

    data class RuleNode(val pattern: MathNode, val replacement: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val varName = when (pattern) {
                is IdentNode -> pattern.name
                else -> pattern.eval(ctx).toDisplayString()
            }
            val repVal = replacement.eval(ctx)
            return MathValue.RuleVal(varName, repVal)
        }
    }

    data class ReplaceAllNode(val expr: MathNode, val ruleNode: MathNode, val repeated: Boolean = false) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val rules = mutableMapOf<String, MathValue>()
            fun collectRules(rVal: MathValue) {
                when (rVal) {
                    is MathValue.RuleVal -> rules[rVal.lhs.lowercase(Locale.ROOT)] = rVal.rhs
                    else -> {}
                }
            }
            when (ruleNode) {
                is ListNode -> ruleNode.items.forEach { collectRules(it.eval(ctx)) }
                else -> collectRules(ruleNode.eval(ctx))
            }

            val saved = mutableMapOf<String, MathValue?>()
            try {
                for ((k, v) in rules) {
                    saved[k] = ctx.env[k]
                    ctx.env[k] = v
                }
                return expr.eval(ctx)
            } finally {
                for ((k, v) in saved) {
                    if (v != null) ctx.env[k] = v else ctx.env.remove(k)
                }
            }
        }
    }

    data class FunctionNode(val body: MathNode, val paramNames: List<String> = emptyList()) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return MathValue.FunctionVal(body, paramNames)
        }
    }

    data class PrefixNode(val func: MathNode, val arg: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val argVal = arg.eval(ctx)
            return invokeFunction(func, listOf(argVal), ctx)
        }
    }

    data class PostfixNode(val arg: MathNode, val func: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val argVal = arg.eval(ctx)
            return invokeFunction(func, listOf(argVal), ctx)
        }
    }

    data class MapNode(val func: MathNode, val listNode: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val lVal = listNode.eval(ctx)
            val elements = lVal.asList()
            val res = mutableListOf<Double>()
            for (item in elements) {
                ctx.tick()
                val out = invokeFunction(func, listOf(MathValue.NumberVal(item)), ctx)
                res.add(out.asDouble())
            }
            return MathValue.ListVal(res)
        }
    }

    data class ApplyNode(val func: MathNode, val exprNode: MathNode, val level1: Boolean = false) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val eVal = exprNode.eval(ctx)
            val args = eVal.asList().map { MathValue.NumberVal(it) }
            return invokeFunction(func, args, ctx)
        }
    }

    data class PartNode(val expr: MathNode, val index: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val eVal = expr.eval(ctx)
            val idxVal = index.eval(ctx).asDouble().toInt()
            val list = eVal.asList()
            if (list.isEmpty()) return MathValue.NumberVal(Double.NaN)
            val targetIdx = if (idxVal > 0) idxVal - 1 else if (idxVal < 0) list.size + idxVal else -1
            if (targetIdx in list.indices) {
                return MathValue.NumberVal(list[targetIdx])
            }
            return MathValue.NumberVal(Double.NaN)
        }
    }

    data class ComparisonNode(val op: String, val left: MathNode, val right: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val l = left.eval(ctx)
            val r = right.eval(ctx)
            val lNum = l.asDouble()
            val rNum = r.asDouble()
            val res = when (op) {
                "==", "===" -> if (!lNum.isNaN() && !rNum.isNaN()) abs(lNum - rNum) < 1e-9 else l == r
                "!=", "=!=" -> if (!lNum.isNaN() && !rNum.isNaN()) abs(lNum - rNum) >= 1e-9 else l != r
                "<" -> lNum < rNum
                "<=" -> lNum <= rNum
                ">" -> lNum > rNum
                ">=" -> lNum >= rNum
                else -> false
            }
            return MathValue.BooleanVal(res)
        }
    }

    data class LogicalOpNode(val op: String, val left: MathNode, val right: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val l = left.eval(ctx)
            return if (op == "&&") {
                if (!l.asBoolean()) MathValue.BooleanVal(false)
                else MathValue.BooleanVal(right.eval(ctx).asBoolean())
            } else {
                if (l.asBoolean()) MathValue.BooleanVal(true)
                else MathValue.BooleanVal(right.eval(ctx).asBoolean())
            }
        }
    }

    data class UnaryOpNode(val op: Char, val child: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val v = child.eval(ctx)
            return when (op) {
                '-' -> when (v) {
                    is MathValue.NumberVal -> MathValue.NumberVal(-v.value)
                    is MathValue.ListVal -> MathValue.ListVal(v.elements.map { -it })
                    else -> error("Cannot negate $v")
                }
                '+' -> v
                '~' -> MathValue.BooleanVal(!v.asBoolean()) // Logical Not
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
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val l = left.eval(ctx)
            val r = right.eval(ctx)

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
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val fn = funcName.lowercase(Locale.ROOT)

            // Custom function defined in environment
            val custom = ctx.env[fn]
            if (custom is MathValue.FunctionVal) {
                val evaluatedArgs = args.map { it.eval(ctx) }
                return invokePureFunction(custom, evaluatedArgs, ctx)
            }

            // Special forms that delay or control argument evaluation
            when (fn) {
                "if" -> {
                    if (args.isEmpty()) return MathValue.NullVal
                    val cond = args[0].eval(ctx).asBoolean()
                    return if (cond) {
                        args.getOrNull(1)?.eval(ctx) ?: MathValue.NullVal
                    } else {
                        args.getOrNull(2)?.eval(ctx) ?: MathValue.NullVal
                    }
                }
                "which" -> {
                    var i = 0
                    while (i < args.size - 1) {
                        val c = args[i].eval(ctx).asBoolean()
                        if (c) return args[i + 1].eval(ctx)
                        i += 2
                    }
                    return MathValue.NullVal
                }
                "while" -> {
                    if (args.size < 2) error("While requires at least 2 arguments")
                    val testNode = args[0]
                    val bodyNode = args[1]
                    var last: MathValue = MathValue.NullVal
                    while (testNode.eval(ctx).asBoolean()) {
                        ctx.tick()
                        try {
                            last = bodyNode.eval(ctx)
                        } catch (_: BreakException) {
                            break
                        } catch (_: ContinueException) {
                            continue
                        }
                    }
                    return last
                }
                "for" -> {
                    if (args.size < 4) error("For requires 4 arguments")
                    val startNode = args[0]
                    val testNode = args[1]
                    val incrNode = args[2]
                    val bodyNode = args[3]
                    startNode.eval(ctx)
                    while (testNode.eval(ctx).asBoolean()) {
                        ctx.tick()
                        try {
                            bodyNode.eval(ctx)
                        } catch (_: BreakException) {
                            break
                        } catch (_: ContinueException) {
                            // fall through to incr
                        }
                        incrNode.eval(ctx)
                    }
                    return MathValue.NullVal
                }
                "do" -> {
                    if (args.size < 2) error("Do requires at least 2 arguments")
                    val bodyNode = args[0]
                    val specNode = args[1]
                    if (specNode is ListNode) {
                        val varNode = specNode.items.getOrNull(0) as? IdentNode
                        val varName = varNode?.name?.lowercase(Locale.ROOT) ?: "i"
                        val min: Double
                        val max: Double
                        val step: Double
                        when (specNode.items.size) {
                            1 -> { min = 1.0; max = specNode.items[0].eval(ctx).asDouble(); step = 1.0 }
                            2 -> { min = 1.0; max = specNode.items[1].eval(ctx).asDouble(); step = 1.0 }
                            3 -> { min = specNode.items[1].eval(ctx).asDouble(); max = specNode.items[2].eval(ctx).asDouble(); step = 1.0 }
                            else -> { min = specNode.items[1].eval(ctx).asDouble(); max = specNode.items[2].eval(ctx).asDouble(); step = specNode.items[3].eval(ctx).asDouble() }
                        }
                        val saved = ctx.env[varName]
                        try {
                            var curr = min
                            val limit = 5000
                            var count = 0
                            if (step > 0.0) {
                                while (curr <= max + 1e-9 && count < limit) {
                                    ctx.tick()
                                    ctx.env[varName] = MathValue.NumberVal(curr)
                                    try {
                                        bodyNode.eval(ctx)
                                    } catch (_: BreakException) {
                                        break
                                    } catch (_: ContinueException) {
                                        // next
                                    }
                                    curr += step
                                    count++
                                }
                            } else if (step < 0.0) {
                                while (curr >= max - 1e-9 && count < limit) {
                                    ctx.tick()
                                    ctx.env[varName] = MathValue.NumberVal(curr)
                                    try {
                                        bodyNode.eval(ctx)
                                    } catch (_: BreakException) {
                                        break
                                    } catch (_: ContinueException) {
                                        // next
                                    }
                                    curr += step
                                    count++
                                }
                            }
                        } finally {
                            if (saved != null) ctx.env[varName] = saved else ctx.env.remove(varName)
                        }
                    } else {
                        val n = specNode.eval(ctx).asDouble().toInt().coerceIn(0, 5000)
                        for (i in 0 until n) {
                            ctx.tick()
                            try {
                                bodyNode.eval(ctx)
                            } catch (_: BreakException) {
                                break
                            } catch (_: ContinueException) {
                                continue
                            }
                        }
                    }
                    return MathValue.NullVal
                }
                "break" -> throw BreakException()
                "continue" -> throw ContinueException()
                "return" -> throw ReturnException(args.getOrNull(0)?.eval(ctx) ?: MathValue.NullVal)
                "table" -> {
                    if (args.isEmpty()) error("Table requires arguments")
                    val exprNode = args[0]
                    if (args.size == 1) return exprNode.eval(ctx)

                    val specNode = args[1]
                    if (specNode is ListNode) {
                        val varNode = specNode.items.getOrNull(0) as? IdentNode
                        val varName = varNode?.name?.lowercase(Locale.ROOT) ?: "x"
                        val min: Double
                        val max: Double
                        val step: Double
                        when (specNode.items.size) {
                            1 -> {
                                min = 1.0
                                max = specNode.items[0].eval(ctx).asDouble()
                                step = 1.0
                            }
                            2 -> {
                                min = 1.0
                                max = specNode.items[1].eval(ctx).asDouble()
                                step = 1.0
                            }
                            3 -> {
                                min = specNode.items[1].eval(ctx).asDouble()
                                max = specNode.items[2].eval(ctx).asDouble()
                                step = 1.0
                            }
                            else -> {
                                min = specNode.items[1].eval(ctx).asDouble()
                                max = specNode.items[2].eval(ctx).asDouble()
                                step = specNode.items[3].eval(ctx).asDouble()
                            }
                        }

                        val result = mutableListOf<Double>()
                        val limit = 5000
                        var curr = min
                        val saved = ctx.env[varName]
                        try {
                            if (step > 0.0) {
                                while (curr <= max + 1e-9 && result.size < limit) {
                                    ctx.tick()
                                    ctx.env[varName] = MathValue.NumberVal(curr)
                                    val item = exprNode.eval(ctx)
                                    result.addAll(item.asList())
                                    curr += step
                                }
                            } else if (step < 0.0) {
                                while (curr >= max - 1e-9 && result.size < limit) {
                                    ctx.tick()
                                    ctx.env[varName] = MathValue.NumberVal(curr)
                                    val item = exprNode.eval(ctx)
                                    result.addAll(item.asList())
                                    curr += step
                                }
                            }
                        } finally {
                            if (saved != null) ctx.env[varName] = saved else ctx.env.remove(varName)
                        }
                        return MathValue.ListVal(result)
                    } else {
                        val n = specNode.eval(ctx).asDouble().toInt().coerceIn(0, 5000)
                        val result = mutableListOf<Double>()
                        for (i in 0 until n) {
                            ctx.tick()
                            result.addAll(exprNode.eval(ctx).asList())
                        }
                        return MathValue.ListVal(result)
                    }
                }
                "nest" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in Nest")
                    var curr = args.getOrNull(1)?.eval(ctx) ?: error("Expected expression in Nest")
                    val n = args.getOrNull(2)?.eval(ctx)?.asDouble()?.toInt()?.coerceIn(0, 1000) ?: 1
                    for (step in 0 until n) {
                        ctx.tick()
                        curr = invokeFunction(fnNode, listOf(curr), ctx)
                    }
                    return curr
                }
                "nestlist" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in NestList")
                    var curr = args.getOrNull(1)?.eval(ctx) ?: error("Expected expression in NestList")
                    val n = args.getOrNull(2)?.eval(ctx)?.asDouble()?.toInt()?.coerceIn(0, 1000) ?: 1
                    val list = mutableListOf<Double>()
                    list.add(curr.asDouble())
                    for (step in 0 until n) {
                        ctx.tick()
                        curr = invokeFunction(fnNode, listOf(curr), ctx)
                        list.add(curr.asDouble())
                    }
                    return MathValue.ListVal(list)
                }
                "fold" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in Fold")
                    if (args.size >= 3) {
                        var acc = args[1].eval(ctx)
                        val list = args[2].eval(ctx).asList()
                        for (item in list) {
                            ctx.tick()
                            acc = invokeFunction(fnNode, listOf(acc, MathValue.NumberVal(item)), ctx)
                        }
                        return acc
                    } else if (args.size == 2) {
                        val list = args[1].eval(ctx).asList()
                        if (list.isEmpty()) return MathValue.NumberVal(Double.NaN)
                        var acc: MathValue = MathValue.NumberVal(list[0])
                        for (i in 1 until list.size) {
                            ctx.tick()
                            acc = invokeFunction(fnNode, listOf(acc, MathValue.NumberVal(list[i])), ctx)
                        }
                        return acc
                    } else {
                        error("Expected 2 or 3 arguments in Fold")
                    }
                }
                "foldlist" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in FoldList")
                    if (args.size >= 3) {
                        var acc = args[1].eval(ctx)
                        val list = args[2].eval(ctx).asList()
                        val res = mutableListOf<Double>()
                        res.add(acc.asDouble())
                        for (item in list) {
                            ctx.tick()
                            acc = invokeFunction(fnNode, listOf(acc, MathValue.NumberVal(item)), ctx)
                            res.add(acc.asDouble())
                        }
                        return MathValue.ListVal(res)
                    } else if (args.size == 2) {
                        val list = args[1].eval(ctx).asList()
                        if (list.isEmpty()) return MathValue.ListVal(emptyList())
                        var acc: MathValue = MathValue.NumberVal(list[0])
                        val res = mutableListOf<Double>()
                        res.add(acc.asDouble())
                        for (i in 1 until list.size) {
                            ctx.tick()
                            acc = invokeFunction(fnNode, listOf(acc, MathValue.NumberVal(list[i])), ctx)
                            res.add(acc.asDouble())
                        }
                        return MathValue.ListVal(res)
                    } else {
                        error("Expected 2 or 3 arguments in FoldList")
                    }
                }
                "select" -> {
                    val list = args.getOrNull(0)?.eval(ctx)?.asList() ?: emptyList()
                    val crit = args.getOrNull(1) ?: error("Expected criteria in Select")
                    val res = mutableListOf<Double>()
                    for (item in list) {
                        ctx.tick()
                        val pass = invokeFunction(crit, listOf(MathValue.NumberVal(item)), ctx).asBoolean()
                        if (pass) res.add(item)
                    }
                    return MathValue.ListVal(res)
                }
                "map" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in Map")
                    val list = args.getOrNull(1)?.eval(ctx)?.asList() ?: emptyList()
                    val res = mutableListOf<Double>()
                    for (item in list) {
                        ctx.tick()
                        val out = invokeFunction(fnNode, listOf(MathValue.NumberVal(item)), ctx)
                        res.add(out.asDouble())
                    }
                    return MathValue.ListVal(res)
                }
                "apply" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in Apply")
                    val list = args.getOrNull(1)?.eval(ctx)?.asList() ?: emptyList()
                    return invokeFunction(fnNode, list.map { MathValue.NumberVal(it) }, ctx)
                }
            }

            val evaluatedArgs = args.map { it.eval(ctx) }

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
                "part" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val idx = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: 1
                    val pos = if (idx > 0) idx - 1 else if (idx < 0) list.size + idx else -1
                    return if (pos in list.indices) MathValue.NumberVal(list[pos]) else MathValue.NumberVal(Double.NaN)
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

            // Predicates
            when (fn) {
                "evenq" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble() ?: return MathValue.BooleanVal(false)
                    val r = kotlin.math.round(n)
                    return MathValue.BooleanVal(abs(n - r) < 1e-9 && (r.toLong() % 2L == 0L))
                }
                "oddq" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble() ?: return MathValue.BooleanVal(false)
                    val r = kotlin.math.round(n)
                    return MathValue.BooleanVal(abs(n - r) < 1e-9 && (r.toLong() % 2L != 0L))
                }
                "integerq" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble() ?: return MathValue.BooleanVal(false)
                    return MathValue.BooleanVal(!n.isNaN() && !n.isInfinite() && abs(n - kotlin.math.round(n)) < 1e-9)
                }
                "positive" -> return MathValue.BooleanVal((evaluatedArgs.getOrNull(0)?.asDouble() ?: Double.NaN) > 0.0)
                "negative" -> return MathValue.BooleanVal((evaluatedArgs.getOrNull(0)?.asDouble() ?: Double.NaN) < 0.0)
                "zeroq" -> return MathValue.BooleanVal(abs(evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0) < 1e-11)
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
                    return MathValue.NumberVal(if (b == 0.0) Double.NaN else a % b)
                }
                "quotient" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.NumberVal(if (b == 0.0) Double.NaN else floor(a / b))
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

internal fun invokeFunction(fnNode: MathNode, args: List<MathValue>, ctx: ExecutionContext): MathValue {
    ctx.tick()
    when (fnNode) {
        is MathNode.IdentNode -> {
            val fnLower = fnNode.name.lowercase(Locale.ROOT)
            val custom = ctx.env[fnLower]
            if (custom is MathValue.FunctionVal) {
                return invokePureFunction(custom, args, ctx)
            }
            return MathNode.CallNode(fnNode.name, args.map { MathNode.ConstantValueNode(it) }).eval(ctx)
        }
        is MathNode.FunctionNode -> {
            return invokePureFunction(MathValue.FunctionVal(fnNode.body, fnNode.paramNames), args, ctx)
        }
        else -> {
            val evaluated = fnNode.eval(ctx)
            if (evaluated is MathValue.FunctionVal) {
                return invokePureFunction(evaluated, args, ctx)
            }
            if (evaluated is MathValue.SymbolVal) {
                return MathNode.CallNode(evaluated.name, args.map { MathNode.ConstantValueNode(it) }).eval(ctx)
            }
            error("Cannot invoke non-function $evaluated")
        }
    }
}

internal fun invokePureFunction(fn: MathValue.FunctionVal, args: List<MathValue>, ctx: ExecutionContext): MathValue {
    val savedSlots = mutableMapOf<String, MathValue?>()
    val savedParams = mutableMapOf<String, MathValue?>()

    try {
        if (fn.paramNames.isNotEmpty()) {
            for (i in fn.paramNames.indices) {
                val p = fn.paramNames[i].lowercase(Locale.ROOT)
                savedParams[p] = ctx.env[p]
                ctx.env[p] = args.getOrElse(i) { MathValue.NullVal }
            }
        }
        for (i in args.indices) {
            val key = "#${i + 1}"
            savedSlots[key] = ctx.env[key]
            ctx.env[key] = args[i]
        }
        savedSlots["#"] = ctx.env["#"]
        if (args.isNotEmpty()) ctx.env["#"] = args[0]

        return fn.body.eval(ctx)
    } finally {
        for ((k, v) in savedSlots) {
            if (v != null) ctx.env[k] = v else ctx.env.remove(k)
        }
        for ((k, v) in savedParams) {
            if (v != null) ctx.env[k] = v else ctx.env.remove(k)
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
    private fun peekNext(): Token = if (idx + 1 < tokens.size) tokens[idx + 1] else Token(TokenType.EOF, "")

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
        val expr = parseCompound()
        if (peek().type != TokenType.EOF) return null
        return expr
    }

    // CompoundExpression: expr1 ; expr2 ; ...
    private fun parseCompound(): MathNode {
        val expressions = mutableListOf<MathNode>()
        expressions.add(parseAssignment())
        while (match(TokenType.SEMICOLON)) {
            if (peek().type == TokenType.EOF || peek().type == TokenType.RBRACKET || peek().type == TokenType.RPAREN || peek().type == TokenType.RBRACE) {
                expressions.add(MathNode.NullNode)
                break
            }
            expressions.add(parseAssignment())
        }
        return if (expressions.size == 1) expressions[0] else MathNode.CompoundNode(expressions)
    }

    // Assignment: x = val, x += val, x -= val, x *= val, x /= val
    private fun parseAssignment(): MathNode {
        val node = parseRule()
        if (node is MathNode.IdentNode) {
            val t = peek().type
            if (t in setOf(TokenType.EQUAL, TokenType.PLUS_EQUAL, TokenType.MINUS_EQUAL, TokenType.STAR_EQUAL, TokenType.SLASH_EQUAL)) {
                consume(t)
                val op = when (t) {
                    TokenType.PLUS_EQUAL -> '+'
                    TokenType.MINUS_EQUAL -> '-'
                    TokenType.STAR_EQUAL -> '*'
                    TokenType.SLASH_EQUAL -> '/'
                    else -> null
                }
                val rhs = withDepth { parseAssignment() } // right-associative
                return MathNode.AssignNode(node.name, rhs, op)
            }
        }
        return node
    }

    // Rule: lhs -> rhs
    private fun parseRule(): MathNode {
        var node = parseReplace()
        if (match(TokenType.ARROW)) {
            val rhs = withDepth { parseRule() } // right-associative
            node = MathNode.RuleNode(node, rhs)
        }
        return node
    }

    // ReplaceAll: expr /. rule, ReplaceRepeated: expr //. rule
    private fun parseReplace(): MathNode {
        var node = parsePostfix()
        while (peek().type in setOf(TokenType.REPLACE_ALL, TokenType.REPLACE_REPEATED)) {
            val op = consume(peek().type)
            val rule = parseRule()
            node = MathNode.ReplaceAllNode(node, rule, repeated = (op.type == TokenType.REPLACE_REPEATED))
        }
        return node
    }

    // Postfix: expr // f
    private fun parsePostfix(): MathNode {
        var node = parseMapApply()
        while (match(TokenType.POSTFIX)) {
            val func = parseMapApply()
            node = MathNode.PostfixNode(node, func)
        }
        return node
    }

    // Map (/@), Apply (@@), Apply at level 1 (@@@)
    private fun parseMapApply(): MathNode {
        var node = parseFunction()
        while (peek().type in setOf(TokenType.MAP, TokenType.APPLY, TokenType.APPLY_LEVEL)) {
            val op = consume(peek().type)
            val right = parseFunction()
            node = when (op.type) {
                TokenType.MAP -> MathNode.MapNode(node, right)
                TokenType.APPLY -> MathNode.ApplyNode(node, right, level1 = false)
                TokenType.APPLY_LEVEL -> MathNode.ApplyNode(node, right, level1 = true)
                else -> node
            }
        }
        return node
    }

    // Pure function: expr &
    private fun parseFunction(): MathNode {
        var node = parseLogicalOr()
        if (match(TokenType.AMP)) {
            node = MathNode.FunctionNode(node)
        }
        return node
    }

    // Logical OR: expr || expr
    private fun parseLogicalOr(): MathNode {
        var node = parseLogicalAnd()
        while (match(TokenType.OR)) {
            val right = parseLogicalAnd()
            node = MathNode.LogicalOpNode("||", node, right)
        }
        return node
    }

    // Logical AND: expr && expr
    private fun parseLogicalAnd(): MathNode {
        var node = parseComparison()
        while (match(TokenType.AND)) {
            val right = parseComparison()
            node = MathNode.LogicalOpNode("&&", node, right)
        }
        return node
    }

    // Comparisons: ==, !=, <, <=, >, >=
    private fun parseComparison(): MathNode {
        var node = parseExpression()
        while (peek().type in setOf(
            TokenType.EQUAL_EQUAL, TokenType.NOT_EQUAL,
            TokenType.LESS, TokenType.LESS_EQUAL,
            TokenType.GREATER, TokenType.GREATER_EQUAL
        )) {
            val op = consume(peek().type).text
            val right = parseExpression()
            node = MathNode.ComparisonNode(op, node, right)
        }
        return node
    }

    // Add / Subtract: + , -
    private fun parseExpression(): MathNode {
        var node = parseTerm()
        while (peek().type in setOf(TokenType.PLUS, TokenType.MINUS)) {
            val op = consume(peek().type).text[0]
            val right = parseTerm()
            node = MathNode.BinaryOpNode(op, node, right)
        }
        return node
    }

    // Multiply / Divide / Modulo: * , / , % , and implicit multiplication
    private fun parseTerm(): MathNode {
        var node = parsePrefix()
        while (peek().type in setOf(TokenType.STAR, TokenType.SLASH) ||
            (peek().type == TokenType.PERCENT && isBinaryPercent()) ||
            canStartImplicitMult()
        ) {
            val op = if (peek().type in setOf(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
                consume(peek().type).text[0]
            } else {
                '*' // implicit multiplication
            }
            val right = parsePrefix()
            node = MathNode.BinaryOpNode(op, node, right)
        }
        return node
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
        return t in setOf(TokenType.IDENT, TokenType.LPAREN, TokenType.LBRACE)
    }

    // Prefix application: f @ x
    private fun parsePrefix(): MathNode {
        var node = parseUnary()
        if (match(TokenType.AT)) {
            val right = withDepth { parsePrefix() } // right-associative: f @ g @ x -> f @ (g @ x)
            node = MathNode.PrefixNode(node, right)
        }
        return node
    }

    // Unary prefix: +, -, !, ++x, --x
    private fun parseUnary(): MathNode {
        if (match(TokenType.PLUS)) return parseUnary()
        if (match(TokenType.MINUS)) return MathNode.UnaryOpNode('-', parseUnary())
        if (match(TokenType.EXCLAMATION)) return MathNode.UnaryOpNode('~', parseUnary()) // Logical NOT
        if (match(TokenType.PLUS_PLUS)) {
            val t = consume(TokenType.IDENT)
            return MathNode.PrefixIncDecNode("++", t.text)
        }
        if (match(TokenType.MINUS_MINUS)) {
            val t = consume(TokenType.IDENT)
            return MathNode.PrefixIncDecNode("--", t.text)
        }
        return parsePower()
    }

    // Power: ^ (right-associative)
    private fun parsePower(): MathNode {
        var node = parsePostfixUnary()
        if (match(TokenType.CARET)) {
            val right = withDepth { parseUnary() }
            node = MathNode.BinaryOpNode('^', node, right)
        }
        return node
    }

    // Postfix unary: x!, x++, x--, list[[index]], function calls
    private fun parsePostfixUnary(): MathNode {
        var node = parsePrimary()
        while (true) {
            when {
                match(TokenType.EXCLAMATION) -> {
                    node = MathNode.UnaryOpNode('!', node)
                }
                match(TokenType.PLUS_PLUS) -> {
                    if (node is MathNode.IdentNode) node = MathNode.PostfixIncDecNode("++", node.name)
                }
                match(TokenType.MINUS_MINUS) -> {
                    if (node is MathNode.IdentNode) node = MathNode.PostfixIncDecNode("--", node.name)
                }
                peek().type == TokenType.LBRACKET && peekNext().type == TokenType.LBRACKET -> {
                    consume(TokenType.LBRACKET)
                    consume(TokenType.LBRACKET)
                    val idx = parseCompound()
                    consume(TokenType.RBRACKET)
                    consume(TokenType.RBRACKET)
                    node = MathNode.PartNode(node, idx)
                }
                peek().type == TokenType.LBRACKET -> {
                    consume(TokenType.LBRACKET)
                    val args = parseArgList(TokenType.RBRACKET)
                    node = when (node) {
                        is MathNode.IdentNode -> MathNode.CallNode(node.name, args)
                        else -> MathNode.CallNode("Function", listOf(node) + args)
                    }
                }
                else -> break
            }
        }
        return node
    }

    private fun parsePrimary(): MathNode {
        val t = peek()
        return when (t.type) {
            TokenType.NUMBER -> {
                consume(TokenType.NUMBER)
                MathNode.NumberNode(t.value)
            }
            TokenType.PERCENT -> {
                consume(TokenType.PERCENT)
                MathNode.OutNode
            }
            TokenType.SLOT -> {
                consume(TokenType.SLOT)
                MathNode.SlotNode(t.intVal)
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
                    else if (lower == "null") MathNode.NullNode
                    else if (lower == "%" || lower == "out") MathNode.OutNode
                    else MathNode.IdentNode(name)
                }
            }
            TokenType.LPAREN -> {
                consume(TokenType.LPAREN)
                val expr = withDepth { parseCompound() }
                consume(TokenType.RPAREN)
                expr
            }
            TokenType.LBRACE -> {
                consume(TokenType.LBRACE)
                val items = withDepth { parseArgList(TokenType.RBRACE) }
                MathNode.ListNode(items)
            }
            else -> error("Unexpected token: ${t.text}")
        }
    }

    private fun parseArgList(closingType: TokenType): List<MathNode> = withDepth {
        val list = mutableListOf<MathNode>()
        if (match(closingType)) return@withDepth list
        while (true) {
            list.add(parseCompound())
            if (match(TokenType.COMMA)) continue
            consume(closingType)
            break
        }
        list
    }
}
