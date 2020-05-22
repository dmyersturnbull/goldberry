package kokellab.goldberry

import scala.util.{Failure, Success, Try}
import kokellab.valar.importer.{DirectoryLoader, Importer, SubmissionResult}
import com.beachape.filemanagement.{Messages, RxMonitor}
import java.io.{BufferedWriter, Closeable, FileWriter}
import java.nio.charset.StandardCharsets

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._
import java.nio.file.{Files, Path, Paths}
import java.nio.file.StandardWatchEventKinds._
import java.util.concurrent.Executors
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import kokellab.valar.core._
import com.typesafe.scalalogging.{LazyLogging, Logger}
import kokellab.goldberry.bot.SauronxBot
import kokellab.goldberry.queue.{DequeueFailedException, ImportQueue}
import kokellab.valar.core.CommonQueries._
import kokellab.valar.core.Tables.SauronxSubmissionParams
import org.apache.axis2.addressing.AddressingConstants.Submission
import slack.SlackUtil
import slack.models.Message
import slack.rtm.SlackRtmClient

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}


object Goldberry extends LazyLogging {

	lazy val bot = new SauronxBot()

	val handle = new ImportHandle(bot)

	val jobs = new ImportQueue(
		handle = handle.apply,
		size = 3,
		uploadDir = Paths.get("/data/uploads"),
		archiveDir = Paths.get("/pix/plates"),
		pendingDir = Paths.get("/data/uploads/pending")
	)

	bot.loop()
}

class ImportHandle(bot: SauronxBot) extends LazyLogging {

	def apply(future: Future[Try[Path]]): Unit = future.onComplete {
		case Success(path) => handleSuccess(path.get)
		case Failure(failure: DequeueFailedException) => handleError(failure)
		case Failure(e) => throw e
	}

	def handleSuccess(path: Path): Unit = {
		logger.error(s"Imported $path")
		bot.announce(s"Finished $path")
	}

	def handleError(failure: DequeueFailedException): Unit = {
		logger.error(s"Failed to import", failure)
		bot.announce(s"Failed to import {}")
	}

}
