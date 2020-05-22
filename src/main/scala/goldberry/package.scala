package kokellab

import java.sql.Timestamp
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import kokellab.valar.core.DateTimeUtils.fromSqlTimestamp

package object goldberry {
	def formatSqlTimestamp(timestamp: Timestamp) = fromSqlTimestamp(timestamp, ZoneId.systemDefault).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}
