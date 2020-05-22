# Goldberry

Goldberry is a simple framework for Slack bots that manage and queue jobs on a server.

A JSON file per command describes the triggering phrases as regex patterns and injects parameters from named capture groups.
The JSON file then specifies the output text and can reference the parameters.
The functionality of a command is implemented in a class extending the trait `Command`.

⚠ Status: Pre-Alpha.

Here's an example command that places a job (identified by a 12-digit hex hash) at the top of the queue.
Admittedly, the parameter syntax is a bit confusing right now.
Basically, `(?:<name>regex)` introduces a parameter and `$job` references it. `@$!name` references reserved parameters, such as the name of the triggering user.

```json
{
  "name": "prioritize-job",
  "triggers": [
	"prioritize (?@<hash>[a-z0-9]{12})",
	"re\\-?prioritize (?@<hash>[a-z0-9]{12})",
	"do (?@<hash>[a-z0-9]{12})",
	"do (?@<hash>[a-z0-9]{12}) now",
	"do (?@<hash>[a-z0-9]{12}) first"
  ],
  "primaryReply": "@$!user, I’ll process $job (“$description”) first.",
  "additionalReplies": []
}
```

The corresponding code is:

```scala
case object PrioritizeCommand extends Command {
	override def path: String = "jobs/prioritize.json"
	override protected def respond(
			trigger: TriggerConfig,
			parameterization: PhraseParameterization,
			message: RichMessage
	): SlackReply = {
		queue.prioritize(parameterization("hash"))
	}
}
```
