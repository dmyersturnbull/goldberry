package kokellab.goldberry.bot.commands

import sys.process._
import java.nio.file.{Files, Path, Paths}

import scala.compat.java8.StreamConverters._
import scala.collection.JavaConverters._
import kokellab.goldberry.bot._
import kokellab.goldberry.bot.commands.Goldberry.reply

case object ReindexCommand extends Command {
	override def path: String = "filesystem/rebuild-index"
	override protected def respond(trigger: TriggerConfig, parameterization: PhraseParameterization, message: RichMessage): SlackReply = {
		val output1 = Seq("bash", "-c", "python", "/data/repos/goldberry/scripts/filesystem/reindex.py --sauronx --overwrite >> /data/logs/reindexing.log &").!!
		logger.info("Reindexing SauronX data")
		//val output2 = Seq("bash", "-c", "python", "/data/repos/goldberry/scripts/filesystem/reindex.py --overwrite >> /data/logs/reindexing.log &").!!
		//logger.info("Reindexing legacy data")
		reply(parameterization, message)
	}
}

case object NameRunsCommand extends Command {
	override def path: String = "filesystem/name-runs"
	override protected def respond(trigger: TriggerConfig, parameterization: PhraseParameterization, message: RichMessage): SlackReply = {
		val output = Seq("bash", "-c", "python /data/repos/goldberry/scripts/valar/name_runs.py >> /data/logs/run_naming.log &").!!
		logger.info("Names for new plate runs will be set.")
		reply(parameterization, message)
	}
}

case object ReimportCommand extends Command {
	override def path: String = "filesystem/reimport"
	override protected def respond(trigger: TriggerConfig, parameterization: PhraseParameterization, message: RichMessage): SlackReply = {
		val output: String = Seq("bash", "-c", s"python /data/repos/goldberry/scripts/valar/reimport.py ${parameterization("hash")} --new ${parameterization("newhash")} >> /data/logs/reimports/${parameterization("hash")}-${parameterization("newhash")} &").!!
		logger.info(s"Will reimport ${parameterization("hash")} after replacing it with ${parameterization("newhash")}")
		reply(parameterization ++ Map("output" -> output), message)
	}
}
