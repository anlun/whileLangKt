package net.podkopaev

import org.jetbrains.format.FormatSet
import org.jetbrains.format.combinators.*
import java.util.*

sealed class Stmt() {
    companion object {
        protected var env: HashMap<String, Int> = HashMap()
        protected var input: List<Int> = listOf()
    }

    class Read(val name: String): Stmt() {
        override fun interpret_help(): List<Int> {
            if (input.isEmpty()) { throw Exception("Empty input stream error!") }
            val value = input[0]
            input = input.drop(1)
            env.put(name, value)
            return listOf()
        }
    }
    class Write(val expr: Expr): Stmt() {
        override fun interpret_help(): List<Int> {
            val value = expr.calc(env)
            return listOf(value)
        }
    }
    class Assign(val name: String, val expr: Expr): Stmt() {
        override fun interpret_help(): List<Int> {
            val value = expr.calc(env)
            env.put(name, value)
            return listOf()
        }
    }
    class Seq(val left: Stmt, val right: Stmt): Stmt() {
        override fun interpret_help(): List<Int> {
            val lresult = left.interpret_help()
            val rresult = right.interpret_help()
            return lresult + rresult
        }
    }
    class If(val expr: Expr, val then: Stmt, val elsep: Stmt): Stmt() {
        override fun interpret_help(): List<Int> {
            val value = expr.calc(env)
            if (value != 0) {
                return then.interpret_help()
            }
            return elsep.interpret_help()
        }
    }
    class While(val expr: Expr, val body: Stmt): Stmt() {
        override fun interpret_help(): List<Int> {
            val value = expr.calc(env)
            if (value == 0) { return listOf() }
            return body.interpret_help() + interpret_help()
        }
    }

    fun interpret(input: List<Int>): List<Int> {
        Companion.input = input
        env = HashMap()
        return interpret_help()
    }

    abstract protected fun interpret_help(): List<Int>
}

val stmtParser: Parser<Stmt> = parser { input ->
    val readp   = sp(litp("read")) - paren(sp(symbol)) + { Stmt.Read(it) as Stmt }
    val writep  = sp(litp("write")) - paren(sp(exprParser)) + { Stmt.Write(it) as Stmt }
    val assignp = combine3p(sp(symbol), sp(litp(":=")), sp(exprParser)) { name, s, expr ->
        Stmt.Assign(name, expr) as Stmt
    }

    val ifp = combine3p(sp(litp("if")) - sp(exprParser),
                        sp(litp("then") - sp(this)),
                        gparen(litp("else"), sp(this), litp("fi"))) {
        expr, then, elseb -> Stmt.If(expr, then, elseb) as Stmt
    }
    val whilep = combinep(sp(litp("while")) - sp(exprParser),
                          gparen(litp("do"), sp(this), litp("od"))) {
        expr, body -> Stmt.While(expr, body) as Stmt
    }

    val corep = sp(readp / writep / assignp / ifp / whilep)
    val parser = leftAssocp(sp(litp(";")), corep) {
        op, s1, s2 ->
        Stmt.Seq(s1, s2)
    }
    parser(input)
}

fun Stmt.toFormatSet(w: Int): FormatSet {
    when (this) {
        is Stmt.Read ->
            return "read(".toSet(w) / name / ")"
        is Stmt.Write ->
            return "write(" / expr.toFormatSet(w) / ")"
        is Stmt.Assign ->
            return name.toSet(w) / " := " / expr.toFormatSet(w)
        is Stmt.Seq -> {
            val leftFS     = left .toFormatSet(w)
            val rightFS    = right.toFormatSet(w)
            val result     = leftFS / "; " / rightFS
            val multiliner = leftFS / ";"  - rightFS
            result choiceUpd multiliner
            return result
        }
        is Stmt.If -> {
            val eFS     = expr .toFormatSet(w)
            val thenFS  = then .toFormatSet(w)
            val elsepFS = elsep.toFormatSet(w)
            val result  = "if "    / eFS     /
                          " then " / thenFS  /
                          " else " / elsepFS / "fi"
            val multiliner = "if "   / eFS     -
                             "then " / thenFS  -
                             "else " / elsepFS -
                             "fi"
            result choiceUpd multiliner
            return result
        }
        is Stmt.While -> {
            val eFS     = expr.toFormatSet(w)
            val bodyfs  = body.toFormatSet(w)
            val result  = "while " / eFS    /
                          " do "   / bodyfs / "od"
            val multiliner = "while " / eFS    -
                             "do "    / bodyfs -
                             "od"
            result choiceUpd multiliner
            return result
        }
    }
}

fun Stmt.print(w: Int): String? = toFormatSet(w).head().toString()