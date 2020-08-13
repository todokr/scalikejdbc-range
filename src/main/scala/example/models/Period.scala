package example.models

import scalikejdbc._

case class Period(
  periodId: String,
  periodName: String,
  during: Any) {

  def save()(implicit session: DBSession = Period.autoSession): Period = Period.save(this)(session)

  def destroy()(implicit session: DBSession = Period.autoSession): Int = Period.destroy(this)(session)

}


object Period extends SQLSyntaxSupport[Period] {

  override val schemaName = Some("public")

  override val tableName = "period"

  override val columns = Seq("period_id", "period_name", "during")

  def apply(p: SyntaxProvider[Period])(rs: WrappedResultSet): Period = apply(p.resultName)(rs)
  def apply(p: ResultName[Period])(rs: WrappedResultSet): Period = new Period(
    periodId = rs.get(p.periodId),
    periodName = rs.get(p.periodName),
    during = rs.any(p.during)
  )

  val p = Period.syntax("p")

  override val autoSession = AutoSession

  def find(periodId: String)(implicit session: DBSession = autoSession): Option[Period] = {
    withSQL {
      select.from(Period as p).where.eq(p.periodId, periodId)
    }.map(Period(p.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[Period] = {
    withSQL(select.from(Period as p)).map(Period(p.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls.count).from(Period as p)).map(rs => rs.long(1)).single.apply().get
  }

  def findBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Option[Period] = {
    withSQL {
      select.from(Period as p).where.append(where)
    }.map(Period(p.resultName)).single.apply()
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Period] = {
    withSQL {
      select.from(Period as p).where.append(where)
    }.map(Period(p.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls.count).from(Period as p).where.append(where)
    }.map(_.long(1)).single.apply().get
  }

  def create(
    periodId: String,
    periodName: String,
    during: Any)(implicit session: DBSession = autoSession): Period = {
    withSQL {
      insert.into(Period).namedValues(
        column.periodId -> periodId,
        column.periodName -> periodName,
        (column.during, ParameterBinder(during, (ps, i) => ps.setObject(i, during)))
      )
    }.update.apply()

    Period(
      periodId = periodId,
      periodName = periodName,
      during = during)
  }

  def batchInsert(entities: collection.Seq[Period])(implicit session: DBSession = autoSession): List[Int] = {
    val params: collection.Seq[Seq[(Symbol, Any)]] = entities.map(entity =>
      Seq(
        Symbol("periodId") -> entity.periodId,
        Symbol("periodName") -> entity.periodName,
        Symbol("during") -> entity.during))
    SQL("""insert into period(
      period_id,
      period_name,
      during
    ) values (
      {periodId},
      {periodName},
      {during}
    )""").batchByName(params.toSeq: _*).apply[List]()
  }

  def save(entity: Period)(implicit session: DBSession = autoSession): Period = {
    withSQL {
      update(Period).set(
        column.periodId -> entity.periodId,
        column.periodName -> entity.periodName,
        (column.during, ParameterBinder(entity.during, (ps, i) => ps.setObject(i, entity.during)))
      ).where.eq(column.periodId, entity.periodId)
    }.update.apply()
    entity
  }

  def destroy(entity: Period)(implicit session: DBSession = autoSession): Int = {
    withSQL { delete.from(Period).where.eq(column.periodId, entity.periodId) }.update.apply()
  }

}
