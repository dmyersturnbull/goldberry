package kokellab.goldberry.queue

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import com.typesafe.scalalogging.LazyLogging
import kokellab.valar.core._
import kokellab.valar.importer.{DirectoryLoader, Importer, SubmissionResult}

import scala.collection.JavaConverters._
import scala.util.Try

class SubmissionArchiver(uploadDir: Path, archiveDir: Path, pendingDir: Path) extends LazyLogging {

	private implicit val db = loadDb()
	import kokellab.valar.core.Tables._
	import kokellab.valar.core.Tables.profile.api._

	def insertNow(submission: Path): Try[Path] = Try {
			val dir = DirectoryLoader.load(submission)
			Importer.insert(dir)
			archive(submission, dir)
			submission
		}

	private def archive(submission: Path, dir: SubmissionResult): Unit = {
		val startTime = dir.environment.datetimeStarted
		assert(Files.exists(submission.resolve("frames.7z")))
		Files.delete(submission.resolve("frames"))
		addInfo(submission, dir)
		val run = exec((PlateRuns filter (_.sauronxSubmissionId === dir.submission.id)).result).headOption
		assert(run.isDefined, "The insert finished running but no plate run exists!")
		val targetDir = ImageStore.pathOf(run.get)
		Files.copy(submission, targetDir)
		assert(Files.exists(targetDir), s"The data from $submission archived to $targetDir was not found!")
		Files.delete(submission)
	}

	private def addInfo(submission: Path, dir: SubmissionResult): Unit = {

		val meta = submission.resolve("metadata")
		Files.createDirectories(meta)

		record(meta.resolve("submission.tsv"), Seq("key", "value"), Seq(
			Seq("id", dir.submission.id),
			Seq("idHashHex", dir.submission.idHashHex),
			Seq("projectId", dir.submission.projectId),
			Seq("samePlateSubmissionId", dir.submission.samePlateSubmissionId),
			Seq("userId", dir.submission.userId),
			Seq("personPlatedId", dir.submission.personPlatedId),
			Seq("darkAdaptationTimeSeconds", dir.submission.darkAdaptationTimeSeconds),
			Seq("datetimeFishPlated", dir.submission.datetimeFishPlated),
			Seq("datetimeDosed", dir.submission.datetimeDosed),
			Seq("created", dir.submission.created)
		))

		record(meta.resolve("params.tsv"), Seq("name", "type", "value"),
			exec((SauronxSubmissionParams filter (_.id === dir.submission.id)).result) map { p =>
				Seq(p.name, p.paramType, p.value)
			}
		)
		record(meta.resolve("template_wells.tsv"), Seq("range", "control_type", "n_fish", "variant", "dpf"),
			exec((TemplateWells filter (_.id === dir.submission.id)).result) map { p =>
				Seq(p.wellRangeExpression, p.controlType, p.nFishExpression, p.fishVariantExpression, p.ageDpfExpression)
			}
		)
		record(meta.resolve("template_treatments.tsv"), Seq("range", "compound", "dose"),
			exec((TemplateTreatments filter (_.id === dir.submission.id)).result) map { p =>
				Seq(p.wellRangeExpression, p.orderedCompoundExpression, p.doseExpression)
			}
		)
	}

	private def record(path: Path, header: Seq[String], lines: Iterable[Seq[Any]]): Unit = {
		Files.write(path, (Seq(header.mkString("\t")) ++ (lines map (_.mkString("\t")))).asJava, StandardCharsets.UTF_8)
	}

}
