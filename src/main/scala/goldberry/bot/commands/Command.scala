package kokellab.goldberry.bot.commands

import java.nio.file.Paths

import com.typesafe.scalalogging.LazyLogging
import kokellab.goldberry.bot._
import kokellab.valar.core.loadDb

import scala.concurrent.duration.Duration


trait Command extends LazyLogging {
	import Command._
	protected implicit val db = loadDb()

	def path: String

	def name: String = Paths.get(path).getFileName.toString

	def say(parameterization: PhraseParameterization, to: Seq[String], channel: String, key: String = defaultPhrase): SlackReply =
		SlackReply.build(config.replies(key), parameterization, to, channel)

	def announce(parameterization: PhraseParameterization, channel: String = jobsChannel, key: String = defaultPhrase): SlackReply =
		SlackReply.build(config.replies(key), parameterization, Nil, channel)

	def reply(parameterization: PhraseParameterization, incoming: RichMessage, moreUsers: Seq[String] = Nil, key: String = defaultPhrase): SlackReply =
		SlackReply.build(config.replies(key), parameterization, Seq(incoming.fromUser) ++ moreUsers, incoming.channel)

	val config: CommandConfig = CommandConfig.read(path)

	def apply(message: RichMessage): Option[SlackReply] = {
		val matcher: Option[(TriggerConfig, Map[String, String])] = (
			config.triggers
				map (p => (p, p.matcher(message.message))) // can't be flattened
				find {case (p, t) => t.isDefined}
				map {case (p, t) => p -> t.get}
		)
		matcher map (m => respond(m._1, PhraseParameterization.of(m._2, message), message))
	}

	protected def respond(trigger: TriggerConfig, parameterization: PhraseParameterization, message: RichMessage): SlackReply

}
object Command {
	val defaultPhrase = "ok"
}


trait AutoCommand extends Command {
	def interval: Duration
}