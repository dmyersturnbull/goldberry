package kokellab.goldberry.bot.commands

import java.util.concurrent.TimeUnit

import kokellab.goldberry._
import kokellab.goldberry.bot._
import kokellab.valar.core.exec

import scala.concurrent.duration.Duration

case object Locate extends Command {

	override def path: String = "jobs/locate-job"

	override protected def respond(trigger: TriggerConfig, parameterization: PhraseParameterization, message: RichMessage): SlackReply = {

		import kokellab.valar.core.Tables._
		import kokellab.valar.core.Tables.profile.api._

		val hash = parameterization.map("hash")

		exec((SauronxSubmissions filter (_.idHashHex === hash)).result).headOption map {sub =>
			val history = exec((SauronxSubmissionHistory filter (_.sauronxSubmissionId === sub.id) sortBy (_.datetimeModified)).result).headOption
			history map { last =>
				val sauron = exec((Saurons filter (_.id === last.sauronId)).result).head
				val extras = Map(
					"description" -> sub.shortDescription,
					"update_time" -> formatSqlTimestamp(last.datetimeModified),
					"status" -> last.status.getOrElse("unknown"),
					"sauron" -> sauron.number.toString
				)
				reply(parameterization ++ extras, message)
			} getOrElse {
				reply(parameterization ++ Map("description" -> sub.shortDescription), message, key = "unused")
			}
		} getOrElse {
			reply(parameterization, message, key = "not_found")
		}

	}

}

case object JobCompleted extends AutoCommand {

	override def path: String = "jobs/auto/job-finished"
	override def interval: Duration = Duration(10, TimeUnit.SECONDS)

	override protected def respond(trigger: TriggerConfig, parameterization: PhraseParameterization, message: RichMessage): SlackReply = {
		announce(parameterization, jobsChannel, "ok")
	}

}