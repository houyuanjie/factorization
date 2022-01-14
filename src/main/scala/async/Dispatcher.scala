package async

import monix.execution.Ack.Continue
import monix.execution.ExecutionModel.AlwaysAsyncExecution
import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.Observer
import monix.reactive.subjects.BehaviorSubject

import scala.concurrent.Future

class Dispatcher[A](val initialState: A) {
  implicit val scheduler: Scheduler = Scheduler(executionModel = AlwaysAsyncExecution)

  private val behaviorStream: BehaviorSubject[A] = BehaviorSubject(initialState)

  /** 处理 */
  def dispatch(a: A): Unit = behaviorStream.onNext(a)

  /** 结束 */
  def cancel(): Unit = behaviorStream.onComplete()

  private def newObserver(process: A => Unit, last: () => Unit): Observer[A] = new Observer[A] {

    override def onNext(a: A): Future[Ack] = {
      process(a)
      Continue
    }

    override def onError(ex: Throwable): Unit = ex.printStackTrace()

    override def onComplete(): Unit = last()
  }

  /** 开始订阅 */
  def subscribe(doAction: A => Unit, onCancel: () => Unit): Cancelable =
    behaviorStream.subscribe(newObserver(doAction, onCancel))
}
