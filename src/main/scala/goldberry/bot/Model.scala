package kokellab.goldberry.bot

import kokellab.goldberry.bot.commands.{PhraseParameterization, UserTextSubstitution}
import slack.SlackUtil
import slack.models.{Message, User => SlackUser}
import slack.rtm.RtmState

case class RichMessage(message: String, fromUser: String, mentions: Seq[String], channel: String) {
	import RichMessage._
	def formulate(state: RtmState): String = {
		val x = if (mentions.isEmpty || channel.startsWith("D")) message
		else s"<@${mentions.flatMap(state.getUserIdForName).mkString("> <@")}> $message"
		re.replaceAllIn(x, m => {
			state.getUserIdForName(m.group(1)) map {id =>
				s"<@$id>"
			} getOrElse m.group(0)
		})
	}
}
object RichMessage {
	val re = """<@([^>]+)>""".r
	def incoming(message: Message, state: RtmState): RichMessage = {
		val mentions = SlackUtil.extractMentionedIds(message.text) flatMap (u => state.getUserById(u))
		val text = UserTextSubstitution.replaceAll("""<@(\w+)>""".r.replaceAllIn(message.text, "").toLowerCase.trim).trim
		RichMessage(
			text,
			state.getUserById(message.user).get.name,
			mentions map (_.name),
			message.channel
		)
	}
	def outgoing(message: String, toUsers: Seq[String], channel: String) = RichMessage(
		message, "goldberry", toUsers, channel
	)
}
