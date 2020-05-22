package kokellab.goldberry.bot.commands


object CommandList {
	// always make sure NotUnderstood is at the end
	val list: List[Command] = List(Hello, Locate, Goldberry, Installed, AllCommands, JobCompleted, ReindexCommand, ReimportCommand, NameRunsCommand, NotUnderstood)
}

