package async

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalajs.dom.*

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object Primes {
  private val numbersEle   = document.getElementById("number")
  private val calculateEle = document.getElementById("calculate")
  private val stopEle      = document.getElementById("stop")
  private val targetEle    = document.getElementById("target")

  /** 初始的元素可见性 */
  private def onCancel(): Unit = {
    numbersEle.asInstanceOf[HTMLElement].style.display = ""
    calculateEle.asInstanceOf[HTMLElement].style.display = ""
    stopEle.asInstanceOf[HTMLElement].style.display = "none"
  }

  sealed trait Action
  case class Start(initial: Long)                  extends Action
  case class PrintResult(n: Vector[Long], t: Long) extends Action

  case class CalculationState(var flag: Boolean)

  case class State(
      nextNumber: Long,
      primes: Vector[Long],
      target: Long,
      isFinished: Boolean = false,
      Error: Option[String] = None
  )

  /** 格式化 Long => String */
  private def makeString(n: Long): String = n.toString.reverse
    .grouped(3)
    .reduceLeft { (x, s) =>
      if (x.length >= 3) x + "," + s
      else x
    }
    .reverse

  private def fillElement(target: Element)(text: String): Unit = target.innerHTML = text

  /** 更新页面 */
  private def doAction(action: Action): Unit = action match {
    case Start(i) =>
      fillElement(targetEle)("1 * " + makeString(i))
    case PrintResult(n, t) =>
      val s =
        if (t != 1) n.map(makeString).mkString("1 * ", " * ", " * " + makeString(t))
        else n.map(makeString).mkString("final:&nbsp;&nbsp;&nbsp;", " * ", " = " + makeString(n.product))

      fillElement(targetEle)(s)
  }

  def main(args: Array[String]): Unit = {
    onCancel()

    calculateEle.addEventListener(
      `type` = "click",
      listener = (_: Event) => {
        // 抽取输入的数字
        val inputNumber = Try(numbersEle.asInstanceOf[HTMLInputElement].value.toLong) match {
          case Success(n) => n
          case Failure(_) => 0
        }

        if (inputNumber > 1) {
          stopEle.asInstanceOf[HTMLElement].style.display = ""
          numbersEle.asInstanceOf[HTMLElement].style.display = "none"
          calculateEle.asInstanceOf[HTMLElement].style.display = "none"

          val initialState = State(1, Vector.empty[Long], inputNumber)

          implicit val mutableState: CalculationState = CalculationState(false)
          implicit val dispatcher: Dispatcher[Action] = new Dispatcher[Action](Start(initialState.target))

          // 开始订阅
          dispatcher.subscribe(doAction, onCancel)

          // 为了后续 remove, 对处理程序进行一下保存
          val stopClickHandler: Event => Unit = (_: Event) => mutableState.flag = true

          stopEle.addEventListener(
            `type` = "click",
            listener = (e: Event) => {
              stopClickHandler(e)
              stopEle.removeEventListener(`type` = "click", listener = stopClickHandler)
            }
          )

          // 开始异步递归执行
          doCalculation(initialState).runToFuture
        } else {
          targetEle.innerHTML = "You must enter a number > 1"
        }
      }
    )
  }

  @tailrec
  private def doDivide(state: State)(implicit dispatcher: Dispatcher[Action]): State =
    state match {
      case State(n, p, t, _, _) if t % n == 0 =>
        val newTarget = t / n
        val newPrimes = p :+ n

        dispatcher.dispatch(PrintResult(newPrimes, newTarget))
        doDivide(state.copy(primes = newPrimes, target = newTarget))
      case _ => state
    }

  private def newState(state: State)(implicit dispatcher: Dispatcher[Action]): State =
    state match {
      case State(n, _, t, _, _) if t % n == 0 => doDivide(state)
      case State(_, _, t, _, _) if t > 1 => state
      case _                             => state.copy(isFinished = true)
    }

  private def doCalculation(
      state: State
  )(implicit dispatcher: Dispatcher[Action], calculationState: CalculationState): Task[Unit] =
    Task
      .eval(state)
      .flatMap {
        case _ if calculationState.flag =>
          Task.now(dispatcher.cancel())
        case State(_, _, _, true, _) =>
          Task.now(dispatcher.cancel())
        case State(_, _, _, _, Some(_)) =>
          doCalculation(state.copy(isFinished = true))
        case State(n, _, t, _, _) if n >= t =>
          doCalculation(state.copy(isFinished = true))
        case State(n, _, _, _, _) =>
          doCalculation(newState(state.copy(nextNumber = n + 1)))
      }
}
