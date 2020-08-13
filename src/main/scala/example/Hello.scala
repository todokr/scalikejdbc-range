package example

import java.sql.ResultSet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import scala.util.matching.Regex

import example.models.{Period => DBPeriod}
import org.postgresql.util.PGobject
import scalikejdbc._
import scalikejdbc.config._

/** fromは閉空間、toは開空間 */
case class During(from: LocalDate, to: Option[LocalDate]) {
  private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
  override def toString =
    s"[${from.format(formatter)},${to.map(_.format(formatter)).getOrElse("")})"
}

case class Period(
  periodId: String,
  periodName: String,
  during: During
)

object Hello extends App {

  implicit val duringPbf: ParameterBinderFactory[During] = ParameterBinderFactory[During] {
    value => (stmt, idx) =>
      {
        val pgOjb = new PGobject()
        pgOjb.setValue(value.toString)
        pgOjb.setType("daterange")
        stmt.setObject(idx, pgOjb)
      }
  }

  private val LimitedDateRangePattern: Regex = """^\[(.+),(.+)\)$""".r
  private val UnlimitedDateRangePattern: Regex = """^\[(.+),\)$""".r

  implicit val duringTypeBinder: TypeBinder[During] = new TypeBinder[During] {
    override def apply(rs: ResultSet, columnIndex: Int): During = ???
    override def apply(rs: ResultSet, columnLabel: String): During = {
      rs.getString(columnLabel) match {
        case LimitedDateRangePattern(from, to) => During(LocalDate.parse(from), Some(LocalDate.parse(to)))
        case UnlimitedDateRangePattern(from)   => During(LocalDate.parse(from), None)
      }
    }
  }

  val p = DBPeriod.p
  DBs.setupAll()

  DB.localTx { implicit session =>
    sql"create table if not exists period (period_id char(26) primary key, period_name varchar not null, during daterange not null, exclude using gist (during with &&));".execute().apply()
  }

  val period1 = Period(
    periodId = "100000000",
    periodName = "from_and_to",
    during = During(
      from = LocalDate.parse("2030-08-01"),
      to = Some(LocalDate.parse("2031-08-01"))
    )
  )

  val period2 = Period(
    periodId = "200000000",
    periodName = "from_only",
    during = During(
      from = LocalDate.parse("2031-08-01"),
      to = None
    )
  )

  // Periodのinsert
  val col = DBPeriod.column
  val insertSqls = Seq(
    insert
      .into(DBPeriod)
      .namedValues(
        col.periodId -> period1.periodId,
        col.periodName -> period1.periodName,
        col.during -> period1.during
      ),
    insert
      .into(DBPeriod)
      .namedValues(
        col.periodId -> period2.periodId,
        col.periodName -> period2.periodName,
        col.during -> period2.during
      )
  )
  DB.localTx { implicit session =>
    insertSqls.foreach { sql =>
      withSQL(sql).update().apply()
    }
  }

  DB.readOnly { implicit session =>
    val periods = withSQL {
      select
        .from(DBPeriod.as(p))
    }.map { rs =>
      Period(
        periodId = rs.string(p.resultName.periodId),
        periodName = rs.string(p.resultName.periodName),
        during = rs.get[During](p.resultName.during)
      )
    }.list().apply()

    println("*" * 100)
    println("All periods")
    println("*" * 100)
    periods.foreach(println)
    println("*" * 100)
  }

  // 指定されたdurationとの重なりを持つPeriodを取得
  DB.readOnly { implicit session =>
    val testDuring = During(from = LocalDate.parse("2030-01-01"), to = Some(LocalDate.parse("2030-12-31")))

    val overlappingPeriods = withSQL {
      select
        .from(DBPeriod.as(p))
        .where(sqls"${p.during} && ${testDuring.toString}::daterange")
    }.map { rs =>
      Period(
        periodId = rs.string(p.resultName.periodId),
        periodName = rs.string(p.resultName.periodName),
        during = rs.get[During](p.resultName.during)
      )
    }.list().apply()

    println("*" * 100)
    println(s"Periods overlap $testDuring")
    println("*" * 100)
    overlappingPeriods.foreach(println)
    println("*" * 100)
  }

  DB.localTx { implicit session =>
    withSQL {
      delete
        .from(DBPeriod.as(p))
        .where
        .in(p.periodId, Seq(period1.periodId, period2.periodId))
    }.update().apply()
  }

  DBs.closeAll()
}
