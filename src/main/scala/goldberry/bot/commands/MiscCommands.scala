package kokellab.goldberry.bot.commands

import java.nio.file.{Files, Path, Paths}

import scala.compat.java8.StreamConverters._
import scala.collection.JavaConverters._
import kokellab.goldberry.bot._

case object Hello extends Command {
	override def path: String = "misc/hello"
	override protected def respond(trigger: TriggerConfig, parameterization: PhraseParameterization, message: RichMessage): SlackReply = {
		if (parameterization contains "veryoffensivelabel") reply(parameterization, message, key = "badly_offended")
		else if (parameterization contains "offensivelabel") reply(parameterization, message, key = "offended")
		else reply(parameterization, message)
	}
}

case object Goldberry extends Command {
	override def path: String = "misc/goldberry"
	override protected def respond(trigger: TriggerConfig, parameterization: PhraseParameterization, message: RichMessage): SlackReply =
		reply(parameterization, message)
}

case object NotUnderstood extends Command {
	override def path: String = "misc/not-understood"
	override protected def respond(trigger: TriggerConfig, parameterization: PhraseParameterization, message: RichMessage): SlackReply =
		reply(parameterization, message)
}


case object Installed extends Command {
	override def path: String = "misc/installed"
	override protected def respond(trigger: TriggerConfig, parameterization: PhraseParameterization, message: RichMessage): SlackReply = {
		val commands = CommandList.list map (_.path)
		reply(parameterization ++ Map("commands" -> commands.mkString("\n")), message)
	}
}
case object AllCommands extends Command {
	override def path: String = "misc/all-commands"
	override protected def respond(trigger: TriggerConfig, parameterization: PhraseParameterization, message: RichMessage): SlackReply = {
		val stream: Stream[Path] = Files.walk(Paths.get("src/main/resources/commands")).toScala
		val commands = stream filter (_.toString.endsWith(".json")) map (_.toString.replace("src/main/resources/commands/", "").replace(".json", ""))
		reply(parameterization ++ Map("commands" -> commands.mkString("\n")), message)
	}
}