package kokellab.goldberry.bot.commands

import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util.regex.Pattern

import argonaut.Argonaut.{casecodec2, casecodec3}
import argonaut.{CodecJson, Parse}
import kokellab.goldberry.bot.RichMessage
import slack.rtm.RtmState

import scala.io.Source

// TODO SlackReply doesn't belong here
case class SlackReply(message: RichMessage) {
	def formulate(state: RtmState): String = message.formulate(state)
}
object SlackReply {
	def build(reply: ReplyConfig, parameterization: PhraseParameterization, to: Seq[String], channel: String): SlackReply = {
		val text = parameterization.map.foldLeft(reply.pattern)((building, sub) => {
			building.replaceAllLiterally("$" + sub._1, sub._2)
		})
		SlackReply(RichMessage.outgoing(text, to, channel))
	}
}

case class ReplyConfig(name: String, pattern: String)
object ReplyConfig {
	implicit def converter: CodecJson[ReplyConfig] = casecodec2(ReplyConfig.apply, ReplyConfig.unapply)("name", "pattern")
}

case class UserTextSubstitution(from: String, to: String) {
	def apply(text: String): String = text.replaceAllLiterally(from, to)
}
object UserTextSubstitution {
	implicit def converter: CodecJson[UserTextSubstitution] = casecodec2(UserTextSubstitution.apply, UserTextSubstitution.unapply)("string", "replacement")
	private val s = Source.fromFile(Paths.get("src/main/resources/substitutions.json").toFile).mkString
	val substitutions: List[UserTextSubstitution] = Parse.decodeOption[List[UserTextSubstitution]](s)
		.getOrElse(throw new IllegalStateException(s"Couldn't parse substitutions file"))
	def replaceAll(text: String): String =  substitutions.foldLeft(text)((building, sub) => sub(building))
}


case class TriggerConfig(pattern: String) {
	val regex = Pattern.compile(pattern)
	val groups: List[String] = ("""\(\?<([a-zA-Z][a-zA-Z0-9]*)>""".r findAllMatchIn pattern map (_.group(1))).toList
	def matcher(text: String): Option[Map[String, String]] = {
		val matcher = regex.matcher(text)
		if (matcher.matches()) Some {{
			groups map (group => group -> matcher.group(group))
		}.toMap} else None
	}
}

case class CommandConfig(name: String, rawTriggers: List[String], rawReplies: List[ReplyConfig]) {
	val triggers: List[TriggerConfig] = rawTriggers map TriggerConfig
	val replies = (rawReplies map (r => r.name -> r)).toMap
}
object CommandConfig {
	def read(path: String): CommandConfig = {
		implicit def converter: CodecJson[CommandConfig] = casecodec3(CommandConfig.apply, CommandConfig.unapply)("name", "triggers", "replies")
		val p = Paths.get("src/main/resources/commands").resolve(path + ".json")
		val data = Source.fromFile(p.toFile).mkString
		Parse.decodeOption[CommandConfig](data).getOrElse(throw new IllegalStateException(s"Couldn't parse $p"))
	}
}


case class PhraseParameterization(map: Map[String, String]) {
	def apply(key: String) = map(key)
	def +(key: String, value: String) = PhraseParameterization(map + (key -> value))
	def ++(m: Map[String, String]) = PhraseParameterization(map ++ m)
	def ++(m: PhraseParameterization) = PhraseParameterization(map ++ m.map)
	def contains(key: String) = map.contains(key)
}
object PhraseParameterization {
	def of(map: Map[String, String], message: RichMessage): PhraseParameterization = {
		val datetime: String = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
		val date: String = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE)
		val time: String = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_TIME)
		PhraseParameterization(map ++ Map(
			"!user" -> message.fromUser, "!channel" -> message.channel, "!datetime" -> datetime, "!date" -> date, "!time" -> time
		))
	}
}
