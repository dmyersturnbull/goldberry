package kokellab.goldberry.bot

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import kokellab.goldberry.bot.commands.{Command, CommandList, SlackReply}
import kokellab.valar.core.loadDb
import kokellab.valar.core.ValarConfig
import slack.SlackUtil
import slack.models.Message
import slack.rtm.SlackRtmClient

import scala.concurrent.Future


class SauronxBot(channel: String = "sauronx-jobs") extends LazyLogging {

	private implicit val db = loadDb()

	private val token = ValarConfig.instance.config.getString("slackToken")

	private implicit val system = ActorSystem("slack")
	private implicit val ec = system.dispatcher

	private val client = SlackRtmClient(token)
	private val selfId = client.state.self.id
	private val state = client.state

	def loop(): Unit = {
		client.onMessage { message =>
			val mentionedIds = SlackUtil.extractMentionedIds(message.text)
			if (mentionedIds.contains(selfId) || SlackUtil.isDirectMsg(message)) {
				process(message)
			}
		}
	}

	private def process(message: Message): Unit = {
		println(message)
		val mentions = SlackUtil.extractMentionedIds(message.text) flatMap (u => state.getUserById(u))
		val rich = RichMessage.incoming(message, state)
		CommandList.list map (_(rich)) find (_.isDefined) map (_.get) foreach send
	}

	def announce(message: String, toUsers: Seq[String] = Nil, channel: String = jobsChannel): Future[Long] =
		send(RichMessage.outgoing(message, toUsers, channel))

	private def send(reply: SlackReply): Future[Long] = send(reply.message)
	private def send(message: RichMessage): Future[Long] =
		client.sendMessage(message.channel, message.formulate(state))

}

object SauronxBot {
	def main(args: Array[String]): Unit = {
		new SauronxBot().loop()
	}
}