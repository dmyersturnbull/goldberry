package kokellab.goldberry.queue

import java.io.Closeable
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds._
import java.util.concurrent.Executors

import akka.actor.ActorSystem
import com.beachape.filemanagement.{Messages, RxMonitor}
import com.typesafe.scalalogging.LazyLogging
import kokellab.valar.core._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


class ImportQueue(
		handle: Future[Try[Path]] => Unit,
		size: Int,
		uploadDir: Path,
		pendingDir: Path,
		archiveDir: Path
) extends Closeable with LazyLogging {

	private implicit val db = loadDb()

	val archiver = new SubmissionArchiver(uploadDir, archiveDir, pendingDir)

	val queue = new mutable.LinkedHashSet[Path]()
	val running = new java.util.concurrent.ConcurrentHashMap[Path, Future[Try[Path]]]()
	private implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(size))
	private val monitor = monitorPath(pendingDir, onNext = { p =>
		enqueue(uploadDir.resolve(p.path.normalize.getFileName))
		tryInsert()
	})

	private def monitorPath(path: Path, onNext: Messages.EventAtPath => Unit): RxMonitor = {
		implicit val system = ActorSystem()
		val monitor = RxMonitor()
		val observable = monitor.observable
		observable.subscribe(
			onNext = onNext,
			onError = { t => throw t },
			onCompleted = { () => logger.warn("Monitor has been shut down") }
		)
		monitor.registerPath(ENTRY_MODIFY, path)
		monitor
	}

	def enqueue(submission: Path): Unit = {
		if (queue contains submission) queue -= submission
		queue += submission
	}
	def tryInsert(): Option[Future[Try[Path]]] = {
		next() map { submission =>
			insertNow(submission)
		}
	}
	def next(): Option[Path] = {
		if (running.size < 2) queue.headOption
		else None
	}

	def insertNow(submission: Path): Future[Try[Path]] = Try {
		assert(!(running containsKey submission), s"$submission is already running")
		val future = Future {
			val result = archiver.insertNow(submission)
			running.remove(submission)
			result
		}
		running.put(submission, future)
		queue -= submission
		future
	} match {
		case Success(path) => path
		case Failure(e: Exception) => throw new DequeueFailedException(submission, e)
		case Failure(e) => throw e
	}

	override def close(): Unit = {
		monitor.stop()
	}
}

class DequeueFailedException(val path: Path, val cause: Exception, val message: String = null) extends Exception(message, cause) {
	def submissionHash: String = path.getFileName.toString
}
