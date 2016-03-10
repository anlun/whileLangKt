package net.podkopaev

import org.jetbrains.format.FormatSet
import org.jetbrains.format.combinators.*

sealed class Expr() {
    class Con(val value: Int): Expr() {
        override fun calc(env: Map<String, Int>): Int = value
    }
    class Var(val name: String): Expr() {
        override fun calc(env: Map<String, Int>): Int = env.getOrElse(name, { 0 })
    }
    class Binop(val op: String, val left: Expr, val right: Expr): Expr() {
        override fun calc(env: Map<String, Int>): Int {
            val l = left.calc(env)
            val r = right.calc(env)
            return when (op) {
                "+" -> l + r
                "-" -> l - r
                "*" -> l * r
                "/" -> l / r
                "%" -> l % r
                "^" -> Math.pow(l.toDouble(), r.toDouble()).toInt()
                else -> throw UnsupportedOperationException()
            }
        }
    }

    override fun hashCode(): Int {
        return when (this) {
            is Con -> value.hashCode()
            is Var -> name.hashCode()
            is Binop -> (op.hashCode() * 31 + left.hashCode()) * 31 + right.hashCode()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Expr) { return false }
        when (other) {
            is Con -> return (this is Con) && value.equals(other.value)
            is Var -> return (this is Var) && name.equals(other.name)
            is Binop -> return (this is Binop) && op.equals(other.op) &&
                    left.equals(other.left) && right.equals(other.right)
        }
        return false
    }

    abstract fun calc(env: Map<String, Int>): Int
}

val exprParser: Parser<Expr> = parser { input ->
    fun rightAssocp(opp: Parser<String>, elemp: Parser<Expr>): Parser<Expr> = parser { input ->
        val emptyRightp: Parser<(Expr) -> Expr> =
                empty + { t -> { l: Expr -> l } }
        val rightp: Parser<Pair<String, Expr>> =
                combinep(opp, this) { op, e -> Pair(op, e) }
        val rightFp: Parser<(Expr) -> Expr> = rightp +
                        { t -> { l: Expr -> Expr.Binop(t.first, l, t.second) }}
        val parser = combinep(elemp, rightFp / emptyRightp) { e, f -> f(e) }
        parser(input)
    }


    val corep = sp (
            (number + { Expr.Con(it) as Expr }) /
            (symbol + { Expr.Var(it) as Expr }) /
            paren(this)
    )
    val op1p = rightAssocp(sp(litp("^")), corep)
    val op2p = leftAssocp (sp(litp("*") / litp("/") / litp("%")), op1p) {
        op, e1, e2 ->
        Expr.Binop(op, e1, e2)
    }
    val op3p = leftAssocp (sp(litp("+") / litp("-")), op2p) {
        op, e1, e2 ->
        Expr.Binop(op, e1, e2)
    }
    op3p(input)
}


fun Expr.toFormatSet(w: Int): FormatSet {
    when (this) {
        is Expr.Con -> return value.toString().toSet(w)
        is Expr.Var -> return name.toSet(w)
        is Expr.Binop -> {
            val l = left.toFormatSet(w)
            val r = right.toFormatSet(w)
            val result = "(" / l / " " / op / " " / r / ")"
            result choiceUpd ("(" / (l / " " / op - r) / ")")
            return result
        }
    }
}
fun Expr.print(w: Int): String? = toFormatSet(w).head()?.toString()
