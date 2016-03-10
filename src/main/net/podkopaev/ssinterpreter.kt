package net.podkopaev

import java.util.*

class SSInterpreter {
    private var env: HashMap<String, Int> = HashMap()
    private var input: Stack<Int> = Stack()

    fun step(stmt: Stmt?): Stmt? {
        if (stmt == null) { return null }
        when (stmt) {
            is Stmt.Read  -> {
                env.put(stmt.name, input.pop())
                return null
            }
            is Stmt.Write -> {
                println("Output: ${stmt.expr.calc(env)}")
                return null
            }
            is Stmt.Assign -> {
                env.put(stmt.name, stmt.expr.calc(env))
                return null
            }
            is Stmt.Seq -> {
                val newLeft = step(stmt.left) ?: return stmt.right
                return Stmt.Seq(newLeft, stmt.right)
            }
            is Stmt.If -> {
                if (stmt.expr.calc(env) != 0) {
                    return stmt.then
                } else {
                    return stmt.elsep
                }
            }
            is Stmt.While -> {
                if (stmt.expr.calc(env) != 0) {
                    return Stmt.Seq(stmt.body, stmt)
                } else {
                    return null
                }
            }
        }
    }

    fun interpret(stmt: Stmt?) {
        var curStmt = stmt
        while (curStmt != null) {
            println(curStmt.print(25))
            println()
            printEnv()
            readLine()
            curStmt = step(curStmt)
        }
    }

    fun init(inputList: List<Int>) {
        env = hashMapOf()
        input = Stack()
        for (i in inputList.reversed()) {
            input.push(i)
        }
    }

    private fun printEnv() {
        print("{ ")
        for (p in env) {
            print("${p.key} = ${p.value}; ")
        }
        println("}")
    }
}

val logpowProgram = """
read(n);
read(k);
r := 1;
while k do
  if (k % 2) then
    r := r * n;
    k := k - 1
  else
    n := n * n;
    k := k / 2
  fi
od;
write(r)
"""

fun main(args: Array<String>) {
    val interpreter = SSInterpreter()
    interpreter.init(listOf(2, 5))
    println("Start")
    interpreter.interpret(stmtParser.get(logpowProgram))
}
